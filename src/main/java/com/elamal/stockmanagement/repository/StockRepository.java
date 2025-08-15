package com.elamal.stockmanagement.repository;

import com.elamal.stockmanagement.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // ===============================
    // REQUÊTES DE BASE
    // ===============================

    // Stock par article (requête la plus utilisée)
    Optional<Stock> findByArticleId(Long articleId);

    // Vérifier l'existence du stock pour un article
    boolean existsByArticleId(Long articleId);

    // Stocks avec quantité supérieure à un seuil
    List<Stock> findByQuantiteActuelleGreaterThan(Integer quantite);

    // Stocks avec quantité égale à zéro
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE s.quantiteActuelle = 0 AND a.actif = true")
    List<Stock> findEmptyStocks();

    // Tous les stocks avec pagination
    Page<Stock> findAll(Pageable pageable);

    // ===============================
    // ALERTES ET NIVEAUX DE STOCK
    // ===============================

    // Stocks critiques (quantité <= stock minimum)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.quantiteActuelle <= a.stockMin AND a.stockMin IS NOT NULL AND a.actif = true " +
            "ORDER BY s.quantiteActuelle ASC")
    List<Stock> findCriticalStocks();

    // Stocks faibles (quantité <= stock minimum * 1.5)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.quantiteActuelle <= (a.stockMin * 1.5) AND a.stockMin IS NOT NULL AND " +
            "s.quantiteActuelle > a.stockMin AND a.actif = true " +
            "ORDER BY s.quantiteActuelle ASC")
    List<Stock> findLowStocks();

    // Stocks excessifs (quantité > stock maximum)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.quantiteActuelle > a.stockMax AND a.stockMax IS NOT NULL AND a.actif = true " +
            "ORDER BY s.quantiteActuelle DESC")
    List<Stock> findExcessiveStocks();

    // Stocks nécessitant un réapprovisionnement urgent
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "((s.quantiteActuelle <= a.stockMin AND a.stockMin IS NOT NULL) OR s.quantiteActuelle = 0) " +
            "AND a.actif = true ORDER BY " +
            "CASE WHEN s.quantiteActuelle = 0 THEN 1 " +
            "     WHEN s.quantiteActuelle <= a.stockMin THEN 2 " +
            "     ELSE 3 END, s.quantiteActuelle ASC")
    List<Stock> findStocksNeedingReorder();

    // Stocks avec problèmes multiples (quantité réservée > disponible, écarts, etc.)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE a.actif = true AND (" +
            "s.quantiteActuelle <= COALESCE(a.stockMin, 0) OR " +
            "s.quantiteActuelle = 0 OR " +
            "(s.ecartInventaire IS NOT NULL AND ABS(s.ecartInventaire) > 0) OR " +
            "s.quantiteReservee > s.quantiteActuelle OR " +
            "(s.quantiteActuelle > COALESCE(a.stockMax, 999999) AND a.stockMax IS NOT NULL)" +
            ") ORDER BY " +
            "CASE WHEN s.quantiteActuelle = 0 THEN 1 " +
            "     WHEN s.quantiteActuelle <= COALESCE(a.stockMin, 0) THEN 2 " +
            "     WHEN s.quantiteReservee > s.quantiteActuelle THEN 3 " +
            "     ELSE 4 END")
    List<Stock> findStocksRequiringAttention();

    // ===============================
    // RECHERCHE ET FILTRAGE
    // ===============================

    // Stocks par catégorie d'article
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "a.categorie = :categorie AND a.actif = true " +
            "ORDER BY s.quantiteActuelle DESC")
    List<Stock> findByArticleCategorie(@Param("categorie") String categorie);

    // Recherche multicritères avec pagination
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "(:categorie IS NULL OR a.categorie = :categorie) AND " +
            "(:minQuantite IS NULL OR s.quantiteActuelle >= :minQuantite) AND " +
            "(:maxQuantite IS NULL OR s.quantiteActuelle <= :maxQuantite) AND " +
            "(:minValeur IS NULL OR s.valeurStock >= :minValeur) AND " +
            "(:maxValeur IS NULL OR s.valeurStock <= :maxValeur) AND " +
            "(:stockFaible IS NULL OR " +
            "  (:stockFaible = true AND s.quantiteActuelle <= COALESCE(a.stockMin, 0)) OR " +
            "  (:stockFaible = false AND s.quantiteActuelle > COALESCE(a.stockMin, 0))) AND " +
            "(:stockVide IS NULL OR " +
            "  (:stockVide = true AND s.quantiteActuelle = 0) OR " +
            "  (:stockVide = false AND s.quantiteActuelle > 0)) AND " +
            "a.actif = true")
    Page<Stock> findWithCriteria(@Param("categorie") String categorie,
                                 @Param("minQuantite") Integer minQuantite,
                                 @Param("maxQuantite") Integer maxQuantite,
                                 @Param("minValeur") BigDecimal minValeur,
                                 @Param("maxValeur") BigDecimal maxValeur,
                                 @Param("stockFaible") Boolean stockFaible,
                                 @Param("stockVide") Boolean stockVide,
                                 Pageable pageable);

    // Recherche textuelle dans les articles du stock
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "(LOWER(a.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            " LOWER(a.designation) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            " LOWER(a.categorie) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            " LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND a.actif = true " +
            "ORDER BY " +
            "CASE WHEN LOWER(a.code) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 1 " +
            "     WHEN LOWER(a.designation) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 2 " +
            "     ELSE 3 END, a.designation")
    Page<Stock> searchStocks(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ===============================
    // STATISTIQUES ET VALEURS
    // ===============================

    // Valeur totale du stock (tous articles actifs)
    @Query("SELECT COALESCE(SUM(s.valeurStock), 0) FROM Stock s JOIN s.article a WHERE a.actif = true")
    BigDecimal getTotalStockValue();

    // Valeur totale du stock par catégorie
    @Query("SELECT a.categorie, COALESCE(SUM(s.valeurStock), 0), COUNT(s), SUM(s.quantiteActuelle) " +
            "FROM Stock s JOIN s.article a WHERE a.actif = true " +
            "GROUP BY a.categorie ORDER BY SUM(s.valeurStock) DESC")
    List<Object[]> getStockValueByCategory();

    // Nombre d'articles en stock par catégorie
    @Query("SELECT a.categorie, " +
            "COUNT(s) as totalArticles, " +
            "COUNT(CASE WHEN s.quantiteActuelle > 0 THEN 1 END) as articlesEnStock, " +
            "COUNT(CASE WHEN s.quantiteActuelle = 0 THEN 1 END) as articlesVides, " +
            "SUM(s.quantiteActuelle) as quantiteTotale, " +
            "COALESCE(SUM(s.valeurStock), 0) as valeurTotale " +
            "FROM Stock s JOIN s.article a WHERE a.actif = true " +
            "GROUP BY a.categorie ORDER BY a.categorie")
    List<Object[]> getDetailedStockStatsByCategory();

    // Statistiques générales du stock
    @Query("SELECT " +
            "COUNT(s) as totalArticles, " +
            "COUNT(CASE WHEN s.quantiteActuelle > 0 THEN 1 END) as articlesEnStock, " +
            "COUNT(CASE WHEN s.quantiteActuelle = 0 THEN 1 END) as articlesVides, " +
            "COUNT(CASE WHEN s.quantiteActuelle <= COALESCE(a.stockMin, 0) AND a.stockMin IS NOT NULL THEN 1 END) as articlesCritiques, " +
            "COUNT(CASE WHEN s.quantiteActuelle > COALESCE(a.stockMax, 999999) AND a.stockMax IS NOT NULL THEN 1 END) as articlesExcessifs, " +
            "COALESCE(SUM(s.valeurStock), 0) as valeurTotale, " +
            "COALESCE(SUM(s.quantiteActuelle), 0) as quantiteTotale, " +
            "COALESCE(SUM(s.quantiteReservee), 0) as quantiteReserveeTotale " +
            "FROM Stock s JOIN s.article a WHERE a.actif = true")
    Object getGeneralStockStatistics();

    // Compter stocks par statut avec détail
    @Query("SELECT " +
            "COUNT(CASE WHEN s.quantiteActuelle = 0 THEN 1 END) as stocksVides, " +
            "COUNT(CASE WHEN s.quantiteActuelle > 0 AND s.quantiteActuelle <= COALESCE(a.stockMin, 0) AND a.stockMin IS NOT NULL THEN 1 END) as stocksCritiques, " +
            "COUNT(CASE WHEN s.quantiteActuelle > COALESCE(a.stockMin, 0) AND s.quantiteActuelle <= (COALESCE(a.stockMin, 0) * 1.5) AND a.stockMin IS NOT NULL THEN 1 END) as stocksFaibles, " +
            "COUNT(CASE WHEN s.quantiteActuelle > COALESCE(a.stockMax, 999999) AND a.stockMax IS NOT NULL THEN 1 END) as stocksExcessifs, " +
            "COUNT(CASE WHEN s.quantiteActuelle > COALESCE(a.stockMin, 0) AND s.quantiteActuelle <= COALESCE(a.stockMax, 999999) THEN 1 END) as stocksNormaux, " +
            "COUNT(CASE WHEN s.ecartInventaire IS NOT NULL AND s.ecartInventaire != 0 THEN 1 END) as stocksAvecEcart, " +
            "COUNT(CASE WHEN s.quantiteReservee > 0 THEN 1 END) as stocksAvecReservation " +
            "FROM Stock s JOIN s.article a WHERE a.actif = true")
    Object getDetailedStockStatusCount();

    // ===============================
    // TOP RANKINGS ET ANALYSES
    // ===============================

    // Top articles par valeur de stock
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE a.actif = true AND s.valeurStock > 0 " +
            "ORDER BY s.valeurStock DESC")
    List<Stock> findTopStocksByValue(Pageable pageable);

    // Top articles par quantité
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE a.actif = true AND s.quantiteActuelle > 0 " +
            "ORDER BY s.quantiteActuelle DESC")
    List<Stock> findTopStocksByQuantity(Pageable pageable);

    // Articles les moins chers en stock
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE a.actif = true AND s.quantiteActuelle > 0 " +
            "ORDER BY s.prixMoyenPondere ASC")
    List<Stock> findCheapestStockedArticles(Pageable pageable);

    // Articles les plus chers en stock
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE a.actif = true AND s.quantiteActuelle > 0 " +
            "ORDER BY s.prixMoyenPondere DESC")
    List<Stock> findMostExpensiveStockedArticles(Pageable pageable);

    // ===============================
    // ANALYSE DE ROTATION
    // ===============================

    // Stocks avec rotation rapide (sorties récentes)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.derniereSortie >= :since AND a.actif = true " +
            "ORDER BY s.derniereSortie DESC")
    List<Stock> findFastMovingStocks(@Param("since") LocalDateTime since);

    // Stocks avec rotation lente (peu ou pas de sorties récentes)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "(s.derniereSortie IS NULL OR s.derniereSortie < :since) AND " +
            "s.quantiteActuelle > 0 AND a.actif = true " +
            "ORDER BY s.quantiteActuelle DESC, s.derniereSortie ASC NULLS FIRST")
    List<Stock> findSlowMovingStocks(@Param("since") LocalDateTime since);

    // Stocks dormants (jamais de sortie ou très anciennes)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "(s.derniereSortie IS NULL OR s.derniereSortie < :cutoffDate) AND " +
            "s.quantiteActuelle > 0 AND a.actif = true " +
            "ORDER BY s.valeurStock DESC")
    List<Stock> findDormantStocks(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ===============================
    // GESTION DES RÉSERVATIONS
    // ===============================

    // Stocks avec réservations
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.quantiteReservee > 0 AND a.actif = true " +
            "ORDER BY s.quantiteReservee DESC")
    List<Stock> findStocksWithReservations();

    // Stocks avec sur-réservation (réservé > disponible)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.quantiteReservee > s.quantiteActuelle AND a.actif = true " +
            "ORDER BY (s.quantiteReservee - s.quantiteActuelle) DESC")
    List<Stock> findOverReservedStocks();

    // ===============================
    // INVENTAIRE ET ÉCARTS
    // ===============================

    // Stocks avec écart d'inventaire
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.ecartInventaire IS NOT NULL AND s.ecartInventaire != 0 AND a.actif = true " +
            "ORDER BY ABS(s.ecartInventaire) DESC")
    List<Stock> findStocksWithInventoryGap();

    // Stocks avec écart d'inventaire positif (surplus)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.ecartInventaire > 0 AND a.actif = true " +
            "ORDER BY s.ecartInventaire DESC")
    List<Stock> findStocksWithPositiveInventoryGap();

    // Stocks avec écart d'inventaire négatif (manquant)
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.ecartInventaire < 0 AND a.actif = true " +
            "ORDER BY s.ecartInventaire ASC")
    List<Stock> findStocksWithNegativeInventoryGap();

    // ===============================
    // HISTORIQUE ET DATES
    // ===============================

    // Stocks modifiés récemment
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.dateModification >= :since AND a.actif = true " +
            "ORDER BY s.dateModification DESC")
    List<Stock> findRecentlyModifiedStocks(@Param("since") LocalDateTime since);

    // Stocks avec dernière entrée ancienne
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.derniereEntree IS NOT NULL AND s.derniereEntree < :since AND a.actif = true " +
            "ORDER BY s.derniereEntree ASC")
    List<Stock> findStocksWithOldLastEntry(@Param("since") LocalDateTime since);

    // Stocks jamais réapprovisionnés
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.derniereEntree IS NULL AND s.quantiteActuelle > 0 AND a.actif = true " +
            "ORDER BY s.quantiteActuelle DESC")
    List<Stock> findStocksNeverReplenished();

    // ===============================
    // FILTRES PAR FOURNISSEUR
    // ===============================

    // Stocks par fournisseur principal
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "a.fournisseurPrincipal.id = :fournisseurId AND a.actif = true " +
            "ORDER BY s.valeurStock DESC")
    List<Stock> findByArticleFournisseurPrincipalId(@Param("fournisseurId") Long fournisseurId);

    // Valeur totale du stock par fournisseur
    @Query("SELECT a.fournisseurPrincipal.nom, a.fournisseurPrincipal.code, " +
            "COUNT(s), COALESCE(SUM(s.valeurStock), 0), SUM(s.quantiteActuelle) " +
            "FROM Stock s JOIN s.article a WHERE a.fournisseurPrincipal IS NOT NULL AND a.actif = true " +
            "GROUP BY a.fournisseurPrincipal.id, a.fournisseurPrincipal.nom, a.fournisseurPrincipal.code " +
            "ORDER BY SUM(s.valeurStock) DESC")
    List<Object[]> getStockValueBySupplier();

    // ===============================
    // FILTRES PAR PRIX
    // ===============================

    // Stocks avec prix moyen dans une fourchette
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.prixMoyenPondere BETWEEN :minPrice AND :maxPrice AND a.actif = true " +
            "ORDER BY s.prixMoyenPondere")
    List<Stock> findByPrixMoyenPondereBetween(@Param("minPrice") BigDecimal minPrice,
                                              @Param("maxPrice") BigDecimal maxPrice);

    // Stocks avec valeur dans une fourchette
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE " +
            "s.valeurStock BETWEEN :minValue AND :maxValue AND a.actif = true " +
            "ORDER BY s.valeurStock DESC")
    List<Stock> findByValeurStockBetween(@Param("minValue") BigDecimal minValue,
                                         @Param("maxValue") BigDecimal maxValue);

    // ===============================
    // MISE À JOUR ET MAINTENANCE
    // ===============================

    // Mise à jour de la quantité réservée
    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.quantiteReservee = :quantiteReservee, " +
            "s.quantiteDisponible = s.quantiteActuelle - :quantiteReservee " +
            "WHERE s.article.id = :articleId")
    int updateQuantiteReservee(@Param("articleId") Long articleId,
                               @Param("quantiteReservee") Integer quantiteReservee);

    // Remise à zéro des réservations
    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.quantiteReservee = 0, " +
            "s.quantiteDisponible = s.quantiteActuelle " +
            "WHERE s.quantiteReservee > 0")
    int resetAllReservations();

    // Mise à jour des dates d'inventaire pour les articles sans écart
    @Modifying
    @Transactional
    @Query("UPDATE Stock s SET s.dateDernierInventaire = :dateInventaire, " +
            "s.quantiteInventaire = s.quantiteActuelle, s.ecartInventaire = 0 " +
            "WHERE s.ecartInventaire IS NULL OR s.ecartInventaire = 0")
    int updateInventoryDatesForConsistentStocks(@Param("dateInventaire") LocalDateTime dateInventaire);

    // ===============================
    // VÉRIFICATIONS ET VALIDATIONS
    // ===============================

    // Articles sans stock initialisé
    @Query("SELECT a.id, a.code, a.designation FROM Article a WHERE a.actif = true AND " +
            "NOT EXISTS (SELECT 1 FROM Stock s WHERE s.article.id = a.id)")
    List<Object[]> findArticleIdsWithoutStock();

    // Vérifier la cohérence des stocks (quantité disponible)
    @Query("SELECT s FROM Stock s WHERE " +
            "s.quantiteDisponible != (s.quantiteActuelle - s.quantiteReservee)")
    List<Stock> findInconsistentStocks();

    // Stocks avec données manquantes
    @Query("SELECT s FROM Stock s JOIN s.article a WHERE a.actif = true AND (" +
            "s.prixMoyenPondere IS NULL OR " +
            "s.valeurStock IS NULL OR " +
            "s.quantiteActuelle IS NULL" +
            ")")
    List<Stock> findStocksWithMissingData();

    // ===============================
    // CLASSIFICATION ABC
    // ===============================

    // Classification ABC par valeur (pour analyse Pareto)
    @Query("SELECT s, s.valeurStock FROM Stock s JOIN s.article a WHERE " +
            "a.actif = true AND s.valeurStock > 0 " +
            "ORDER BY s.valeurStock DESC")
    List<Object[]> findAllStocksForABCAnalysis();

    // Articles classe A (80% de la valeur totale)
    @Query(value = "SELECT s.* FROM stocks s " +
            "JOIN articles a ON s.article_id = a.id " +
            "WHERE a.actif = true AND s.valeur_stock > 0 " +
            "ORDER BY s.valeur_stock DESC " +
            "LIMIT (SELECT CEIL(COUNT(*) * 0.2) FROM stocks s2 " +
            "        JOIN articles a2 ON s2.article_id = a2.id " +
            "        WHERE a2.actif = true AND s2.valeur_stock > 0)",
            nativeQuery = true)
    List<Stock> findClassAStocks();

    // ===============================
    // MÉTHODES UTILITAIRES
    // ===============================

    // Compter le nombre total d'articles en stock
    @Query("SELECT COUNT(s) FROM Stock s JOIN s.article a WHERE s.quantiteActuelle > 0 AND a.actif = true")
    Long countArticlesInStock();

    // Compter les articles par statut de stock
    @Query("SELECT " +
            "COUNT(CASE WHEN s.quantiteActuelle = 0 THEN 1 END), " +
            "COUNT(CASE WHEN s.quantiteActuelle > 0 AND s.quantiteActuelle <= COALESCE(a.stockMin, 0) THEN 1 END), " +
            "COUNT(CASE WHEN s.quantiteActuelle > COALESCE(a.stockMin, 0) THEN 1 END) " +
            "FROM Stock s JOIN s.article a WHERE a.actif = true")
    Object[] countArticlesByStockStatus();

    // Somme des quantités par catégorie
    @Query("SELECT a.categorie, COALESCE(SUM(s.quantiteActuelle), 0) " +
            "FROM Stock s JOIN s.article a WHERE a.actif = true " +
            "GROUP BY a.categorie ORDER BY SUM(s.quantiteActuelle) DESC")
    List<Object[]> getTotalQuantityByCategory();
}
