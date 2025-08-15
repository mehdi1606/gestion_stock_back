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
public class InventaireDTO {

    @NotNull(message = "L'article est obligatoire")
    private Long articleId;

    @NotNull(message = "La quantité inventaire est obligatoire")
    @Min(value = 0, message = "La quantité inventaire ne peut pas être négative")
    private Integer quantiteInventaire;

    private LocalDateTime dateInventaire = LocalDateTime.now();

    @NotBlank(message = "L'utilisateur est obligatoire")
    private String utilisateur;

    @Size(max = 1000, message = "Les observations ne peuvent pas dépasser 1000 caractères")
    private String observations;

    // Informations calculées (lecture seule)
    private Integer quantiteSysteme;
    private Integer ecart;
    private Boolean ajustementNecessaire;
}
