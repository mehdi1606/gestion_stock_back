package com.elamal.stockmanagement.controller;
import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Articles", description = "API de gestion des articles")
@CrossOrigin(origins = "*")
public class ArticleController {
   @Autowired
    private  ArticleService articleService;

    // ===============================
    // OPÉRATIONS CRUD
    // ===============================

    /**
     * Créer un nouvel article
     */
    @PostMapping
    @Operation(summary = "Créer un article", description = "Créer un nouvel article dans le système")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Article créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Article avec ce code existe déjà")
    })
    public ResponseEntity<ApiResponseDTO<ArticleDTO>> createArticle(
            @Valid @RequestBody ArticleDTO articleDTO) {

        try {
            ArticleDTO createdArticle = articleService.createArticle(articleDTO);

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.success(
                    createdArticle,
                    "Article créé avec succès"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de la création de l'article"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer un article par ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un article", description = "Récupérer un article par son ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article trouvé"),
            @ApiResponse(responseCode = "404", description = "Article non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<ArticleDTO>> getArticleById(
            @Parameter(description = "ID de l'article") @PathVariable Long id) {


        try {
            ArticleDTO article = articleService.getArticleById(id);

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.success(article);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Récupérer un article par code
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Récupérer un article par code", description = "Récupérer un article par son code unique")
    public ResponseEntity<ApiResponseDTO<ArticleDTO>> getArticleByCode(
            @Parameter(description = "Code de l'article") @PathVariable String code) {


        try {
            ArticleDTO article = articleService.getArticleByCode(code);

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.success(article);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Mettre à jour un article
     */
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un article", description = "Mettre à jour un article existant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Article non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<ArticleDTO>> updateArticle(
            @Parameter(description = "ID de l'article") @PathVariable Long id,
            @Valid @RequestBody ArticleDTO articleDTO) {


        try {
            ArticleDTO updatedArticle = articleService.updateArticle(id, articleDTO);

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.success(
                    updatedArticle,
                    "Article mis à jour avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Supprimer un article (suppression logique)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article", description = "Supprimer un article (suppression logique)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article supprimé avec succès"),
            @ApiResponse(responseCode = "404", description = "Article non trouvé"),
            @ApiResponse(responseCode = "409", description = "Impossible de supprimer (stock en cours)")
    })
    public ResponseEntity<ApiResponseDTO<Void>> deleteArticle(
            @Parameter(description = "ID de l'article") @PathVariable Long id) {


        try {
            articleService.deleteArticle(id);

            ApiResponseDTO<Void> response = ApiResponseDTO.success(
                    null,
                    "Article supprimé avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {

            ApiResponseDTO<Void> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (RuntimeException e) {

            ApiResponseDTO<Void> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Réactiver un article
     */
    @PatchMapping("/{id}/reactivate")
    @Operation(summary = "Réactiver un article", description = "Réactiver un article désactivé")
    public ResponseEntity<ApiResponseDTO<ArticleDTO>> reactivateArticle(
            @Parameter(description = "ID de l'article") @PathVariable Long id) {


        try {
            ArticleDTO reactivatedArticle = articleService.reactivateArticle(id);

            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.success(
                    reactivatedArticle,
                    "Article réactivé avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            ApiResponseDTO<ArticleDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // ===============================
    // OPÉRATIONS DE CONSULTATION
    // ===============================

    /**
     * Récupérer tous les articles avec pagination
     */
    @GetMapping
    @Operation(summary = "Lister les articles", description = "Récupérer tous les articles avec pagination")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ArticleDTO>>> getAllArticles(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "dateCreation") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "DESC") String sortDirection) {


        try {
            PagedResponseDTO<ArticleDTO> articles = articleService.getAllArticles(page, size, sortBy, sortDirection);

            ApiResponseDTO<PagedResponseDTO<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<PagedResponseDTO<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer tous les articles actifs
     */
    @GetMapping("/active")
    @Operation(summary = "Lister les articles actifs", description = "Récupérer tous les articles actifs")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getActiveArticles() {

        try {
            List<ArticleDTO> articles = articleService.getAllActiveArticles();

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles actifs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rechercher des articles
     */
    @PostMapping("/search")
    @Operation(summary = "Rechercher des articles", description = "Rechercher des articles avec critères")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ArticleDTO>>> searchArticles(
            @Valid @RequestBody SearchCriteriaDTO criteria) {


        try {
            PagedResponseDTO<ArticleDTO> articles = articleService.searchArticles(criteria);

            ApiResponseDTO<PagedResponseDTO<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<PagedResponseDTO<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche d'articles"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche textuelle rapide
     */
    @GetMapping("/search")
    @Operation(summary = "Recherche textuelle", description = "Recherche textuelle rapide dans les articles")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ArticleDTO>>> searchArticlesByText(
            @Parameter(description = "Terme de recherche") @RequestParam String q,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size) {


        try {
            PagedResponseDTO<ArticleDTO> articles = articleService.searchArticlesByText(q, page, size);

            ApiResponseDTO<PagedResponseDTO<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<PagedResponseDTO<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche textuelle"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // GESTION DU STOCK ET ALERTES
    // ===============================

    /**
     * Récupérer les articles avec stock critique
     */
    @GetMapping("/stock/critical")
    @Operation(summary = "Articles en stock critique", description = "Récupérer les articles avec stock critique")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getCriticalStockArticles() {


        try {
            List<ArticleDTO> articles = articleService.getArticlesWithCriticalStock();

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles critiques"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles avec stock faible
     */
    @GetMapping("/stock/low")
    @Operation(summary = "Articles en stock faible", description = "Récupérer les articles avec stock faible")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getLowStockArticles() {


        try {
            List<ArticleDTO> articles = articleService.getArticlesWithLowStock();

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles avec stock faible"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles sans stock
     */
    @GetMapping("/stock/empty")
    @Operation(summary = "Articles sans stock", description = "Récupérer les articles sans stock")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getEmptyStockArticles() {


        try {
            List<ArticleDTO> articles = articleService.getArticlesWithoutStock();

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles sans stock"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles avec stock excessif
     */
    @GetMapping("/stock/excessive")
    @Operation(summary = "Articles en stock excessif", description = "Récupérer les articles avec stock excessif")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getExcessiveStockArticles() {


        try {
            List<ArticleDTO> articles = articleService.getArticlesWithExcessiveStock();

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles avec stock excessif"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // STATISTIQUES ET ANALYSES
    // ===============================

    /**
     * Récupérer le top des articles par valeur de stock
     */
    @GetMapping("/stats/top-by-value")
    @Operation(summary = "Top articles par valeur", description = "Récupérer le top des articles par valeur de stock")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getTopArticlesByValue(
            @Parameter(description = "Nombre d'articles à retourner") @RequestParam(defaultValue = "10") int limit) {


        try {
            List<ArticleDTO> articles = articleService.getTopArticlesByStockValue(limit);

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération du top articles par valeur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les statistiques par catégorie
     */
    @GetMapping("/stats/by-category")
    @Operation(summary = "Statistiques par catégorie", description = "Récupérer les statistiques des articles par catégorie")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getStatsByCategory() {


        try {
            List<Object[]> stats = articleService.getStockStatsByCategory();

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(stats);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques par catégorie"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer les articles les plus consommés
     */
    @GetMapping("/stats/most-consumed")
    @Operation(summary = "Articles les plus consommés", description = "Récupérer les articles les plus consommés")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getMostConsumedArticles(
            @Parameter(description = "Nombre de jours") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Limite d'articles") @RequestParam(defaultValue = "10") int limit) {


        try {
            List<Object[]> articles = articleService.getMostConsumedArticles(days, limit);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles les plus consommés"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // GESTION PAR CATÉGORIE
    // ===============================

    /**
     * Récupérer les articles par catégorie
     */
    @GetMapping("/category/{categorie}")
    @Operation(summary = "Articles par catégorie", description = "Récupérer les articles d'une catégorie spécifique")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getArticlesByCategory(
            @Parameter(description = "Nom de la catégorie") @PathVariable String categorie) {


        try {
            List<ArticleDTO> articles = articleService.getArticlesByCategory(categorie);

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles par catégorie"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer toutes les catégories
     */
    @GetMapping("/categories")
    @Operation(summary = "Lister les catégories", description = "Récupérer toutes les catégories d'articles")
    public ResponseEntity<ApiResponseDTO<List<String>>> getAllCategories() {


        try {
            List<String> categories = articleService.getAllCategories();

            ApiResponseDTO<List<String>> response = ApiResponseDTO.success(categories);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<String>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des catégories"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // GESTION PAR FOURNISSEUR
    // ===============================

    /**
     * Récupérer les articles d'un fournisseur
     */
    @GetMapping("/supplier/{fournisseurId}")
    @Operation(summary = "Articles par fournisseur", description = "Récupérer les articles d'un fournisseur spécifique")
    public ResponseEntity<ApiResponseDTO<List<ArticleDTO>>> getArticlesBySupplier(
            @Parameter(description = "ID du fournisseur") @PathVariable Long fournisseurId) {


        try {
            List<ArticleDTO> articles = articleService.getArticlesByFournisseur(fournisseurId);

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.success(articles);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<List<ArticleDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des articles par fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // VALIDATION ET VÉRIFICATION
    // ===============================

    /**
     * Vérifier l'existence d'un code article
     */
    @GetMapping("/exists/code/{code}")
    @Operation(summary = "Vérifier l'existence d'un code", description = "Vérifier si un code article existe")
    public ResponseEntity<ApiResponseDTO<Boolean>> checkCodeExists(
            @Parameter(description = "Code de l'article") @PathVariable String code) {


        try {
            Boolean exists = articleService.existsByCode(code);

            ApiResponseDTO<Boolean> response = ApiResponseDTO.success(exists);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<Boolean> response = ApiResponseDTO.error(
                    "Erreur lors de la vérification du code"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Compter les articles par statut
     */
    @GetMapping("/count")
    @Operation(summary = "Compter les articles", description = "Compter les articles par statut actif/inactif")
    public ResponseEntity<ApiResponseDTO<Long>> countArticlesByStatus(
            @Parameter(description = "Statut actif (true/false)") @RequestParam Boolean actif) {


        try {
            Long count = articleService.countArticlesByStatus(actif);

            ApiResponseDTO<Long> response = ApiResponseDTO.success(count);
            return ResponseEntity.ok(response);

        } catch (Exception e) {

            ApiResponseDTO<Long> response = ApiResponseDTO.error(
                    "Erreur lors du comptage des articles"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
