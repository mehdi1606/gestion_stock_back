package com.elamal.stockmanagement.dto;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {

    private Long id;

    @NotNull(message = "L'article est obligatoire")
    private Long articleId;

    // Informations de l'article (lecture seule)
    private String articleCode;
    private String articleDesignation;
    private String articleCategorie;
    private String articleUnite;
    private Integer articleStockMin;
    private Integer articleStockMax;

    @Min(value = 0, message = "La quantité actuelle ne peut pas être négative")
    private Integer quantiteActuelle = 0;

    @Min(value = 0, message = "La quantité réservée ne peut pas être négative")
    private Integer quantiteReservee = 0;

    private Integer quantiteDisponible = 0; // Calculé automatiquement

    @DecimalMin(value = "0.0", message = "Le prix moyen pondéré doit être positif")
    @Digits(integer = 8, fraction = 2, message = "Format de prix invalide")
    private BigDecimal prixMoyenPondere;

    private BigDecimal valeurStock; // Calculé automatiquement

    private LocalDateTime derniereEntree;
    private LocalDateTime derniereSortie;
    private LocalDateTime dateDernierInventaire;

    @Min(value = 0, message = "La quantité d'inventaire ne peut pas être négative")
    private Integer quantiteInventaire;

    private Integer ecartInventaire; // Calculé automatiquement

    private LocalDateTime dateModification;

    // Statuts du stock (calculés automatiquement)
    private Boolean stockFaible;
    private Boolean stockCritique;
    private Boolean stockExcessif;
    private String statutStock; // "NORMAL", "FAIBLE", "CRITIQUE", "EXCESSIF"

    // Constructeur pour initialisation
    public StockDTO(Long articleId, Integer quantiteInitiale, BigDecimal prixInitial) {
        this.articleId = articleId;
        this.quantiteActuelle = quantiteInitiale;
        this.quantiteReservee = 0;
        this.prixMoyenPondere = prixInitial;
        calculateFields();
    }

    // Méthodes de calcul automatique
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

        // Calcul des statuts de stock
        calculateStockStatus();
    }

    private void calculateStockStatus() {
        if (this.articleStockMin != null && this.quantiteActuelle != null) {
            if (this.quantiteActuelle <= 0) {
                this.stockCritique = true;
                this.stockFaible = false;
                this.stockExcessif = false;
                this.statutStock = "CRITIQUE";
            } else if (this.quantiteActuelle < (this.articleStockMin * 0.5)) {
                this.stockCritique = true;
                this.stockFaible = false;
                this.stockExcessif = false;
                this.statutStock = "CRITIQUE";
            } else if (this.quantiteActuelle <= this.articleStockMin) {
                this.stockCritique = false;
                this.stockFaible = true;
                this.stockExcessif = false;
                this.statutStock = "FAIBLE";
            } else if (this.articleStockMax != null && this.quantiteActuelle > this.articleStockMax) {
                this.stockCritique = false;
                this.stockFaible = false;
                this.stockExcessif = true;
                this.statutStock = "EXCESSIF";
            } else {
                this.stockCritique = false;
                this.stockFaible = false;
                this.stockExcessif = false;
                this.statutStock = "NORMAL";
            }
        } else {
            this.stockCritique = this.quantiteActuelle != null && this.quantiteActuelle <= 0;
            this.stockFaible = false;
            this.stockExcessif = false;
            this.statutStock = this.stockCritique ? "CRITIQUE" : "NORMAL";
        }
    }

    // Méthodes utilitaires
    public boolean needsReorder() {
        return this.stockFaible || this.stockCritique;
    }

    public String getStockStatusColor() {
        switch (this.statutStock) {
            case "CRITIQUE": return "red";
            case "FAIBLE": return "orange";
            case "EXCESSIF": return "blue";
            default: return "green";
        }
    }

    public Double getRotationRate() {
        // Calcul du taux de rotation (à implémenter selon les besoins)
        // Nécessite les données de consommation sur une période
        return 0.0;
    }
}
