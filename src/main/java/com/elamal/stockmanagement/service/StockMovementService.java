package com.elamal.stockmanagement.service;

import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.entity.*;
import com.elamal.stockmanagement.repository.*;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ArticleRepository articleRepository;
    private final FournisseurRepository fournisseurRepository;
    private final StockRepository stockRepository;

    private   ModelMapper modelMapper;

    // ===============================
    // OPÉRATIONS D'ENTRÉE DE STOCK
    // ===============================

    public StockMovementDTO processEntreeStock(EntreeStockRequestDTO entreeRequest) {
        log.info("Traitement d'une entrée de stock pour l'article ID: {}, quantité: {}",
                entreeRequest.getArticleId(), entreeRequest.getQuantite());

        // Validation de la demande
        validateEntreeStockRequest(entreeRequest);

        // Récupération des entités
        Article article = articleRepository.findById(entreeRequest.getArticleId())
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + entreeRequest.getArticleId()));

        Fournisseur fournisseur = fournisseurRepository.findById(entreeRequest.getFournisseurId())
                .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + entreeRequest.getFournisseurId()));

        // Récupération ou création du stock
        Stock stock = getOrCreateStock(article);
        Integer stockAvant = stock.getQuantiteActuelle();

        // Création du mouvement
        StockMovement mouvement = new StockMovement();
        mouvement.setArticle(article);
        mouvement.setTypeMouvement(TypeMouvement.ENTREE);
        mouvement.setQuantite(entreeRequest.getQuantite());
        mouvement.setPrixUnitaire(entreeRequest.getPrixUnitaire());
        mouvement.setValeurTotale(entreeRequest.getPrixUnitaire().multiply(BigDecimal.valueOf(entreeRequest.getQuantite())));
        mouvement.setFournisseur(fournisseur);
        mouvement.setMotif(entreeRequest.getMotif() != null ? entreeRequest.getMotif() : "Entrée de stock");
        mouvement.setNumeroBon(entreeRequest.getNumeroBon());
        mouvement.setNumeroFacture(entreeRequest.getNumeroFacture());
        mouvement.setDateMouvement(entreeRequest.getDateMouvement());
        mouvement.setUtilisateur(entreeRequest.getUtilisateur());
        mouvement.setObservations(entreeRequest.getObservations());
        mouvement.setStockAvant(stockAvant);

        // Mise à jour du stock avec calcul du PMP
        updateStockForEntree(stock, entreeRequest.getQuantite(), entreeRequest.getPrixUnitaire());

        // Finalisation du mouvement avec le stock après
        mouvement.setStockApres(stock.getQuantiteActuelle());

        // Sauvegarde
        StockMovement savedMovement = stockMovementRepository.save(mouvement);
        stockRepository.save(stock);

        log.info("Entrée de stock effectuée avec succès: ID={}, Article={}, Quantité={}",
                savedMovement.getId(), article.getNom(), entreeRequest.getQuantite());

        return convertToDTO(savedMovement);
    }

    // ===============================
    // OPÉRATIONS DE SORTIE DE STOCK
    // ===============================

    public StockMovementDTO processSortieStock(SortieStockRequestDTO sortieRequest) {
        log.info("Traitement d'une sortie de stock pour l'article ID: {}, quantité: {}",
                sortieRequest.getArticleId(), sortieRequest.getQuantite());

        // Validation de la demande
        validateSortieStockRequest(sortieRequest);

        // Récupération des entités
        Article article = articleRepository.findById(sortieRequest.getArticleId())
                .orElseThrow(() -> new RuntimeException("Article introuvable avec l'ID: " + sortieRequest.getArticleId()));

        Stock stock = stockRepository.findByArticleId(sortieRequest.getArticleId())
                .orElseThrow(() -> new RuntimeException("Stock introuvable pour l'article ID: " + sortieRequest.getArticleId()));

        // Vérification de la disponibilité
        if (stock.getQuantiteDisponible() < sortieRequest.getQuantite()) {
            throw new IllegalStateException(
                    String.format("Stock insuffisant. Disponible: %d, Demandé: %d",
                            stock.getQuantiteDisponible(), sortieRequest.getQuantite()));
        }

        Integer stockAvant = stock.getQuantiteActuelle();

        // Création du mouvement
        StockMovement mouvement = new StockMovement();
        mouvement.setArticle(article);
        mouvement.setTypeMouvement(TypeMouvement.SORTIE);
        mouvement.setQuantite(sortieRequest.getQuantite());
        mouvement.setClient(sortieRequest.getClient());
        mouvement.setMotif(sortieRequest.getMotif() != null ? sortieRequest.getMotif() : "Sortie de stock");
        mouvement.setNumeroBon(sortieRequest.getNumeroBon());
        mouvement.setDateMouvement(sortieRequest.getDateMouvement());
        mouvement.setUtilisateur(sortieRequest.getUtilisateur());
        mouvement.setObservations(sortieRequest.getObservations());
        mouvement.setStockAvant(stockAvant);

        // Mise à jour du stock
        updateStockForSortie(stock, sortieRequest.getQuantite());

        // Finalisation du mouvement avec le stock après
        mouvement.setStockApres(stock.getQuantiteActuelle());

        // Sauvegarde
        StockMovement savedMovement = stockMovementRepository.save(mouvement);
        stockRepository.save(stock);

        log.info("Sortie de stock effectuée avec succès: ID={}, Article={}, Quantité={}",
                savedMovement.getId(), article.getNom(), sortieRequest.getQuantite());

        return convertToDTO(savedMovement);
    }

    // ===============================
    // OPÉRATIONS DE LECTURE
    // ===============================

    @Transactional(readOnly = true)
    public StockMovementDTO getMovementById(Long id) {
        log.debug("Récupération du mouvement ID: {}", id);

        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mouvement introuvable avec l'ID: " + id));

        return convertToDTO(movement);
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> getAllMovements(int page, int size, String sortBy, String sortDirection) {
        log.debug("Récupération des mouvements - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDirection);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<StockMovement> movementPage = stockMovementRepository.findAllByOrderByDateMouvementDesc(pageable);

        List<StockMovementDTO> movementDTOs = movementPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                movementDTOs,
                movementPage.getNumber(),
                movementPage.getSize(),
                movementPage.getTotalElements(),
                movementPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> getArticleMovementHistory(Long articleId, int page, int size) {
        log.debug("Récupération de l'historique des mouvements pour l'article ID: {}", articleId);

        Pageable pageable = PageRequest.of(page, size);
        Page<StockMovement> movementPage = stockMovementRepository.findByArticleIdOrderByDateMouvementDesc(articleId, pageable);

        List<StockMovementDTO> movementDTOs = movementPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                movementDTOs,
                movementPage.getNumber(),
                movementPage.getSize(),
                movementPage.getTotalElements(),
                movementPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getTodayMovements() {
        log.debug("Récupération des mouvements d'aujourd'hui");

        List<StockMovement> entrees = stockMovementRepository.findTodayEntries();
        List<StockMovement> sorties = stockMovementRepository.findTodayExits();

        entrees.addAll(sorties);

        return entrees.stream()
                .sorted((m1, m2) -> m2.getDateMouvement().compareTo(m1.getDateMouvement()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Long countTodayMovementsByType(TypeMouvement typeMouvement) {
        log.debug("Comptage des mouvements d'aujourd'hui par type: {}", typeMouvement);

        return stockMovementRepository.countTodayMovementsByType(typeMouvement);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalEntryValue(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Calcul de la valeur totale des entrées entre {} et {}", startDate, endDate);

        return stockMovementRepository.getTotalEntryValueBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExitValue(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Calcul de la valeur totale des sorties entre {} et {}", startDate, endDate);

        return stockMovementRepository.getTotalExitValueBetween(startDate, endDate);
    }

    // ===============================
    // MÉTHODES PRIVÉES - VALIDATION
    // ===============================

    private void validateEntreeStockRequest(EntreeStockRequestDTO request) {
        if (request.getArticleId() == null) {
            throw new IllegalArgumentException("L'ID de l'article est obligatoire");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        if (request.getPrixUnitaire() == null || request.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le prix unitaire doit être positif");
        }
        if (request.getFournisseurId() == null) {
            throw new IllegalArgumentException("L'ID du fournisseur est obligatoire");
        }
        if (request.getUtilisateur() == null || request.getUtilisateur().trim().isEmpty()) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire");
        }
    }

    private void validateSortieStockRequest(SortieStockRequestDTO request) {
        if (request.getArticleId() == null) {
            throw new IllegalArgumentException("L'ID de l'article est obligatoire");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        if (request.getClient() == null || request.getClient().trim().isEmpty()) {
            throw new IllegalArgumentException("Le client/destinataire est obligatoire");
        }
        if (request.getUtilisateur() == null || request.getUtilisateur().trim().isEmpty()) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire");
        }
    }

    // ===============================
    // MÉTHODES PRIVÉES - MISE À JOUR STOCK
    // ===============================

    private void updateStockForEntree(Stock stock, Integer quantite, BigDecimal prixUnitaire) {
        Integer quantiteActuelle = stock.getQuantiteActuelle();
        BigDecimal prixMoyenActuel = stock.getPrixMoyenPondere();

        // Calcul du nouveau prix moyen pondéré (PMP)
        BigDecimal nouveauPMP;
        if (quantiteActuelle == 0 || prixMoyenActuel == null) {
            // Premier stock ou prix moyen non défini
            nouveauPMP = prixUnitaire;
        } else {
            // Calcul PMP: (Stock actuel * Prix moyen actuel + Quantité entrée * Prix entrée) / Total quantité
            BigDecimal valeurActuelle = prixMoyenActuel.multiply(BigDecimal.valueOf(quantiteActuelle));
            BigDecimal valeurEntree = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            BigDecimal valeurTotale = valeurActuelle.add(valeurEntree);
            Integer quantiteTotale = quantiteActuelle + quantite;

            nouveauPMP = valeurTotale.divide(BigDecimal.valueOf(quantiteTotale), 4, RoundingMode.HALF_UP);
        }

        // Mise à jour du stock
        stock.setQuantiteActuelle(quantiteActuelle + quantite);
        stock.setPrixMoyenPondere(nouveauPMP);
        stock.setValeurStock(nouveauPMP.multiply(BigDecimal.valueOf(stock.getQuantiteActuelle())));
        stock.setDerniereEntree(LocalDateTime.now());

        // Recalcul de la quantité disponible
        Integer quantiteReservee = stock.getQuantiteReservee() != null ? stock.getQuantiteReservee() : 0;
        stock.setQuantiteDisponible(stock.getQuantiteActuelle() - quantiteReservee);

        log.debug("Stock mis à jour pour entrée - Article: {}, Nouvelle quantité: {}, Nouveau PMP: {}",
                stock.getArticle().getNom(), stock.getQuantiteActuelle(), nouveauPMP);
    }

    private void updateStockForSortie(Stock stock, Integer quantite) {
        Integer quantiteActuelle = stock.getQuantiteActuelle();

        // Mise à jour du stock (le PMP reste inchangé)
        stock.setQuantiteActuelle(quantiteActuelle - quantite);

        // Recalcul de la valeur du stock
        if (stock.getPrixMoyenPondere() != null) {
            stock.setValeurStock(stock.getPrixMoyenPondere().multiply(BigDecimal.valueOf(stock.getQuantiteActuelle())));
        }

        stock.setDerniereSortie(LocalDateTime.now());

        // Recalcul de la quantité disponible
        Integer quantiteReservee = stock.getQuantiteReservee() != null ? stock.getQuantiteReservee() : 0;
        stock.setQuantiteDisponible(stock.getQuantiteActuelle() - quantiteReservee);

        log.debug("Stock mis à jour pour sortie - Article: {}, Nouvelle quantité: {}",
                stock.getArticle().getNom(), stock.getQuantiteActuelle());
    }

    // ===============================
    // MÉTHODES PRIVÉES - UTILITAIRES
    // ===============================

    private Stock getOrCreateStock(Article article) {
        Optional<Stock> existingStock = stockRepository.findByArticleId(article.getId());

        if (existingStock.isPresent()) {
            return existingStock.get();
        } else {
            // Création d'un nouveau stock
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
            log.info("Nouveau stock créé pour l'article: {}", article.getNom());

            return savedStock;
        }
    }

    private StockMovementDTO convertToDTO(StockMovement movement) {
        StockMovementDTO dto = new StockMovementDTO();

        // Mapping des champs de base
        dto.setId(movement.getId());
        dto.setArticleId(movement.getArticle().getId());
        dto.setTypeMouvement(movement.getTypeMouvement());
        dto.setQuantite(movement.getQuantite());
        dto.setPrixUnitaire(movement.getPrixUnitaire());
        dto.setValeurTotale(movement.getValeurTotale());
        dto.setMotif(movement.getMotif());
        dto.setNumeroBon(movement.getNumeroBon());
        dto.setNumeroFacture(movement.getNumeroFacture());
        dto.setClient(movement.getClient());
        dto.setDateMouvement(movement.getDateMouvement());
        dto.setUtilisateur(movement.getUtilisateur());
        dto.setObservations(movement.getObservations());
        dto.setStockAvant(movement.getStockAvant());
        dto.setStockApres(movement.getStockApres());
        dto.setDateCreation(movement.getDateCreation());

        // Enrichissement avec les informations de l'article
        if (movement.getArticle() != null) {
            dto.setArticleNom(movement.getArticle().getNom());
            dto.setArticleUnite(movement.getArticle().getUnite());
            dto.setArticleCategorie(movement.getArticle().getCategorie());
        }

        // Enrichissement avec les informations du fournisseur
        if (movement.getFournisseur() != null) {
            dto.setFournisseurId(movement.getFournisseur().getId());
            dto.setFournisseurNom(movement.getFournisseur().getNom());
            dto.setFournisseurNom(movement.getFournisseur().getNom());
        }

        // Calcul de la valeur totale si manquante
        if (dto.getValeurTotale() == null && dto.getQuantite() != null && dto.getPrixUnitaire() != null) {
            dto.setValeurTotale(dto.getPrixUnitaire().multiply(BigDecimal.valueOf(dto.getQuantite())));
        }

        // Attribution de la description du type de mouvement
        if (dto.getTypeMouvement() != null) {
            dto.setTypeMouvementDescription(dto.getTypeMouvement().getDescription());
        }

        // Définition du statut (par défaut valide)
        dto.setStatusMouvement("VALIDE");
        dto.setMouvementValide(true);

        return dto;
    }
    public List<StockMovementDTO> processBatchEntreeStock(List<EntreeStockRequestDTO> entreeRequests) {
        log.info("Traitement d'un lot de {} entrées de stock", entreeRequests.size());

        List<StockMovementDTO> results = new ArrayList<>();

        for (EntreeStockRequestDTO request : entreeRequests) {
            try {
                StockMovementDTO result = processEntreeStock(request);
                results.add(result);
            } catch (Exception e) {
                log.error("Erreur lors du traitement de l'entrée pour article ID: {}", request.getArticleId(), e);
                // Vous pouvez choisir de continuer ou d'arrêter en cas d'erreur
                throw new RuntimeException("Erreur lors du traitement de l'entrée pour article ID: " + request.getArticleId(), e);
            }
        }

        return results;
    }

    /**
     * Effectuer plusieurs sorties de stock en lot
     */
    public List<StockMovementDTO> processBatchSortieStock(List<SortieStockRequestDTO> sortieRequests) {
        log.info("Traitement d'un lot de {} sorties de stock", sortieRequests.size());

        List<StockMovementDTO> results = new ArrayList<>();

        for (SortieStockRequestDTO request : sortieRequests) {
            try {
                StockMovementDTO result = processSortieStock(request);
                results.add(result);
            } catch (Exception e) {
                log.error("Erreur lors du traitement de la sortie pour article ID: {}", request.getArticleId(), e);
                // Vous pouvez choisir de continuer ou d'arrêter en cas d'erreur
                throw new RuntimeException("Erreur lors du traitement de la sortie pour article ID: " + request.getArticleId(), e);
            }
        }

        return results;
    }

    /**
     * Rechercher des mouvements avec critères
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> searchMovements(SearchCriteriaDTO criteria) {
        log.debug("Recherche de mouvements avec critères: {}", criteria);

        Sort sort = Sort.by(Sort.Direction.fromString(criteria.getSortDirection()), criteria.getSortBy());
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        // Conversion des dates si fournies
        if (criteria.getDateDebut() != null) {
            startDate = LocalDateTime.parse(criteria.getDateDebut() + "T00:00:00");
        }
        if (criteria.getDateFin() != null) {
            endDate = LocalDateTime.parse(criteria.getDateFin() + "T23:59:59");
        }

        Page<StockMovement> movementPage = stockMovementRepository.findWithCriteria(
                criteria.getFournisseurId() != null ? criteria.getFournisseurId() : null,
                criteria.getTypeMouvement(),
                criteria.getFournisseurId(),
                startDate,
                endDate,
                null, // utilisateur - peut être ajouté aux critères
                pageable
        );

        List<StockMovementDTO> movementDTOs = movementPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                movementDTOs,
                movementPage.getNumber(),
                movementPage.getSize(),
                movementPage.getTotalElements(),
                movementPage.getTotalPages()
        );
    }

    /**
     * Rechercher des mouvements par numéro de bon
     */
    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovementsByNumeroBon(String numeroBon) {
        log.debug("Recherche de mouvements par numéro de bon: {}", numeroBon);

        List<StockMovement> movements = stockMovementRepository.findByNumeroBon(numeroBon);
        return movements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Rechercher des mouvements par numéro de facture
     */
    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovementsByNumeroFacture(String numeroFacture) {
        log.debug("Recherche de mouvements par numéro de facture: {}", numeroFacture);

        List<StockMovement> movements = stockMovementRepository.findByNumeroFacture(numeroFacture);
        return movements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recherche textuelle dans les mouvements
     */
    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> searchMovementsByText(String searchTerm, int page, int size) {
        log.debug("Recherche textuelle dans les mouvements: {}", searchTerm);

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateMouvement").descending());
        Page<StockMovement> movementPage = stockMovementRepository.searchMovements(searchTerm, pageable);

        List<StockMovementDTO> movementDTOs = movementPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                movementDTOs,
                movementPage.getNumber(),
                movementPage.getSize(),
                movementPage.getTotalElements(),
                movementPage.getTotalPages()
        );
    }

    /**
     * Détecter les mouvements suspects (quantités importantes)
     */
    @Transactional(readOnly = true)
    public List<StockMovementDTO> getSuspiciousMovements(Integer threshold) {
        log.debug("Détection des mouvements suspects avec seuil: {}", threshold);

        List<StockMovement> movements = stockMovementRepository.findSuspiciousMovements(threshold);
        return movements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les mouvements d'un fournisseur
     */
    @Transactional(readOnly = true)
    public List<StockMovementDTO> getFournisseurMovements(Long fournisseurId) {
        log.debug("Récupération des mouvements du fournisseur ID: {}", fournisseurId);

        List<StockMovement> movements = stockMovementRepository.findByFournisseurIdOrderByDateMouvementDesc(fournisseurId);
        return movements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
