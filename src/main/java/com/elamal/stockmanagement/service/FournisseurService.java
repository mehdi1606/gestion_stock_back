        package com.elamal.stockmanagement.service;



        import com.elamal.stockmanagement.dto.FournisseurDTO;
        import com.elamal.stockmanagement.dto.PagedResponseDTO;
        import com.elamal.stockmanagement.dto.SearchCriteriaDTO;
        import com.elamal.stockmanagement.entity.Article;
        import com.elamal.stockmanagement.entity.Fournisseur;
        import com.elamal.stockmanagement.entity.StockMovement;
        import com.elamal.stockmanagement.repository.FournisseurRepository;
        import com.elamal.stockmanagement.repository.ArticleRepository;
        import com.elamal.stockmanagement.repository.StockMovementRepository;
        import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        import org.modelmapper.ModelMapper;
        import org.springframework.data.domain.Page;
        import org.springframework.data.domain.PageRequest;
        import org.springframework.data.domain.Pageable;
        import org.springframework.data.domain.Sort;
        import org.springframework.stereotype.Service;
        import org.springframework.transaction.annotation.Transactional;

        import java.time.LocalDateTime;
        import java.util.List;
        import java.util.Optional;
        import java.util.regex.Pattern;
        import java.util.stream.Collectors;

        @Service
        @RequiredArgsConstructor
        @Slf4j
        @Transactional
        public class FournisseurService {

            private final FournisseurRepository fournisseurRepository;
            private final ArticleRepository articleRepository;
            private final StockMovementRepository stockMovementRepository;
            private final ModelMapper modelMapper;

            // Patterns de validation
            private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
            private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\.\\(\\)]{6,20}$");

            // ===============================
            // OPÉRATIONS CRUD DE BASE
            // ===============================

            /**
             * Créer un nouveau fournisseur
             */
            public FournisseurDTO createFournisseur(FournisseurDTO fournisseurDTO) {
                log.info("Création d'un nouveau fournisseur: {}", fournisseurDTO.getNom());

                // Validation métier
                validateFournisseurForCreation(fournisseurDTO);

                // Conversion DTO -> Entity
                Fournisseur fournisseur = modelMapper.map(fournisseurDTO, Fournisseur.class);

                // Normalisation des données
                normalizeFournisseurData(fournisseur);

                // Sauvegarde
                Fournisseur savedFournisseur = fournisseurRepository.save(fournisseur);

                log.info("Fournisseur créé avec succès: ID={}, Nom={}", savedFournisseur.getId(), savedFournisseur.getNom());

                return convertToDTO(savedFournisseur);
            }

            /**
             * Mettre à jour un fournisseur existant
             */
            public FournisseurDTO updateFournisseur(Long id, FournisseurDTO fournisseurDTO) {
                log.info("Mise à jour du fournisseur ID: {}", id);

                Fournisseur existingFournisseur = fournisseurRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + id));

                // Validation métier pour mise à jour
                validateFournisseurForUpdate(id, fournisseurDTO);

                // Mise à jour des champs
                updateFournisseurFields(existingFournisseur, fournisseurDTO);

                // Normalisation des données
                normalizeFournisseurData(existingFournisseur);

                Fournisseur updatedFournisseur = fournisseurRepository.save(existingFournisseur);

                log.info("Fournisseur mis à jour avec succès: ID={}, Nom={}", updatedFournisseur.getId(), updatedFournisseur.getNom());

                return convertToDTO(updatedFournisseur);
            }

            /**
             * Supprimer un fournisseur (suppression logique)
             */
            public void deleteFournisseur(Long id) {
                log.info("Suppression du fournisseur ID: {}", id);

                Fournisseur fournisseur = fournisseurRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + id));

                // Vérifications avant suppression
                validateFournisseurForDeletion(fournisseur);

                // Suppression logique (désactivation)
                fournisseur.setActif(false);
                fournisseurRepository.save(fournisseur);

                log.info("Fournisseur supprimé (désactivé) avec succès: ID={}, Nom={}", fournisseur.getId(), fournisseur.getNom());
            }

            /**
             * Réactiver un fournisseur
             */
            public FournisseurDTO reactivateFournisseur(Long id) {
                log.info("Réactivation du fournisseur ID: {}", id);

                Fournisseur fournisseur = fournisseurRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + id));

                fournisseur.setActif(true);
                Fournisseur reactivatedFournisseur = fournisseurRepository.save(fournisseur);

                log.info("Fournisseur réactivé avec succès: ID={}, Nom={}", reactivatedFournisseur.getId(), reactivatedFournisseur.getNom());

                return convertToDTO(reactivatedFournisseur);
            }

            // ===============================
            // OPÉRATIONS DE LECTURE
            // ===============================

            /**
             * Récupérer un fournisseur par ID
             */
            @Transactional(readOnly = true)
            public FournisseurDTO getFournisseurById(Long id) {
                log.debug("Récupération du fournisseur ID: {}", id);

                Fournisseur fournisseur = fournisseurRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + id));

                return convertToDTO(fournisseur);
            }

            /**
             * Récupérer un fournisseur par Nom
             */
            @Transactional(readOnly = true)
            public FournisseurDTO getFournisseurByNom(String Nom) {
                log.debug("Récupération du fournisseur par Nom: {}", Nom);

                Fournisseur fournisseur = fournisseurRepository.findByNom(Nom)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec le Nom: " + Nom));

                return convertToDTO(fournisseur);
            }

            /**
             * Récupérer tous les fournisseurs actifs
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getAllActiveFournisseurs() {
                log.debug("Récupération de tous les fournisseurs actifs");

                List<Fournisseur> fournisseurs = fournisseurRepository.findByActifTrue();
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Récupérer tous les fournisseurs avec pagination
             */
            @Transactional(readOnly = true)
            public PagedResponseDTO<FournisseurDTO> getAllFournisseurs(int page, int size, String sortBy, String sortDirection) {
                log.debug("Récupération des fournisseurs - Page: {}, Size: {}, Sort: {} {}", page, size, sortBy, sortDirection);

                Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Fournisseur> fournisseurPage = fournisseurRepository.findAll(pageable);

                List<FournisseurDTO> fournisseurDTOs = fournisseurPage.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());

                return new PagedResponseDTO<>(
                        fournisseurDTOs,
                        fournisseurPage.getNumber(),
                        fournisseurPage.getSize(),
                        fournisseurPage.getTotalElements(),
                        fournisseurPage.getTotalPages()
                );
            }

            /**
             * Recherche de fournisseurs avec critères
             */
            @Transactional(readOnly = true)
            public PagedResponseDTO<FournisseurDTO> searchFournisseurs(SearchCriteriaDTO criteria) {
                log.debug("Recherche de fournisseurs avec critères: {}", criteria);

                Sort sort = Sort.by(Sort.Direction.fromString(criteria.getSortDirection()), criteria.getSortBy());
                Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

                Page<Fournisseur> fournisseurPage = fournisseurRepository.findWithCriteria(
                        criteria.getQuery(),
                        null, // ville - peut être ajouté aux critères
                        null, // pays - peut être ajouté aux critères
                        criteria.getActif(),
                        pageable
                );

                List<FournisseurDTO> fournisseurDTOs = fournisseurPage.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());

                return new PagedResponseDTO<>(
                        fournisseurDTOs,
                        fournisseurPage.getNumber(),
                        fournisseurPage.getSize(),
                        fournisseurPage.getTotalElements(),
                        fournisseurPage.getTotalPages()
                );
            }

            /**
             * Recherche textuelle de fournisseurs
             */
            @Transactional(readOnly = true)
            public PagedResponseDTO<FournisseurDTO> searchFournisseursByText(String searchTerm, int page, int size) {
                log.debug("Recherche textuelle de fournisseurs: {}", searchTerm);

                Pageable pageable = PageRequest.of(page, size, Sort.by("nom"));
                Page<Fournisseur> fournisseurPage = fournisseurRepository.searchFournisseurs(searchTerm, pageable);

                List<FournisseurDTO> fournisseurDTOs = fournisseurPage.getContent().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());

                return new PagedResponseDTO<>(
                        fournisseurDTOs,
                        fournisseurPage.getNumber(),
                        fournisseurPage.getSize(),
                        fournisseurPage.getTotalElements(),
                        fournisseurPage.getTotalPages()
                );
            }

            // ===============================
            // RECHERCHE SPÉCIALISÉE
            // ===============================

            /**
             * Recherche par nom (insensible à la casse)
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursByNom(String nom) {
                log.debug("Recherche de fournisseurs par nom: {}", nom);

                List<Fournisseur> fournisseurs = fournisseurRepository.findByNomContainingIgnoreCase(nom);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Recherche par ville
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursByVille(String ville) {
                log.debug("Recherche de fournisseurs par ville: {}", ville);

                List<Fournisseur> fournisseurs = fournisseurRepository.findByVille(ville);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Recherche par pays
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursByPays(String pays) {
                log.debug("Recherche de fournisseurs par pays: {}", pays);

                List<Fournisseur> fournisseurs = fournisseurRepository.findByPays(pays);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Recherche par délai de livraison maximum
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursByMaxDelaiLivraison(Integer maxDelai) {
                log.debug("Recherche de fournisseurs avec délai de livraison <= {} jours", maxDelai);

                List<Fournisseur> fournisseurs = fournisseurRepository.findByDelaiLivraisonLessThanEqualAndActifTrue(maxDelai);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            // ===============================
            // ANALYSES ET STATISTIQUES
            // ===============================

            /**
             * Récupérer les fournisseurs avec le plus d'articles
             */
            @Transactional(readOnly = true)
            public List<Object[]> getFournisseursWithMostArticles(int limit) {
                log.debug("Récupération du top {} fournisseurs par nombre d'articles", limit);

                Pageable pageable = PageRequest.of(0, limit);
                return fournisseurRepository.findFournisseursWithMostArticles(pageable);
            }

            /**
             * Récupérer les fournisseurs avec mouvements récents
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursWithRecentMovements(int days) {
                log.debug("Récupération des fournisseurs avec mouvements sur {} derniers jours", days);

                LocalDateTime since = LocalDateTime.now().minusDays(days);
                List<Fournisseur> fournisseurs = fournisseurRepository.findFournisseursWithRecentMovements(since);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Récupérer les fournisseurs sans mouvement récent
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursWithoutRecentMovements(int days) {
                log.debug("Récupération des fournisseurs sans mouvement depuis {} jours", days);

                List<Fournisseur> fournisseurs = fournisseurRepository.findFournisseursWithoutMovementSince(days);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Top fournisseurs par valeur d'achats
             */
            @Transactional(readOnly = true)
            public List<Object[]> getTopFournisseursByPurchaseValue(int days, int limit) {
                log.debug("Top {} fournisseurs par valeur d'achats sur {} jours", limit, days);

                LocalDateTime since = LocalDateTime.now().minusDays(days);
                Pageable pageable = PageRequest.of(0, limit);
                return fournisseurRepository.findTopFournisseursByPurchaseValue(since, pageable);
            }

            /**
             * Top fournisseurs par nombre de livraisons
             */
            @Transactional(readOnly = true)
            public List<Object[]> getTopFournisseursByDeliveryCount(int days, int limit) {
                log.debug("Top {} fournisseurs par nombre de livraisons sur {} jours", limit, days);

                LocalDateTime since = LocalDateTime.now().minusDays(days);
                Pageable pageable = PageRequest.of(0, limit);
                return fournisseurRepository.findFournisseursByDeliveryCount(since, pageable);
            }

            /**
             * Statistiques par ville
             */
            @Transactional(readOnly = true)
            public List<Object[]> getStatisticsByCity() {
                log.debug("Récupération des statistiques par ville");

                return fournisseurRepository.getStatisticsByCity();
            }

            /**
             * Statistiques par pays
             */
            @Transactional(readOnly = true)
            public List<Object[]> getStatisticsByCountry() {
                log.debug("Récupération des statistiques par pays");

                return fournisseurRepository.getStatisticsByCountry();
            }

            /**
             * Compter les fournisseurs par statut
             */
            @Transactional(readOnly = true)
            public Long countFournisseursByStatus(Boolean actif) {
                log.debug("Comptage des fournisseurs par statut actif: {}", actif);

                return fournisseurRepository.countByActif(actif);
            }

            // ===============================
            // GESTION PAR CONDITIONS
            // ===============================

            /**
             * Fournisseurs par conditions de paiement
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursByConditionsPaiement(String conditionsPaiement) {
                log.debug("Recherche de fournisseurs par conditions de paiement: {}", conditionsPaiement);

                List<Fournisseur> fournisseurs = fournisseurRepository.findByConditionsPaiementAndActifTrue(conditionsPaiement);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            /**
             * Fournisseurs par fourchette de délai de livraison
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursByDelaiLivraisonRange(Integer minDelai, Integer maxDelai) {
                log.debug("Recherche de fournisseurs avec délai de livraison entre {} et {} jours", minDelai, maxDelai);

                List<Fournisseur> fournisseurs = fournisseurRepository.findByDelaiLivraisonBetweenAndActifTrue(minDelai, maxDelai);
                return fournisseurs.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            // ===============================
            // VALIDATION ET VÉRIFICATION
            // ===============================

            /**
             * Vérifier si un Nom fournisseur existe
             */
            @Transactional(readOnly = true)
            public boolean existsByNom(String Nom) {
                return fournisseurRepository.existsByNom(Nom);
            }

            /**
             * Vérifier si un Nom fournisseur existe (en excluant un ID)
             */
            @Transactional(readOnly = true)
            public boolean existsByNomAndIdNot(String Nom, Long id) {
                return fournisseurRepository.existsByNomAndIdNot(Nom, id);
            }

            /**
             * Vérifier si un email existe (en excluant un ID)
             */
            @Transactional(readOnly = true)
            public boolean existsByEmailAndIdNot(String email, Long id) {
                return fournisseurRepository.existsByEmailAndIdNot(email, id);
            }

            /**
             * Vérifier si un téléphone existe (en excluant un ID)
             */
            @Transactional(readOnly = true)
            public boolean existsByTelephoneAndIdNot(String telephone, Long id) {
                return fournisseurRepository.existsByTelephoneAndIdNot(telephone, id);
            }

            // ===============================
            // MÉTHODES PRIVÉES - VALIDATION
            // ===============================

            private void validateFournisseurForCreation(FournisseurDTO fournisseurDTO) {
                // Validation du Nom
                if (fournisseurDTO.getNom() == null || fournisseurDTO.getNom().trim().isEmpty()) {
                    throw new IllegalArgumentException("Le Nom fournisseur est obligatoire");
                }

                if (existsByNom(fournisseurDTO.getNom())) {
                    throw new IllegalArgumentException("Un fournisseur avec ce Nom existe déjà: " + fournisseurDTO.getNom());
                }

                // Validation du nom
                if (fournisseurDTO.getNom() == null || fournisseurDTO.getNom().trim().isEmpty()) {
                    throw new IllegalArgumentException("Le nom du fournisseur est obligatoire");
                }

                // Validation de l'email
                if (fournisseurDTO.getEmail() != null && !fournisseurDTO.getEmail().trim().isEmpty()) {
                    if (!EMAIL_PATTERN.matcher(fournisseurDTO.getEmail()).matches()) {
                        throw new IllegalArgumentException("Format d'email invalide: " + fournisseurDTO.getEmail());
                    }

                    Optional<Fournisseur> existingByEmail = fournisseurRepository.findByEmail(fournisseurDTO.getEmail());
                    if (existingByEmail.isPresent()) {
                        throw new IllegalArgumentException("Un fournisseur avec cet email existe déjà: " + fournisseurDTO.getEmail());
                    }
                }

                // Validation du téléphone
                if (fournisseurDTO.getTelephone() != null && !fournisseurDTO.getTelephone().trim().isEmpty()) {
                    if (!PHONE_PATTERN.matcher(fournisseurDTO.getTelephone()).matches()) {
                        throw new IllegalArgumentException("Format de téléphone invalide: " + fournisseurDTO.getTelephone());
                    }

                    Optional<Fournisseur> existingByPhone = fournisseurRepository.findByTelephone(fournisseurDTO.getTelephone());
                    if (existingByPhone.isPresent()) {
                        throw new IllegalArgumentException("Un fournisseur avec ce téléphone existe déjà: " + fournisseurDTO.getTelephone());
                    }
                }

                // Validation du délai de livraison
                if (fournisseurDTO.getDelaiLivraison() != null && fournisseurDTO.getDelaiLivraison() < 0) {
                    throw new IllegalArgumentException("Le délai de livraison ne peut pas être négatif");
                }

                if (fournisseurDTO.getDelaiLivraison() != null && fournisseurDTO.getDelaiLivraison() > 365) {
                    throw new IllegalArgumentException("Le délai de livraison ne peut pas dépasser 365 jours");
                }
            }

            private void validateFournisseurForUpdate(Long id, FournisseurDTO fournisseurDTO) {
                // Validation du Nom (unicité)
                if (fournisseurDTO.getNom() != null && existsByNomAndIdNot(fournisseurDTO.getNom(), id)) {
                    throw new IllegalArgumentException("Un autre fournisseur avec ce Nom existe déjà: " + fournisseurDTO.getNom());
                }

                // Validation du nom
                if (fournisseurDTO.getNom() != null && fournisseurDTO.getNom().trim().isEmpty()) {
                    throw new IllegalArgumentException("Le nom du fournisseur ne peut pas être vide");
                }

                // Validation de l'email
                if (fournisseurDTO.getEmail() != null && !fournisseurDTO.getEmail().trim().isEmpty()) {
                    if (!EMAIL_PATTERN.matcher(fournisseurDTO.getEmail()).matches()) {
                        throw new IllegalArgumentException("Format d'email invalide: " + fournisseurDTO.getEmail());
                    }

                    if (existsByEmailAndIdNot(fournisseurDTO.getEmail(), id)) {
                        throw new IllegalArgumentException("Un autre fournisseur avec cet email existe déjà: " + fournisseurDTO.getEmail());
                    }
                }

                // Validation du téléphone
                if (fournisseurDTO.getTelephone() != null && !fournisseurDTO.getTelephone().trim().isEmpty()) {
                    if (!PHONE_PATTERN.matcher(fournisseurDTO.getTelephone()).matches()) {
                        throw new IllegalArgumentException("Format de téléphone invalide: " + fournisseurDTO.getTelephone());
                    }

                    if (existsByTelephoneAndIdNot(fournisseurDTO.getTelephone(), id)) {
                        throw new IllegalArgumentException("Un autre fournisseur avec ce téléphone existe déjà: " + fournisseurDTO.getTelephone());
                    }
                }

                // Validation du délai de livraison
                if (fournisseurDTO.getDelaiLivraison() != null && fournisseurDTO.getDelaiLivraison() < 0) {
                    throw new IllegalArgumentException("Le délai de livraison ne peut pas être négatif");
                }

                if (fournisseurDTO.getDelaiLivraison() != null && fournisseurDTO.getDelaiLivraison() > 365) {
                    throw new IllegalArgumentException("Le délai de livraison ne peut pas dépasser 365 jours");
                }
            }

            private void validateFournisseurForDeletion(Fournisseur fournisseur) {
                // Vérifier s'il y a des articles liés
                List<Article> articles = articleRepository.findByFournisseurPrincipalId(fournisseur.getId());
                if (!articles.isEmpty()) {
                    throw new IllegalStateException("Impossible de supprimer un fournisseur ayant des articles associés");
                }

                // Vérifier s'il y a des mouvements récents (30 derniers jours)
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
                List<StockMovement> recentMovements = stockMovementRepository.findByFournisseurIdOrderByDateMouvementDesc(fournisseur.getId());
                if (!recentMovements.isEmpty()) {
                    // Vérification plus fine si nécessaire
                    log.warn("Tentative de suppression d'un fournisseur avec des mouvements récents: {}", fournisseur.getNom());
                }
            }

            // ===============================
            // MÉTHODES PRIVÉES - UTILITAIRES
            // ===============================

            private void updateFournisseurFields(Fournisseur existingFournisseur, FournisseurDTO fournisseurDTO) {
                if (fournisseurDTO.getNom() != null) {
                    existingFournisseur.setNom(fournisseurDTO.getNom());
                }
                if (fournisseurDTO.getNom() != null) {
                    existingFournisseur.setNom(fournisseurDTO.getNom());
                }
                if (fournisseurDTO.getRaisonSociale() != null) {
                    existingFournisseur.setRaisonSociale(fournisseurDTO.getRaisonSociale());
                }
                if (fournisseurDTO.getAdresse() != null) {
                    existingFournisseur.setAdresse(fournisseurDTO.getAdresse());
                }
                if (fournisseurDTO.getVille() != null) {
                    existingFournisseur.setVille(fournisseurDTO.getVille());
                }
                if (fournisseurDTO.getCodePostal() != null) {
                    existingFournisseur.setCodePostal(fournisseurDTO.getCodePostal());
                }
                if (fournisseurDTO.getPays() != null) {
                    existingFournisseur.setPays(fournisseurDTO.getPays());
                }
                if (fournisseurDTO.getTelephone() != null) {
                    existingFournisseur.setTelephone(fournisseurDTO.getTelephone());
                }
                if (fournisseurDTO.getFax() != null) {
                    existingFournisseur.setFax(fournisseurDTO.getFax());
                }
                if (fournisseurDTO.getEmail() != null) {
                    existingFournisseur.setEmail(fournisseurDTO.getEmail());
                }
                if (fournisseurDTO.getSiteWeb() != null) {
                    existingFournisseur.setSiteWeb(fournisseurDTO.getSiteWeb());
                }
                if (fournisseurDTO.getContactPrincipal() != null) {
                    existingFournisseur.setContactPrincipal(fournisseurDTO.getContactPrincipal());
                }
                if (fournisseurDTO.getTelephoneContact() != null) {
                    existingFournisseur.setTelephoneContact(fournisseurDTO.getTelephoneContact());
                }
                if (fournisseurDTO.getEmailContact() != null) {
                    existingFournisseur.setEmailContact(fournisseurDTO.getEmailContact());
                }
                if (fournisseurDTO.getConditionsPaiement() != null) {
                    existingFournisseur.setConditionsPaiement(fournisseurDTO.getConditionsPaiement());
                }
                if (fournisseurDTO.getDelaiLivraison() != null) {
                    existingFournisseur.setDelaiLivraison(fournisseurDTO.getDelaiLivraison());
                }
                if (fournisseurDTO.getActif() != null) {
                    existingFournisseur.setActif(fournisseurDTO.getActif());
                }
                if (fournisseurDTO.getNotes() != null) {
                    existingFournisseur.setNotes(fournisseurDTO.getNotes());
                }
            }

            private void normalizeFournisseurData(Fournisseur fournisseur) {
                // Normalisation du Nom (majuscules)
                if (fournisseur.getNom() != null) {
                    fournisseur.setNom(fournisseur.getNom().trim().toUpperCase());
                }

                // Normalisation du nom
                if (fournisseur.getNom() != null) {
                    fournisseur.setNom(fournisseur.getNom().trim());
                }

                // Normalisation de l'email (minuscules)
                if (fournisseur.getEmail() != null) {
                    fournisseur.setEmail(fournisseur.getEmail().trim().toLowerCase());
                }

                // Normalisation de l'email de contact (minuscules)
                if (fournisseur.getEmailContact() != null) {
                    fournisseur.setEmailContact(fournisseur.getEmailContact().trim().toLowerCase());
                }

                // Normalisation du téléphone (suppression des espaces)
                if (fournisseur.getTelephone() != null) {
                    fournisseur.setTelephone(fournisseur.getTelephone().replaceAll("\\s+", ""));
                }

                // Normalisation du téléphone de contact
                if (fournisseur.getTelephoneContact() != null) {
                    fournisseur.setTelephoneContact(fournisseur.getTelephoneContact().replaceAll("\\s+", ""));
                }

                // Normalisation du fax
                if (fournisseur.getFax() != null) {
                    fournisseur.setFax(fournisseur.getFax().replaceAll("\\s+", ""));
                }

                // Normalisation de la ville (première lettre majuscule)
                if (fournisseur.getVille() != null) {
                    fournisseur.setVille(capitalizeFirstLetter(fournisseur.getVille().trim()));
                }

                // Normalisation du pays (première lettre majuscule)
                if (fournisseur.getPays() != null) {
                    fournisseur.setPays(capitalizeFirstLetter(fournisseur.getPays().trim()));
                }

                // Normalisation du site web (ajout de http:// si nécessaire)
                if (fournisseur.getSiteWeb() != null && !fournisseur.getSiteWeb().trim().isEmpty()) {
                    String siteWeb = fournisseur.getSiteWeb().trim().toLowerCase();
                    if (!siteWeb.startsWith("http://") && !siteWeb.startsWith("https://")) {
                        siteWeb = "http://" + siteWeb;
                    }
                    fournisseur.setSiteWeb(siteWeb);
                }
            }

            private String capitalizeFirstLetter(String text) {
                if (text == null || text.isEmpty()) {
                    return text;
                }
                return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            }

            private FournisseurDTO convertToDTO(Fournisseur fournisseur) {
                FournisseurDTO dto = modelMapper.map(fournisseur, FournisseurDTO.class);

                // Ajouter les statistiques calculées
                enrichDTOWithStatistics(dto, fournisseur);

                return dto;
            }

            private void enrichDTOWithStatistics(FournisseurDTO dto, Fournisseur fournisseur) {
                try {
                    // Nombre d'articles liés
                    List<Article> articles = articleRepository.findByFournisseurPrincipalId(fournisseur.getId());
                    dto.setNombreArticles(articles.size());

                    // Nombre de mouvements
                    List<StockMovement> mouvements = stockMovementRepository.findByFournisseurIdOrderByDateMouvementDesc(fournisseur.getId());
                    dto.setNombreMouvements(mouvements.size());

                    // Dernier mouvement
                    if (!mouvements.isEmpty()) {
                        // Extraction de la date du dernier mouvement
                        // Cette logique dépend de la structure exacte retournée par la requête
                        dto.setDerniereMouvement(LocalDateTime.now()); // Placeholder
                    }

                } catch (Exception e) {
                    log.warn("Erreur lors de l'enrichissement des statistiques pour le fournisseur {}: {}",
                            fournisseur.getId(), e.getMessage());
                    // Valeurs par défaut en cas d'erreur
                    dto.setNombreArticles(0);
                    dto.setNombreMouvements(0);
                }
            }

            // ===============================
            // MÉTHODES UTILITAIRES PUBLIQUES
            // ===============================

            /**
             * Obtenir toutes les villes des fournisseurs
             */
            @Transactional(readOnly = true)
            public List<String> getAllCities() {
                log.debug("Récupération de toutes les villes des fournisseurs");

                return fournisseurRepository.findAll().stream()
                        .map(Fournisseur::getVille)
                        .filter(ville -> ville != null && !ville.trim().isEmpty())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
            }

            /**
             * Obtenir tous les pays des fournisseurs
             */
            @Transactional(readOnly = true)
            public List<String> getAllCountries() {
                log.debug("Récupération de tous les pays des fournisseurs");

                return fournisseurRepository.findAll().stream()
                        .map(Fournisseur::getPays)
                        .filter(pays -> pays != null && !pays.trim().isEmpty())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
            }

            /**
             * Obtenir toutes les conditions de paiement
             */
            @Transactional(readOnly = true)
            public List<String> getAllConditionsPaiement() {
                log.debug("Récupération de toutes les conditions de paiement");

                return fournisseurRepository.findAll().stream()
                        .map(Fournisseur::getConditionsPaiement)
                        .filter(condition -> condition != null && !condition.trim().isEmpty())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
            }

            /**
             * Vérifier la validité des informations de contact
             */
            @Transactional(readOnly = true)
            public boolean isContactInfoValid(Long fournisseurId) {
                log.debug("Vérification des informations de contact pour le fournisseur ID: {}", fournisseurId);

                Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + fournisseurId));

                // Un fournisseur a des informations de contact valides s'il a au moins :
                // - Un téléphone OU un email
                // - Une adresse complète (adresse + ville)

                boolean hasValidPhone = fournisseur.getTelephone() != null &&
                        !fournisseur.getTelephone().trim().isEmpty() &&
                        PHONE_PATTERN.matcher(fournisseur.getTelephone()).matches();

                boolean hasValidEmail = fournisseur.getEmail() != null &&
                        !fournisseur.getEmail().trim().isEmpty() &&
                        EMAIL_PATTERN.matcher(fournisseur.getEmail()).matches();

                boolean hasValidAddress = fournisseur.getAdresse() != null &&
                        !fournisseur.getAdresse().trim().isEmpty() &&
                        fournisseur.getVille() != null &&
                        !fournisseur.getVille().trim().isEmpty();

                return (hasValidPhone || hasValidEmail) && hasValidAddress;
            }

            /**
             * Obtenir les fournisseurs avec informations incomplètes
             */
            @Transactional(readOnly = true)
            public List<FournisseurDTO> getFournisseursWithIncompleteInfo() {
                log.debug("Récupération des fournisseurs avec informations incomplètes");

                List<Fournisseur> allFournisseurs = fournisseurRepository.findByActifTrue();

                return allFournisseurs.stream()
                        .filter(f -> !isContactInfoValidForFournisseur(f))
                        .map(this::convertToDTO)
                        .collect(Collectors.toList());
            }

            private boolean isContactInfoValidForFournisseur(Fournisseur fournisseur) {
                boolean hasValidPhone = fournisseur.getTelephone() != null &&
                        !fournisseur.getTelephone().trim().isEmpty() &&
                        PHONE_PATTERN.matcher(fournisseur.getTelephone()).matches();

                boolean hasValidEmail = fournisseur.getEmail() != null &&
                        !fournisseur.getEmail().trim().isEmpty() &&
                        EMAIL_PATTERN.matcher(fournisseur.getEmail()).matches();

                boolean hasValidAddress = fournisseur.getAdresse() != null &&
                        !fournisseur.getAdresse().trim().isEmpty() &&
                        fournisseur.getVille() != null &&
                        !fournisseur.getVille().trim().isEmpty();

                return (hasValidPhone || hasValidEmail) && hasValidAddress;
            }

            /**
             * Dupliquer un fournisseur (pour créer un fournisseur similaire)
             */
            public FournisseurDTO duplicateFournisseur(Long fournisseurId, String newNom) {
                log.info("Duplication du fournisseur ID: {} avec nouveau Nom: {}", fournisseurId, newNom);

                Fournisseur originalFournisseur = fournisseurRepository.findById(fournisseurId)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + fournisseurId));

                // Validation du nouveau Nom
                if (existsByNom(newNom)) {
                    throw new IllegalArgumentException("Un fournisseur avec ce Nom existe déjà: " + newNom);
                }

                // Création du nouveau fournisseur
                Fournisseur newFournisseur = new Fournisseur();

                // Copie des informations (sauf ID, Nom, nom, et timestamps)
                newFournisseur.setNom(newNom);
                newFournisseur.setNom(newNom);
                newFournisseur.setRaisonSociale(originalFournisseur.getRaisonSociale());
                newFournisseur.setAdresse(originalFournisseur.getAdresse());
                newFournisseur.setVille(originalFournisseur.getVille());
                newFournisseur.setCodePostal(originalFournisseur.getCodePostal());
                newFournisseur.setPays(originalFournisseur.getPays());
                newFournisseur.setSiteWeb(originalFournisseur.getSiteWeb());
                newFournisseur.setContactPrincipal(originalFournisseur.getContactPrincipal());
                newFournisseur.setConditionsPaiement(originalFournisseur.getConditionsPaiement());
                newFournisseur.setDelaiLivraison(originalFournisseur.getDelaiLivraison());
                newFournisseur.setActif(true);
                newFournisseur.setNotes("Dupliqué depuis: " + originalFournisseur.getNom());

                // Normalisation des données
                normalizeFournisseurData(newFournisseur);

                Fournisseur savedFournisseur = fournisseurRepository.save(newFournisseur);

                log.info("Fournisseur dupliqué avec succès: ID={}, Nom={}", savedFournisseur.getId(), savedFournisseur.getNom());

                return convertToDTO(savedFournisseur);
            }

            /**
             * Fusionner deux fournisseurs (garder le premier, transférer les données du second)
             */
            @Transactional
            public FournisseurDTO mergeFournisseurs(Long keepFournisseurId, Long deleteFournisseurId) {
                log.info("Fusion des fournisseurs: garder ID={}, supprimer ID={}", keepFournisseurId, deleteFournisseurId);

                Fournisseur keepFournisseur = fournisseurRepository.findById(keepFournisseurId)
                        .orElseThrow(() -> new RuntimeException("Fournisseur à garder introuvable avec l'ID: " + keepFournisseurId));

                Fournisseur deleteFournisseur = fournisseurRepository.findById(deleteFournisseurId)
                        .orElseThrow(() -> new RuntimeException("Fournisseur à supprimer introuvable avec l'ID: " + deleteFournisseurId));

                // Transférer les articles du fournisseur à supprimer
                List<Article> articlesToTransfer = articleRepository.findByFournisseurPrincipalId(deleteFournisseurId);
                for (Article articleData : articlesToTransfer) {
                    // Logic to update article's supplier would go here
                    // This requires accessing the Article entity properly
                }

                // Ajouter une note sur la fusion
                String mergeNote = String.format("Fusionné avec fournisseur %s (%s) le %s",
                        deleteFournisseur.getNom(),
                        deleteFournisseur.getNom(),
                        LocalDateTime.now().toString());

                if (keepFournisseur.getNotes() != null && !keepFournisseur.getNotes().trim().isEmpty()) {
                    keepFournisseur.setNotes(keepFournisseur.getNotes() + "\n" + mergeNote);
                } else {
                    keepFournisseur.setNotes(mergeNote);
                }

                // Supprimer le fournisseur (suppression logique)
                deleteFournisseur.setActif(false);
                deleteFournisseur.setNotes(deleteFournisseur.getNotes() + "\nFusionné avec " + keepFournisseur.getNom());

                fournisseurRepository.save(deleteFournisseur);
                Fournisseur updatedKeepFournisseur = fournisseurRepository.save(keepFournisseur);

                log.info("Fusion des fournisseurs terminée avec succès");

                return convertToDTO(updatedKeepFournisseur);
            }

            /**
             * Obtenir un rapport de performance d'un fournisseur
             */
            @Transactional(readOnly = true)
            public Object[] getFournisseurPerformanceReport(Long fournisseurId, int days) {
                log.debug("Génération du rapport de performance pour le fournisseur ID: {} sur {} jours", fournisseurId, days);

                Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable avec l'ID: " + fournisseurId));

                LocalDateTime since = LocalDateTime.now().minusDays(days);

                // Collecte des métriques de performance
                // Cette méthode retournerait un objet avec :
                // - Nombre de livraisons
                // - Valeur totale des achats
                // - Délai moyen de livraison respecté
                // - Nombre d'articles fournis
                // - Évaluation globale

                return new Object[]{
                        fournisseur.getId(),
                        fournisseur.getNom(),
                        0, // Nombre de livraisons (à calculer)
                        0, // Valeur totale (à calculer)
                        fournisseur.getDelaiLivraison(),
                        0, // Délai moyen réel (à calculer)
                        "A évaluer" // Performance globale
                };
            }
        }
