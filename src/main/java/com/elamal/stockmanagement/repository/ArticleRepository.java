package com.elamal.stockmanagement.repository;


import com.elamal.stockmanagement.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // Recherche par code
    Optional<Article> findByCode(String code);

    // Vérifier l'existence par code (pour validation)
    boolean existsByCode(String code);

    // Vérifier l'existence par code excluant un ID (pour mise à jour)
    boolean existsByCodeAndIdNot(String code, Long id);

    // Recherche par désignation (insensible à la casse)
    List<Article> findByDesignationContainingIgnoreCase(String designation);

    // Recherche par catégorie
    List<Article> findByCategorie(String categorie);

    // Articles actifs seulement
    List<Article> findByActifTrue();

    // Articles actifs paginés
    Page<Article> findByActifTrue(Pageable pageable);

        // Articles par fournisseur principal
    List<Article> findByFournisseurPrincipalId(Long fournisseurId);

    // Recherche multicritères avec pagination
    @Query("SELECT a FROM Article a WHERE " +
            "(:query IS NULL OR LOWER(a.designation) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "                   LOWER(a.code) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "                   LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:categorie IS NULL OR a.categorie = :categorie) AND " +
            "(:fournisseurId IS NULL OR a.fournisseurPrincipal.id = :fournisseurId) AND " +
            "(:actif IS NULL OR a.actif = :actif)")
    Page<Article> findWithCriteria(@Param("query") String query,
                                   @Param("categorie") String categorie,
                                   @Param("fournisseurId") Long fournisseurId,
                                   @Param("actif") Boolean actif,
                                   Pageable pageable);

    // Articles avec stock critique (quantité <= stock minimum)
    @Query("SELECT a FROM Article a JOIN a.stock s WHERE " +
            "s.quantiteActuelle <= a.stockMin AND a.actif = true")
    List<Article> findArticlesWithCriticalStock();

    // Articles avec stock faible (quantité <= stock minimum * 1.5)
    @Query("SELECT a FROM Article a JOIN a.stock s WHERE " +
            "s.quantiteActuelle <= (a.stockMin * 1.5) AND a.actif = true")
    List<Article> findArticlesWithLowStock();

    // Articles sans stock (quantité = 0)
    @Query("SELECT a FROM Article a JOIN a.stock s WHERE " +
            "s.quantiteActuelle = 0 AND a.actif = true")
    List<Article> findArticlesWithoutStock();

    // Articles avec stock excessif (quantité > stock maximum)
    @Query("SELECT a FROM Article a JOIN a.stock s WHERE " +
            "s.quantiteActuelle > a.stockMax AND a.stockMax IS NOT NULL AND a.actif = true")
    List<Article> findArticlesWithExcessiveStock();

    // Top articles par valeur de stock
    @Query("SELECT a FROM Article a JOIN a.stock s WHERE a.actif = true " +
            "ORDER BY s.valeurStock DESC")
    List<Article> findTopArticlesByStockValue(Pageable pageable);

    // Articles par catégorie avec statistiques
    @Query("SELECT a.categorie, COUNT(a), SUM(s.quantiteActuelle), SUM(s.valeurStock) " +
            "FROM Article a JOIN a.stock s WHERE a.actif = true " +
            "GROUP BY a.categorie ORDER BY a.categorie")
    List<Object[]> findStockStatsByCategory();

    // Articles sans mouvement depuis X jours
    @Query("SELECT a FROM Article a WHERE a.actif = true AND " +
            "(SELECT MAX(sm.dateMouvement) FROM StockMovement sm WHERE sm.article = a) < " +
            "CURRENT_DATE - :days DAY")
    List<Article> findArticlesWithoutMovementSince(@Param("days") int days);

    // Articles les plus consommés (par quantité de sortie)
    @Query("SELECT a, SUM(sm.quantite) as totalSortie FROM Article a " +
            "JOIN StockMovement sm ON sm.article = a " +
            "WHERE sm.typeMouvement = 'SORTIE' AND " +
            "sm.dateMouvement >= CURRENT_DATE - :days DAY " +
            "GROUP BY a ORDER BY totalSortie DESC")
    List<Object[]> findMostConsumedArticles(@Param("days") int days, Pageable pageable);

    // Compter articles par statut
    @Query("SELECT COUNT(a) FROM Article a WHERE a.actif = :actif")
    Long countByActif(@Param("actif") Boolean actif);

    // Compter articles par catégorie
    @Query("SELECT a.categorie, COUNT(a) FROM Article a WHERE a.actif = true " +
            "GROUP BY a.categorie ORDER BY a.categorie")
    List<Object[]> countByCategory();

    // Articles avec prix unitaire dans une fourchette
    List<Article> findByPrixUnitaireBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    // Articles créés dans une période
    @Query("SELECT a FROM Article a WHERE a.dateCreation BETWEEN :startDate AND :endDate " +
            "ORDER BY a.dateCreation DESC")
    List<Article> findByDateCreationBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                            @Param("endDate") java.time.LocalDateTime endDate);

    // Recherche full-text avancée
    @Query("SELECT DISTINCT a FROM Article a LEFT JOIN a.fournisseurPrincipal f WHERE " +
            "LOWER(a.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.designation) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(a.categorie) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Article> searchArticles(@Param("searchTerm") String searchTerm, Pageable pageable);
}
