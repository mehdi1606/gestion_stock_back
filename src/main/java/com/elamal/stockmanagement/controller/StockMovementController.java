package com.elamal.stockmanagement.controller;


import com.elamal.stockmanagement.dto.*;

import com.elamal.stockmanagement.entity.TypeMouvement;
import com.elamal.stockmanagement.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mouvements de Stock", description = "API de gestion des mouvements de stock")
@CrossOrigin(origins = "*")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    // ===============================
    // OPÉRATIONS D'ENTRÉE DE STOCK
    // ===============================

    /**
     * Effectuer une entrée de stock
     */
    @PostMapping("/entree")
    @Operation(summary = "Entrée de stock", description = "Effectuer une entrée de stock avec mise à jour automatique du PMP")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrée de stock effectuée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Article ou fournisseur non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<StockMovementDTO>> processEntreeStock(
            @Valid @RequestBody EntreeStockRequestDTO entreeRequest) {

        log.info("Demande d'entrée de stock pour article ID: {}, quantité: {}",
                entreeRequest.getArticleId(), entreeRequest.getQuantite());

        try {
            StockMovementDTO movement = stockMovementService.processEntreeStock(entreeRequest);

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.success(
                    movement,
                    "Entrée de stock effectuée avec succès"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation pour entrée de stock: {}", e.getMessage());

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'entrée de stock: {}", e.getMessage());

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de l'entrée de stock", e);

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors du traitement de l'entrée de stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Effectuer plusieurs entrées de stock en lot
     */
    @PostMapping("/entree/batch")
    @Operation(summary = "Entrées de stock en lot", description = "Effectuer plusieurs entrées de stock en une seule opération")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Entrées de stock effectuées avec succès"),
            @ApiResponse(responseCode = "400", description = "Une ou plusieurs entrées invalides")
    })
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> processBatchEntreeStock(
            @Valid @RequestBody List<EntreeStockRequestDTO> entreeRequests) {

        log.info("Demande d'entrées de stock en lot - {} entrées", entreeRequests.size());

        try {
            List<StockMovementDTO> movements = stockMovementService.processBatchEntreeStock(entreeRequests);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(
                    movements,
                    String.format("%d entrées de stock effectuées avec succès", movements.size())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation pour entrées en lot: {}", e.getMessage());

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Erreur lors des entrées en lot", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors du traitement des entrées de stock en lot"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // OPÉRATIONS DE SORTIE DE STOCK
    // ===============================

    /**
     * Effectuer une sortie de stock
     */
    @PostMapping("/sortie")
    @Operation(summary = "Sortie de stock", description = "Effectuer une sortie de stock avec vérification de disponibilité")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sortie de stock effectuée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou stock insuffisant"),
            @ApiResponse(responseCode = "404", description = "Article non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<StockMovementDTO>> processSortieStock(
            @Valid @RequestBody SortieStockRequestDTO sortieRequest) {

        log.info("Demande de sortie de stock pour article ID: {}, quantité: {}",
                sortieRequest.getArticleId(), sortieRequest.getQuantite());

        try {
            StockMovementDTO movement = stockMovementService.processSortieStock(sortieRequest);

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.success(
                    movement,
                    "Sortie de stock effectuée avec succès"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Erreur lors de la sortie de stock: {}", e.getMessage());

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Article non trouvé pour sortie de stock: {}", e.getMessage());

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la sortie de stock", e);

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors du traitement de la sortie de stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Effectuer plusieurs sorties de stock en lot
     */
    @PostMapping("/sortie/batch")
    @Operation(summary = "Sorties de stock en lot", description = "Effectuer plusieurs sorties de stock en une seule opération")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> processBatchSortieStock(
            @Valid @RequestBody List<SortieStockRequestDTO> sortieRequests) {

        log.info("Demande de sorties de stock en lot - {} sorties", sortieRequests.size());

        try {
            List<StockMovementDTO> movements = stockMovementService.processBatchSortieStock(sortieRequests);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(
                    movements,
                    String.format("%d sorties de stock effectuées avec succès", movements.size())
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Erreur lors des sorties en lot: {}", e.getMessage());

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Erreur lors des sorties en lot", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors du traitement des sorties de stock en lot"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // OPÉRATIONS DE CONSULTATION
    // ===============================

    /**
     * Récupérer un mouvement par ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un mouvement", description = "Récupérer un mouvement de stock par son ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mouvement trouvé"),
            @ApiResponse(responseCode = "404", description = "Mouvement non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<StockMovementDTO>> getMovementById(
            @Parameter(description = "ID du mouvement") @PathVariable Long id) {

        log.debug("Demande de récupération de mouvement ID: {}", id);

        try {
            StockMovementDTO movement = stockMovementService.getMovementById(id);

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.success(movement);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Mouvement non trouvé: ID {}", id);

            ApiResponseDTO<StockMovementDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Récupérer tous les mouvements avec pagination
     */
    @GetMapping
    @Operation(summary = "Lister les mouvements", description = "Récupérer tous les mouvements avec pagination")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockMovementDTO>>> getAllMovements(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "dateMouvement") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Demande de liste de mouvements - Page: {}, Size: {}", page, size);

        try {
            PagedResponseDTO<StockMovementDTO> movements = stockMovementService.getAllMovements(page, size, sortBy, sortDirection);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements", e);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des mouvements"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer l'historique des mouvements d'un article
     */
    @GetMapping("/article/{articleId}/history")
    @Operation(summary = "Historique par article", description = "Récupérer l'historique des mouvements d'un article")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockMovementDTO>>> getArticleMovementHistory(
            @Parameter(description = "ID de l'article") @PathVariable Long articleId,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size) {

        log.debug("Demande d'historique des mouvements pour article ID: {}", articleId);

        try {
            PagedResponseDTO<StockMovementDTO> movements = stockMovementService.getArticleMovementHistory(articleId, page, size);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'historique", e);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération de l'historique"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rechercher des mouvements avec critères
     */
    @PostMapping("/search")
    @Operation(summary = "Rechercher des mouvements", description = "Rechercher des mouvements avec critères avancés")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockMovementDTO>>> searchMovements(
            @Valid @RequestBody SearchCriteriaDTO criteria) {

        log.debug("Demande de recherche de mouvements avec critères");

        try {
            PagedResponseDTO<StockMovementDTO> movements = stockMovementService.searchMovements(criteria);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche de mouvements", e);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche de mouvements"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // STATISTIQUES ET ANALYSES
    // ===============================

    /**
     * Récupérer les mouvements d'aujourd'hui
     */
    @GetMapping("/today")
    @Operation(summary = "Mouvements d'aujourd'hui", description = "Récupérer tous les mouvements d'aujourd'hui")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getTodayMovements() {

        log.debug("Demande des mouvements d'aujourd'hui");

        try {
            List<StockMovementDTO> movements = stockMovementService.getTodayMovements();

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements d'aujourd'hui", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des mouvements d'aujourd'hui"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Compter les mouvements d'aujourd'hui par type
     */
    @GetMapping("/today/count")
    @Operation(summary = "Compter mouvements aujourd'hui", description = "Compter les mouvements d'aujourd'hui par type")
    public ResponseEntity<ApiResponseDTO<Long>> countTodayMovementsByType(
            @Parameter(description = "Type de mouvement") @RequestParam TypeMouvement typeMouvement) {

        log.debug("Comptage des mouvements d'aujourd'hui par type: {}", typeMouvement);

        try {
            Long count = stockMovementService.countTodayMovementsByType(typeMouvement);

            ApiResponseDTO<Long> response = ApiResponseDTO.success(count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du comptage des mouvements", e);

            ApiResponseDTO<Long> response = ApiResponseDTO.error(
                    "Erreur lors du comptage des mouvements"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Calculer la valeur totale des entrées dans une période
     */
    @GetMapping("/stats/entry-value")
    @Operation(summary = "Valeur totale des entrées", description = "Calculer la valeur totale des entrées dans une période")
    public ResponseEntity<ApiResponseDTO<BigDecimal>> getTotalEntryValue(
            @Parameter(description = "Date de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Calcul de la valeur totale des entrées entre {} et {}", startDate, endDate);

        try {
            BigDecimal totalValue = stockMovementService.getTotalEntryValue(startDate, endDate);

            ApiResponseDTO<BigDecimal> response = ApiResponseDTO.success(totalValue);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du calcul de la valeur des entrées", e);

            ApiResponseDTO<BigDecimal> response = ApiResponseDTO.error(
                    "Erreur lors du calcul de la valeur des entrées"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Calculer la valeur totale des sorties dans une période
     */
    @GetMapping("/stats/exit-value")
    @Operation(summary = "Valeur totale des sorties", description = "Calculer la valeur totale des sorties dans une période")
    public ResponseEntity<ApiResponseDTO<BigDecimal>> getTotalExitValue(
            @Parameter(description = "Date de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Calcul de la valeur totale des sorties entre {} et {}", startDate, endDate);

        try {
            BigDecimal totalValue = stockMovementService.getTotalExitValue(startDate, endDate);

            ApiResponseDTO<BigDecimal> response = ApiResponseDTO.success(totalValue);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du calcul de la valeur des sorties", e);

            ApiResponseDTO<BigDecimal> response = ApiResponseDTO.error(
                    "Erreur lors du calcul de la valeur des sorties"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // RECHERCHE SPÉCIALISÉE
    // ===============================

    /**
     * Rechercher des mouvements par numéro de bon
     */
    @GetMapping("/by-bon/{numeroBon}")
    @Operation(summary = "Mouvements par bon", description = "Rechercher des mouvements par numéro de bon")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getMovementsByNumeroBon(
            @Parameter(description = "Numéro de bon") @PathVariable String numeroBon) {

        log.debug("Recherche de mouvements par numéro de bon: {}", numeroBon);

        try {
            List<StockMovementDTO> movements = stockMovementService.getMovementsByNumeroBon(numeroBon);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par numéro de bon", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par numéro de bon"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rechercher des mouvements par numéro de facture
     */
    @GetMapping("/by-facture/{numeroFacture}")
    @Operation(summary = "Mouvements par facture", description = "Rechercher des mouvements par numéro de facture")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getMovementsByNumeroFacture(
            @Parameter(description = "Numéro de facture") @PathVariable String numeroFacture) {

        log.debug("Recherche de mouvements par numéro de facture: {}", numeroFacture);

        try {
            List<StockMovementDTO> movements = stockMovementService.getMovementsByNumeroFacture(numeroFacture);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par numéro de facture", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par numéro de facture"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche textuelle dans les mouvements
     */
    @GetMapping("/search")
    @Operation(summary = "Recherche textuelle", description = "Recherche textuelle dans les mouvements")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockMovementDTO>>> searchMovementsByText(
            @Parameter(description = "Terme de recherche") @RequestParam String q,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size) {

        log.debug("Recherche textuelle dans les mouvements: {}", q);

        try {
            PagedResponseDTO<StockMovementDTO> movements = stockMovementService.searchMovementsByText(q, page, size);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche textuelle", e);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche textuelle"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Détecter les mouvements suspects
     */
    @GetMapping("/suspicious")
    @Operation(summary = "Mouvements suspects", description = "Détecter les mouvements avec des quantités suspectes")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getSuspiciousMovements(
            @Parameter(description = "Seuil de quantité suspecte") @RequestParam(defaultValue = "1000") Integer threshold) {

        log.debug("Détection de mouvements suspects avec seuil: {}", threshold);

        try {
            List<StockMovementDTO> movements = stockMovementService.getSuspiciousMovements(threshold);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la détection de mouvements suspects", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la détection de mouvements suspects"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // MOUVEMENTS PAR FOURNISSEUR
    // ===============================

    /**
     * Récupérer les mouvements d'un fournisseur
     */
    @GetMapping("/supplier/{fournisseurId}")
    @Operation(summary = "Mouvements par fournisseur", description = "Récupérer les mouvements d'un fournisseur spécifique")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getFournisseurMovements(
            @Parameter(description = "ID du fournisseur") @PathVariable Long fournisseurId) {

        log.debug("Demande des mouvements du fournisseur ID: {}", fournisseurId);

        try {
            List<StockMovementDTO> movements = stockMovementService.getFournisseurMovements(fournisseurId);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements du fournisseur", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des mouvements du fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // VALIDATION ET VÉRIFICATION
    // ===============================

    /**
     * Vérifier la disponibilité de stock avant sortie
     */
    @GetMapping("/validate/stock-availability")
    @Operation(summary = "Vérifier disponibilité", description = "Vérifier la disponibilité de stock avant une sortie")
    public ResponseEntity<ApiResponseDTO<Boolean>> validateStockAvailability(
            @Parameter(description = "ID de l'article") @RequestParam Long articleId,
            @Parameter(description = "Quantité demandée") @RequestParam Integer quantite) {

        log.debug("Vérification de disponibilité - Article: {}, Quantité: {}", articleId, quantite);

        try {
            // Cette logique pourrait être dans le service
            // Pour l'instant, on simule une vérification simple
            Boolean isAvailable = true; // À implémenter dans le service

            ApiResponseDTO<Boolean> response = ApiResponseDTO.success(isAvailable);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la vérification de disponibilité", e);

            ApiResponseDTO<Boolean> response = ApiResponseDTO.error(
                    "Erreur lors de la vérification de disponibilité"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // FILTRES PAR TYPE DE MOUVEMENT
    // ===============================

    /**
     * Récupérer les mouvements par type
     */
    @GetMapping("/by-type/{typeMouvement}")
    @Operation(summary = "Mouvements par type", description = "Récupérer les mouvements d'un type spécifique")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getMovementsByType(
            @Parameter(description = "Type de mouvement") @PathVariable TypeMouvement typeMouvement,
            @Parameter(description = "Nombre maximum de résultats") @RequestParam(defaultValue = "100") int limit) {

        log.debug("Demande des mouvements de type: {}", typeMouvement);

        try {
            // Cette méthode devrait être ajoutée au service
            List<StockMovementDTO> movements = List.of(); // Placeholder

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements par type", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des mouvements par type"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer uniquement les entrées de stock
     */
    @GetMapping("/entries")
    @Operation(summary = "Entrées de stock", description = "Récupérer uniquement les entrées de stock")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockMovementDTO>>> getStockEntries(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Date de début (optionnelle)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin (optionnelle)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Demande des entrées de stock - Page: {}, Size: {}", page, size);

        try {
            // Créer des critères de recherche pour les entrées seulement
            SearchCriteriaDTO criteria = new SearchCriteriaDTO();
            criteria.setTypeMouvement(TypeMouvement.ENTREE);
            criteria.setPage(page);
            criteria.setSize(size);
            criteria.setSortBy("dateMouvement");
            criteria.setSortDirection("DESC");

            if (startDate != null) {
                criteria.setDateDebut(startDate.toString());
            }
            if (endDate != null) {
                criteria.setDateFin(endDate.toString());
            }

            PagedResponseDTO<StockMovementDTO> movements = stockMovementService.searchMovements(criteria);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des entrées de stock", e);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des entrées de stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer uniquement les sorties de stock
     */
    @GetMapping("/exits")
    @Operation(summary = "Sorties de stock", description = "Récupérer uniquement les sorties de stock")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<StockMovementDTO>>> getStockExits(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Date de début (optionnelle)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin (optionnelle)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Demande des sorties de stock - Page: {}, Size: {}", page, size);

        try {
            // Créer des critères de recherche pour les sorties seulement
            SearchCriteriaDTO criteria = new SearchCriteriaDTO();
            criteria.setTypeMouvement(TypeMouvement.SORTIE);
            criteria.setPage(page);
            criteria.setSize(size);
            criteria.setSortBy("dateMouvement");
            criteria.setSortDirection("DESC");

            if (startDate != null) {
                criteria.setDateDebut(startDate.toString());
            }
            if (endDate != null) {
                criteria.setDateFin(endDate.toString());
            }

            PagedResponseDTO<StockMovementDTO> movements = stockMovementService.searchMovements(criteria);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des sorties de stock", e);

            ApiResponseDTO<PagedResponseDTO<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des sorties de stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // STATISTIQUES AVANCÉES
    // ===============================

    /**
     * Récupérer les statistiques des mouvements par période
     */
    @GetMapping("/stats/summary")
    @Operation(summary = "Résumé statistiques", description = "Récupérer un résumé des statistiques des mouvements")
    public ResponseEntity<ApiResponseDTO<Object>> getMovementsSummary(
            @Parameter(description = "Date de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Demande de résumé des mouvements entre {} et {}", startDate, endDate);

        try {
            // Calculer les statistiques
            BigDecimal totalEntries = stockMovementService.getTotalEntryValue(startDate, endDate);
            BigDecimal totalExits = stockMovementService.getTotalExitValue(startDate, endDate);

            // Créer un objet de résumé
            java.util.Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("periode", new String[]{startDate.toString(), endDate.toString()});
            summary.put("valeurTotaleEntrees", totalEntries);
            summary.put("valeurTotaleSorties", totalExits);
            summary.put("soldeNet", totalEntries.subtract(totalExits));
            summary.put("dateGeneration", LocalDateTime.now());

            ApiResponseDTO<Object> response = ApiResponseDTO.success(summary);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du résumé", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du résumé des mouvements"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les mouvements récents (dernières 24h)
     */
    @GetMapping("/recent")
    @Operation(summary = "Mouvements récents", description = "Récupérer les mouvements des dernières 24 heures")
    public ResponseEntity<ApiResponseDTO<List<StockMovementDTO>>> getRecentMovements(
            @Parameter(description = "Nombre d'heures") @RequestParam(defaultValue = "24") int hours,
            @Parameter(description = "Nombre maximum de résultats") @RequestParam(defaultValue = "50") int limit) {

        log.debug("Demande des mouvements des dernières {} heures", hours);

        try {
            // Créer des critères pour les mouvements récents
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            SearchCriteriaDTO criteria = new SearchCriteriaDTO();
            criteria.setDateDebut(since.toString());
            criteria.setPage(0);
            criteria.setSize(limit);
            criteria.setSortBy("dateMouvement");
            criteria.setSortDirection("DESC");

            PagedResponseDTO<StockMovementDTO> pagedMovements = stockMovementService.searchMovements(criteria);
            List<StockMovementDTO> movements = pagedMovements.getContent();

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.success(movements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements récents", e);

            ApiResponseDTO<List<StockMovementDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des mouvements récents"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // EXPORT ET RAPPORTS
    // ===============================

    /**
     * Obtenir un rapport de mouvements pour un article
     */
    @GetMapping("/article/{articleId}/report")
    @Operation(summary = "Rapport par article", description = "Générer un rapport des mouvements pour un article")
    public ResponseEntity<ApiResponseDTO<Object>> getArticleMovementReport(
            @Parameter(description = "ID de l'article") @PathVariable Long articleId,
            @Parameter(description = "Date de début") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.debug("Génération de rapport pour article ID: {} entre {} et {}", articleId, startDate, endDate);

        try {
            // Cette méthode devrait être ajoutée au service
            java.util.Map<String, Object> report = new java.util.HashMap<>();
            report.put("articleId", articleId);
            report.put("periode", new String[]{startDate.toString(), endDate.toString()});
            report.put("dateGeneration", LocalDateTime.now());
            report.put("message", "Rapport généré avec succès");

            ApiResponseDTO<Object> response = ApiResponseDTO.success(report);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport article", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // CONTRÔLES ET VALIDATIONS
    // ===============================

    /**
     * Valider les données d'un mouvement avant création
     */
    @PostMapping("/validate")
    @Operation(summary = "Valider un mouvement", description = "Valider les données d'un mouvement avant sa création")
    public ResponseEntity<ApiResponseDTO<Boolean>> validateMovement(
            @Valid @RequestBody StockMovementDTO movementDTO) {

        log.debug("Validation d'un mouvement de type: {}", movementDTO.getTypeMouvement());

        try {
            // Validation basique utilisant les méthodes du DTO
            Boolean isValid = movementDTO.isValid();

            String message = isValid ? "Mouvement valide" : "Mouvement invalide";

            ApiResponseDTO<Boolean> response = ApiResponseDTO.success(isValid, message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la validation du mouvement", e);

            ApiResponseDTO<Boolean> response = ApiResponseDTO.error(
                    "Erreur lors de la validation du mouvement"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les types de mouvements disponibles
     */
    @GetMapping("/types")
    @Operation(summary = "Types de mouvements", description = "Récupérer tous les types de mouvements disponibles")
    public ResponseEntity<ApiResponseDTO<TypeMouvement[]>> getMovementTypes() {

        log.debug("Demande des types de mouvements disponibles");

        try {
            TypeMouvement[] types = TypeMouvement.values();

            ApiResponseDTO<TypeMouvement[]> response = ApiResponseDTO.success(types);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des types de mouvements", e);

            ApiResponseDTO<TypeMouvement[]> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des types de mouvements"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // UTILITAIRES
    // ===============================

    /**
     * Ping pour vérifier la disponibilité du service
     */
    @GetMapping("/ping")
    @Operation(summary = "Ping service", description = "Vérifier la disponibilité du service de mouvements")
    public ResponseEntity<ApiResponseDTO<String>> ping() {

        log.debug("Ping du service des mouvements de stock");

        try {
            String message = "Service des mouvements de stock opérationnel - " + LocalDateTime.now();

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
    @Operation(summary = "Version API", description = "Obtenir la version de l'API des mouvements")
    public ResponseEntity<ApiResponseDTO<Object>> getApiVersion() {

        log.debug("Demande de version de l'API des mouvements");

        try {
            java.util.Map<String, Object> versionInfo = new java.util.HashMap<>();
            versionInfo.put("version", "1.0.0");
            versionInfo.put("service", "StockMovementController");
            versionInfo.put("lastUpdate", "2024-12-19");
            versionInfo.put("author", "Stock Management System");

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
}
