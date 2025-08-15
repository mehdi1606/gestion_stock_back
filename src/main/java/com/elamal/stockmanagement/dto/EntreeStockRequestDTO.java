package com.elamal.stockmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// DTO pour les requêtes d'entrée de stock
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntreeStockRequestDTO {

    @NotNull(message = "L'article est obligatoire")
    private Long articleId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être positive")
    private Integer quantite;

    @NotNull(message = "Le prix unitaire est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix unitaire doit être positif")
    private BigDecimal prixUnitaire;

    @NotNull(message = "Le fournisseur est obligatoire")
    private Long fournisseurId;

    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String motif;

    @Size(max = 100, message = "Le numéro de bon ne peut pas dépasser 100 caractères")
    private String numeroBon;

    @Size(max = 100, message = "Le numéro de facture ne peut pas dépasser 100 caractères")
    private String numeroFacture;

    private LocalDateTime dateMouvement = LocalDateTime.now();

    @Size(max = 1000, message = "Les observations ne peuvent pas dépasser 1000 caractères")
    private String observations;

    @NotBlank(message = "L'utilisateur est obligatoire")
    private String utilisateur;
}
