package com.elamal.stockmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    // Statistiques générales
    private StatsGeneralesDTO statsGenerales;

    // Données pour les graphiques
    private List<ChartDataDTO> stockParCategorie;
    private List<ChartDataDTO> mouvementsTendance;
    private List<ChartDataDTO> topArticlesConsommes;

    // Alertes
    private List<AlerteStockDTO> alertes;

    // Activités récentes
    private List<ActiviteRecenteDTO> activitesRecentes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsGeneralesDTO {
        private Integer totalArticles;
        private Integer totalArticlesActifs;
        private BigDecimal valeurTotaleStock;
        private Integer mouvementsAujourdhui;
        private Integer entreesAujourdhui;
        private Integer sortiesAujourdhui;
        private Integer articlesCritiques;
        private Integer articlesFaibles;
        private Integer totalFournisseurs;
        private Integer fournisseursActifs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataDTO {
        private String label;
        private Object value; // Peut être Integer, BigDecimal, etc.
        private Map<String, Object> metadata; // Données supplémentaires
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlerteStockDTO {
        private Long articleId;
        private String articleNom;
        private String typeAlerte; // CRITIQUE, FAIBLE, EXCESSIF
        private Integer quantiteActuelle;
        private Integer seuilReference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiviteRecenteDTO {
        private Long id;
        private String type;
        private String description;
        private LocalDateTime dateActivite;
        private String utilisateur;
    }
}
