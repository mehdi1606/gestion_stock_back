package com.elamal.stockmanagement.repository;

import com.elamal.stockmanagement.entity.StockMovement;
import com.elamal.stockmanagement.entity.TypeMouvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // ===============================
    // MÉTHODES REQUISES PAR LE SERVICE
    // ===============================

    // Méthodes de base avec pagination
    Page<StockMovement> findByTypeMouvement(TypeMouvement typeMouvement, Pageable pageable);

    Page<StockMovement> findByDateMouvementBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<StockMovement> findByDateMouvementBetweenOrderByDateMouvementDesc(LocalDateTime startDate, LocalDateTime endDate);

    Page<StockMovement> findByTypeMouvementAndDateMouvementBetween(
            TypeMouvement typeMouvement, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Historique par article
    Page<StockMovement> findByArticleIdOrderByDateMouvementDesc(Long articleId, Pageable pageable);

    // Comptage par type et date
    Long countByTypeMouvementAndDateMouvementBetween(
            TypeMouvement typeMouvement, LocalDateTime startDate, LocalDateTime endDate);

    // Sommes pour statistiques
    @Query("SELECT COALESCE(SUM(sm.valeurTotale), 0) FROM StockMovement sm WHERE sm.typeMouvement = :typeMouvement AND sm.dateMouvement BETWEEN :startDate AND :endDate")
    BigDecimal sumValeurTotaleByTypeMouvementAndDateMouvementBetween(
            @Param("typeMouvement") TypeMouvement typeMouvement,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Recherche par numéros de documents
    List<StockMovement> findByNumeroBon(String numeroBon);
    List<StockMovement> findByNumeroFacture(String numeroFacture);

    // Recherche textuelle
    Page<StockMovement> findByMotifContainingIgnoreCaseOrObservationsContainingIgnoreCase(
            String motif, String observations, Pageable pageable);

    // Mouvements suspects
    List<StockMovement> findByQuantiteGreaterThanOrderByQuantiteDesc(Integer threshold);

    // Mouvements par fournisseur
    List<StockMovement> findByFournisseurIdOrderByDateMouvementDesc(Long fournisseurId);

    // Mouvements récents
    Page<StockMovement> findByDateMouvementGreaterThanEqual(LocalDateTime since, Pageable pageable);

    // ===============================
    // MÉTHODES EXISTANTES CONSERVÉES
    // ===============================

    List<StockMovement> findByArticleIdOrderByDateMouvementDesc(Long articleId);

    List<StockMovement> findByTypeMouvementOrderByDateMouvementDesc(TypeMouvement typeMouvement);

    List<StockMovement> findByUtilisateurOrderByDateMouvementDesc(String utilisateur);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findByDateMouvementBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    // Mouvements avec pagination et tri
    Page<StockMovement> findAllByOrderByDateMouvementDesc(Pageable pageable);

    // Recherche multicritères avec pagination
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "(:articleId IS NULL OR sm.article.id = :articleId) AND " +
            "(:typeMouvement IS NULL OR sm.typeMouvement = :typeMouvement) AND " +
            "(:fournisseurId IS NULL OR sm.fournisseur.id = :fournisseurId) AND " +
            "(:startDate IS NULL OR sm.dateMouvement >= :startDate) AND " +
            "(:endDate IS NULL OR sm.dateMouvement <= :endDate) AND " +
            "(:utilisateur IS NULL OR LOWER(sm.utilisateur) LIKE LOWER(CONCAT('%', :utilisateur, '%'))) " +
            "ORDER BY sm.dateMouvement DESC")
    Page<StockMovement> findWithCriteria(@Param("articleId") Long articleId,
                                         @Param("typeMouvement") TypeMouvement typeMouvement,
                                         @Param("fournisseurId") Long fournisseurId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("utilisateur") String utilisateur,
                                         Pageable pageable);

    // Entrées d'aujourd'hui
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.typeMouvement = 'ENTREE' AND " +
            "CAST(sm.dateMouvement AS date) = CURRENT_DATE " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findTodayEntries();

    // Sorties d'aujourd'hui
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE', 'TRANSFERT_SORTIE') AND " +
            "CAST(sm.dateMouvement AS date) = CURRENT_DATE " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findTodayExits();

    // Compter mouvements par type aujourd'hui
    @Query("SELECT COUNT(sm) FROM StockMovement sm WHERE " +
            "sm.typeMouvement = :typeMouvement AND " +
            "CAST(sm.dateMouvement AS date) = CURRENT_DATE")
    Long countTodayMovementsByType(@Param("typeMouvement") TypeMouvement typeMouvement);

    // Valeur totale des entrées dans une période
    @Query("SELECT COALESCE(SUM(sm.valeurTotale), 0) FROM StockMovement sm WHERE " +
            "sm.typeMouvement IN ('ENTREE', 'RETOUR_CLIENT') AND " +
            "sm.dateMouvement BETWEEN :startDate AND :endDate")
    BigDecimal getTotalEntryValueBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // Valeur totale des sorties dans une période
    @Query("SELECT COALESCE(SUM(sm.valeurTotale), 0) FROM StockMovement sm WHERE " +
            "sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE') AND " +
            "sm.dateMouvement BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExitValueBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Derniers mouvements par article
    @Query("SELECT sm FROM StockMovement sm WHERE sm.article.id = :articleId " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findLastMovementsByArticle(@Param("articleId") Long articleId,
                                                   Pageable pageable);

    // Dernier mouvement d'entrée par article
    @Query("SELECT sm FROM StockMovement sm WHERE sm.article.id = :articleId AND " +
            "sm.typeMouvement IN ('ENTREE', 'RETOUR_CLIENT', 'TRANSFERT_ENTREE') " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findLastEntryByArticle(@Param("articleId") Long articleId,
                                               Pageable pageable);

    // Dernier mouvement de sortie par article
    @Query("SELECT sm FROM StockMovement sm WHERE sm.article.id = :articleId AND " +
            "sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE', 'TRANSFERT_SORTIE') " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findLastExitByArticle(@Param("articleId") Long articleId,
                                              Pageable pageable);

    // Statistiques par type de mouvement dans une période
    @Query("SELECT sm.typeMouvement, COUNT(sm), SUM(sm.quantite), SUM(sm.valeurTotale) " +
            "FROM StockMovement sm WHERE sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.typeMouvement ORDER BY sm.typeMouvement")
    List<Object[]> getMovementStatsByType(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Statistiques par article (top consommés)
    @Query("SELECT sm.article, SUM(sm.quantite) as totalQuantite FROM StockMovement sm " +
            "WHERE sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE') AND " +
            "sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.article ORDER BY totalQuantite DESC")
    List<Object[]> getTopConsumedArticles(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    // Statistiques par fournisseur (top livraisons)
    @Query("SELECT sm.fournisseur, COUNT(sm), SUM(sm.valeurTotale) FROM StockMovement sm " +
            "WHERE sm.typeMouvement = 'ENTREE' AND sm.fournisseur IS NOT NULL AND " +
            "sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.fournisseur ORDER BY COUNT(sm) DESC")
    List<Object[]> getTopSuppliersByDeliveries(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    // Mouvements par utilisateur dans une période
    @Query("SELECT sm.utilisateur, COUNT(sm), SUM(sm.quantite) FROM StockMovement sm " +
            "WHERE sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.utilisateur ORDER BY COUNT(sm) DESC")
    List<Object[]> getMovementStatsByUser(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    // Tendance des mouvements (par jour)
    @Query("SELECT CAST(sm.dateMouvement AS date), sm.typeMouvement, COUNT(sm), SUM(sm.quantite) " +
            "FROM StockMovement sm WHERE sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(sm.dateMouvement AS date), sm.typeMouvement " +
            "ORDER BY CAST(sm.dateMouvement AS date)")
    List<Object[]> getMovementTrend(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Recherche dans les motifs et observations (version étendue)
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "LOWER(sm.motif) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(sm.observations) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(sm.numeroBon) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(sm.numeroFacture) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY sm.dateMouvement DESC")
    Page<StockMovement> searchMovements(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Mouvements suspects (quantités importantes) - version alternative
    @Query("SELECT sm FROM StockMovement sm WHERE sm.quantite > :threshold " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findSuspiciousMovements(@Param("threshold") Integer threshold);

    // Mouvements sans prix (pour les sorties)
    @Query("SELECT sm FROM StockMovement sm WHERE sm.prixUnitaire IS NULL AND " +
            "sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE')")
    List<StockMovement> findMovementsWithoutPrice();

    // Total quantité entrée par article
    @Query("SELECT COALESCE(SUM(sm.quantite), 0) FROM StockMovement sm WHERE " +
            "sm.article.id = :articleId AND " +
            "sm.typeMouvement IN ('ENTREE', 'RETOUR_CLIENT', 'TRANSFERT_ENTREE')")
    Integer getTotalEntryQuantityByArticle(@Param("articleId") Long articleId);

    // Total quantité sortie par article
    @Query("SELECT COALESCE(SUM(sm.quantite), 0) FROM StockMovement sm WHERE " +
            "sm.article.id = :articleId AND " +
            "sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE', 'TRANSFERT_SORTIE')")
    Integer getTotalExitQuantityByArticle(@Param("articleId") Long articleId);

    // ===============================
    // MÉTHODES ADDITIONNELLES POUR LE SERVICE COMPLET
    // ===============================

    // Recherche par article et période (méthode manquante pour le service)
    @Query("SELECT sm FROM StockMovement sm WHERE sm.article.id = :articleId AND sm.dateMouvement BETWEEN :startDate AND :endDate ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findByArticleIdAndDateMouvementBetween(@Param("articleId") Long articleId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate);

    // Mouvements d'aujourd'hui pour getTodayMovements (version simplifiée)
    @Query("SELECT sm FROM StockMovement sm WHERE CAST(sm.dateMouvement AS date) = CURRENT_DATE ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findTodayMovements();

    // Compter tous les mouvements d'aujourd'hui
    @Query("SELECT COUNT(sm) FROM StockMovement sm WHERE CAST(sm.dateMouvement AS date) = CURRENT_DATE")
    Long countTodayMovements();

    // Derniers mouvements sans limite spécifique
    @Query("SELECT sm FROM StockMovement sm ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findRecentMovements(Pageable pageable);

    // Mouvements par période avec tri
    @Query("SELECT sm FROM StockMovement sm WHERE sm.dateMouvement BETWEEN :startDate AND :endDate ORDER BY sm.dateMouvement DESC")
    Page<StockMovement> findMovementsByPeriod(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    // Recherche avancée pour le service
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "(:articleId IS NULL OR sm.article.id = :articleId) AND " +
            "(:typeMouvement IS NULL OR sm.typeMouvement = :typeMouvement) AND " +
            "(:fournisseurId IS NULL OR sm.fournisseur.id = :fournisseurId) AND " +
            "(:startDate IS NULL OR sm.dateMouvement >= :startDate) AND " +
            "(:endDate IS NULL OR sm.dateMouvement <= :endDate) " +
            "ORDER BY sm.dateMouvement DESC")
    Page<StockMovement> findMovementsWithFilters(@Param("articleId") Long articleId,
                                                 @Param("typeMouvement") TypeMouvement typeMouvement,
                                                 @Param("fournisseurId") Long fournisseurId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 Pageable pageable);

    // Statistiques globales
    @Query("SELECT COUNT(sm) FROM StockMovement sm WHERE sm.typeMouvement = :typeMouvement")
    Long countByTypeMouvement(@Param("typeMouvement") TypeMouvement typeMouvement);

    // Valeur totale par type de mouvement (toute période)
    @Query("SELECT COALESCE(SUM(sm.valeurTotale), 0) FROM StockMovement sm WHERE sm.typeMouvement = :typeMouvement")
    BigDecimal sumValeurTotaleByTypeMouvement(@Param("typeMouvement") TypeMouvement typeMouvement);

    // Top articles par valeur de mouvement
    @Query("SELECT sm.article, SUM(sm.valeurTotale) as totalValeur FROM StockMovement sm " +
            "WHERE sm.typeMouvement = :typeMouvement AND sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY sm.article ORDER BY totalValeur DESC")
    List<Object[]> getTopArticlesByValue(@Param("typeMouvement") TypeMouvement typeMouvement,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         Pageable pageable);

    // Évolution du stock dans le temps
    @Query("SELECT DATE(sm.dateMouvement), " +
            "SUM(CASE WHEN sm.typeMouvement IN ('ENTREE', 'RETOUR_CLIENT') THEN sm.quantite ELSE 0 END) as entrees, " +
            "SUM(CASE WHEN sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE') THEN sm.quantite ELSE 0 END) as sorties " +
            "FROM StockMovement sm WHERE sm.article.id = :articleId AND sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(sm.dateMouvement) ORDER BY DATE(sm.dateMouvement)")
    List<Object[]> getStockEvolutionByArticle(@Param("articleId") Long articleId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    @Query("SELECT COUNT(sm) FROM StockMovement sm WHERE " +
            "sm.typeMouvement = :type AND " +
            "sm.dateMouvement BETWEEN :startDate AND :endDate")
    Long countMovementsByTypeAndDateRange(@Param("type") TypeMouvement type,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

}
