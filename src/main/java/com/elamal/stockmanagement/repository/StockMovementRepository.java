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

    List<StockMovement> findByArticleIdOrderByDateMouvementDesc(Long articleId);

    Page<StockMovement> findByArticleIdOrderByDateMouvementDesc(Long articleId, Pageable pageable);

    List<StockMovement> findByTypeMouvementOrderByDateMouvementDesc(TypeMouvement typeMouvement);

    List<StockMovement> findByFournisseurIdOrderByDateMouvementDesc(Long fournisseurId);

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

    // Entrées de stock aujourd'hui - FIXED
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.typeMouvement IN ('ENTREE', 'RETOUR_CLIENT', 'TRANSFERT_ENTREE') AND " +
            "CAST(sm.dateMouvement AS date) = CURRENT_DATE " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findTodayEntries();

    // Sorties de stock aujourd'hui - FIXED
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "sm.typeMouvement IN ('SORTIE', 'RETOUR_FOURNISSEUR', 'PERTE', 'TRANSFERT_SORTIE') AND " +
            "CAST(sm.dateMouvement AS date) = CURRENT_DATE " +
            "ORDER BY sm.dateMouvement DESC")
    List<StockMovement> findTodayExits();

    // Compter mouvements par type aujourd'hui - FIXED
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

    // Statistiques par type de mouvement dans une période - FIXED
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

    // Tendance des mouvements (par jour) - FIXED
    @Query("SELECT CAST(sm.dateMouvement AS date), sm.typeMouvement, COUNT(sm), SUM(sm.quantite) " +
            "FROM StockMovement sm WHERE sm.dateMouvement BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(sm.dateMouvement AS date), sm.typeMouvement " +
            "ORDER BY CAST(sm.dateMouvement AS date)")
    List<Object[]> getMovementTrend(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // Mouvements par numéro de bon
    List<StockMovement> findByNumeroBon(String numeroBon);

    // Mouvements par numéro de facture
    List<StockMovement> findByNumeroFacture(String numeroFacture);

    // Recherche dans les motifs et observations
    @Query("SELECT sm FROM StockMovement sm WHERE " +
            "LOWER(sm.motif) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(sm.observations) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(sm.numeroBon) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(sm.numeroFacture) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY sm.dateMouvement DESC")
    Page<StockMovement> searchMovements(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Mouvements suspects (quantités importantes)
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
}
