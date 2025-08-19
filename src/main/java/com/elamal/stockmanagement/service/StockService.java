package com.elamal.stockmanagement.service;

import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.entity.*;
import com.elamal.stockmanagement.repository.*;
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
public class StockService {

    private final StockRepository stockRepository;
    private final ArticleRepository articleRepository;
    private final StockMovementRepository stockMovementRepository;
    private  ModelMapper modelMapper;

    // ===============================
    // CONSULTATION DU STOCK
    // ===============================

    /**
     * Récupérer le stock d'un article par ID
     */
    @Transactional(readOnly = true)
    public StockDTO getStockByArticleId(Long articleId) {
        log.debug("Récupération du stock pour l'article ID: {}", articleId);

        Stock stock = stockRepository.findByArticleId(articleId)
                .orElseThrow(() -> new RuntimeException("Stock introuvable pour l'article ID: " + articleId));

        return convertToDTO(stock);
    }

    /**
     * Récupérer tous les stocks avec pagination
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<StockDTO> getAllStocks(int page, int size, String sortBy, String sortDirection) {
        log.debug("Récupération des stocks - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDirection);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Stock> stockPage = stockRepository.findAll(pageable);

        List<StockDTO> stockDTOs = stockPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                stockDTOs,
                stockPage.getNumber(),
                stockPage.getSize(),
                stockPage.getTotalElements(),
                stockPage.getTotalPages()
        );
    }

    /**
     * Recherche de stocks avec critères
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<StockDTO> searchStocks(SearchCriteriaDTO criteria) {
        log.debug("Recherche de stocks avec critères: {}", criteria);

        Sort sort = Sort.by(Sort.Direction.fromString(criteria.getSortDirection()), criteria.getSortBy());
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        Page<Stock> stockPage = stockRepository.findWithCriteria(
                criteria.getCategorie(),
                null, // minQuantite
                null, // maxQuantite
                null, // minValeur
                null, // maxValeur
                criteria.getStockFaible(),
                criteria.getStockCritique(),
                pageable
        );

        List<StockDTO> stockDTOs = stockPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                stockDTOs,
                stockPage.getNumber(),
                stockPage.getSize(),
                stockPage.getTotalElements(),
                stockPage.getTotalPages()
        );
    }

    /**
     * Recherche textuelle dans les stocks
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<StockDTO> searchStocksByText(String searchTerm, int page, int size) {
        log.debug("Recherche textuelle dans les stocks: {}", searchTerm);

        Pageable pageable = PageRequest.of(page, size, Sort.by("article.designation"));
        Page<Stock> stockPage = stockRepository.searchStocks(searchTerm, pageable);

        List<StockDTO> stockDTOs = stockPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                stockDTOs,
                stockPage.getNumber(),
                stockPage.getSize(),
                stockPage.getTotalElements(),
                stockPage.getTotalPages()
        );
    }

    // ===============================
    // ALERTES ET NIVEAUX DE STOCK
    // ===============================

    /**
     * Récupérer les stocks critiques
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getCriticalStocks() {
        log.debug("Récupération des stocks critiques");

        List<Stock> stocks = stockRepository.findCriticalStocks();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks faibles
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getLowStocks() {
        log.debug("Récupération des stocks faibles");

        List<Stock> stocks = stockRepository.findLowStocks();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks vides
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getEmptyStocks() {
        log.debug("Récupération des stocks vides");

        List<Stock> stocks = stockRepository.findEmptyStocks();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks excessifs
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getExcessiveStocks() {
        log.debug("Récupération des stocks excessifs");

        List<Stock> stocks = stockRepository.findExcessiveStocks();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks nécessitant un réapprovisionnement
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksNeedingReorder() {
        log.debug("Récupération des stocks nécessitant un réapprovisionnement");

        List<Stock> stocks = stockRepository.findStocksNeedingReorder();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks nécessitant attention
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksRequiringAttention() {
        log.debug("Récupération des stocks nécessitant attention");

        List<Stock> stocks = stockRepository.findStocksRequiringAttention();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // STATISTIQUES ET VALEURS
    // ===============================

    /**
     * Calculer la valeur totale du stock
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalStockValue() {
        log.debug("Calcul de la valeur totale du stock");

        return stockRepository.getTotalStockValue();
    }

    /**
     * Récupérer les statistiques générales du stock
     */
    @Transactional(readOnly = true)
    public Object getGeneralStockStatistics() {
        log.debug("Récupération des statistiques générales du stock");

        return stockRepository.getGeneralStockStatistics();
    }

    /**
     * Récupérer les statistiques détaillées par statut
     */
    @Transactional(readOnly = true)
    public Object getDetailedStockStatusCount() {
        log.debug("Récupération du comptage détaillé par statut");

        return stockRepository.getDetailedStockStatusCount();
    }

    /**
     * Récupérer la valeur du stock par catégorie
     */
    @Transactional(readOnly = true)
    public List<Object[]> getStockValueByCategory() {
        log.debug("Récupération de la valeur du stock par catégorie");

        return stockRepository.getStockValueByCategory();
    }

