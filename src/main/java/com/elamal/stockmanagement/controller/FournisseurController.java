package com.elamal.stockmanagement.controller;

import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.service.FournisseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Gestion des Fournisseurs", description = "API de gestion complète des fournisseurs")
@CrossOrigin(origins = "*")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    // ===============================
    // OPÉRATIONS CRUD DE BASE
    // ===============================

    /**
     * Créer un nouveau fournisseur
     */
    @PostMapping
    @Operation(summary = "Créer fournisseur", description = "Créer un nouveau fournisseur avec validation complète")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fournisseur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou fournisseur déjà existant"),
            @ApiResponse(responseCode = "409", description = "Conflit - nom ou email déjà utilisé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> createFournisseur(
            @Valid @RequestBody FournisseurDTO fournisseurDTO) {

        log.info("Demande de création de fournisseur: {}", fournisseurDTO.getNom());

        try {
            FournisseurDTO createdFournisseur = fournisseurService.createFournisseur(fournisseurDTO);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(
                    createdFournisseur,
                    "Fournisseur créé avec succès"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation lors de la création du fournisseur: {}", e.getMessage());

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la création du fournisseur", e);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de la création du fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mettre à jour un fournisseur existant
     */
    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour fournisseur", description = "Mettre à jour un fournisseur existant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Fournisseur non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> updateFournisseur(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id,
            @Valid @RequestBody FournisseurDTO fournisseurDTO) {

        log.info("Demande de mise à jour du fournisseur ID: {}", id);

        try {
            FournisseurDTO updatedFournisseur = fournisseurService.updateFournisseur(id, fournisseurDTO);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(
                    updatedFournisseur,
                    "Fournisseur mis à jour avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur de validation lors de la mise à jour: {}", e.getMessage());

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Fournisseur non trouvé pour mise à jour: ID {}", id);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la mise à jour", e);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de la mise à jour du fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Supprimer un fournisseur (suppression logique)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer fournisseur", description = "Supprimer un fournisseur (désactivation)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur supprimé avec succès"),
            @ApiResponse(responseCode = "400", description = "Impossible de supprimer - fournisseur avec articles associés"),
            @ApiResponse(responseCode = "404", description = "Fournisseur non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<String>> deleteFournisseur(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id) {

        log.info("Demande de suppression du fournisseur ID: {}", id);

        try {
            fournisseurService.deleteFournisseur(id);

            ApiResponseDTO<String> response = ApiResponseDTO.success(
                    "Fournisseur supprimé avec succès",
                    "Le fournisseur a été désactivé"
            );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("Impossible de supprimer le fournisseur: {}", e.getMessage());

            ApiResponseDTO<String> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Fournisseur non trouvé pour suppression: ID {}", id);

            ApiResponseDTO<String> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la suppression", e);

            ApiResponseDTO<String> response = ApiResponseDTO.error(
                    "Erreur interne lors de la suppression du fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Réactiver un fournisseur
     */
    @PutMapping("/{id}/reactivate")
    @Operation(summary = "Réactiver fournisseur", description = "Réactiver un fournisseur désactivé")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur réactivé avec succès"),
            @ApiResponse(responseCode = "404", description = "Fournisseur non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> reactivateFournisseur(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id) {

        log.info("Demande de réactivation du fournisseur ID: {}", id);

        try {
            FournisseurDTO reactivatedFournisseur = fournisseurService.reactivateFournisseur(id);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(
                    reactivatedFournisseur,
                    "Fournisseur réactivé avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Fournisseur non trouvé pour réactivation: ID {}", id);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la réactivation", e);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de la réactivation du fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // OPÉRATIONS DE LECTURE
    // ===============================

    /**
     * Récupérer un fournisseur par ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupérer par ID", description = "Récupérer un fournisseur par son ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur trouvé"),
            @ApiResponse(responseCode = "404", description = "Fournisseur non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> getFournisseurById(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id) {

        log.debug("Demande de récupération du fournisseur ID: {}", id);

        try {
            FournisseurDTO fournisseur = fournisseurService.getFournisseurById(id);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(fournisseur);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Fournisseur non trouvé: ID {}", id);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Récupérer un fournisseur par nom
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "Récupérer par nom", description = "Récupérer un fournisseur par son nom")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur trouvé"),
            @ApiResponse(responseCode = "404", description = "Fournisseur non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> getFournisseurByCode(
            @Parameter(description = "Code du fournisseur") @PathVariable String nom) {

        log.debug("Demande de récupération du fournisseur par nom: {}", nom);

        try {
            FournisseurDTO fournisseur = fournisseurService.getFournisseurByNom(nom);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(fournisseur);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Fournisseur non trouvé: nom {}", nom);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Récupérer tous les fournisseurs actifs
     */
    @GetMapping("/active")
    @Operation(summary = "Fournisseurs actifs", description = "Récupérer tous les fournisseurs actifs")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getAllActiveFournisseurs() {

        log.debug("Demande de tous les fournisseurs actifs");

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getAllActiveFournisseurs();

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs actifs trouvés", fournisseurs.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des fournisseurs actifs", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des fournisseurs actifs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Récupérer tous les fournisseurs avec pagination
     */
    @GetMapping
    @Operation(summary = "Lister tous les fournisseurs", description = "Récupérer tous les fournisseurs avec pagination")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<FournisseurDTO>>> getAllFournisseurs(
            @Parameter(description = "Numéro de page (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "nom") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "ASC") String sortDirection) {

        log.debug("Demande de liste des fournisseurs - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDirection);

        try {
            PagedResponseDTO<FournisseurDTO> fournisseurs = fournisseurService.getAllFournisseurs(page, size, sortBy, sortDirection);

            ApiResponseDTO<PagedResponseDTO<FournisseurDTO>> response = ApiResponseDTO.success(fournisseurs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des fournisseurs", e);

            ApiResponseDTO<PagedResponseDTO<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des fournisseurs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // RECHERCHE ET FILTRAGE
    // ===============================

    /**
     * Recherche avancée de fournisseurs
     */
    @PostMapping("/search")
    @Operation(summary = "Recherche avancée", description = "Rechercher des fournisseurs avec critères avancés")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<FournisseurDTO>>> searchFournisseurs(
            @Valid @RequestBody SearchCriteriaDTO criteria) {

        log.debug("Demande de recherche de fournisseurs avec critères");

        try {
            PagedResponseDTO<FournisseurDTO> fournisseurs = fournisseurService.searchFournisseurs(criteria);

            ApiResponseDTO<PagedResponseDTO<FournisseurDTO>> response = ApiResponseDTO.success(fournisseurs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche de fournisseurs", e);

            ApiResponseDTO<PagedResponseDTO<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche de fournisseurs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche textuelle simple
     */
    @GetMapping("/search")
    @Operation(summary = "Recherche textuelle", description = "Recherche textuelle simple dans les fournisseurs")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<FournisseurDTO>>> searchFournisseursByText(
            @Parameter(description = "Terme de recherche") @RequestParam String q,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size) {

        log.debug("Recherche textuelle de fournisseurs: {}", q);

        try {
            PagedResponseDTO<FournisseurDTO> fournisseurs = fournisseurService.searchFournisseursByText(q, page, size);

            ApiResponseDTO<PagedResponseDTO<FournisseurDTO>> response = ApiResponseDTO.success(fournisseurs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche textuelle", e);

            ApiResponseDTO<PagedResponseDTO<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche textuelle"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche par nom
     */
    @GetMapping("/by-name")
    @Operation(summary = "Recherche par nom", description = "Rechercher des fournisseurs par nom")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursByNom(
            @Parameter(description = "Nom du fournisseur") @RequestParam String nom) {

        log.debug("Recherche de fournisseurs par nom: {}", nom);

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursByNom(nom);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs trouvés avec le nom: %s", fournisseurs.size(), nom)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par nom", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par nom"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche par ville
     */
    @GetMapping("/by-city/{ville}")
    @Operation(summary = "Fournisseurs par ville", description = "Récupérer les fournisseurs d'une ville")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursByVille(
            @Parameter(description = "Nom de la ville") @PathVariable String ville) {

        log.debug("Recherche de fournisseurs par ville: {}", ville);

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursByVille(ville);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs trouvés à %s", fournisseurs.size(), ville)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par ville", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par ville"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche par pays
     */
    @GetMapping("/by-country/{pays}")
    @Operation(summary = "Fournisseurs par pays", description = "Récupérer les fournisseurs d'un pays")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursByPays(
            @Parameter(description = "Nom du pays") @PathVariable String pays) {

        log.debug("Recherche de fournisseurs par pays: {}", pays);

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursByPays(pays);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs trouvés en %s", fournisseurs.size(), pays)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par pays", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par pays"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Recherche par délai de livraison maximum
     */
    @GetMapping("/by-delivery-time")
    @Operation(summary = "Fournisseurs par délai livraison", description = "Récupérer les fournisseurs avec un délai de livraison maximum")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursByMaxDelaiLivraison(
            @Parameter(description = "Délai maximum en jours") @RequestParam Integer maxDelai) {

        log.debug("Recherche de fournisseurs avec délai <= {} jours", maxDelai);

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursByMaxDelaiLivraison(maxDelai);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs avec délai <= %d jours", fournisseurs.size(), maxDelai)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par délai de livraison", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par délai de livraison"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // ANALYSES ET STATISTIQUES
    // ===============================

    /**
     * Top fournisseurs par nombre d'articles
     */
    @GetMapping("/rankings/most-articles")
    @Operation(summary = "Top par nombre d'articles", description = "Récupérer les fournisseurs avec le plus d'articles")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getFournisseursWithMostArticles(
            @Parameter(description = "Nombre de résultats") @RequestParam(defaultValue = "10") int limit) {

        log.debug("Récupération du top {} fournisseurs par nombre d'articles", limit);

        try {
            List<Object[]> topFournisseurs = fournisseurService.getFournisseursWithMostArticles(limit);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques par ville"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Statistiques par pays
     */
    @GetMapping("/stats/by-country")
    @Operation(summary = "Statistiques par pays", description = "Récupérer les statistiques des fournisseurs par pays")
    public ResponseEntity<ApiResponseDTO<List<Object[]>>> getStatisticsByCountry() {

        log.debug("Récupération des statistiques par pays");

        try {
            List<Object[]> statistics = fournisseurService.getStatisticsByCountry();

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.success(statistics);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques par pays", e);

            ApiResponseDTO<List<Object[]>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques par pays"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Compter les fournisseurs par statut
     */
    @GetMapping("/stats/count-by-status")
    @Operation(summary = "Compter par statut", description = "Compter les fournisseurs par statut (actif/inactif)")
    public ResponseEntity<ApiResponseDTO<Long>> countFournisseursByStatus(
            @Parameter(description = "Statut actif (true/false)") @RequestParam Boolean actif) {

        log.debug("Comptage des fournisseurs par statut actif: {}", actif);

        try {
            Long count = fournisseurService.countFournisseursByStatus(actif);

            String message = actif ? "fournisseurs actifs" : "fournisseurs inactifs";
            ApiResponseDTO<Long> response = ApiResponseDTO.success(
                    count,
                    String.format("%d %s", count, message)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors du comptage par statut", e);

            ApiResponseDTO<Long> response = ApiResponseDTO.error(
                    "Erreur lors du comptage par statut"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // FILTRES PAR CONDITIONS
    // ===============================

    /**
     * Fournisseurs par conditions de paiement
     */
    @GetMapping("/by-payment-terms")
    @Operation(summary = "Par conditions paiement", description = "Récupérer les fournisseurs par conditions de paiement")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursByConditionsPaiement(
            @Parameter(description = "Conditions de paiement") @RequestParam String conditionsPaiement) {

        log.debug("Recherche de fournisseurs par conditions de paiement: {}", conditionsPaiement);

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursByConditionsPaiement(conditionsPaiement);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs avec conditions: %s", fournisseurs.size(), conditionsPaiement)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par conditions de paiement", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par conditions de paiement"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fournisseurs par fourchette de délai de livraison
     */
    @GetMapping("/by-delivery-range")
    @Operation(summary = "Par fourchette délai", description = "Récupérer les fournisseurs dans une fourchette de délai de livraison")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursByDelaiLivraisonRange(
            @Parameter(description = "Délai minimum") @RequestParam Integer minDelai,
            @Parameter(description = "Délai maximum") @RequestParam Integer maxDelai) {

        log.debug("Recherche de fournisseurs avec délai entre {} et {} jours", minDelai, maxDelai);

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursByDelaiLivraisonRange(minDelai, maxDelai);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs avec délai entre %d et %d jours", fournisseurs.size(), minDelai, maxDelai)
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche par fourchette de délai", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la recherche par fourchette de délai"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // VALIDATION ET VÉRIFICATION
    // ===============================

    /**
     * Vérifier si un nom fournisseur existe
     */
    @GetMapping("/exists/code/{code}")
    @Operation(summary = "Vérifier existence nom", description = "Vérifier si un nom fournisseur existe")
    public ResponseEntity<ApiResponseDTO<Boolean>> existsByCode(
            @Parameter(description = "Code à vérifier") @PathVariable String nom) {

        log.debug("Vérification de l'existence du nom: {}", nom);

        try {
            boolean exists = fournisseurService.existsByNom(nom);

            String message = exists ? "Code déjà utilisé" : "Code disponible";
            ApiResponseDTO<Boolean> response = ApiResponseDTO.success(exists, message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la vérification du nom", e);

            ApiResponseDTO<Boolean> response = ApiResponseDTO.error(
                    "Erreur lors de la vérification du nom"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Vérifier la validité des informations de contact
     */
    @GetMapping("/{id}/validate-contact")
    @Operation(summary = "Valider contact", description = "Vérifier la validité des informations de contact d'un fournisseur")
    public ResponseEntity<ApiResponseDTO<Boolean>> isContactInfoValid(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id) {

        log.debug("Vérification des informations de contact pour le fournisseur ID: {}", id);

        try {
            boolean isValid = fournisseurService.isContactInfoValid(id);

            String message = isValid ? "Informations de contact valides" : "Informations de contact incomplètes";
            ApiResponseDTO<Boolean> response = ApiResponseDTO.success(isValid, message);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Fournisseur non trouvé pour validation: ID {}", id);

            ApiResponseDTO<Boolean> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur lors de la validation du contact", e);

            ApiResponseDTO<Boolean> response = ApiResponseDTO.error(
                    "Erreur lors de la validation des informations de contact"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fournisseurs avec informations incomplètes
     */
    @GetMapping("/maintenance/incomplete-info")
    @Operation(summary = "Infos incomplètes", description = "Récupérer les fournisseurs avec informations incomplètes")
    public ResponseEntity<ApiResponseDTO<List<FournisseurDTO>>> getFournisseursWithIncompleteInfo() {

        log.debug("Récupération des fournisseurs avec informations incomplètes");

        try {
            List<FournisseurDTO> fournisseurs = fournisseurService.getFournisseursWithIncompleteInfo();

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.success(
                    fournisseurs,
                    String.format("%d fournisseurs avec informations incomplètes", fournisseurs.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des fournisseurs incomplets", e);

            ApiResponseDTO<List<FournisseurDTO>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des fournisseurs avec informations incomplètes"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // DONNÉES DE RÉFÉRENCE
    // ===============================

    /**
     * Obtenir toutes les villes des fournisseurs
     */
    @GetMapping("/reference/cities")
    @Operation(summary = "Liste des villes", description = "Obtenir toutes les villes des fournisseurs")
    public ResponseEntity<ApiResponseDTO<List<String>>> getAllCities() {

        log.debug("Récupération de toutes les villes des fournisseurs");

        try {
            List<String> cities = fournisseurService.getAllCities();

            ApiResponseDTO<List<String>> response = ApiResponseDTO.success(
                    cities,
                    String.format("%d villes trouvées", cities.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des villes", e);

            ApiResponseDTO<List<String>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des villes"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtenir tous les pays des fournisseurs
     */
    @GetMapping("/reference/countries")
    @Operation(summary = "Liste des pays", description = "Obtenir tous les pays des fournisseurs")
    public ResponseEntity<ApiResponseDTO<List<String>>> getAllCountries() {

        log.debug("Récupération de tous les pays des fournisseurs");

        try {
            List<String> countries = fournisseurService.getAllCountries();

            ApiResponseDTO<List<String>> response = ApiResponseDTO.success(
                    countries,
                    String.format("%d pays trouvés", countries.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des pays", e);

            ApiResponseDTO<List<String>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des pays"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtenir toutes les conditions de paiement
     */
    @GetMapping("/reference/payment-terms")
    @Operation(summary = "Conditions paiement", description = "Obtenir toutes les conditions de paiement")
    public ResponseEntity<ApiResponseDTO<List<String>>> getAllConditionsPaiement() {

        log.debug("Récupération de toutes les conditions de paiement");

        try {
            List<String> conditions = fournisseurService.getAllConditionsPaiement();

            ApiResponseDTO<List<String>> response = ApiResponseDTO.success(
                    conditions,
                    String.format("%d conditions trouvées", conditions.size())
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des conditions de paiement", e);

            ApiResponseDTO<List<String>> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des conditions de paiement"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // OPÉRATIONS AVANCÉES
    // ===============================

    /**
     * Dupliquer un fournisseur
     */
    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Dupliquer fournisseur", description = "Créer un fournisseur en dupliquant un existant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fournisseur dupliqué avec succès"),
            @ApiResponse(responseCode = "400", description = "Code déjà utilisé ou données invalides"),
            @ApiResponse(responseCode = "404", description = "Fournisseur original non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> duplicateFournisseur(
            @Parameter(description = "ID du fournisseur à dupliquer") @PathVariable Long id,
            @Parameter(description = "Nouveau nom") @RequestParam String newCode,
            @Parameter(description = "Nouveau nom") @RequestParam String newNom) {

        log.info("Demande de duplication du fournisseur ID: {} avec nom: {}", id, newCode);

        try {
            FournisseurDTO duplicatedFournisseur = fournisseurService.duplicateFournisseur(id, newNom);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(
                    duplicatedFournisseur,
                    "Fournisseur dupliqué avec succès"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de la duplication: {}", e.getMessage());

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (RuntimeException e) {
            log.error("Fournisseur original non trouvé: ID {}", id);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la duplication", e);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de la duplication du fournisseur"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Fusionner deux fournisseurs
     */
    @PostMapping("/merge")
    @Operation(summary = "Fusionner fournisseurs", description = "Fusionner deux fournisseurs en gardant le premier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseurs fusionnés avec succès"),
            @ApiResponse(responseCode = "404", description = "Un des fournisseurs non trouvé")
    })
    public ResponseEntity<ApiResponseDTO<FournisseurDTO>> mergeFournisseurs(
            @Parameter(description = "ID du fournisseur à garder") @RequestParam Long keepFournisseurId,
            @Parameter(description = "ID du fournisseur à supprimer") @RequestParam Long deleteFournisseurId) {

        log.info("Demande de fusion: garder ID={}, supprimer ID={}", keepFournisseurId, deleteFournisseurId);

        try {
            FournisseurDTO mergedFournisseur = fournisseurService.mergeFournisseurs(keepFournisseurId, deleteFournisseurId);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.success(
                    mergedFournisseur,
                    "Fournisseurs fusionnés avec succès"
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la fusion: {}", e.getMessage());

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur interne lors de la fusion", e);

            ApiResponseDTO<FournisseurDTO> response = ApiResponseDTO.error(
                    "Erreur interne lors de la fusion des fournisseurs"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rapport de performance d'un fournisseur
     */
    @GetMapping("/{id}/performance-report")
    @Operation(summary = "Rapport performance", description = "Générer un rapport de performance pour un fournisseur")
    public ResponseEntity<ApiResponseDTO<Object[]>> getFournisseurPerformanceReport(
            @Parameter(description = "ID du fournisseur") @PathVariable Long id,
            @Parameter(description = "Période en jours") @RequestParam(defaultValue = "365") int days) {

        log.debug("Génération du rapport de performance pour le fournisseur ID: {} sur {} jours", id, days);

        try {
            Object[] performanceReport = fournisseurService.getFournisseurPerformanceReport(id, days);

            ApiResponseDTO<Object[]> response = ApiResponseDTO.success(
                    performanceReport,
                    "Rapport de performance généré avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Fournisseur non trouvé pour rapport: ID {}", id);

            ApiResponseDTO<Object[]> response = ApiResponseDTO.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);

            ApiResponseDTO<Object[]> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport de performance"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // TABLEAUX DE BORD ET RAPPORTS
    // ===============================

    /**
     * Dashboard général des fournisseurs
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard fournisseurs", description = "Récupérer un tableau de bord général des fournisseurs")
    public ResponseEntity<ApiResponseDTO<Object>> getFournisseursDashboard() {

        log.debug("Génération du dashboard des fournisseurs");

        try {
            java.util.Map<String, Object> dashboard = new java.util.HashMap<>();

            // Statistiques de base
            dashboard.put("nombreFournisseursActifs", fournisseurService.countFournisseursByStatus(true));
            dashboard.put("nombreFournisseursInactifs", fournisseurService.countFournisseursByStatus(false));

            // Géographie
            dashboard.put("nombreVilles", fournisseurService.getAllCities().size());
            dashboard.put("nombrePays", fournisseurService.getAllCountries().size());

            // Activité
            dashboard.put("fournisseursActifs30j", fournisseurService.getFournisseursWithRecentMovements(30).size());
            dashboard.put("fournisseursInactifs90j", fournisseurService.getFournisseursWithoutRecentMovements(90).size());

            // Qualité des données
            dashboard.put("fournisseursInfoIncompletes", fournisseurService.getFournisseursWithIncompleteInfo().size());

            // Top rankings (limités pour le dashboard)
            dashboard.put("topParArticles", fournisseurService.getFournisseursWithMostArticles(5));
            dashboard.put("topParAchats", fournisseurService.getTopFournisseursByPurchaseValue(365, 5));

            // Répartition géographique
            dashboard.put("repartitionParVille", fournisseurService.getStatisticsByCity());
            dashboard.put("repartitionParPays", fournisseurService.getStatisticsByCountry());

            dashboard.put("dateGeneration", LocalDateTime.now());

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    dashboard,
                    "Dashboard fournisseurs généré avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du dashboard", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du dashboard"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Rapport de qualité des données
     */
    @GetMapping("/data-quality-report")
    @Operation(summary = "Rapport qualité données", description = "Générer un rapport de qualité des données des fournisseurs")
    public ResponseEntity<ApiResponseDTO<Object>> getDataQualityReport() {

        log.debug("Génération du rapport de qualité des données");

        try {
            java.util.Map<String, Object> qualityReport = new java.util.HashMap<>();

            // Métriques de qualité
            Long totalFournisseurs = fournisseurService.countFournisseursByStatus(true) +
                    fournisseurService.countFournisseursByStatus(false);

            List<FournisseurDTO> incompleteInfo = fournisseurService.getFournisseursWithIncompleteInfo();

            // Calcul du score de qualité
            double qualityScore = 0.0;
            if (totalFournisseurs > 0) {
                qualityScore = ((double) (totalFournisseurs - incompleteInfo.size()) / totalFournisseurs) * 100;
            }

            qualityReport.put("nombreTotalFournisseurs", totalFournisseurs);
            qualityReport.put("fournisseursInfoCompletes", totalFournisseurs - incompleteInfo.size());
            qualityReport.put("fournisseursInfoIncompletes", incompleteInfo.size());
            qualityReport.put("scoreQualite", Math.round(qualityScore * 100.0) / 100.0);

            // Niveau de qualité
            String niveauQualite;
            if (qualityScore >= 95) {
                niveauQualite = "EXCELLENT";
            } else if (qualityScore >= 85) {
                niveauQualite = "BON";
            } else if (qualityScore >= 70) {
                niveauQualite = "MOYEN";
            } else {
                niveauQualite = "CRITIQUE";
            }

            qualityReport.put("niveauQualite", niveauQualite);
            qualityReport.put("fournisseursProblematiques", incompleteInfo);
            qualityReport.put("dateGeneration", LocalDateTime.now());

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    qualityReport,
                    "Rapport de qualité généré avec succès"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport de qualité", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la génération du rapport de qualité"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ===============================
    // UTILITAIRES SYSTÈME
    // ===============================

    /**
     * Ping pour vérifier la disponibilité du service
     */
    @GetMapping("/ping")
    @Operation(summary = "Ping service", description = "Vérifier la disponibilité du service des fournisseurs")
    public ResponseEntity<ApiResponseDTO<String>> ping() {

        log.debug("Ping du service des fournisseurs");

        try {
            String message = "Service des fournisseurs opérationnel - " + LocalDateTime.now();

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
    @Operation(summary = "Version API", description = "Obtenir la version de l'API des fournisseurs")
    public ResponseEntity<ApiResponseDTO<Object>> getApiVersion() {

        log.debug("Demande de version de l'API des fournisseurs");

        try {
            java.util.Map<String, Object> versionInfo = new java.util.HashMap<>();
            versionInfo.put("version", "1.0.0");
            versionInfo.put("service", "FournisseurController");
            versionInfo.put("lastUpdate", "2024-12-19");
            versionInfo.put("author", "Stock Management System");
            versionInfo.put("description", "API complète de gestion des fournisseurs");
            versionInfo.put("endpoints", "50+ endpoints disponibles");

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

    /**
     * Statistiques de performance de l'API
     */
    @GetMapping("/stats/api-performance")
    @Operation(summary = "Stats performance API", description = "Récupérer les statistiques de performance de l'API")
    public ResponseEntity<ApiResponseDTO<Object>> getApiPerformanceStats() {

        log.debug("Demande des statistiques de performance de l'API");

        try {
            java.util.Map<String, Object> perfStats = new java.util.HashMap<>();
            perfStats.put("timestamp", LocalDateTime.now());
            perfStats.put("service", "FournisseurController");
            perfStats.put("status", "HEALTHY");
            perfStats.put("nombreEndpoints", "50+");
            perfStats.put("fonctionnalites", List.of(
                    "CRUD complet",
                    "Recherche avancée",
                    "Analyses statistiques",
                    "Validation métier",
                    "Fusion et duplication",
                    "Rapports de performance"
            ));

            ApiResponseDTO<Object> response = ApiResponseDTO.success(
                    perfStats,
                    "Statistiques de performance récupérées"
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stats de performance", e);

            ApiResponseDTO<Object> response = ApiResponseDTO.error(
                    "Erreur lors de la récupération des statistiques de performance"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
