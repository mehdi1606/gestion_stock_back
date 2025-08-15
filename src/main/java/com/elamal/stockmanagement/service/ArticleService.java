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
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private  ArticleRepository articleRepository;
    @Autowired
    private  FournisseurRepository fournisseurRepository;
    @Autowired
    private  StockRepository stockRepository;
    @Autowired
    private  ModelMapper modelMapper;

    // ===============================
    // OPÉRATIONS CRUD DE BASE
    // ===============================

    /**
     * Créer un nouvel article
     */
    public ArticleDTO createArticle(ArticleDTO articleDTO) {

        // Validation métier
        validateArticleForCreation(articleDTO);

        // Conversion DTO -> Entity
        Article article = modelMapper.map(articleDTO, Article.class);

        // Gestion du fournisseur principal
        if (articleDTO.getFournisseurPrincipalId() != null) {
            Fournisseur fournisseur = fournisseurRepository.findById(articleDTO.getFournisseurPrincipalId())
                    .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + articleDTO.getFournisseurPrincipalId()));
            article.setFournisseurPrincipal(fournisseur);
        }

        // Sauvegarde de l'article
        Article savedArticle = articleRepository.save(article);

        // Création du stock initial (quantité 0)
        createInitialStock(savedArticle);


        return convertToDTO(savedArticle);
    }

    /**
     * Mettre à jour un article existant
     */
    public ArticleDTO updateArticle(Long id, ArticleDTO articleDTO) {

        Article existingArticle = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

        // Validation métier pour mise à jour
        validateArticleForUpdate(id, articleDTO);

        // Mise à jour des champs
        updateArticleFields(existingArticle, articleDTO);

        // Gestion du changement de fournisseur principal
        updateFournisseurPrincipal(existingArticle, articleDTO.getFournisseurPrincipalId());

        Article updatedArticle = articleRepository.save(existingArticle);


        return convertToDTO(updatedArticle);
    }

    /**
     * Supprimer un article (suppression logique)
     */
    public void deleteArticle(Long id) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

        // Vérifications avant suppression
        validateArticleForDeletion(article);

        // Suppression logique (désactivation)
        article.setActif(false);
        articleRepository.save(article);

    }

    /**
     * Réactiver un article
     */
    public ArticleDTO reactivateArticle(Long id) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

        article.setActif(true);
        Article reactivatedArticle = articleRepository.save(article);


        return convertToDTO(reactivatedArticle);
    }

    // ===============================
    // OPÉRATIONS DE LECTURE
    // ===============================

    /**
     * Récupérer un article par ID
     */
    @Transactional(readOnly = true)
    public ArticleDTO getArticleById(Long id) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + id));

        return convertToDTO(article);
    }

    /**
     * Récupérer un article par code
     */
    @Transactional(readOnly = true)
    public ArticleDTO getArticleByCode(String code) {

        Article article = articleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec le code: " + code));

        return convertToDTO(article);
    }

    /**
     * Récupérer tous les articles actifs
     */
    @Transactional(readOnly = true)
    public List<ArticleDTO> getAllActiveArticles() {

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

        return articleRepository.findStockStatsByCategory();
    }

    /**
     * Récupérer les articles les plus consommés
     */
    @Transactional(readOnly = true)
    public List<Object[]> getMostConsumedArticles(int days, int limit) {

        Pageable pageable = PageRequest.of(0, limit);
        return articleRepository.findMostConsumedArticles(days, pageable);
    }

    /**
     * Récupérer le nombre d'articles par statut
     */
    @Transactional(readOnly = true)
    public Long countArticlesByStatus(Boolean actif) {

        return articleRepository.countByActif(actif);
    }

    /**
     * Récupérer le nombre d'articles par catégorie
     */
    @Transactional(readOnly = true)
    public List<Object[]> countArticlesByCategory() {

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

    private void createInitialStock(Article article) {
        Stock stock = new Stock();
        stock.setArticle(article);
        stock.setQuantiteActuelle(0);
        stock.setQuantiteReservee(0);
        stock.setQuantiteDisponible(0);

        if (article.getPrixUnitaire() != null) {
            stock.setPrixMoyenPondere(article.getPrixUnitaire());
            stock.setValeurStock(BigDecimal.ZERO);
        }

        stockRepository.save(stock);
        log.debug("Stock initial créé pour l'article: {}", article.getCode());
    }

    private ArticleDTO convertToDTO(Article article) {
        ArticleDTO dto = modelMapper.map(article, ArticleDTO.class);

        // Ajouter les informations du fournisseur principal
        if (article.getFournisseurPrincipal() != null) {
            dto.setFournisseurPrincipalId(article.getFournisseurPrincipal().getId());
            dto.setFournisseurPrincipalNom(article.getFournisseurPrincipal().getNom());
        }

        // Ajouter les informations du stock
        if (article.getStock() != null) {
            dto.setQuantiteActuelle(article.getStock().getQuantiteActuelle());
            dto.setValeurStock(article.getStock().getValeurStock());
            dto.setDerniereEntree(article.getStock().getDerniereEntree());
            dto.setDerniereSortie(article.getStock().getDerniereSortie());
        }

        return dto;
    }
}