    /**
     * Récupérer les statistiques détaillées par catégorie
     */
    @Transactional(readOnly = true)
    public List<Object[]> getDetailedStockStatsByCategory() {
        log.debug("Récupération des statistiques détaillées par catégorie");

        return stockRepository.getDetailedStockStatsByCategory();
    }

    // ===============================
    // TOP RANKINGS ET ANALYSES
    // ===============================

    /**
     * Récupérer le top des stocks par valeur
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getTopStocksByValue(int limit) {
        log.debug("Récupération du top {} stocks par valeur", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Stock> stocks = stockRepository.findTopStocksByValue(pageable);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer le top des stocks par quantité
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getTopStocksByQuantity(int limit) {
        log.debug("Récupération du top {} stocks par quantité", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Stock> stocks = stockRepository.findTopStocksByQuantity(pageable);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les articles les moins chers en stock
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getCheapestStockedArticles(int limit) {
        log.debug("Récupération des {} articles les moins chers en stock", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Stock> stocks = stockRepository.findCheapestStockedArticles(pageable);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les articles les plus chers en stock
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getMostExpensiveStockedArticles(int limit) {
        log.debug("Récupération des {} articles les plus chers en stock", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Stock> stocks = stockRepository.findMostExpensiveStockedArticles(pageable);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // ANALYSE DE ROTATION
    // ===============================

    /**
     * Récupérer les stocks à rotation rapide
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getFastMovingStocks(int days) {
        log.debug("Récupération des stocks à rotation rapide (derniers {} jours)", days);

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Stock> stocks = stockRepository.findFastMovingStocks(since);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks à rotation lente
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getSlowMovingStocks(int days) {
        log.debug("Récupération des stocks à rotation lente (pas de sortie depuis {} jours)", days);

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Stock> stocks = stockRepository.findSlowMovingStocks(since);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks dormants
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getDormantStocks(int days) {
        log.debug("Récupération des stocks dormants (aucune sortie depuis {} jours)", days);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<Stock> stocks = stockRepository.findDormantStocks(cutoffDate);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // GESTION DES RÉSERVATIONS
    // ===============================

    /**
     * Récupérer les stocks avec réservations
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksWithReservations() {
        log.debug("Récupération des stocks avec réservations");

        List<Stock> stocks = stockRepository.findStocksWithReservations();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks sur-réservés
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getOverReservedStocks() {
        log.debug("Récupération des stocks sur-réservés");

        List<Stock> stocks = stockRepository.findOverReservedStocks();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour la quantité réservée d'un article
     */
    public void updateQuantiteReservee(Long articleId, Integer quantiteReservee) {
        log.info("Mise à jour de la quantité réservée pour l'article ID: {} -> {}", articleId, quantiteReservee);

        // Validation
        if (quantiteReservee < 0) {
            throw new IllegalArgumentException("La quantité réservée ne peut pas être négative");
        }

        // Vérification de l'existence du stock
        Stock stock = stockRepository.findByArticleId(articleId)
                .orElseThrow(() -> new RuntimeException("Stock introuvable pour l'article ID: " + articleId));

        // Vérification que la quantité réservée ne dépasse pas le stock disponible
        if (quantiteReservee > stock.getQuantiteActuelle()) {
            throw new IllegalArgumentException(
                    String.format("Impossible de réserver %d unités. Stock disponible: %d",
                            quantiteReservee, stock.getQuantiteActuelle()));
        }

        // Mise à jour
        int updatedRows = stockRepository.updateQuantiteReservee(articleId, quantiteReservee);

        if (updatedRows > 0) {
            log.info("Quantité réservée mise à jour avec succès pour l'article ID: {}", articleId);
        } else {
            throw new RuntimeException("Échec de la mise à jour de la quantité réservée");
        }
    }

    /**
     * Remettre à zéro toutes les réservations
     */
    public void resetAllReservations() {
        log.info("Remise à zéro de toutes les réservations");

        int updatedRows = stockRepository.resetAllReservations();

        log.info("Réservations remises à zéro pour {} articles", updatedRows);
    }

    // ===============================
    // INVENTAIRE ET ÉCARTS
    // ===============================

    /**
     * Récupérer les stocks avec écart d'inventaire
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksWithInventoryGap() {
        log.debug("Récupération des stocks avec écart d'inventaire");

        List<Stock> stocks = stockRepository.findStocksWithInventoryGap();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks avec écart positif (surplus)
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksWithPositiveInventoryGap() {
        log.debug("Récupération des stocks avec écart positif");

        List<Stock> stocks = stockRepository.findStocksWithPositiveInventoryGap();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks avec écart négatif (manquant)
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksWithNegativeInventoryGap() {
        log.debug("Récupération des stocks avec écart négatif");

        List<Stock> stocks = stockRepository.findStocksWithNegativeInventoryGap();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour les dates d'inventaire pour les stocks cohérents
     */
    public void updateInventoryDatesForConsistentStocks() {
        log.info("Mise à jour des dates d'inventaire pour les stocks cohérents");

        LocalDateTime now = LocalDateTime.now();
        int updatedRows = stockRepository.updateInventoryDatesForConsistentStocks(now);

        log.info("Dates d'inventaire mises à jour pour {} stocks", updatedRows);
    }

