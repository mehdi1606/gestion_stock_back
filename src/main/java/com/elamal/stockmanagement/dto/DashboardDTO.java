package com.elamal.stockmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        private String color;
        private Map<String, Object> metadata; // Données supplémentaires

        public ChartDataDTO(String label, Object value) {
            this.label = label;
            this.value = value;
        }

        public ChartDataDTO(String label, Object value, String color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlerteStockDTO {
        private Long articleId;
        private String articleCode;
        private String articleDesignation;
        private String typeAlerte; // "CRITIQUE", "FAIBLE", "EXCESSIF"
        private Integer quantiteActuelle;
        private Integer stockMin;
        private Integer stockMax;
        private String message;
        private String priorite; // "HAUTE", "MOYENNE", "BASSE"
        private LocalDate dateAlerte;

        public AlerteStockDTO(Long articleId, String articleCode, String articleDesignation,
                              String typeAlerte, Integer quantiteActuelle, Integer stockMin) {
            this.articleId = articleId;
            this.articleCode = articleCode;
            this.articleDesignation = articleDesignation;
            this.typeAlerte = typeAlerte;
            this.quantiteActuelle = quantiteActuelle;
            this.stockMin = stockMin;
            this.dateAlerte = LocalDate.now();

            // Génération automatique du message et de la priorité
            generateMessageAndPriority();
        }

        private void generateMessageAndPriority() {
            switch (this.typeAlerte) {
                case "CRITIQUE":
                    this.message = "Stock critique : " + this.quantiteActuelle + " unités restantes";
                    this.priorite = "HAUTE";
                    break;
                case "FAIBLE":
                    this.message = "Stock faible : " + this.quantiteActuelle + " unités (min: " + this.stockMin + ")";
                    this.priorite = "MOYENNE";
                    break;
                case "EXCESSIF":
                    this.message = "Stock excessif : " + this.quantiteActuelle + " unités (max: " + this.stockMax + ")";
                    this.priorite = "BASSE";
                    break;
                default:
                    this.message = "Vérification requise pour l'article";
                    this.priorite = "BASSE";
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiviteRecenteDTO {
        private String type; // "ENTREE", "SORTIE", "CREATION_ARTICLE", etc.
        private String description;
        private String utilisateur;
        private LocalDate date;
        private String icon;
        private String color;

        public ActiviteRecenteDTO(String type, String description, String utilisateur) {
            this.type = type;
            this.description = description;
            this.utilisateur = utilisateur;
            this.date = LocalDate.now();

            // Attribution automatique des icônes et couleurs
            setIconAndColor();
        }

        private void setIconAndColor() {
            switch (this.type) {
                case "ENTREE":
                    this.icon = "arrow-up";
                    this.color = "green";
                    break;
                case "SORTIE":
                    this.icon = "arrow-down";
                    this.color = "red";
                    break;
                case "CREATION_ARTICLE":
                    this.icon = "plus";
                    this.color = "blue";
                    break;
                case "MODIFICATION_ARTICLE":
                    this.icon = "edit";
                    this.color = "orange";
                    break;
                case "CREATION_FOURNISSEUR":
                    this.icon = "user-plus";
                    this.color = "purple";
                    break;
                default:
                    this.icon = "info";
                    this.color = "gray";
            }
        }
    }

    // Constructeur pour initialisation avec valeurs par défaut
    public DashboardDTO(StatsGeneralesDTO statsGenerales) {
        this.statsGenerales = statsGenerales;
        this.stockParCategorie = List.of();
        this.mouvementsTendance = List.of();
        this.topArticlesConsommes = List.of();
        this.alertes = List.of();
        this.activitesRecentes = List.of();
    }
}
