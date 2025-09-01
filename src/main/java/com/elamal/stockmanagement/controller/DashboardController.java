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
     * Récupérer uniquement les statistiques générales - CRITICAL FOR FRONTEND
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
    // DONNÉES POUR GRAPHIQUES - REQUIRED BY FRONTEND
    // ===============================

    /**
     * Récupérer les données de stock par catégorie - REQUIRED
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
     * Récupérer la tendance des mouvements - REQUIRED
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
     * Récupérer les top articles consommés - REQUIRED
     */
    @GetMapping("/charts/top-consumed-articles")
    @Operation(summary = "Top articles consommés", description = "Récupérer les articles les plus consommés")
    public ResponseEntity<ApiResponseDTO<List<ChartDataDTO>>> getTopConsumedArticles(
            @Parameter(description = "Nombre de jours") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Nombre d'articles") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Demande des {} top articles consommés sur {} jours", limit, days);

        try {
            List<ChartDataDTO> topArticles = dashboardService.getTopConsumedArticles(days, limit);
            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.success(
                    topArticles,
                    String.format("Top %d articles sur %d jours récupéré", limit, days)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des top articles consommés", e);
            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des top articles consommés"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer l'évolution de la valeur du stock - REQUIRED
     */
    @GetMapping("/charts/stock-value-evolution")
    @Operation(summary = "Évolution valeur stock", description = "Récupérer l'évolution de la valeur du stock")
    public ResponseEntity<ApiResponseDTO<List<ChartDataDTO>>> getStockValueEvolution(
            @Parameter(description = "Nombre de jours") @RequestParam(defaultValue = "30") int days) {

        log.debug("Demande de l'évolution de la valeur du stock sur {} jours", days);

        try {
            List<ChartDataDTO> evolutionData = dashboardService.getStockValueEvolution(days);
            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.success(
                    evolutionData,
                    String.format("Évolution sur %d jours récupérée", days)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'évolution de la valeur", e);
            ApiResponseDTO<List<ChartDataDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de l'évolution de la valeur du stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // STATISTIQUES SPÉCIALISÉES - REQUIRED BY FRONTEND
    // ===============================

    /**
     * Récupérer les statistiques des mouvements d'aujourd'hui - REQUIRED
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
     * Récupérer les performances des fournisseurs - REQUIRED
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
                    String.format("Performance de %d fournisseurs récupérée", performance.size())
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

    // ===============================
    // UTILITAIRES SYSTÈME - HEALTH CHECK ENDPOINT
    // ===============================

    /**
     * Health check pour le frontend - CRITICAL
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Vérifier la santé de l'API")
    public ResponseEntity<ApiResponseDTO<String>> healthCheck() {
        log.debug("Health check du service dashboard");

        try {
            String message = "API Dashboard opérationnelle - " + LocalDateTime.now();
            ApiResponseDTO<String> response = ApiResponseDTO.success(message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du health check", e);
            ApiResponseDTO<String> response = ApiResponseDTO.error("Service indisponible");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    versionInfo,
                    "Version de l'API récupérée"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la version", e);
            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de la version"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
