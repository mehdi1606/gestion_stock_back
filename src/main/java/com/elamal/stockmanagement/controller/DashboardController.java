package com.elamal.stockmanagement.controller;

import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.dto.DashboardDTO.*;
import com.elamal.stockmanagement.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard & Analytics", description = "API de tableau de bord et d'analyses statistiques")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    // ===============================
    // DASHBOARD PRINCIPAL
    // ===============================

    /**
     * Récupérer toutes les données du dashboard
     */
    @GetMapping
    @Operation(summary = "Dashboard complet", description = "Récupérer toutes les données du tableau de bord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard généré avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur interne lors de la génération")
    })
    public ResponseEntity<ApiResponseDTO<DashboardDTO>> getDashboardData() {

        log.info("Demande de génération du dashboard complet");

        try {
            DashboardDTO dashboard = dashboardService.getDashboardData();

            ApiResponseDTO<DashboardDTO> response = ApiResponseDTO.success(
                    dashboard,
                    "Dashboard généré avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du dashboard", e);

            ApiResponseDTO<DashboardDTO> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du dashboard"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer uniquement les statistiques générales
     */
    @GetMapping("/stats/general")
    @Operation(summary = "Statistiques générales", description = "Récupérer les statistiques générales du système")
    public ResponseEntity<ApiResponseDTO<StatsGeneralesDTO>> getGeneralStatistics() {

        log.debug("Demande des statistiques générales");

        try {
            StatsGeneralesDTO stats = dashboardService.getGeneralStatistics();

            ApiResponseDTO<StatsGeneralesDTO> response = ApiResponseDTO.success(
                    stats,
                    "Statistiques générales récupérées avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques générales", e);

            ApiResponseDTO<StatsGeneralesDTO> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques générales"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // DONNÉES POUR GRAPHIQUES
    // ===============================

    /**
     * Récupérer les données de stock par catégorie
     */
    @GetMapping("/charts/stock-by-category")
    @Operation(summary = "Stock par catégorie", description = "Récupérer la répartition du stock par catégorie")
    public ResponseEntity<ApiResponseDTO<List<ChartDataDTO>>> getStockByCategory() {

        log.debug("Demande des données de stock par catégorie");

        try {
            List<ChartDataDTO> stockData = dashboardService.getStockByCategory();

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.success(
                    stockData,
                    String.format("Données de %d catégories récupérées", stockData.size())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du stock par catégorie", e);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des données de stock par catégorie"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer la tendance des mouvements
     */
    @GetMapping("/charts/movements-trend")
    @Operation(summary = "Tendance mouvements", description = "Récupérer la tendance des mouvements de stock")
    public ResponseEntity<ApiResponseDTO<List<ChartDataDTO>>> getMovementsTrend(
            @Parameter(description = "Nombre de jours") @RequestParam(defaultValue = "7") int days) {

        log.debug("Demande de la tendance des mouvements sur {} jours", days);

        try {
            List<ChartDataDTO> trendData = dashboardService.getMovementsTrend(days);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.success(
                    trendData,
                    String.format("Tendance sur %d jours récupérée", days)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la tendance des mouvements", e);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de la tendance des mouvements"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles les plus consommés
     */
    @GetMapping("/charts/top-consumed-articles")
    @Operation(summary = "Articles les plus consommés", description = "Récupérer le classement des articles les plus consommés")
    public ResponseEntity<ApiResponseDTO<List<ChartDataDTO>>> getTopConsumedArticles(
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Nombre d'articles") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Demande du top {} articles consommés sur {} jours", limit, days);

        try {
            List<ChartDataDTO> topArticles = dashboardService.getTopConsumedArticles(days, limit);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.success(
                    topArticles,
                    String.format("Top %d articles sur %d jours récupéré", limit, days)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des articles les plus consommés", e);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles les plus consommés"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer l'évolution des valeurs de stock
     */
    @GetMapping("/charts/stock-value-evolution")
    @Operation(summary = "Évolution valeur stock", description = "Récupérer l'évolution de la valeur du stock")
    public ResponseEntity<ApiResponseDTO<List<ChartDataDTO>>> getStockValueEvolution(
            @Parameter(description = "Nombre de jours") @RequestParam(defaultValue = "30") int days) {

        log.debug("Demande de l'évolution de la valeur du stock sur {} jours", days);

        try {
            List<ChartDataDTO> evolution = dashboardService.getStockValueEvolution(days);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.success(
                    evolution,
                    String.format("Évolution sur %d jours récupérée", days)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'évolution du stock", e);

            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de l'évolution de la valeur du stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // ALERTES ET ACTIVITÉS
    // ===============================

    /**
     * Récupérer toutes les alertes de stock
     */
    @GetMapping("/alerts")
    @Operation(summary = "Alertes de stock", description = "Récupérer toutes les alertes de stock actives")
    public ResponseEntity<ApiResponseDTO<List<AlerteStockDTO>>> getStockAlerts() {

        log.debug("Demande des alertes de stock");

        try {
            List<AlerteStockDTO> alerts = dashboardService.getStockAlerts();

            ApiResponseDTO<List<AlerteStockDTO>> response = ApiResponseDTO.success(
                    alerts,
                    String.format("%d alertes actives trouvées", alerts.size())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des alertes", e);

            ApiResponseDTO<List<AlerteStockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des alertes de stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les activités récentes
     */
    @GetMapping("/activities/recent")
    @Operation(summary = "Activités récentes", description = "Récupérer les dernières activités du système")
    public ResponseEntity<ApiResponseDTO<List<ActiviteRecenteDTO>>> getRecentActivities(
            @Parameter(description = "Nombre d'activités") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Demande des {} dernières activités", limit);

        try {
            List<ActiviteRecenteDTO> activities = dashboardService.getRecentActivities(limit);

            ApiResponseDTO<List<ActiviteRecenteDTO>> response = ApiResponseDTO.success(
                    activities,
                    String.format("%d activités récentes récupérées", activities.size())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des activités récentes", e);

            ApiResponseDTO<List<ActiviteRecenteDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des activités récentes"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // STATISTIQUES SPÉCIALISÉES
    // ===============================

    /**
     * Récupérer les statistiques des mouvements d'aujourd'hui
     */
    @GetMapping("/stats/today-movements")
    @Operation(summary = "Mouvements aujourd'hui", description = "Récupérer les statistiques des mouvements d'aujourd'hui")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getTodayMovementStats() {

        log.debug("Demande des statistiques des mouvements d'aujourd'hui");

        try {
            Map<String, Object> stats = dashboardService.getTodayMovementStats();

            ApiResponseDTO<Map<String, Object>> response = ApiResponseDTO.success(
                    stats,
                    "Statistiques d'aujourd'hui récupérées avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stats d'aujourd'hui", e);

            ApiResponseDTO<Map<String, Object>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques d'aujourd'hui"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les performances des fournisseurs
     */
    @GetMapping("/stats/supplier-performance")
    @Operation(summary = "Performance fournisseurs", description = "Récupérer les performances des fournisseurs")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getSupplierPerformance(
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "365") int days,
            @Parameter(description = "Nombre de fournisseurs") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Demande des performances des {} top fournisseurs sur {} jours", limit, days);

        try {
            List<Map<String, Object>> performance = dashboardService.getSupplierPerformance(days, limit);

            ApiResponseDTO<List<Map<String, Object>>> response = ApiResponseDTO.success(
                    performance,
                    String.format("Performance de %d fournisseurs sur %d jours", limit, days)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des performances fournisseurs", e);

            ApiResponseDTO<List<Map<String, Object>>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des performances des fournisseurs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les indicateurs clés de performance (KPI)
     */
    @GetMapping("/kpis")
    @Operation(summary = "Indicateurs KPI", description = "Récupérer tous les indicateurs clés de performance")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getKPIs() {

        log.debug("Demande des indicateurs clés de performance");

        try {
            Map<String, Object> kpis = dashboardService.getKPIs();

            ApiResponseDTO<Map<String, Object>> response = ApiResponseDTO.success(
                    kpis,
                    "KPIs récupérés avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des KPIs", e);

            ApiResponseDTO<Map<String, Object>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des indicateurs KPI"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // RAPPORTS ET EXPORTS
    // ===============================

    /**
     * Générer un rapport de synthèse
     */
    @GetMapping("/reports/summary")
    @Operation(summary = "Rapport de synthèse", description = "Générer un rapport de synthèse complet")
    public ResponseEntity<ApiResponseDTO<Object>> getSummaryReport() {

        log.info("Génération du rapport de synthèse");

        try {
            // Compilation des données principales
            java.util.Map<String, Object> summaryReport = new java.util.HashMap<>();

            summaryReport.put("statistiquesGenerales", dashboardService.getGeneralStatistics());
            summaryReport.put("kpis", dashboardService.getKPIs());
            summaryReport.put("alertes", dashboardService.getStockAlerts());
            summaryReport.put("mouvementsAujourdhui", dashboardService.getTodayMovementStats());
            summaryReport.put("performanceFournisseurs", dashboardService.getSupplierPerformance(30, 5));

            summaryReport.put("dateGeneration", LocalDateTime.now());
            summaryReport.put("typeRapport", "SYNTHESE_COMPLETE");

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    summaryReport,
                    "Rapport de synthèse généré avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport de synthèse", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport de synthèse"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Générer un rapport d'alertes
     */
    @GetMapping("/reports/alerts")
    @Operation(summary = "Rapport d'alertes", description = "Générer un rapport détaillé des alertes")
    public ResponseEntity<ApiResponseDTO<Object>> getAlertsReport() {

        log.info("Génération du rapport d'alertes");

        try {
            List<AlerteStockDTO> alertes = dashboardService.getStockAlerts();

            // Analyse des alertes
            java.util.Map<String, Object> alertsReport = new java.util.HashMap<>();

            // Comptage par type d'alerte
            java.util.Map<String, Long> alertsByType = alertes.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            AlerteStockDTO::getTypeAlerte,
                            java.util.stream.Collectors.counting()
                    ));

            // Comptage par priorité
            java.util.Map<String, Long> alertsByPriority = alertes.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            AlerteStockDTO::getPriorite,
                            java.util.stream.Collectors.counting()
                    ));

            alertsReport.put("nombreTotalAlertes", alertes.size());
            alertsReport.put("repartitionParType", alertsByType);
            alertsReport.put("repartitionParPriorite", alertsByPriority);
            alertsReport.put("detailAlertes", alertes);
            alertsReport.put("dateGeneration", LocalDateTime.now());
            alertsReport.put("typeRapport", "ALERTES_DETAILLEES");

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    alertsReport,
                    String.format("Rapport d'alertes généré - %d alertes analysées", alertes.size())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport d'alertes", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport d'alertes"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Générer un rapport de performance
     */
    @GetMapping("/reports/performance")
    @Operation(summary = "Rapport de performance", description = "Générer un rapport de performance du système")
    public ResponseEntity<ApiResponseDTO<Object>> getPerformanceReport(
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "30") int days) {

        log.info("Génération du rapport de performance sur {} jours", days);

        try {
            java.util.Map<String, Object> performanceReport = new java.util.HashMap<>();

            // KPIs de performance
            Map<String, Object> kpis = dashboardService.getKPIs();

            // Données de mouvement sur la période
            Map<String, Object> todayStats = dashboardService.getTodayMovementStats();

            // Performance des fournisseurs
            List<Map<String, Object>> supplierPerf = dashboardService.getSupplierPerformance(days, 10);

            performanceReport.put("periode", days + " jours");
            performanceReport.put("kpisGlobaux", kpis);
            performanceReport.put("statistiquesAujourdhui", todayStats);
            performanceReport.put("performanceFournisseurs", supplierPerf);
            performanceReport.put("dateGeneration", LocalDateTime.now());
            performanceReport.put("typeRapport", "PERFORMANCE_SYSTEME");

            // Calcul d'un score de performance global (exemple)
            int scorePerformance = calculateGlobalPerformanceScore(kpis, todayStats);
            performanceReport.put("scorePerformanceGlobal", scorePerformance);

            String niveauPerformance;
            if (scorePerformance >= 85) {
                niveauPerformance = "EXCELLENT";
            } else if (scorePerformance >= 70) {
                niveauPerformance = "BON";
            } else if (scorePerformance >= 50) {
                niveauPerformance = "MOYEN";
            } else {
                niveauPerformance = "CRITIQUE";
            }
            performanceReport.put("niveauPerformance", niveauPerformance);

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    performanceReport,
                    String.format("Rapport de performance généré pour %d jours", days)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport de performance", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport de performance"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // ENDPOINTS DE RAFRAÎCHISSEMENT
    // ===============================

    /**
     * Rafraîchir les données du dashboard
     */
    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir dashboard", description = "Forcer le rafraîchissement des données du dashboard")
    public ResponseEntity<ApiResponseDTO<String>> refreshDashboard() {

        log.info("Demande de rafraîchissement du dashboard");

        try {
            // Dans une implémentation réelle, vous pourriez vider les caches ici
            // ou déclencher une recalculation des métriques

            String message = "Dashboard rafraîchi avec succès à " + LocalDateTime.now();

            ApiResponseDTO<String> response = ApiResponseDTO.success(
                    message,
                    "Rafraîchissement effectué"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du rafraîchissement du dashboard", e);

            ApiResponseDTO<String> response = ApiResponseDTO.error(
                    "Erreur lors du rafraîchissement du dashboard"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtenir les métadonnées du dashboard
     */
    @GetMapping("/metadata")
    @Operation(summary = "Métadonnées dashboard", description = "Récupérer les métadonnées et informations du dashboard")
    public ResponseEntity<ApiResponseDTO<Object>> getDashboardMetadata() {

        log.debug("Demande des métadonnées du dashboard");

        try {
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();

            metadata.put("version", "1.0.0");
            metadata.put("lastUpdate", LocalDateTime.now());
            metadata.put("dataSourceType", "REAL_TIME");
            metadata.put("refreshInterval", "5 minutes");
            metadata.put("availableCharts", java.util.List.of(
                    "stockParCategorie",
                    "mouvementsTendance",
                    "topArticlesConsommes",
                    "evolutionValeurStock"
            ));
            metadata.put("availableKPIs", java.util.List.of(
                    "tauxRotationStock",
                    "tauxCouvertureStock",
                    "tauxRuptureStock",
                    "valeurMoyenneStock"
            ));
            metadata.put("alertTypes", java.util.List.of(
                    "CRITIQUE", "FAIBLE", "EXCESSIF"
            ));

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    metadata,
                    "Métadonnées du dashboard récupérées"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des métadonnées", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des métadonnées du dashboard"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // UTILITAIRES SYSTÈME
    // ===============================

    /**
     * Ping pour vérifier la disponibilité du service
     */
    @GetMapping("/ping")
    @Operation(summary = "Ping service", description = "Vérifier la disponibilité du service dashboard")
    public ResponseEntity<ApiResponseDTO<String>> ping() {

        log.debug("Ping du service dashboard");

        try {
            String message = "Service dashboard opérationnel - " + LocalDateTime.now();

            ApiResponseDTO<String> response = ApiResponseDTO.success(message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du ping", e);

            ApiResponseDTO<String> response = ApiResponseDTO.error("Service indisponible");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtenir la version de l'API
     */
    @GetMapping("/version")
    @Operation(summary = "Version API", description = "Obtenir la version de l'API dashboard")
    public ResponseEntity<ApiResponseDTO<Object>> getApiVersion() {

        log.debug("Demande de version de l'API dashboard");

        try {
            java.util.Map<String, Object> versionInfo = new java.util.HashMap<>();
            versionInfo.put("version", "1.0.0");
            versionInfo.put("service", "DashboardController");
            versionInfo.put("lastUpdate", "2024-12-19");
            versionInfo.put("author", "Stock Management System");
            versionInfo.put("description", "API complète de dashboard et analytics");
            versionInfo.put("features", java.util.List.of(
                    "Dashboard temps réel",
                    "Graphiques interactifs",
                    "Alertes automatiques",
                    "KPIs avancés",
                    "Rapports personnalisés"
            ));

            ApiResponseDTO<Object> response = ApiResponseDTO.success(versionInfo);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la version", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de la version"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // MÉTHODES PRIVÉES UTILITAIRES
    // ===============================

    /**
     * Calculer un score de performance global
     */
    private int calculateGlobalPerformanceScore(Map<String, Object> kpis, Map<String, Object> todayStats) {
        // Implémentation simplifiée d'un score de performance
        // Dans un vrai système, vous utiliseriez des métriques plus sophistiquées

        int score = 50; // Score de base

        // Bonus pour l'activité du jour
        Object totalMovements = todayStats.get("totalMouvements");
        if (totalMovements != null && ((Number) totalMovements).intValue() > 10) {
            score += 10;
        }

        // Bonus pour la rotation du stock
        Object rotationRate = kpis.get("tauxRotationStock");
        if (rotationRate != null && ((Number) rotationRate).doubleValue() > 0.5) {
            score += 15;
        }

        // Malus pour le taux de rupture
        Object stockoutRate = kpis.get("tauxRuptureStock");
        if (stockoutRate != null && ((Number) stockoutRate).doubleValue() > 10) {
            score -= 20;
        }

        // Assurer que le score reste dans la fourchette 0-100
        return Math.max(0, Math.min(100, score));
    }
}
