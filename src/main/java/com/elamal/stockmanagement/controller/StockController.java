package com.elamal.stockmanagement.controller;

import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Stocks", description = "API de gestion et consultation des stocks")
@CrossOrigin(origins = "*")
public class StockController {

    private final StockService stockService;

    // ===============================
    // CONSULTATION DU STOCK
    // ===============================

    /**
     * Récupérer le stock d'un article par ID
     */
    @GetMapping("/article/{articleId}")
    @Operation(summary = "Stock par article", description = "Récupérer le stock d'un article spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock trouvé"),
            @ApiResponse(responseCode = "404", description = "Stock non trouvé pour cet article")
    })
    public ResponseEntity<ApiResponseDTO<StockDTO>> getStockByArticleId(
            @Parameter(description = "ID de l'article") @PathVariable Long articleId) {

        log.debug("Demande de consultation du stock pour l'article ID: {}", articleId);

        try {
            StockDTO stock = stockService.getStockByArticleId(articleId);

            ApiResponseDTO<StockDTO> response = ApiResponseDTO.success(stock);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Stock non trouvé pour l'article ID: {}", articleId);

            ApiResponseDTO<StockDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Récupérer tous les stocks avec pagination
     */
    @GetMapping
    @Operation(summary = "Lister tous les stocks", description = "Récupérer tous les stocks avec pagination et tri")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockDTO>>> getAllStocks(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "ASC") String sortDirection) {

        log.debug("Demande de liste de stocks - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDirection);

        try {
            PagedResponseDTO<StockDTO> stocks = stockService.getAllStocks(page, size, sortBy, sortDirection);

            ApiResponseDTO<PagedResponseDTO<StockDTO>> response = ApiResponseDTO.success(stocks);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks", e);

            ApiResponseDTO<PagedResponseDTO<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rechercher des stocks avec critères
     */
    @PostMapping("/search")
    @Operation(summary = "Recherche avancée", description = "Rechercher des stocks avec critères avancés")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockDTO>>> searchStocks(
            @Valid @RequestBody SearchCriteriaDTO criteria) {

        log.debug("Demande de recherche de stocks avec critères");

        try {
            PagedResponseDTO<StockDTO> stocks = stockService.searchStocks(criteria);

            ApiResponseDTO<PagedResponseDTO<StockDTO>> response = ApiResponseDTO.success(stocks);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche de stocks", e);

            ApiResponseDTO<PagedResponseDTO<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche de stocks"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche textuelle dans les stocks
     */
    @GetMapping("/search")
    @Operation(summary = "Recherche textuelle", description = "Recherche textuelle dans les stocks")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockDTO>>> searchStocksByText(
            @Parameter(description = "Terme de recherche") @RequestParam String q,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size) {

        log.debug("Recherche textuelle dans les stocks: {}", q);

        try {
            PagedResponseDTO<StockDTO> stocks = stockService.searchStocksByText(q, page, size);

            ApiResponseDTO<PagedResponseDTO<StockDTO>> response = ApiResponseDTO.success(stocks);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche textuelle", e);

            ApiResponseDTO<PagedResponseDTO<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche textuelle"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // ALERTES ET NIVEAUX DE STOCK
    // ===============================

    /**
     * Récupérer les stocks critiques
     */
    @GetMapping("/alerts/critical")
    @Operation(summary = "Stocks critiques", description = "Récupérer tous les stocks en niveau critique")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getCriticalStocks() {

        log.debug("Demande des stocks critiques");

        try {
            List<StockDTO> stocks = stockService.getCriticalStocks();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks critiques trouvés", stocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks critiques", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks critiques"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks faibles
     */
    @GetMapping("/alerts/low")
    @Operation(summary = "Stocks faibles", description = "Récupérer tous les stocks en niveau faible")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getLowStocks() {

        log.debug("Demande des stocks faibles");

        try {
            List<StockDTO> stocks = stockService.getLowStocks();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks faibles trouvés", stocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks faibles", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks faibles"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks vides
     */
    @GetMapping("/alerts/empty")
    @Operation(summary = "Stocks vides", description = "Récupérer tous les stocks vides")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getEmptyStocks() {

        log.debug("Demande des stocks vides");

        try {
            List<StockDTO> stocks = stockService.getEmptyStocks();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks vides trouvés", stocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks vides", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks vides"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks excessifs
     */
    @GetMapping("/alerts/excessive")
    @Operation(summary = "Stocks excessifs", description = "Récupérer tous les stocks en niveau excessif")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getExcessiveStocks() {

        log.debug("Demande des stocks excessifs");

        try {
            List<StockDTO> stocks = stockService.getExcessiveStocks();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks excessifs trouvés", stocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks excessifs", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks excessifs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks nécessitant un réapprovisionnement
     */
    @GetMapping("/alerts/reorder")
    @Operation(summary = "Stocks à réapprovisionner", description = "Récupérer les stocks nécessitant un réapprovisionnement")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksNeedingReorder() {

        log.debug("Demande des stocks nécessitant un réapprovisionnement");

        try {
            List<StockDTO> stocks = stockService.getStocksNeedingReorder();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks nécessitent un réapprovisionnement", stocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks à réapprovisionner", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks à réapprovisionner"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks nécessitant attention
     */
    @GetMapping("/alerts/attention")
    @Operation(summary = "Stocks nécessitant attention", description = "Récupérer les stocks nécessitant une attention particulière")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksRequiringAttention() {

        log.debug("Demande des stocks nécessitant attention");

        try {
            List<StockDTO> stocks = stockService.getStocksRequiringAttention();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks nécessitent attention", stocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks nécessitant attention", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks nécessitant attention"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // STATISTIQUES ET VALEURS
    // ===============================

    /**
     * Calculer la valeur totale du stock
     */
    @GetMapping("/stats/total-value")
    @Operation(summary = "Valeur totale du stock", description = "Calculer la valeur totale de tous les stocks")
    public ResponseEntity<ApiResponseDTO<BigDecimal>> getTotalStockValue() {

        log.debug("Calcul de la valeur totale du stock");

        try {
            BigDecimal totalValue = stockService.getTotalStockValue();

            ApiResponseDTO<BigDecimal> response = ApiResponseDTO.success(
                    totalValue,
                    "Valeur totale du stock calculée avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du calcul de la valeur totale", e);

            ApiResponseDTO<BigDecimal> response = ApiResponseDTO.error(
                    "Erreur lors du calcul de la valeur totale du stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les statistiques générales du stock
     */
    @GetMapping("/stats/general")
    @Operation(summary = "Statistiques générales", description = "Récupérer les statistiques générales du stock")
    public ResponseEntity<ApiResponseDTO<Object>> getGeneralStockStatistics() {

        log.debug("Récupération des statistiques générales du stock");

        try {
            Object statistics = stockService.getGeneralStockStatistics();

            ApiResponseDTO<Object> response = ApiResponseDTO.success(statistics);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques générales", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques générales"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les statistiques détaillées par statut
     */
    @GetMapping("/stats/status-count")
    @Operation(summary = "Comptage par statut", description = "Récupérer le comptage détaillé des stocks par statut")
    public ResponseEntity<ApiResponseDTO<Object>> getDetailedStockStatusCount() {

        log.debug("Récupération du comptage détaillé par statut");

        try {
            Object statusCount = stockService.getDetailedStockStatusCount();

            ApiResponseDTO<Object> response = ApiResponseDTO.success(statusCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du comptage par statut", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération du comptage par statut"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer la valeur du stock par catégorie
     */
    @GetMapping("/stats/value-by-category")
    @Operation(summary = "Valeur par catégorie", description = "Récupérer la valeur du stock répartie par catégorie")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getStockValueByCategory() {

        log.debug("Récupération de la valeur du stock par catégorie");

        try {
            List<Object[]> valueByCategory = stockService.getStockValueByCategory();

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(valueByCategory);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la valeur par catégorie", e);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de la valeur par catégorie"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les statistiques détaillées par catégorie
     */
    @GetMapping("/stats/detailed-by-category")
    @Operation(summary = "Statistiques par catégorie", description = "Récupérer les statistiques détaillées par catégorie")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getDetailedStockStatsByCategory() {

        log.debug("Récupération des statistiques détaillées par catégorie");

        try {
            List<Object[]> statsByCategory = stockService.getDetailedStockStatsByCategory();

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(statsByCategory);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques par catégorie", e);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques par catégorie"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // TOP RANKINGS ET ANALYSES
    // ===============================

    /**
     * Récupérer le top des stocks par valeur
     */
    @GetMapping("/rankings/top-by-value")
    @Operation(summary = "Top stocks par valeur", description = "Récupérer le classement des stocks par valeur")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getTopStocksByValue(
            @Parameter(description = "Nombre de résultats") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Récupération du top {} stocks par valeur", limit);

        try {
            List<StockDTO> topStocks = stockService.getTopStocksByValue(limit);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    topStocks,
                    String.format("Top %d stocks par valeur", limit)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du top par valeur", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération du top par valeur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer le top des stocks par quantité
     */
    @GetMapping("/rankings/top-by-quantity")
    @Operation(summary = "Top stocks par quantité", description = "Récupérer le classement des stocks par quantité")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getTopStocksByQuantity(
            @Parameter(description = "Nombre de résultats") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Récupération du top {} stocks par quantité", limit);

        try {
            List<StockDTO> topStocks = stockService.getTopStocksByQuantity(limit);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    topStocks,
                    String.format("Top %d stocks par quantité", limit)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du top par quantité", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération du top par quantité"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles les moins chers en stock
     */
    @GetMapping("/rankings/cheapest")
    @Operation(summary = "Articles les moins chers", description = "Récupérer les articles les moins chers en stock")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getCheapestStockedArticles(
            @Parameter(description = "Nombre de résultats") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Récupération des {} articles les moins chers", limit);

        try {
            List<StockDTO> cheapestStocks = stockService.getCheapestStockedArticles(limit);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    cheapestStocks,
                    String.format("%d articles les moins chers en stock", limit)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des articles les moins chers", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles les moins chers"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles les plus chers en stock
     */
    @GetMapping("/rankings/most-expensive")
    @Operation(summary = "Articles les plus chers", description = "Récupérer les articles les plus chers en stock")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getMostExpensiveStockedArticles(
            @Parameter(description = "Nombre de résultats") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Récupération des {} articles les plus chers", limit);

        try {
            List<StockDTO> expensiveStocks = stockService.getMostExpensiveStockedArticles(limit);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    expensiveStocks,
                    String.format("%d articles les plus chers en stock", limit)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des articles les plus chers", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles les plus chers"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // ANALYSE DE ROTATION
    // ===============================

    /**
     * Récupérer les stocks à rotation rapide
     */
    @GetMapping("/rotation/fast-moving")
    @Operation(summary = "Stocks à rotation rapide", description = "Récupérer les stocks avec une rotation rapide")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getFastMovingStocks(
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "30") int days) {

        log.debug("Récupération des stocks à rotation rapide (derniers {} jours)", days);

        try {
            List<StockDTO> fastMovingStocks = stockService.getFastMovingStocks(days);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    fastMovingStocks,
                    String.format("Stocks à rotation rapide sur %d jours", days)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks à rotation rapide", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks à rotation rapide"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks à rotation lente
     */
    @GetMapping("/rotation/slow-moving")
    @Operation(summary = "Stocks à rotation lente", description = "Récupérer les stocks avec une rotation lente")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getSlowMovingStocks(
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "90") int days) {

        log.debug("Récupération des stocks à rotation lente (derniers {} jours)", days);

        try {
            List<StockDTO> slowMovingStocks = stockService.getSlowMovingStocks(days);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    slowMovingStocks,
                    String.format("Stocks à rotation lente sur %d jours", days)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks à rotation lente", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks à rotation lente"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks dormants
     */
    @GetMapping("/rotation/dormant")
    @Operation(summary = "Stocks dormants", description = "Récupérer les stocks dormants sans mouvement")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getDormantStocks(
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "180") int days) {

        log.debug("Récupération des stocks dormants (derniers {} jours)", days);

        try {
            List<StockDTO> dormantStocks = stockService.getDormantStocks(days);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    dormantStocks,
                    String.format("Stocks dormants sur %d jours", days)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks dormants", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks dormants"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // GESTION DES RÉSERVATIONS
    // ===============================

    /**
     * Récupérer les stocks avec réservations
     */
    @GetMapping("/reservations/with-reservations")
    @Operation(summary = "Stocks avec réservations", description = "Récupérer les stocks ayant des réservations")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksWithReservations() {

        log.debug("Récupération des stocks avec réservations");

        try {
            List<StockDTO> stocksWithReservations = stockService.getStocksWithReservations();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocksWithReservations,
                    String.format("%d stocks avec réservations", stocksWithReservations.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks avec réservations", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks avec réservations"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks sur-réservés
     */
    @GetMapping("/reservations/over-reserved")
    @Operation(summary = "Stocks sur-réservés", description = "Récupérer les stocks avec des sur-réservations")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getOverReservedStocks() {

        log.debug("Récupération des stocks sur-réservés");

        try {
            List<StockDTO> overReservedStocks = stockService.getOverReservedStocks();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    overReservedStocks,
                    String.format("%d stocks sur-réservés", overReservedStocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks sur-réservés", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks sur-réservés"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mettre à jour la quantité réservée d'un article
     */
    @PutMapping("/reservations/article/{articleId}")
    @Operation(summary = "Mettre à jour réservation", description = "Mettre à jour la quantité réservée d'un article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantité réservée mise à jour"),
            @ApiResponse(responseCode = "400", description = "Quantité invalide"),
            @ApiResponse(responseCode = "404", description = "Article non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<String>> updateQuantiteReservee(
            @Parameter(description = "ID de l'article") @PathVariable Long articleId,
            @Parameter(description = "Nouvelle quantité réservée") @RequestParam Integer quantiteReservee) {

        log.info("Mise à jour de la quantité réservée pour l'article ID: {} -> {}", articleId, quantiteReservee);

        try {
            stockService.updateQuantiteReservee(articleId, quantiteReservee);

            ApiResponseDTO<String> response = ApiResponseDTO.success(
                    "Quantité réservée mise à jour avec succès",
                    String.format("Quantité réservée mise à jour: %d pour l'article ID: %d", quantiteReservee, articleId)
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation pour mise à jour réservation: {}", e.getMessage());

            ApiResponseDTO<String> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Article non trouvé pour mise à jour réservation: {}", e.getMessage());

            ApiResponseDTO<String> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la réservation", e);

            ApiResponseDTO<String> response = ApiResponseDTO.error(
                    "Erreur interne lors de la mise à jour de la réservation"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Remettre à zéro toutes les réservations
     */
    @PutMapping("/reservations/reset-all")
    @Operation(summary = "Reset toutes réservations", description = "Remettre à zéro toutes les réservations")
    public ResponseEntity<ApiResponseDTO<String>> resetAllReservations() {

        log.info("Remise à zéro de toutes les réservations");

        try {
            stockService.resetAllReservations();

            ApiResponseDTO<String> response = ApiResponseDTO.success(
                    "Toutes les réservations ont été remises à zéro",
                    "Opération de reset effectuée avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du reset des réservations", e);

            ApiResponseDTO<String> response = ApiResponseDTO.error(
                    "Erreur lors de la remise à zéro des réservations"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // INVENTAIRE ET ÉCARTS
    // ===============================

    /**
     * Récupérer les stocks avec écart d'inventaire
     */
    @GetMapping("/inventory/with-gap")
    @Operation(summary = "Stocks avec écart inventaire", description = "Récupérer les stocks avec écart d'inventaire")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksWithInventoryGap() {

        log.debug("Récupération des stocks avec écart d'inventaire");

        try {
            List<StockDTO> stocksWithGap = stockService.getStocksWithInventoryGap();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocksWithGap,
                    String.format("%d stocks avec écart d'inventaire", stocksWithGap.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks avec écart", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks avec écart d'inventaire"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks avec écart positif (surplus)
     */
    @GetMapping("/inventory/positive-gap")
    @Operation(summary = "Stocks avec surplus", description = "Récupérer les stocks avec écart positif (surplus)")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksWithPositiveInventoryGap() {

        log.debug("Récupération des stocks avec écart positif");

        try {
            List<StockDTO> stocksWithPositiveGap = stockService.getStocksWithPositiveInventoryGap();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocksWithPositiveGap,
                    String.format("%d stocks avec surplus", stocksWithPositiveGap.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks avec surplus", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks avec surplus"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks avec écart négatif (manquant)
     */
    @GetMapping("/inventory/negative-gap")
    @Operation(summary = "Stocks avec manque", description = "Récupérer les stocks avec écart négatif (manquant)")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksWithNegativeInventoryGap() {

        log.debug("Récupération des stocks avec écart négatif");

        try {
            List<StockDTO> stocksWithNegativeGap = stockService.getStocksWithNegativeInventoryGap();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocksWithNegativeGap,
                    String.format("%d stocks avec manque", stocksWithNegativeGap.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks avec manque", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks avec manque"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mettre à jour les dates d'inventaire pour les stocks cohérents
     */
    @PutMapping("/inventory/update-dates")
    @Operation(summary = "Mettre à jour dates inventaire", description = "Mettre à jour les dates d'inventaire pour les stocks cohérents")
    public ResponseEntity<ApiResponseDTO<String>> updateInventoryDatesForConsistentStocks() {

        log.info("Mise à jour des dates d'inventaire pour les stocks cohérents");

        try {
            stockService.updateInventoryDatesForConsistentStocks();

            ApiResponseDTO<String> response = ApiResponseDTO.success(
                    "Dates d'inventaire mises à jour pour les stocks cohérents",
                    "Opération effectuée avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour des dates d'inventaire", e);

            ApiResponseDTO<String> response = ApiResponseDTO.error(
                    "Erreur lors de la mise à jour des dates d'inventaire"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // FILTRES ET RECHERCHES SPÉCIALISÉES
    // ===============================

    /**
     * Récupérer les stocks par catégorie
     */
    @GetMapping("/filter/category/{categorie}")
    @Operation(summary = "Stocks par catégorie", description = "Récupérer les stocks d'une catégorie spécifique")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksByCategory(
            @Parameter(description = "Nom de la catégorie") @PathVariable String categorie) {

        log.debug("Récupération des stocks de la catégorie: {}", categorie);

        try {
            List<StockDTO> stocks = stockService.getStocksByCategory(categorie);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks trouvés pour la catégorie: %s", stocks.size(), categorie)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks par catégorie", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks par catégorie"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks par fournisseur
     */
    @GetMapping("/filter/supplier/{fournisseurId}")
    @Operation(summary = "Stocks par fournisseur", description = "Récupérer les stocks d'un fournisseur spécifique")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksBySupplier(
            @Parameter(description = "ID du fournisseur") @PathVariable Long fournisseurId) {

        log.debug("Récupération des stocks du fournisseur ID: {}", fournisseurId);

        try {
            List<StockDTO> stocks = stockService.getStocksBySupplier(fournisseurId);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks trouvés pour le fournisseur ID: %d", stocks.size(), fournisseurId)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks par fournisseur", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks par fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer la valeur du stock par fournisseur
     */
    @GetMapping("/stats/value-by-supplier")
    @Operation(summary = "Valeur par fournisseur", description = "Récupérer la valeur du stock répartie par fournisseur")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getStockValueBySupplier() {

        log.debug("Récupération de la valeur du stock par fournisseur");

        try {
            List<Object[]> valueBySupplier = stockService.getStockValueBySupplier();

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(valueBySupplier);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la valeur par fournisseur", e);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de la valeur par fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks dans une fourchette de prix
     */
    @GetMapping("/filter/price-range")
    @Operation(summary = "Stocks par fourchette prix", description = "Récupérer les stocks dans une fourchette de prix")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksByPriceRange(
            @Parameter(description = "Prix minimum") @RequestParam BigDecimal minPrice,
            @Parameter(description = "Prix maximum") @RequestParam BigDecimal maxPrice) {

        log.debug("Récupération des stocks avec prix entre {} et {}", minPrice, maxPrice);

        try {
            List<StockDTO> stocks = stockService.getStocksByPriceRange(minPrice, maxPrice);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks trouvés dans la fourchette %s - %s", stocks.size(), minPrice, maxPrice)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération par fourchette de prix", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération par fourchette de prix"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks dans une fourchette de valeur
     */
    @GetMapping("/filter/value-range")
    @Operation(summary = "Stocks par fourchette valeur", description = "Récupérer les stocks dans une fourchette de valeur")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksByValueRange(
            @Parameter(description = "Valeur minimum") @RequestParam BigDecimal minValue,
            @Parameter(description = "Valeur maximum") @RequestParam BigDecimal maxValue) {

        log.debug("Récupération des stocks avec valeur entre {} et {}", minValue, maxValue);

        try {
            List<StockDTO> stocks = stockService.getStocksByValueRange(minValue, maxValue);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocks,
                    String.format("%d stocks trouvés dans la fourchette %s - %s", stocks.size(), minValue, maxValue)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération par fourchette de valeur", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération par fourchette de valeur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // UTILITAIRES ET MAINTENANCE
    // ===============================

    /**
     * Vérifier les stocks incohérents
     */
    @GetMapping("/maintenance/inconsistent")
    @Operation(summary = "Stocks incohérents", description = "Vérifier et récupérer les stocks avec des incohérences")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getInconsistentStocks() {

        log.debug("Vérification des stocks incohérents");

        try {
            List<StockDTO> inconsistentStocks = stockService.getInconsistentStocks();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    inconsistentStocks,
                    String.format("%d stocks incohérents trouvés", inconsistentStocks.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la vérification des stocks incohérents", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la vérification des stocks incohérents"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les stocks avec données manquantes
     */
    @GetMapping("/maintenance/missing-data")
    @Operation(summary = "Stocks données manquantes", description = "Récupérer les stocks avec des données manquantes")
    public ResponseEntity<ApiResponseDTO<List<StockDTO>>> getStocksWithMissingData() {

        log.debug("Récupération des stocks avec données manquantes");

        try {
            List<StockDTO> stocksWithMissingData = stockService.getStocksWithMissingData();

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.success(
                    stocksWithMissingData,
                    String.format("%d stocks avec données manquantes", stocksWithMissingData.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stocks avec données manquantes", e);

            ApiResponseDTO<List<StockDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des stocks avec données manquantes"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles sans stock initialisé
     */
    @GetMapping("/maintenance/articles-without-stock")
    @Operation(summary = "Articles sans stock", description = "Récupérer les articles sans stock initialisé")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getArticlesWithoutStock() {

        log.debug("Récupération des articles sans stock initialisé");

        try {
            List<Object[]> articlesWithoutStock = stockService.getArticlesWithoutStock();

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(
                    articlesWithoutStock,
                    String.format("%d articles sans stock initialisé", articlesWithoutStock.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des articles sans stock", e);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles sans stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Compter le nombre d'articles en stock
     */
    @GetMapping("/stats/count-articles")
    @Operation(summary = "Compter articles en stock", description = "Compter le nombre total d'articles en stock")
    public ResponseEntity<ApiResponseDTO<Long>> countArticlesInStock() {

        log.debug("Comptage des articles en stock");

        try {
            Long count = stockService.countArticlesInStock();

            ApiResponseDTO<Long> response = ApiResponseDTO.success(
                    count,
                    String.format("%d articles en stock", count)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du comptage des articles", e);

            ApiResponseDTO<Long> response = ApiResponseDTO.error(
                    "Erreur lors du comptage des articles en stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Initialiser le stock pour un article
     */
    @PostMapping("/initialize/{articleId}")
    @Operation(summary = "Initialiser stock", description = "Initialiser le stock pour un article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock initialisé avec succès"),
            @ApiResponse(responseCode = "400", description = "Stock déjà existant ou article invalide"),
            @ApiResponse(responseCode = "404", description = "Article non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<StockDTO>> initializeStockForArticle(
            @Parameter(description = "ID de l'article") @PathVariable Long articleId) {

        log.info("Initialisation du stock pour l'article ID: {}", articleId);

        try {
            StockDTO initializedStock = stockService.initializeStockForArticle(articleId);

            ApiResponseDTO<StockDTO> response = ApiResponseDTO.success(
                    initializedStock,
                    "Stock initialisé avec succès"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur d'initialisation de stock: {}", e.getMessage());

            ApiResponseDTO<StockDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Article non trouvé pour initialisation: {}", e.getMessage());

            ApiResponseDTO<StockDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du stock", e);

            ApiResponseDTO<StockDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de l'initialisation du stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // TABLEAUX DE BORD ET RAPPORTS
    // ===============================

    /**
     * Dashboard général des stocks
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stocks", description = "Récupérer un tableau de bord général des stocks")
    public ResponseEntity<ApiResponseDTO<Object>> getStocksDashboard() {

        log.debug("Génération du dashboard des stocks");

        try {
            // Création d'un dashboard avec plusieurs métriques
            java.util.Map<String, Object> dashboard = new java.util.HashMap<>();

            // Statistiques générales
            dashboard.put("statistiquesGenerales", stockService.getGeneralStockStatistics());
            dashboard.put("valeurTotale", stockService.getTotalStockValue());
            dashboard.put("nombreArticles", stockService.countArticlesInStock());

            // Alertes
            dashboard.put("stocksCritiques", stockService.getCriticalStocks().size());
            dashboard.put("stocksFaibles", stockService.getLowStocks().size());
            dashboard.put("stocksVides", stockService.getEmptyStocks().size());
            dashboard.put("stocksExcessifs", stockService.getExcessiveStocks().size());

            // Top rankings (limités pour le dashboard)
            dashboard.put("topValeur", stockService.getTopStocksByValue(5));
            dashboard.put("topQuantite", stockService.getTopStocksByQuantity(5));

            // Réservations
            dashboard.put("stocksAvecReservations", stockService.getStocksWithReservations().size());
            dashboard.put("stocksSurReserves", stockService.getOverReservedStocks().size());

            // Inventaire
            dashboard.put("stocksAvecEcart", stockService.getStocksWithInventoryGap().size());

            dashboard.put("dateGeneration", LocalDateTime.now());

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    dashboard,
                    "Dashboard généré avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du dashboard", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du dashboard"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rapport de santé des stocks
     */
    @GetMapping("/health-report")
    @Operation(summary = "Rapport santé stocks", description = "Générer un rapport de santé général des stocks")
    public ResponseEntity<ApiResponseDTO<Object>> getStockHealthReport() {

        log.debug("Génération du rapport de santé des stocks");

        try {
            java.util.Map<String, Object> healthReport = new java.util.HashMap<>();

            // Métriques de santé
            healthReport.put("stocksIncohérents", stockService.getInconsistentStocks().size());
            healthReport.put("stocksDonnéesManquantes", stockService.getStocksWithMissingData().size());
            healthReport.put("articilesSansStock", stockService.getArticlesWithoutStock().size());

            // Rotation des stocks
            healthReport.put("stocksDormants180j", stockService.getDormantStocks(180).size());
            healthReport.put("stocksRotationLente90j", stockService.getSlowMovingStocks(90).size());
            healthReport.put("stocksRotationRapide30j", stockService.getFastMovingStocks(30).size());

            // Indicateurs de performance
            BigDecimal valeurTotale = stockService.getTotalStockValue();
            Long nombreArticles = stockService.countArticlesInStock();

            if (nombreArticles > 0) {
                BigDecimal valeurMoyenneParArticle = valeurTotale.divide(BigDecimal.valueOf(nombreArticles), 2, BigDecimal.ROUND_HALF_UP);
                healthReport.put("valeurMoyenneParArticle", valeurMoyenneParArticle);
            }

            // Score de santé global (exemple simple)
            int scoreProblemes = stockService.getInconsistentStocks().size() +
                    stockService.getStocksWithMissingData().size() +
                    stockService.getOverReservedStocks().size();

            String niveauSante;
            if (scoreProblemes == 0) {
                niveauSante = "EXCELLENT";
            } else if (scoreProblemes < 5) {
                niveauSante = "BON";
            } else if (scoreProblemes < 15) {
                niveauSante = "MOYEN";
            } else {
                niveauSante = "CRITIQUE";
            }

            healthReport.put("niveauSanteGlobal", niveauSante);
            healthReport.put("scoreProblemes", scoreProblemes);
            healthReport.put("dateGeneration", LocalDateTime.now());

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    healthReport,
                    "Rapport de santé généré avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport de santé", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport de santé"
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
    @Operation(summary = "Ping service", description = "Vérifier la disponibilité du service des stocks")
    public ResponseEntity<ApiResponseDTO<String>> ping() {

        log.debug("Ping du service des stocks");

        try {
            String message = "Service des stocks opérationnel - " + LocalDateTime.now();

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
    @Operation(summary = "Version API", description = "Obtenir la version de l'API des stocks")
    public ResponseEntity<ApiResponseDTO<Object>> getApiVersion() {

        log.debug("Demande de version de l'API des stocks");

        try {
            java.util.Map<String, Object> versionInfo = new java.util.HashMap<>();
            versionInfo.put("version", "1.0.0");
            versionInfo.put("service", "StockController");
            versionInfo.put("lastUpdate", "2024-12-19");
            versionInfo.put("author", "Stock Management System");
            versionInfo.put("description", "API complète de gestion des stocks");

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

    /**
     * Statistiques de performance de l'API
     */
    @GetMapping("/stats/performance")
    @Operation(summary = "Stats performance API", description = "Récupérer les statistiques de performance de l'API")
    public ResponseEntity<ApiResponseDTO<Object>> getPerformanceStats() {

        log.debug("Demande des statistiques de performance");

        try {
            java.util.Map<String, Object> perfStats = new java.util.HashMap<>();
            perfStats.put("timestamp", LocalDateTime.now());
            perfStats.put("uptime", "Service opérationnel");
            perfStats.put("nombreEndpoints", "45+ endpoints disponibles");
            perfStats.put("status", "HEALTHY");

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    perfStats,
                    "Statistiques de performance récupérées"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stats de performance", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques de performance"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
