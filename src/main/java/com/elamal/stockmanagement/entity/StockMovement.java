package com.elamal.stockmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 20)
    private TypeMouvement typeMouvement;

    @Column(name = "quantite", nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire", precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "valeur_totale", precision = 12, scale = 2)
    private BigDecimal valeurTotale;

    @Column(name = "motif", length = 500)
    private String motif;

    @Column(name = "numero_bon", length = 100)
    private String numeroBon; // N° bon de livraison ou de sortie

    @Column(name = "numero_facture", length = 100)
    private String numeroFacture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur; // Pour les entrées

    @Column(name = "client", length = 200)
    private String client; // Pour les sorties

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;

    @Column(name = "utilisateur", length = 100)
    private String utilisateur; // Qui a effectué le mouvement

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "stock_avant")
    private Integer stockAvant; // Stock avant le mouvement

    @Column(name = "stock_apres")
    private Integer stockApres; // Stock après le mouvement

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Méthode pour calculer la valeur totale automatiquement
    @PrePersist
    @PreUpdate
    public void calculateValeurTotale() {
        if (this.quantite != null && this.prixUnitaire != null) {
            this.valeurTotale = this.prixUnitaire.multiply(BigDecimal.valueOf(this.quantite));
        }
    }

    // Constructeur pour entrée de stock
    public StockMovement(Article article, TypeMouvement type, Integer quantite,
                         BigDecimal prixUnitaire, Fournisseur fournisseur,
                         String motif, String utilisateur) {
        this.article = article;
        this.typeMouvement = type;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.fournisseur = fournisseur;
        this.motif = motif;
        this.utilisateur = utilisateur;
        this.dateMouvement = LocalDateTime.now();
    }

    // Constructeur pour sortie de stock
    public StockMovement(Article article, TypeMouvement type, Integer quantite,
                         String client, String motif, String utilisateur) {
        this.article = article;
        this.typeMouvement = type;
        this.quantite = quantite;
        this.client = client;
        this.motif = motif;
        this.utilisateur = utilisateur;
        this.dateMouvement = LocalDateTime.now();
    }
}
