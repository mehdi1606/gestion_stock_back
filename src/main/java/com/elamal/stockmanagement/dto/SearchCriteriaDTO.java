package com.elamal.stockmanagement.dto;
import com.elamal.stockmanagement.entity.TypeMouvement;
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
public class SearchCriteriaDTO {
    private String query; // Recherche textuelle générale
    private String categorie;
    private Long fournisseurId;
    private Boolean actif;
    private String dateDebut;
    private String dateFin;
    private TypeMouvement typeMouvement;
    private Boolean stockFaible;
    private Boolean stockCritique;

    // Pagination
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "dateCreation";
    private String sortDirection = "DESC";
}
