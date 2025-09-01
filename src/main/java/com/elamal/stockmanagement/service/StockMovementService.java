package com.elamal.stockmanagement.service;

import com.elamal.stockmanagement.dto.*;
import com.elamal.stockmanagement.entity.*;
import com.elamal.stockmanagement.exception.StockManagementException;
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
import java.time.format.DateTimeParseException;
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
    private final MessageService messageService;

    @Autowired
    private ModelMapper modelMapper;

    // ===============================
    // OPÉRATIONS D'ENTRÉE DE STOCK
    // ===============================

    public StockMovementDTO processEntreeStock(EntreeStockRequestDTO entreeRequest) {
        log.info("Traitement d'une entrée de stock pour l'article ID: {}, quantité: {}",
                entreeRequest.getArticleId(), entreeRequest.getQuantite());

        try {
            validateEntreeStockRequest(entreeRequest);

            Article article = articleRepository.findById(entreeRequest.getArticleId())
                    .orElseThrow(() -> new StockManagementException("error.article.not.found", entreeRequest.getArticleId()));

            Fournisseur fournisseur = fournisseurRepository.findById(entreeRequest.getFournisseurId())
                    .orElseThrow(() -> new StockManagementException("error.supplier.not.found", entreeRequest.getFournisseurId()));

            Stock stock = getOrCreateStock(article);
            Integer stockAvant = stock.getQuantiteActuelle();

            StockMovement mouvement = createEntryMovement(entreeRequest, article, fournisseur, stockAvant);

            updateStockForEntree(stock, entreeRequest.getQuantite(), entreeRequest.getPrixUnitaire());

            mouvement.setStockApres(stock.getQuantiteActuelle());

            StockMovement savedMovement = stockMovementRepository.save(mouvement);
            stockRepository.save(stock);

            log.info("Entrée de stock effectuée avec succès: ID={}, Article={}, Quantité={}",
                    savedMovement.getId(), article.getNom(), entreeRequest.getQuantite());

            return convertToDTO(savedMovement);

        } catch (StockManagementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'entrée de stock", e);
            throw new StockManagementException("error.entry.processing", e, e.getMessage());
        }
    }

    public List<StockMovementDTO> processBatchEntreeStock(List<EntreeStockRequestDTO> entreeRequests) {
        log.info("Traitement d'un lot de {} entrées de stock", entreeRequests.size());

        List<StockMovementDTO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < entreeRequests.size(); i++) {
            try {
                StockMovementDTO result = processEntreeStock(entreeRequests.get(i));
                results.add(result);
            } catch (Exception e) {
                log.error("Erreur lors du traitement de l'entrée {} pour article ID: {}",
                        i + 1, entreeRequests.get(i).getArticleId(), e);
                errors.add(messageService.getMessage("error.entry.processing",
                        "Ligne " + (i + 1) + ": " + e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new StockManagementException("error.batch.processing", String.join("; ", errors));
        }

        return results;
    }

    // ===============================
    // OPÉRATIONS DE SORTIE DE STOCK
    // ===============================

    public StockMovementDTO processSortieStock(SortieStockRequestDTO sortieRequest) {
        log.info("Traitement d'une sortie de stock pour l'article ID: {}, quantité: {}",
                sortieRequest.getArticleId(), sortieRequest.getQuantite());

        try {
            validateSortieStockRequest(sortieRequest);

            Article article = articleRepository.findById(sortieRequest.getArticleId())
                    .orElseThrow(() -> new StockManagementException("error.article.not.found", sortieRequest.getArticleId()));

            Stock stock = stockRepository.findByArticleId(sortieRequest.getArticleId())
                    .orElseThrow(() -> new StockManagementException("error.stock.not.found", sortieRequest.getArticleId()));

            if (stock.getQuantiteDisponible() < sortieRequest.getQuantite()) {
                throw new StockManagementException("error.stock.insufficient",
                        stock.getQuantiteDisponible(), sortieRequest.getQuantite());
            }

            Integer stockAvant = stock.getQuantiteActuelle();

            StockMovement mouvement = createExitMovement(sortieRequest, article, stock, stockAvant);

            updateStockForSortie(stock, sortieRequest.getQuantite());

            mouvement.setStockApres(stock.getQuantiteActuelle());

            StockMovement savedMovement = stockMovementRepository.save(mouvement);
            stockRepository.save(stock);

            log.info("Sortie de stock effectuée avec succès: ID={}, Article={}, Quantité={}",
                    savedMovement.getId(), article.getNom(), sortieRequest.getQuantite());

            return convertToDTO(savedMovement);

        } catch (StockManagementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la sortie de stock", e);
            throw new StockManagementException("error.exit.processing", e, e.getMessage());
        }
    }

    public List<StockMovementDTO> processBatchSortieStock(List<SortieStockRequestDTO> sortieRequests) {
        log.info("Traitement d'un lot de {} sorties de stock", sortieRequests.size());

        List<StockMovementDTO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < sortieRequests.size(); i++) {
            try {
                StockMovementDTO result = processSortieStock(sortieRequests.get(i));
                results.add(result);
            } catch (Exception e) {
                log.error("Erreur lors du traitement de la sortie {} pour article ID: {}",
                        i + 1, sortieRequests.get(i).getArticleId(), e);
                errors.add(messageService.getMessage("error.exit.processing",
                        "Ligne " + (i + 1) + ": " + e.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new StockManagementException("error.batch.processing", String.join("; ", errors));
        }

        return results;
    }

    // ===============================
    // OPÉRATIONS DE LECTURE
    // ===============================

    @Transactional(readOnly = true)
    public StockMovementDTO getMovementById(Long id) {
        log.debug("Récupération du mouvement ID: {}", id);

        try {
            StockMovement movement = stockMovementRepository.findById(id)
                    .orElseThrow(() -> new StockManagementException("error.movement.not.found", id));

            return convertToDTO(movement);
        } catch (StockManagementException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du mouvement ID: {}", id, e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> getAllMovements(int page, int size, String sortBy, String sortDirection) {
        log.debug("Récupération des mouvements - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDirection);

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<StockMovement> movementPage = stockMovementRepository.findAll(pageable);

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
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> getArticleMovementHistory(Long articleId, int page, int size) {
        log.debug("Récupération de l'historique des mouvements pour l'article ID: {}", articleId);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateMouvement").descending());
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
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'historique pour l'article: {}", articleId, e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getTodayMovements() {
        log.debug("Récupération des mouvements d'aujourd'hui");

        try {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            List<StockMovement> movements = stockMovementRepository.findByDateMouvementBetweenOrderByDateMouvementDesc(
                    startOfDay, endOfDay
            );

            return movements.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements d'aujourd'hui", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public Long countTodayMovementsByType(TypeMouvement typeMouvement) {
        log.debug("Comptage des mouvements d'aujourd'hui par type: {}", typeMouvement);

        try {
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

            return stockMovementRepository.countByTypeMouvementAndDateMouvementBetween(
                    typeMouvement, startOfDay, endOfDay
            );
        } catch (Exception e) {
            log.error("Erreur lors du comptage des mouvements par type: {}", typeMouvement, e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalEntryValue(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Calcul de la valeur totale des entrées entre {} et {}", startDate, endDate);

        try {
            BigDecimal total = stockMovementRepository.sumValeurTotaleByTypeMouvementAndDateMouvementBetween(
                    TypeMouvement.ENTREE, startDate, endDate
            );
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la valeur des entrées", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalExitValue(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Calcul de la valeur totale des sorties entre {} et {}", startDate, endDate);

        try {
            BigDecimal total = stockMovementRepository.sumValeurTotaleByTypeMouvementAndDateMouvementBetween(
                    TypeMouvement.SORTIE, startDate, endDate
            );
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la valeur des sorties", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    // ===============================
    // OPERATIONS DE RECHERCHE
    // ===============================

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> searchMovements(SearchCriteriaDTO criteria) {
        log.debug("Recherche de mouvements avec critères: {}", criteria);

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(criteria.getSortDirection()), criteria.getSortBy());
            Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

            LocalDateTime startDate = parseDate(criteria.getDateDebut());
            LocalDateTime endDate = parseDate(criteria.getDateFin());

            Page<StockMovement> movementPage = findMovementsWithCriteria(criteria, startDate, endDate, pageable);

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
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de mouvements", e);
            throw new StockManagementException("error.search.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovementsByNumeroBon(String numeroBon) {
        log.debug("Recherche de mouvements par numéro de bon: {}", numeroBon);

        try {
            List<StockMovement> movements = stockMovementRepository.findByNumeroBon(numeroBon);
            return movements.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la recherche par numéro de bon", e);
            throw new StockManagementException("error.search.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovementsByNumeroFacture(String numeroFacture) {
        log.debug("Recherche de mouvements par numéro de facture: {}", numeroFacture);

        try {
            List<StockMovement> movements = stockMovementRepository.findByNumeroFacture(numeroFacture);
            return movements.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la recherche par numéro de facture", e);
            throw new StockManagementException("error.search.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> searchMovementsByText(String searchTerm, int page, int size) {
        log.debug("Recherche textuelle dans les mouvements: {}", searchTerm);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateMouvement").descending());

            Page<StockMovement> movementPage = stockMovementRepository
                    .findByMotifContainingIgnoreCaseOrObservationsContainingIgnoreCase(
                            searchTerm, searchTerm, pageable);

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
        } catch (Exception e) {
            log.error("Erreur lors de la recherche textuelle", e);
            throw new StockManagementException("error.search.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getSuspiciousMovements(Integer threshold) {
        log.debug("Détection des mouvements suspects avec seuil: {}", threshold);

        try {
            List<StockMovement> movements = stockMovementRepository.findByQuantiteGreaterThanOrderByQuantiteDesc(threshold);
            return movements.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la détection de mouvements suspects", e);
            throw new StockManagementException("error.search.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getFournisseurMovements(Long fournisseurId) {
        log.debug("Récupération des mouvements du fournisseur ID: {}", fournisseurId);

        try {
            List<StockMovement> movements = stockMovementRepository.findByFournisseurIdOrderByDateMouvementDesc(fournisseurId);
            return movements.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements du fournisseur", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    // ===============================
    // METHODES DE VALIDATION
    // ===============================

    @Transactional(readOnly = true)
    public Boolean validateStockAvailability(Long articleId, Integer quantite) {
        log.debug("Vérification de disponibilité - Article: {}, Quantité: {}", articleId, quantite);

        try {
            Optional<Stock> stockOpt = stockRepository.findByArticleId(articleId);

            if (stockOpt.isPresent()) {
                Stock stock = stockOpt.get();
                return stock.getQuantiteDisponible() >= quantite;
            }

            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de disponibilité", e);
            throw new StockManagementException("error.validation.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovementsByType(TypeMouvement typeMouvement, int limit) {
        log.debug("Récupération des mouvements de type: {} avec limite: {}", typeMouvement, limit);

        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by("dateMouvement").descending());
            Page<StockMovement> movementPage = stockMovementRepository.findByTypeMouvement(typeMouvement, pageable);

            return movementPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements par type", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> getStockEntries(int page, int size, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Récupération des entrées de stock - Page: {}, Size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateMouvement").descending());
            Page<StockMovement> movementPage;

            if (startDate != null && endDate != null) {
                movementPage = stockMovementRepository.findByTypeMouvementAndDateMouvementBetween(
                        TypeMouvement.ENTREE, startDate, endDate, pageable);
            } else {
                movementPage = stockMovementRepository.findByTypeMouvement(TypeMouvement.ENTREE, pageable);
            }

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
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des entrées de stock", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<StockMovementDTO> getStockExits(int page, int size, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Récupération des sorties de stock - Page: {}, Size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateMouvement").descending());
            Page<StockMovement> movementPage;

            if (startDate != null && endDate != null) {
                movementPage = stockMovementRepository.findByTypeMouvementAndDateMouvementBetween(
                        TypeMouvement.SORTIE, startDate, endDate, pageable);
            } else {
                movementPage = stockMovementRepository.findByTypeMouvement(TypeMouvement.SORTIE, pageable);
            }

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
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des sorties de stock", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getRecentMovements(int hours, int limit) {
        log.debug("Récupération des mouvements des dernières {} heures", hours);

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            Pageable pageable = PageRequest.of(0, limit, Sort.by("dateMouvement").descending());

            Page<StockMovement> movementPage = stockMovementRepository.findByDateMouvementGreaterThanEqual(since, pageable);

            return movementPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des mouvements récents", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    // ===============================
    // MÉTHODES PRIVÉES - VALIDATION
    // ===============================

    private void validateEntreeStockRequest(EntreeStockRequestDTO request) {
        if (request.getArticleId() == null) {
            throw new StockManagementException("error.article.id.required");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0) {
            throw new StockManagementException("error.quantity.positive");
        }
        if (request.getPrixUnitaire() == null || request.getPrixUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
            throw new StockManagementException("error.unit.price.positive");
        }
        if (request.getFournisseurId() == null) {
            throw new StockManagementException("error.supplier.id.required");
        }
        if (request.getUtilisateur() == null || request.getUtilisateur().trim().isEmpty()) {
            throw new StockManagementException("error.user.required");
        }
    }

    private void validateSortieStockRequest(SortieStockRequestDTO request) {
        if (request.getArticleId() == null) {
            throw new StockManagementException("error.article.id.required");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0) {
            throw new StockManagementException("error.quantity.positive");
        }
        if (request.getClient() == null || request.getClient().trim().isEmpty()) {
            throw new StockManagementException("error.client.required");
        }
        if (request.getUtilisateur() == null || request.getUtilisateur().trim().isEmpty()) {
            throw new StockManagementException("error.user.required");
        }
    }

    // ===============================
    // MÉTHODES PRIVÉES - HELPER
    // ===============================

    private StockMovement createEntryMovement(EntreeStockRequestDTO request, Article article,
                                              Fournisseur fournisseur, Integer stockAvant) {
        StockMovement mouvement = new StockMovement();
        mouvement.setArticle(article);
        mouvement.setTypeMouvement(TypeMouvement.ENTREE);
        mouvement.setQuantite(request.getQuantite());
        mouvement.setPrixUnitaire(request.getPrixUnitaire());
        mouvement.setValeurTotale(request.getPrixUnitaire().multiply(BigDecimal.valueOf(request.getQuantite())));
        mouvement.setFournisseur(fournisseur);
        mouvement.setMotif(request.getMotif() != null ? request.getMotif() : "Entrée de stock");
        mouvement.setNumeroBon(request.getNumeroBon());
        mouvement.setNumeroFacture(request.getNumeroFacture());
        mouvement.setDateMouvement(request.getDateMouvement() != null ? request.getDateMouvement() : LocalDateTime.now());
        mouvement.setUtilisateur(request.getUtilisateur());
        mouvement.setObservations(request.getObservations());
        mouvement.setStockAvant(stockAvant);
        return mouvement;
    }

    private StockMovement createExitMovement(SortieStockRequestDTO request, Article article,
                                             Stock stock, Integer stockAvant) {
        StockMovement mouvement = new StockMovement();
        mouvement.setArticle(article);
        mouvement.setTypeMouvement(TypeMouvement.SORTIE);
        mouvement.setQuantite(request.getQuantite());
        mouvement.setClient(request.getClient());
        mouvement.setMotif(request.getMotif() != null ? request.getMotif() : "Sortie de stock");
        mouvement.setNumeroBon(request.getNumeroBon());
        mouvement.setDateMouvement(request.getDateMouvement() != null ? request.getDateMouvement() : LocalDateTime.now());
        mouvement.setUtilisateur(request.getUtilisateur());
        mouvement.setObservations(request.getObservations());
        mouvement.setStockAvant(stockAvant);

        if (stock.getPrixMoyenPondere() != null) {
            mouvement.setPrixUnitaire(stock.getPrixMoyenPondere());
            mouvement.setValeurTotale(stock.getPrixMoyenPondere().multiply(BigDecimal.valueOf(request.getQuantite())));
        }

        return mouvement;
    }

    private Page<StockMovement> findMovementsWithCriteria(SearchCriteriaDTO criteria,
                                                          LocalDateTime startDate,
                                                          LocalDateTime endDate,
                                                          Pageable pageable) {
        if (criteria.getTypeMouvement() != null && startDate != null && endDate != null) {
            return stockMovementRepository.findByTypeMouvementAndDateMouvementBetween(
                    criteria.getTypeMouvement(), startDate, endDate, pageable);
        } else if (criteria.getTypeMouvement() != null) {
            return stockMovementRepository.findByTypeMouvement(criteria.getTypeMouvement(), pageable);
        } else if (startDate != null && endDate != null) {
            return stockMovementRepository.findByDateMouvementBetween(startDate, endDate, pageable);
        } else {
            return stockMovementRepository.findAll(pageable);
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateStr);
        } catch (DateTimeParseException e1) {
            try {
                if (dateStr.length() == 10) {
                    return LocalDateTime.parse(dateStr + "T00:00:00");
                }
                throw e1;
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(dateStr.replace(" ", "T"));
                } catch (DateTimeParseException e3) {
                    log.warn("Format de date invalide: {}", dateStr);
                    return null;
                }
            }
        }
    }

    // ===============================
    // MÉTHODES PRIVÉES - MISE À JOUR STOCK
    // ===============================

    private void updateStockForEntree(Stock stock, Integer quantite, BigDecimal prixUnitaire) {
        try {
            Integer quantiteActuelle = stock.getQuantiteActuelle() != null ? stock.getQuantiteActuelle() : 0;
            BigDecimal prixMoyenActuel = stock.getPrixMoyenPondere();

            BigDecimal nouveauPMP = calculateNewPMP(quantiteActuelle, prixMoyenActuel, quantite, prixUnitaire);

            stock.setQuantiteActuelle(quantiteActuelle + quantite);
            stock.setPrixMoyenPondere(nouveauPMP);
            stock.setValeurStock(nouveauPMP.multiply(BigDecimal.valueOf(stock.getQuantiteActuelle())));
            stock.setDerniereEntree(LocalDateTime.now());

            Integer quantiteReservee = stock.getQuantiteReservee() != null ? stock.getQuantiteReservee() : 0;
            stock.setQuantiteDisponible(stock.getQuantiteActuelle() - quantiteReservee);

            log.debug(messageService.getMessage("info.stock.updated", stock.getQuantiteActuelle()));

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du stock pour entrée", e);
            throw new StockManagementException("error.stock.update.failed", e);
        }
    }

    private void updateStockForSortie(Stock stock, Integer quantite) {
        try {
            Integer quantiteActuelle = stock.getQuantiteActuelle() != null ? stock.getQuantiteActuelle() : 0;

            stock.setQuantiteActuelle(quantiteActuelle - quantite);

            if (stock.getPrixMoyenPondere() != null) {
                stock.setValeurStock(stock.getPrixMoyenPondere().multiply(BigDecimal.valueOf(stock.getQuantiteActuelle())));
            }

            stock.setDerniereSortie(LocalDateTime.now());

            Integer quantiteReservee = stock.getQuantiteReservee() != null ? stock.getQuantiteReservee() : 0;
            stock.setQuantiteDisponible(stock.getQuantiteActuelle() - quantiteReservee);

            log.debug(messageService.getMessage("info.stock.updated", stock.getQuantiteActuelle()));

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du stock pour sortie", e);
            throw new StockManagementException("error.stock.update.failed", e);
        }
    }

    private BigDecimal calculateNewPMP(Integer quantiteActuelle, BigDecimal prixMoyenActuel,
                                       Integer quantiteEntree, BigDecimal prixEntree) {
        if (quantiteActuelle == 0 || prixMoyenActuel == null) {
            return prixEntree;
        } else {
            BigDecimal valeurActuelle = prixMoyenActuel.multiply(BigDecimal.valueOf(quantiteActuelle));
            BigDecimal valeurEntree = prixEntree.multiply(BigDecimal.valueOf(quantiteEntree));
            BigDecimal valeurTotale = valeurActuelle.add(valeurEntree);
            Integer quantiteTotale = quantiteActuelle + quantiteEntree;

            return valeurTotale.divide(BigDecimal.valueOf(quantiteTotale), 4, RoundingMode.HALF_UP);
        }
    }

    // ===============================
    // MÉTHODES PRIVÉES - UTILITAIRES
    // ===============================

    private Stock getOrCreateStock(Article article) {
        try {
            Optional<Stock> existingStock = stockRepository.findByArticleId(article.getId());

            if (existingStock.isPresent()) {
                return existingStock.get();
            } else {
                Stock newStock = createNewStock(article);
                Stock savedStock = stockRepository.save(newStock);
                log.info(messageService.getMessage("info.stock.created", article.getNom()));
                return savedStock;
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération/création du stock pour l'article: {}", article.getId(), e);
            throw new StockManagementException("error.stock.creation.failed", e);
        }
    }

    private Stock createNewStock(Article article) {
        Stock newStock = new Stock();
        newStock.setArticle(article);
        newStock.setQuantiteActuelle(0);
        newStock.setQuantiteReservee(0);
        newStock.setQuantiteDisponible(0);

        if (article.getPrixUnitaire() != null) {
            newStock.setPrixMoyenPondere(article.getPrixUnitaire());
        }

        newStock.setValeurStock(BigDecimal.ZERO);
        return newStock;
    }

    private StockMovementDTO convertToDTO(StockMovement movement) {
        try {
            StockMovementDTO dto = new StockMovementDTO();

            mapBasicFields(dto, movement);
            enrichWithArticleInfo(dto, movement);
            enrichWithSupplierInfo(dto, movement);
            finalizeDTO(dto);

            return dto;

        } catch (Exception e) {
            log.error("Erreur lors de la conversion du mouvement en DTO: {}", movement.getId(), e);
            throw new StockManagementException("error.conversion.failed", e);
        }
    }

    private void mapBasicFields(StockMovementDTO dto, StockMovement movement) {
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
    }

    private void enrichWithArticleInfo(StockMovementDTO dto, StockMovement movement) {
        if (movement.getArticle() != null) {
            dto.setArticleNom(movement.getArticle().getNom());
            dto.setArticleUnite(movement.getArticle().getUnite());
            dto.setArticleCategorie(movement.getArticle().getCategorie());
        }
    }

    private void enrichWithSupplierInfo(StockMovementDTO dto, StockMovement movement) {
        if (movement.getFournisseur() != null) {
            dto.setFournisseurId(movement.getFournisseur().getId());
            dto.setFournisseurNom(movement.getFournisseur().getNom());
        }
    }

    private void finalizeDTO(StockMovementDTO dto) {
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
    }

    // ===============================
    // MÉTHODES STATISTIQUES AVANCÉES
    // ===============================

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getMovementStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Calcul des statistiques de mouvements entre {} et {}", startDate, endDate);

        try {
            java.util.Map<String, Object> stats = new java.util.HashMap<>();

            // Compteurs par type
            Long totalEntries = stockMovementRepository.countByTypeMouvementAndDateMouvementBetween(
                    TypeMouvement.ENTREE, startDate, endDate);
            Long totalExits = stockMovementRepository.countByTypeMouvementAndDateMouvementBetween(
                    TypeMouvement.SORTIE, startDate, endDate);

            // Valeurs par type
            BigDecimal entryValue = getTotalEntryValue(startDate, endDate);
            BigDecimal exitValue = getTotalExitValue(startDate, endDate);

            stats.put("periode", new String[]{startDate.toString(), endDate.toString()});
            stats.put("totalEntries", totalEntries);
            stats.put("totalExits", totalExits);
            stats.put("totalMovements", totalEntries + totalExits);
            stats.put("entryValue", entryValue);
            stats.put("exitValue", exitValue);
            stats.put("netValue", entryValue.subtract(exitValue));
            stats.put("dateGeneration", LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            log.error("Erreur lors du calcul des statistiques", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getTopMovementsByValue(TypeMouvement typeMouvement, int limit) {
        log.debug("Récupération du top {} des mouvements par valeur pour le type: {}", limit, typeMouvement);

        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by("valeurTotale").descending());
            Page<StockMovement> movementPage = stockMovementRepository.findByTypeMouvement(typeMouvement, pageable);

            return movementPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération du top des mouvements par valeur", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        log.debug("Récupération des mouvements groupés par {} entre {} et {}", groupBy, startDate, endDate);

        try {
            List<StockMovement> movements = stockMovementRepository.findByDateMouvementBetweenOrderByDateMouvementDesc(startDate, endDate);

            // Groupement selon la période demandée (DAY, WEEK, MONTH)
            return movements.stream()
                    .collect(Collectors.groupingBy(
                            m -> formatDateForGrouping(m.getDateMouvement(), groupBy),
                            Collectors.toList()
                    ))
                    .entrySet().stream()
                    .map(entry -> {
                        java.util.Map<String, Object> group = new java.util.HashMap<>();
                        group.put("periode", entry.getKey());
                        group.put("totalMovements", entry.getValue().size());
                        group.put("entries", entry.getValue().stream().filter(m -> m.getTypeMouvement() == TypeMouvement.ENTREE).count());
                        group.put("exits", entry.getValue().stream().filter(m -> m.getTypeMouvement() == TypeMouvement.SORTIE).count());
                        group.put("totalValue", entry.getValue().stream()
                                .filter(m -> m.getValeurTotale() != null)
                                .map(StockMovement::getValeurTotale)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                        return group;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors du groupement des mouvements par date", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    private String formatDateForGrouping(LocalDateTime date, String groupBy) {
        switch (groupBy.toUpperCase()) {
            case "DAY":
                return date.toLocalDate().toString();
            case "WEEK":
                return date.getYear() + "-W" + date.getDayOfYear() / 7;
            case "MONTH":
                return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
            default:
                return date.toLocalDate().toString();
        }
    }

    // ===============================
    // MÉTHODES D'EXPORT ET RAPPORTS
    // ===============================

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> generateMovementReport(Long articleId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Génération de rapport pour article {} entre {} et {}", articleId, startDate, endDate);

        try {
            Article article = articleRepository.findById(articleId)
                    .orElseThrow(() -> new StockManagementException("error.article.not.found", articleId));

            List<StockMovement> movements = stockMovementRepository.findByArticleIdAndDateMouvementBetween(articleId, startDate, endDate);

            java.util.Map<String, Object> report = new java.util.HashMap<>();
            report.put("article", java.util.Map.of(
                    "id", article.getId(),
                    "nom", article.getNom(),
                    "unite", article.getUnite(),
                    "categorie", article.getCategorie()
            ));

            report.put("periode", new String[]{startDate.toString(), endDate.toString()});
            report.put("totalMouvements", movements.size());

            long entries = movements.stream().filter(m -> m.getTypeMouvement() == TypeMouvement.ENTREE).count();
            long exits = movements.stream().filter(m -> m.getTypeMouvement() == TypeMouvement.SORTIE).count();

            report.put("entries", entries);
            report.put("exits", exits);

            BigDecimal entryValue = movements.stream()
                    .filter(m -> m.getTypeMouvement() == TypeMouvement.ENTREE && m.getValeurTotale() != null)
                    .map(StockMovement::getValeurTotale)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal exitValue = movements.stream()
                    .filter(m -> m.getTypeMouvement() == TypeMouvement.SORTIE && m.getValeurTotale() != null)
                    .map(StockMovement::getValeurTotale)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            report.put("entryValue", entryValue);
            report.put("exitValue", exitValue);
            report.put("netValue", entryValue.subtract(exitValue));

            List<StockMovementDTO> movementDTOs = movements.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            report.put("mouvements", movementDTOs);
            report.put("dateGeneration", LocalDateTime.now());

            return report;

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport", e);
            throw new StockManagementException("error.report.generation.failed", e);
        }
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getArticleMovementSummary(List<Long> articleIds, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Génération de résumé pour {} articles entre {} et {}", articleIds.size(), startDate, endDate);

        try {
            return articleIds.stream()
                    .map(articleId -> {
                        try {
                            Article article = articleRepository.findById(articleId).orElse(null);
                            if (article == null) return null;

                            List<StockMovement> movements = stockMovementRepository.findByArticleIdAndDateMouvementBetween(articleId, startDate, endDate);

                            java.util.Map<String, Object> summary = new java.util.HashMap<>();
                            summary.put("articleId", articleId);
                            summary.put("articleNom", article.getNom());
                            summary.put("totalMouvements", movements.size());
                            summary.put("entries", movements.stream().filter(m -> m.getTypeMouvement() == TypeMouvement.ENTREE).count());
                            summary.put("exits", movements.stream().filter(m -> m.getTypeMouvement() == TypeMouvement.SORTIE).count());

                            return summary;
                        } catch (Exception e) {
                            log.warn("Erreur lors du traitement de l'article {}: {}", articleId, e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la génération du résumé des articles", e);
            throw new StockManagementException("error.retrieval.failed", e);
        }
    }

    // ===============================
    // MÉTHODES DE MAINTENANCE
    // ===============================

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getServiceHealth() {
        log.debug("Vérification de la santé du service");

        try {
            java.util.Map<String, Object> health = new java.util.HashMap<>();
            health.put("status", "UP");
            health.put("service", "StockMovementService");
            health.put("version", "1.0.0");
            health.put("timestamp", LocalDateTime.now());

            // Statistiques de base
            Long todayEntries = countTodayMovementsByType(TypeMouvement.ENTREE);
            Long todayExits = countTodayMovementsByType(TypeMouvement.SORTIE);

            health.put("todayStats", java.util.Map.of(
                    "entries", todayEntries,
                    "exits", todayExits,
                    "total", todayEntries + todayExits
            ));

            // Test de connectivité base de données
            Long totalMovements = stockMovementRepository.count();
            health.put("database", java.util.Map.of(
                    "status", "UP",
                    "totalMovements", totalMovements
            ));

            health.put("message", messageService.getMessage("info.service.operational"));

            return health;

        } catch (Exception e) {
            log.error("Erreur lors de la vérification de santé du service", e);
            java.util.Map<String, Object> errorHealth = new java.util.HashMap<>();
            errorHealth.put("status", "DOWN");
            errorHealth.put("error", e.getMessage());
            errorHealth.put("timestamp", LocalDateTime.now());
            return errorHealth;
        }
    }
}
