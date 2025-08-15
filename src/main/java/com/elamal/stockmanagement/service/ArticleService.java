package com.elamal.stockmanagement.service;

import com.elamal.stockmanagement.dto.ArticleDTO;
import com.elamal.stockmanagement.dto.PagedResponseDTO;
import com.elamal.stockmanagement.dto.SearchCriteriaDTO;
import com.elamal.stockmanagement.entity.Article;
import com.elamal.stockmanagement.entity.Fournisseur;
import com.elamal.stockmanagement.entity.Stock;
import com.elamal.stockmanagement.repository.ArticleRepository;
import com.elamal.stockmanagement.repository.FournisseurRepository;
import com.elamal.stockmanagement.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ArticleService {

    // FIXED: Use consistent dependency injection (final fields with @RequiredArgsConstructor)
    private final ArticleRepository articleRepository;
    private final FournisseurRepository fournisseurRepository;
    private final StockRepository stockRepository;
    private final ModelMapper modelMapper;

    // ===============================
    // OPÉRATIONS CRUD DE BASE
    // ===============================

    /**
     * Créer un nouvel article
     */
    public ArticleDTO createArticle(ArticleDTO articleDTO) {
        try {
            log.info("Création d'un nouvel article avec le code: {}", articleDTO.getCode());

            // Validation métier
            validateArticleForCreation(articleDTO);

            // FIXED: Manual mapping instead of ModelMapper to avoid field mapping issues
            Article article = new Article();
            article.setCode(articleDTO.getCode());
            article.setDesignation(articleDTO.getDesignation());
            article.setDescription(articleDTO.getDescription());
            article.setCategorie(articleDTO.getCategorie());
            article.setUnite(articleDTO.getUnite());
            article.setPrixUnitaire(articleDTO.getPrixUnitaire());
            article.setStockMin(articleDTO.getStockMin());
            article.setStockMax(articleDTO.getStockMax());
            article.setActif(articleDTO.getActif() != null ? articleDTO.getActif() : true);

            // Gestion du fournisseur principal
            if (articleDTO.getFournisseurPrincipalId() != null) {
                Fournisseur fournisseur = fournisseurRepository.findById(articleDTO.getFournisseurPrincipalId())
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + articleDTO.getFournisseurPrincipalId()));
                article.setFournisseurPrincipal(fournisseur);
            }

            // Sauvegarde de l'article
            Article savedArticle = articleRepository.save(article);
            log.info("Article sauvegardé avec succès: ID={}, Code={}", savedArticle.getId(), savedArticle.getCode());

            // Création du stock initial (quantité 0)
            createInitialStock(savedArticle);

            return convertToDTO(savedArticle);

        } catch (Exception e) {
            log.error("Erreur lors de la création de l'article: {}", e.getMessage(), e);
            throw e; // Rethrow to get the actual error in the controller
        }
    }

    /**
     * Mettre à jour un article existant
     */
    public ArticleDTO updateArticle(Long id, ArticleDTO articleDTO) {
        try {
            log.info("Mise à jour de l'article ID: {}", id);

            Article existingArticle = articleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

            // Validation métier pour mise à jour
            validateArticleForUpdate(id, articleDTO);

            // Mise à jour des champs
            updateArticleFields(existingArticle, articleDTO);

            // Gestion du changement de fournisseur principal
            updateFournisseurPrincipal(existingArticle, articleDTO.getFournisseurPrincipalId());

            Article updatedArticle = articleRepository.save(existingArticle);
            log.info("Article mis à jour avec succès: ID={}", updatedArticle.getId());

            return convertToDTO(updatedArticle);

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'article ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Supprimer un article (suppression logique)
     */
    public void deleteArticle(Long id) {
        try {
            log.info("Suppression de l'article ID: {}", id);

            Article article = articleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

            // Vérifications avant suppression
            validateArticleForDeletion(article);

            // Suppression logique (désactivation)
            article.setActif(false);
            articleRepository.save(article);

            log.info("Article supprimé avec succès: ID={}", id);

        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'article ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Réactiver un article
     */
    public ArticleDTO reactivateArticle(Long id) {
        try {
            log.info("Réactivation de l'article ID: {}", id);

            Article article = articleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

            article.setActif(true);
            Article reactivatedArticle = articleRepository.save(article);

            log.info("Article réactivé avec succès: ID={}", id);

            return convertToDTO(reactivatedArticle);

        } catch (Exception e) {
            log.error("Erreur lors de la réactivation de l'article ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // ===============================
    // OPÉRATIONS DE LECTURE
    // ===============================

    /**
     * Récupérer un article par ID
     */
    @Transactional(readOnly = true)
    public ArticleDTO getArticleById(Long id) {
        log.debug("Récupération de l'article ID: {}", id);

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

        return convertToDTO(article);
    }

    /**
     * Récupérer un article par code
     */
    @Transactional(readOnly = true)
    public ArticleDTO getArticleByCode(String code) {
        log.debug("Récupération de l'article avec le code: {}", code);

        Article article = articleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec le code: " + code));

        return convertToDTO(article);
    }

    /**
     * Récupérer tous les articles actifs
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getAllActiveArticles() {
        log.debug("Récupération de tous les articles actifs");

        List<Article> articles = articleRepository.findByActifTrue();
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les articles avec pagination
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<ArticleDTO> getAllArticles(int page, int size, String sortBy, String sortDirection) {
        log.debug("Récupération des articles avec pagination - Page: {}, Size: {}", page, size);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Article> articlePage = articleRepository.findAll(pageable);

        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages()
        );
    }

    /**
     * Recherche d'articles avec critères
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<ArticleDTO> searchArticles(SearchCriteriaDTO criteria) {
        log.debug("Recherche d'articles avec critères");

        Sort sort = Sort.by(Sort.Direction.fromString(criteria.getSortDirection()), criteria.getSortBy());
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        Page<Article> articlePage = articleRepository.findWithCriteria(
                criteria.getQuery(),
                criteria.getCategorie(),
                criteria.getFournisseurId(),
                criteria.getActif(),
                pageable
        );

        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages()
        );
    }

    /**
     * Recherche textuelle d'articles
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<ArticleDTO> searchArticlesByText(String searchTerm, int page, int size) {
        log.debug("Recherche textuelle d'articles avec le terme: {}", searchTerm);

        Pageable pageable = PageRequest.of(page, size, Sort.by("designation"));
        Page<Article> articlePage = articleRepository.searchArticles(searchTerm, pageable);

        List<ArticleDTO> articleDTOs = articlePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                articleDTOs,
                articlePage.getNumber(),
                articlePage.getSize(),
                articlePage.getTotalElements(),
                articlePage.getTotalPages()
        );
    }

    // ===============================
    // GESTION DU STOCK ET ALERTES
    // ===============================

    /**
     * Récupérer les articles avec stock critique
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesWithCriticalStock() {
        log.debug("Récupération des articles avec stock critique");

        List<Article> articles = articleRepository.findArticlesWithCriticalStock();
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les articles avec stock faible
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesWithLowStock() {
        log.debug("Récupération des articles avec stock faible");

        List<Article> articles = articleRepository.findArticlesWithLowStock();
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les articles sans stock
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesWithoutStock() {
        log.debug("Récupération des articles sans stock");

        List<Article> articles = articleRepository.findArticlesWithoutStock();
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les articles avec stock excessif
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesWithExcessiveStock() {
        log.debug("Récupération des articles avec stock excessif");

        List<Article> articles = articleRepository.findArticlesWithExcessiveStock();
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // STATISTIQUES ET ANALYSES
    // ===============================

    /**
     * Récupérer les top articles par valeur de stock
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getTopArticlesByStockValue(int limit) {
        log.debug("Récupération du top {} articles par valeur de stock", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Article> articles = articleRepository.findTopArticlesByStockValue(pageable);
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les statistiques du stock par catégorie
     */
    @Transactional(readOnly = true)
    public List<Object[]> getStockStatsByCategory() {
        log.debug("Récupération des statistiques par catégorie");

        return articleRepository.findStockStatsByCategory();
    }

    /**
     * Récupérer les articles les plus consommés
     */
    @Transactional(readOnly = true)
    public List<Object[]> getMostConsumedArticles(int days, int limit) {
        log.debug("Récupération des articles les plus consommés sur {} jours", days);

        Pageable pageable = PageRequest.of(0, limit);
        return articleRepository.findMostConsumedArticles(days, pageable);
    }

    /**
     * Récupérer le nombre d'articles par statut
     */
    @Transactional(readOnly = true)
    public Long countArticlesByStatus(Boolean actif) {
        log.debug("Comptage des articles par statut: {}", actif);

        return articleRepository.countByActif(actif);
    }

    /**
     * Récupérer le nombre d'articles par catégorie
     */
    @Transactional(readOnly = true)
    public List<Object[]> countArticlesByCategory() {
        log.debug("Comptage des articles par catégorie");

        return articleRepository.countByCategory();
    }

    // ===============================
    // GESTION PAR CATÉGORIE
    // ===============================

    /**
     * Récupérer les articles par catégorie
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesByCategory(String categorie) {
        log.debug("Récupération des articles de la catégorie: {}", categorie);

        List<Article> articles = articleRepository.findByCategorie(categorie);
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer toutes les catégories d'articles
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        log.debug("Récupération de toutes les catégories");

        return articleRepository.findAll().stream()
                .map(Article::getCategorie)
                .filter(categorie -> categorie != null && !categorie.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // ===============================
    // GESTION PAR FOURNISSEUR
    // ===============================

    /**
     * Récupérer les articles d'un fournisseur
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getArticlesByFournisseur(Long fournisseurId) {
        log.debug("Récupération des articles du fournisseur ID: {}", fournisseurId);

        List<Article> articles = articleRepository.findByFournisseurPrincipalId(fournisseurId);
        return articles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // VALIDATION ET VÉRIFICATION
    // ===============================

    /**
     * Vérifier si un code article existe
     */
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return articleRepository.existsByCode(code);
    }

    /**
     * Vérifier si un code article existe (en excluant un ID)
     */
    @Transactional(readOnly = true)
    public boolean existsByCodeAndIdNot(String code, Long id) {
        return articleRepository.existsByCodeAndIdNot(code, id);
    }

    // ===============================
    // MÉTHODES PRIVÉES - VALIDATION
    // ===============================

    private void validateArticleForCreation(ArticleDTO articleDTO) {
        log.debug("Validation de l'article pour création: {}", articleDTO.getCode());

        if (articleDTO.getCode() == null || articleDTO.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Le code article est obligatoire");
        }

        if (existsByCode(articleDTO.getCode())) {
            throw new IllegalArgumentException("Un article avec ce code existe déjà: " + articleDTO.getCode());
        }

        if (articleDTO.getDesignation() == null || articleDTO.getDesignation().trim().isEmpty()) {
            throw new IllegalArgumentException("La désignation est obligatoire");
        }

        if (articleDTO.getPrixUnitaire() != null && articleDTO.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit être positif");
        }

        if (articleDTO.getStockMin() != null && articleDTO.getStockMin() < 0) {
            throw new IllegalArgumentException("Le stock minimum ne peut pas être négatif");
        }

        if (articleDTO.getStockMax() != null && articleDTO.getStockMax() < 0) {
            throw new IllegalArgumentException("Le stock maximum ne peut pas être négatif");
        }

        if (articleDTO.getStockMin() != null && articleDTO.getStockMax() != null &&
                articleDTO.getStockMin() > articleDTO.getStockMax()) {
            throw new IllegalArgumentException("Le stock minimum ne peut pas être supérieur au stock maximum");
        }
    }

    private void validateArticleForUpdate(Long id, ArticleDTO articleDTO) {
        log.debug("Validation de l'article pour mise à jour: ID={}", id);

        if (articleDTO.getCode() != null && existsByCodeAndIdNot(articleDTO.getCode(), id)) {
            throw new IllegalArgumentException("Un autre article avec ce code existe déjà: " + articleDTO.getCode());
        }

        if (articleDTO.getDesignation() != null && articleDTO.getDesignation().trim().isEmpty()) {
            throw new IllegalArgumentException("La désignation ne peut pas être vide");
        }

        if (articleDTO.getPrixUnitaire() != null && articleDTO.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit être positif");
        }

        if (articleDTO.getStockMin() != null && articleDTO.getStockMin() < 0) {
            throw new IllegalArgumentException("Le stock minimum ne peut pas être négatif");
        }

        if (articleDTO.getStockMax() != null && articleDTO.getStockMax() < 0) {
            throw new IllegalArgumentException("Le stock maximum ne peut pas être négatif");
        }

        if (articleDTO.getStockMin() != null && articleDTO.getStockMax() != null &&
                articleDTO.getStockMin() > articleDTO.getStockMax()) {
            throw new IllegalArgumentException("Le stock minimum ne peut pas être supérieur au stock maximum");
        }
    }

    private void validateArticleForDeletion(Article article) {
        log.debug("Validation de l'article pour suppression: {}", article.getCode());

        // Vérifier s'il y a du stock
        Optional<Stock> stock = stockRepository.findByArticleId(article.getId());
        if (stock.isPresent() && stock.get().getQuantiteActuelle() > 0) {
            throw new IllegalStateException("Impossible de supprimer un article avec du stock en cours");
        }

        // Vérifier s'il y a des mouvements récents (30 derniers jours)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        // Cette vérification pourrait être ajoutée si nécessaire
    }

    // ===============================
    // MÉTHODES PRIVÉES - UTILITAIRES
    // ===============================

    private void updateArticleFields(Article existingArticle, ArticleDTO articleDTO) {
        if (articleDTO.getCode() != null) {
            existingArticle.setCode(articleDTO.getCode());
        }
        if (articleDTO.getDesignation() != null) {
            existingArticle.setDesignation(articleDTO.getDesignation());
        }
        if (articleDTO.getDescription() != null) {
            existingArticle.setDescription(articleDTO.getDescription());
        }
        if (articleDTO.getCategorie() != null) {
            existingArticle.setCategorie(articleDTO.getCategorie());
        }
        if (articleDTO.getUnite() != null) {
            existingArticle.setUnite(articleDTO.getUnite());
        }
        if (articleDTO.getPrixUnitaire() != null) {
            existingArticle.setPrixUnitaire(articleDTO.getPrixUnitaire());
        }
        if (articleDTO.getStockMin() != null) {
            existingArticle.setStockMin(articleDTO.getStockMin());
        }
        if (articleDTO.getStockMax() != null) {
            existingArticle.setStockMax(articleDTO.getStockMax());
        }
        if (articleDTO.getActif() != null) {
            existingArticle.setActif(articleDTO.getActif());
        }
    }

    private void updateFournisseurPrincipal(Article article, Long fournisseurId) {
        if (fournisseurId != null) {
            Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                    .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + fournisseurId));
            article.setFournisseurPrincipal(fournisseur);
        } else {
            article.setFournisseurPrincipal(null);
        }
    }

    // FIXED: Enhanced createInitialStock with better error handling
    private void createInitialStock(Article article) {
        try {
            log.debug("Création du stock initial pour l'article: {}", article.getCode());

            // Vérifier si le stock existe déjà
            Optional<Stock> existingStock = stockRepository.findByArticleId(article.getId());
            if (existingStock.isPresent()) {
                log.warn("Stock déjà existant pour l'article: {}", article.getCode());
                return;
            }

            Stock stock = new Stock();
            stock.setArticle(article);
            stock.setQuantiteActuelle(0);
            stock.setQuantiteReservee(0);
            stock.setQuantiteDisponible(0);

            if (article.getPrixUnitaire() != null) {
                stock.setPrixMoyenPondere(article.getPrixUnitaire());
                stock.setValeurStock(BigDecimal.ZERO);
            } else {
                stock.setPrixMoyenPondere(BigDecimal.ZERO);
                stock.setValeurStock(BigDecimal.ZERO);
            }

            stockRepository.save(stock);
            log.debug("Stock initial créé avec succès pour l'article: {}", article.getCode());

        } catch (Exception e) {
            log.error("Erreur lors de la création du stock initial pour l'article {}: {}",
                    article.getCode(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création du stock initial", e);
        }
    }

    // FIXED: Enhanced convertToDTO with better null handling
    private ArticleDTO convertToDTO(Article article) {
        try {
            log.trace("Conversion de l'article vers DTO: {}", article.getCode());

            ArticleDTO dto = new ArticleDTO();

            // Mapping manuel pour éviter les problèmes de ModelMapper
            dto.setId(article.getId());
            dto.setCode(article.getCode());
            dto.setDesignation(article.getDesignation());
            dto.setDescription(article.getDescription());
            dto.setCategorie(article.getCategorie());
            dto.setUnite(article.getUnite());
            dto.setPrixUnitaire(article.getPrixUnitaire());
            dto.setStockMin(article.getStockMin());
            dto.setStockMax(article.getStockMax());
            dto.setActif(article.getActif());
            dto.setDateCreation(article.getDateCreation());
            dto.setDateModification(article.getDateModification());

            // Ajouter les informations du fournisseur principal
            if (article.getFournisseurPrincipal() != null) {
                dto.setFournisseurPrincipalId(article.getFournisseurPrincipal().getId());
                dto.setFournisseurPrincipalNom(article.getFournisseurPrincipal().getNom());
            }

            // Ajouter les informations du stock (avec gestion des proxies lazy)
            if (article.getStock() != null) {
                try {
                    dto.setQuantiteActuelle(article.getStock().getQuantiteActuelle());
                    dto.setValeurStock(article.getStock().getValeurStock());
                    dto.setDerniereEntree(article.getStock().getDerniereEntree());
                    dto.setDerniereSortie(article.getStock().getDerniereSortie());
                } catch (Exception e) {
                    // En cas d'erreur avec le lazy loading du stock, continuer sans les infos de stock
                    log.warn("Impossible de charger les informations de stock pour l'article {}: {}",
                            article.getCode(), e.getMessage());
                }
            }

            return dto;

        } catch (Exception e) {
            log.error("Erreur lors de la conversion de l'article vers DTO: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la conversion de l'article", e);
        }
    }
}
