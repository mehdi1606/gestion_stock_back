package com.elamal.stockmanagement.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false, unique = true)
    private Article article;

    @Column(name = "quantite_actuelle", nullable = false)
    private Integer quantiteActuelle = 0;

    @Column(name = "quantite_reservee")
    private Integer quantiteReservee = 0; // Stock réservé/en attente

    @Column(name = "quantite_disponible")
    private Integer quantiteDisponible = 0; // quantiteActuelle - quantiteReservee

    @Column(name = "prix_moyen_pondere", precision = 10, scale = 2)
    private BigDecimal prixMoyenPondere; // Prix moyen pondéré (PMP)

    @Column(name = "valeur_stock", precision = 12, scale = 2)
    private BigDecimal valeurStock; // quantiteActuelle * prixMoyenPondere

    @Column(name = "derniere_entree")
    private LocalDateTime derniereEntree;

    @Column(name = "derniere_sortie")
    private LocalDateTime derniereSortie;

    @Column(name = "date_dernier_inventaire")
    private LocalDateTime dateDernierInventaire;

    @Column(name = "quantite_inventaire")
    private Integer quantiteInventaire; // Dernière quantité d'inventaire

    @Column(name = "ecart_inventaire")
    private Integer ecartInventaire; // Écart par rapport à l'inventaire

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // Méthodes de calcul automatique
    @PrePersist
    @PreUpdate
    public void calculateFields() {
        // Calcul de la quantité disponible
        if (this.quantiteActuelle != null && this.quantiteReservee != null) {
            this.quantiteDisponible = this.quantiteActuelle - this.quantiteReservee;
        }

        // Calcul de la valeur du stock
        if (this.quantiteActuelle != null && this.prixMoyenPondere != null) {
            this.valeurStock = this.prixMoyenPondere.multiply(BigDecimal.valueOf(this.quantiteActuelle));
        }

        // Calcul de l'écart d'inventaire
        if (this.quantiteActuelle != null && this.quantiteInventaire != null) {
            this.ecartInventaire = this.quantiteActuelle - this.quantiteInventaire;
        }
    }

    // Méthodes utilitaires
    public boolean isStockFaible() {
        if (this.article != null && this.article.getStockMin() != null) {
            return this.quantiteActuelle <= this.article.getStockMin();
        }
        return false;
    }

    public boolean isStockCritique() {
        if (this.article != null && this.article.getStockMin() != null) {
            return this.quantiteActuelle < (this.article.getStockMin() * 0.5);
        }
        return this.quantiteActuelle <= 0;
    }

    public boolean isStockExcessif() {
        if (this.article != null && this.article.getStockMax() != null) {
            return this.quantiteActuelle > this.article.getStockMax();
        }
        return false;
    }

    // Constructeur personnalisé
    public Stock(Article article, Integer quantiteInitiale, BigDecimal prixInitial) {
        this.article = article;
        this.quantiteActuelle = quantiteInitiale;
        this.quantiteReservee = 0;
        this.prixMoyenPondere = prixInitial;
    }

    // Méthode pour mettre à jour le stock après un mouvement
    public void updateAfterMovement(Integer quantiteMouvement, BigDecimal prixUnitaire, boolean isEntree) {
        if (isEntree) {
            // Calcul du nouveau prix moyen pondéré pour les entrées
            if (this.quantiteActuelle > 0 && this.prixMoyenPondere != null) {
                BigDecimal valeurActuelle = this.prixMoyenPondere.multiply(BigDecimal.valueOf(this.quantiteActuelle));
                BigDecimal valeurNouvelleEntree = prixUnitaire.multiply(BigDecimal.valueOf(quantiteMouvement));
                BigDecimal valeurTotale = valeurActuelle.add(valeurNouvelleEntree);
                Integer quantiteTotale = this.quantiteActuelle + quantiteMouvement;
                this.prixMoyenPondere = valeurTotale.divide(BigDecimal.valueOf(quantiteTotale), 2, BigDecimal.ROUND_HALF_UP);
            } else {
                this.prixMoyenPondere = prixUnitaire;
            }

            this.quantiteActuelle += quantiteMouvement;
            this.derniereEntree = LocalDateTime.now();
        } else {
            // Sortie de stock
            this.quantiteActuelle -= quantiteMouvement;
            this.derniereSortie = LocalDateTime.now();
        }

        // Recalcul automatique des champs
        calculateFields();
    }
}
