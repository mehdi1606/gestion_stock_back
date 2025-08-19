package com.elamal.stockmanagement.repository;


import com.elamal.stockmanagement.entity.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    // Recherche par Nom
    Optional<Fournisseur> findByNom(String nom);

    // Vérifier l'existence par Nom
    boolean existsByNom(String Nom);

    // Vérifier l'existence par Nom excluant un ID (pour mise à jour)
    boolean existsByNomAndIdNot(String Nom, Long id);

    // Recherche par nom (insensible à la casse)
    List<Fournisseur> findByNomContainingIgnoreCase(String nom);

    // Fournisseurs actifs seulement
    List<Fournisseur> findByActifTrue();

    // Fournisseurs actifs paginés
    Page<Fournisseur> findByActifTrue(Pageable pageable);

    // Recherche par email
    Optional<Fournisseur> findByEmail(String email);

    // Recherche par téléphone
    Optional<Fournisseur> findByTelephone(String telephone);

    // Recherche par ville
    List<Fournisseur> findByVille(String ville);

    // Recherche par pays
    List<Fournisseur> findByPays(String pays);

    // Recherche multicritères avec pagination
    @Query("SELECT f FROM Fournisseur f WHERE " +
            "(:query IS NULL OR LOWER(f.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "                   LOWER(f.raisonSociale) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "                   LOWER(f.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:ville IS NULL OR LOWER(f.ville) = LOWER(:ville)) AND " +
            "(:pays IS NULL OR LOWER(f.pays) = LOWER(:pays)) AND " +
            "(:actif IS NULL OR f.actif = :actif)")
    Page<Fournisseur> findWithCriteria(@Param("query") String query,
                                       @Param("ville") String ville,
                                       @Param("pays") String pays,
                                       @Param("actif") Boolean actif,
                                       Pageable pageable);

    // Fournisseurs avec le plus d'articles
    @Query("SELECT f, COUNT(a) as articleCount FROM Fournisseur f " +
            "LEFT JOIN f.articles a WHERE f.actif = true " +
            "GROUP BY f ORDER BY articleCount DESC")
    List<Object[]> findFournisseursWithMostArticles(Pageable pageable);

    // Fournisseurs avec mouvements récents
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
            "JOIN f.mouvements sm WHERE sm.dateMouvement >= :since " +
            "AND f.actif = true ORDER BY f.nom")
    List<Fournisseur> findFournisseursWithRecentMovements(@Param("since") LocalDateTime since);

    // Fournisseurs par délai de livraison
    List<Fournisseur> findByDelaiLivraisonLessThanEqualAndActifTrue(Integer maxDelai);

    // Fournisseurs sans mouvement depuis X jours
    @Query("SELECT f FROM Fournisseur f WHERE f.actif = true AND " +
            "(SELECT MAX(sm.dateMouvement) FROM StockMovement sm WHERE sm.fournisseur = f) < " +
            "CURRENT_DATE - :days DAY")
    List<Fournisseur> findFournisseursWithoutMovementSince(@Param("days") int days);

    // Top fournisseurs par valeur d'achats
    @Query("SELECT f, SUM(sm.valeurTotale) as totalAchats FROM Fournisseur f " +
            "JOIN f.mouvements sm WHERE sm.typeMouvement = 'ENTREE' AND " +
            "sm.dateMouvement >= :since GROUP BY f ORDER BY totalAchats DESC")
    List<Object[]> findTopFournisseursByPurchaseValue(@Param("since") LocalDateTime since,
                                                      Pageable pageable);

    // Fournisseurs par nombre de livraisons
    @Query("SELECT f, COUNT(sm) as livraisons FROM Fournisseur f " +
            "JOIN f.mouvements sm WHERE sm.typeMouvement = 'ENTREE' AND " +
            "sm.dateMouvement >= :since GROUP BY f ORDER BY livraisons DESC")
    List<Object[]> findFournisseursByDeliveryCount(@Param("since") LocalDateTime since,
                                                   Pageable pageable);

    // Statistiques par ville
    @Query("SELECT f.ville, COUNT(f), COUNT(CASE WHEN f.actif = true THEN 1 END) " +
            "FROM Fournisseur f GROUP BY f.ville ORDER BY f.ville")
    List<Object[]> getStatisticsByCity();

    // Statistiques par pays
    @Query("SELECT f.pays, COUNT(f), COUNT(CASE WHEN f.actif = true THEN 1 END) " +
            "FROM Fournisseur f GROUP BY f.pays ORDER BY f.pays")
    List<Object[]> getStatisticsByCountry();

    // Compter fournisseurs par statut
    @Query("SELECT COUNT(f) FROM Fournisseur f WHERE f.actif = :actif")
    Long countByActif(@Param("actif") Boolean actif);

    // Fournisseurs créés dans une période
    @Query("SELECT f FROM Fournisseur f WHERE f.dateCreation BETWEEN :startDate AND :endDate " +
            "ORDER BY f.dateCreation DESC")
    List<Fournisseur> findByDateCreationBetween(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    // Recherche full-text avancée
    @Query("SELECT f FROM Fournisseur f WHERE " +
            "LOWER(f.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.raisonSociale) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.contactPrincipal) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(f.ville) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Fournisseur> searchFournisseurs(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Vérification unicité email (excluant un ID)
    boolean existsByEmailAndIdNot(String email, Long id);

    // Vérification unicité téléphone (excluant un ID)
    boolean existsByTelephoneAndIdNot(String telephone, Long id);

    // Fournisseurs avec conditions de paiement spécifiques
    List<Fournisseur> findByConditionsPaiementAndActifTrue(String conditionsPaiement);

    // Fournisseurs dans une fourchette de délai de livraison
    List<Fournisseur> findByDelaiLivraisonBetweenAndActifTrue(Integer minDelai, Integer maxDelai);
}
