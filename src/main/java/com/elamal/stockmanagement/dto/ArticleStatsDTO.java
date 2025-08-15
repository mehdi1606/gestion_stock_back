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
public class ArticleStatsDTO {
    private Long articleId;
    private String articleCode;
    private String articleDesignation;

    // Statistiques de mouvement
    private Integer totalEntrees;
    private Integer totalSorties;
    private BigDecimal valeurTotaleEntrees;
    private BigDecimal valeurTotaleSorties;

    // Statistiques de période
    private Integer entreesThisMois;
    private Integer sortiesThisMois;
    private Integer entreesLastMois;
    private Integer sortiesLastMois;

    // Données de tendance
    private Double tauxRotation;
    private Integer joursStockMoyen;
    private LocalDateTime derniereEntree;
    private LocalDateTime derniereSortie;
}