    // ===============================
    // FILTRES ET RECHERCHES SPÉCIALISÉES
    // ===============================

    /**
     * Récupérer les stocks par catégorie
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksByCategory(String categorie) {
        log.debug("Récupération des stocks de la catégorie: {}", categorie);

        List<Stock> stocks = stockRepository.findByArticleCategorie(categorie);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks par fournisseur
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksBySupplier(Long fournisseurId) {
        log.debug("Récupération des stocks du fournisseur ID: {}", fournisseurId);

        List<Stock> stocks = stockRepository.findByArticleFournisseurPrincipalId(fournisseurId);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer la valeur du stock par fournisseur
     */
    @Transactional(readOnly = true)
    public List<Object[]> getStockValueBySupplier() {
        log.debug("Récupération de la valeur du stock par fournisseur");

        return stockRepository.getStockValueBySupplier();
    }

    /**
     * Récupérer les stocks dans une fourchette de prix
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("Récupération des stocks avec prix entre {} et {}", minPrice, maxPrice);

        List<Stock> stocks = stockRepository.findByPrixMoyenPondereBetween(minPrice, maxPrice);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks dans une fourchette de valeur
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksByValueRange(BigDecimal minValue, BigDecimal maxValue) {
        log.debug("Récupération des stocks avec valeur entre {} et {}", minValue, maxValue);

        List<Stock> stocks = stockRepository.findByValeurStockBetween(minValue, maxValue);
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ===============================
    // UTILITAIRES ET MAINTENANCE
    // ===============================

    /**
     * Vérifier les stocks incohérents
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getInconsistentStocks() {
        log.debug("Vérification des stocks incohérents");

        List<Stock> stocks = stockRepository.findInconsistentStocks();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les stocks avec données manquantes
     */
    @Transactional(readOnly = true)
    public List<StockDTO> getStocksWithMissingData() {
        log.debug("Récupération des stocks avec données manquantes");

        List<Stock> stocks = stockRepository.findStocksWithMissingData();
        return stocks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les articles sans stock initialisé
     */
    @Transactional(readOnly = true)
    public List<Object[]> getArticlesWithoutStock() {
        log.debug("Récupération des articles sans stock initialisé");

        return stockRepository.findArticleIdsWithoutStock();
    }

    /**
     * Compter le nombre d'articles en stock
     */
    @Transactional(readOnly = true)
    public Long countArticlesInStock() {
        log.debug("Comptage des articles en stock");

        return stockRepository.countArticlesInStock();
    }

    /**
     * Initialiser le stock pour un article
     */
    public StockDTO initializeStockForArticle(Long articleId) {
        log.info("Initialisation du stock pour l'article ID: {}", articleId);

        // Vérifier si le stock existe déjà
        Optional<Stock> existingStock = stockRepository.findByArticleId(articleId);
        if (existingStock.isPresent()) {
            throw new IllegalArgumentException("Le stock existe déjà pour cet article");
        }

        // Récupérer l'article
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + articleId));

        // Créer le stock initial
        Stock newStock = new Stock();
        newStock.setArticle(article);
        newStock.setQuantiteActuelle(0);
        newStock.setQuantiteReservee(0);
        newStock.setQuantiteDisponible(0);

        if (article.getPrixUnitaire() != null) {
            newStock.setPrixMoyenPondere(article.getPrixUnitaire());
        }

        newStock.setValeurStock(BigDecimal.ZERO);

        Stock savedStock = stockRepository.save(newStock);


        return convertToDTO(savedStock);
    }

    // ===============================
    // MÉTHODES PRIVÉES - CONVERSION
    // ===============================

    private StockDTO convertToDTO(Stock stock) {
        StockDTO dto = new StockDTO();

        // Mapping des champs de base
        dto.setId(stock.getId());
        dto.setArticleId(stock.getArticle().getId());
        dto.setQuantiteActuelle(stock.getQuantiteActuelle());
        dto.setQuantiteReservee(stock.getQuantiteReservee());
        dto.setQuantiteDisponible(stock.getQuantiteDisponible());
        dto.setPrixMoyenPondere(stock.getPrixMoyenPondere());
        dto.setValeurStock(stock.getValeurStock());
        dto.setDerniereEntree(stock.getDerniereEntree());
        dto.setDerniereSortie(stock.getDerniereSortie());
        dto.setDateDernierInventaire(stock.getDateDernierInventaire());
        dto.setQuantiteInventaire(stock.getQuantiteInventaire());
        dto.setEcartInventaire(stock.getEcartInventaire());
        dto.setDateModification(stock.getDateModification());

        // Enrichissement avec les informations de l'article
        if (stock.getArticle() != null) {
            Article article = stock.getArticle();
            dto.setArticleNom(article.getNom());
            dto.setArticleCategorie(article.getCategorie());
            dto.setArticleUnite(article.getUnite());
            dto.setArticleStockMin(article.getStockMin());
            dto.setArticleStockMax(article.getStockMax());
        }

        // Calcul des champs automatiques
        dto.calculateFields();

        return dto;
    }
}
