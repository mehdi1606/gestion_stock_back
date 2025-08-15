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
public class ArticleDTO {

    private Long id;

    @NotBlank(message = "Le code article est obligatoire")
    @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 200, message = "La désignation ne peut pas dépasser 200 caractères")
    private String designation;

    @Size(max = 1000, message = "La description ne peut pas dépasser 1000 caractères")
    private String description;

    @Size(max = 100, message = "La catégorie ne peut pas dépasser 100 caractères")
    private String categorie;

    @Size(max = 20, message = "L'unité ne peut pas dépasser 20 caractères")
    private String unite;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix unitaire doit être positif")
    @Digits(integer = 8, fraction = 2, message = "Format de prix invalide")
    private BigDecimal prixUnitaire;

    @Min(value = 0, message = "Le stock minimum ne peut pas être négatif")
    private Integer stockMin;

    @Min(value = 0, message = "Le stock maximum ne peut pas être négatif")
    private Integer stockMax;

    private Boolean actif = true;

    // Informations du fournisseur principal
    private Long fournisseurPrincipalId;
    private String fournisseurPrincipalNom;

    // Informations du stock actuel (lecture seule)
    private Integer quantiteActuelle;
    private BigDecimal valeurStock;
    private LocalDateTime derniereEntree;
    private LocalDateTime derniereSortie;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Constructeur pour création
    public ArticleDTO(String code, String designation, String categorie, String unite, BigDecimal prixUnitaire) {
        this.code = code;
        this.designation = designation;
        this.categorie = categorie;
        this.unite = unite;
        this.prixUnitaire = prixUnitaire;
        this.actif = true;
    }

    // Méthodes utilitaires
    public boolean isStockFaible() {
        if (this.stockMin != null && this.quantiteActuelle != null) {
            return this.quantiteActuelle <= this.stockMin;
        }
        return false;
    }

    public boolean isStockCritique() {
        if (this.stockMin != null && this.quantiteActuelle != null) {
            return this.quantiteActuelle < (this.stockMin * 0.5);
        }
        return this.quantiteActuelle != null && this.quantiteActuelle <= 0;
    }
}
