    package com.elamal.stockmanagement.service;

    import com.elamal.stockmanagement.dto.DashboardDTO;
    import com.elamal.stockmanagement.dto.DashboardDTO.*;
    import com.elamal.stockmanagement.entity.*;
    import com.elamal.stockmanagement.repository.*;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import java.math.BigDecimal;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.*;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    @Transactional(readOnly = true)
    public class DashboardService {

        private final ArticleRepository articleRepository;
        private final FournisseurRepository fournisseurRepository;
        private final StockRepository stockRepository;
        private final StockMovementRepository stockMovementRepository;

        // ===============================
        // DASHBOARD PRINCIPAL
        // ===============================

        /**
         * Récupérer toutes les données du dashboard
         */
        public DashboardDTO getDashboardData() {
            log.info("Génération des données du dashboard");

            // Génération des statistiques générales
            StatsGeneralesDTO statsGenerales = generateGeneralStats();

            // Génération des données pour les graphiques
            List<ChartDataDTO> stockParCategorie = generateStockByCategory();
            List<ChartDataDTO> mouvementsTendance = generateMovementsTrend(7);
            List<ChartDataDTO> topArticlesConsommes = generateTopConsumedArticles(30, 10);

            // Génération des alertes
            List<AlerteStockDTO> alertes = generateStockAlerts();

            // Génération des activités récentes
            List<ActiviteRecenteDTO> activitesRecentes = generateRecentActivities(10);

            DashboardDTO dashboard = new DashboardDTO();
            dashboard.setStatsGenerales(statsGenerales);
            dashboard.setStockParCategorie(stockParCategorie);
            dashboard.setMouvementsTendance(mouvementsTendance);
            dashboard.setTopArticlesConsommes(topArticlesConsommes);
            dashboard.setAlertes(alertes);
            dashboard.setActivitesRecentes(activitesRecentes);

            log.info("Dashboard généré avec succès - {} alertes, {} activités",
                    alertes.size(), activitesRecentes.size());

            return dashboard;
        }

        /**
         * Récupérer uniquement les statistiques générales
         */
        public StatsGeneralesDTO getGeneralStatistics() {
            log.debug("Génération des statistiques générales");

            return generateGeneralStats();
        }

        /**
         * Récupérer les données de stock par catégorie
         */
        public List<ChartDataDTO> getStockByCategory() {
            log.debug("Génération des données de stock par catégorie");

            return generateStockByCategory();
        }

        /**
         * Récupérer la tendance des mouvements
         */
        public List<ChartDataDTO> getMovementsTrend(int days) {
            log.debug("Génération de la tendance des mouvements sur {} jours", days);

            return generateMovementsTrend(days);
        }

        /**
         * Récupérer les articles les plus consommés
         */
        public List<ChartDataDTO> getTopConsumedArticles(int days, int limit) {
            log.debug("Génération du top {} articles consommés sur {} jours", limit, days);

            return generateTopConsumedArticles(days, limit);
        }

        /**
         * Récupérer toutes les alertes de stock
         */
        public List<AlerteStockDTO> getStockAlerts() {
            log.debug("Génération des alertes de stock");

            return generateStockAlerts();
        }

        /**
         * Récupérer les activités récentes
         */
        public List<ActiviteRecenteDTO> getRecentActivities(int limit) {
            log.debug("Génération des {} dernières activités", limit);

            return generateRecentActivities(limit);
        }

        // ===============================
        // STATISTIQUES SPÉCIALISÉES
        // ===============================

        /**
         * Récupérer les statistiques des mouvements d'aujourd'hui
         */
        public Map<String, Object> getTodayMovementStats() {
            log.debug("Génération des statistiques des mouvements d'aujourd'hui");

            Map<String, Object> stats = new HashMap<>();

            // Comptage par type de mouvement
            Long entrees = stockMovementRepository.countTodayMovementsByType(TypeMouvement.ENTREE);
            Long sorties = stockMovementRepository.countTodayMovementsByType(TypeMouvement.SORTIE);
            Long total = entrees + sorties;

            // Valeurs financières
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

            BigDecimal valeurEntrees = stockMovementRepository.getTotalEntryValueBetween(startOfDay, endOfDay);
            BigDecimal valeurSorties = stockMovementRepository.getTotalExitValueBetween(startOfDay, endOfDay);

            stats.put("nombreEntrees", entrees);
            stats.put("nombreSorties", sorties);
            stats.put("totalMouvements", total);
            stats.put("valeurEntrees", valeurEntrees);
            stats.put("valeurSorties", valeurSorties);
            stats.put("soldeJour", valeurEntrees.subtract(valeurSorties));

            return stats;
        }

        /**
         * Récupérer les performances des fournisseurs
         */
        public List<Map<String, Object>> getSupplierPerformance(int days, int limit) {
            log.debug("Génération des performances des {} top fournisseurs sur {} jours", limit, days);

            LocalDateTime since = LocalDateTime.now().minusDays(days);
            Pageable pageable = PageRequest.of(0, limit);

            List<Object[]> results = fournisseurRepository.findTopFournisseursByPurchaseValue(since, pageable);

            return results.stream().map(result -> {
                Map<String, Object> performance = new HashMap<>();
                performance.put("fournisseurId", result[0]);
                performance.put("fournisseurNom", result[1]);
                performance.put("valeurAchats", result[2]);
                performance.put("nombreLivraisons", result.length > 3 ? result[3] : 0);
                return performance;
            }).collect(Collectors.toList());
        }

        /**
         * Récupérer l'évolution des valeurs de stock
         */
        public List<ChartDataDTO> getStockValueEvolution(int days) {
            log.debug("Génération de l'évolution des valeurs de stock sur {} jours", days);

            List<ChartDataDTO> evolution = new ArrayList<>();

            // Simulation de l'évolution (dans un vrai système, vous stockeriez l'historique)
            BigDecimal valeurActuelle = stockRepository.getTotalStockValue();

            for (int i = days; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                // Simulation d'une variation quotidienne
                BigDecimal variation = valeurActuelle.multiply(BigDecimal.valueOf(0.01 * Math.random()));
                BigDecimal valeurJour = valeurActuelle.subtract(variation);

                ChartDataDTO dataPoint = new ChartDataDTO();
                dataPoint.setLabel(date.toString());
                dataPoint.setValue(valeurJour);
                dataPoint.setColor("#3498db");

                evolution.add(dataPoint);
            }

            return evolution;
        }

        /**
         * Récupérer les indicateurs clés de performance (KPI)
         */
        public Map<String, Object> getKPIs() {
            log.debug("Génération des indicateurs clés de performance");

            Map<String, Object> kpis = new HashMap<>();

            // KPI Stock
            Object stockStats = stockRepository.getGeneralStockStatistics();
            Object[] stockData = (Object[]) stockStats;

            kpis.put("tauxRotationStock", calculateStockRotationRate());
            kpis.put("tauxCouvertureStock", calculateStockCoverageRate());
            kpis.put("tauxRuptureStock", calculateStockoutRate());
            kpis.put("valeurMoyenneStock", stockRepository.getTotalStockValue());

            // KPI Mouvement
            LocalDateTime startMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endMonth = LocalDate.now().atTime(23, 59, 59);

            BigDecimal valeurEntreesMois = stockMovementRepository.getTotalEntryValueBetween(startMonth, endMonth);
            BigDecimal valeurSortiesMois = stockMovementRepository.getTotalExitValueBetween(startMonth, endMonth);

            kpis.put("valeurEntreesMois", valeurEntreesMois);
            kpis.put("valeurSortiesMois", valeurSortiesMois);
            kpis.put("soldeMois", valeurEntreesMois.subtract(valeurSortiesMois));

            return kpis;
        }

        // ===============================
        // MÉTHODES PRIVÉES - GÉNÉRATION DONNÉES
        // ===============================

        private StatsGeneralesDTO generateGeneralStats() {
            StatsGeneralesDTO stats = new StatsGeneralesDTO();

            // Statistiques des articles
            Long totalArticles = articleRepository.count();
            Long articlesActifs = articleRepository.countByActif(true);

            // Statistiques du stock
            BigDecimal valeurTotaleStock = stockRepository.getTotalStockValue();
            Object stockStatsObj = stockRepository.getDetailedStockStatusCount();
            Object[] stockStats = (Object[]) stockStatsObj;

            Integer articlesCritiques = ((Number) stockStats[1]).intValue(); // stocksCritiques
            Integer articlesFaibles = ((Number) stockStats[2]).intValue();   // stocksFaibles

            // Statistiques des mouvements d'aujourd'hui
            Long entreesAujourdhui = stockMovementRepository.countTodayMovementsByType(TypeMouvement.ENTREE);
            Long sortiesAujourdhui = stockMovementRepository.countTodayMovementsByType(TypeMouvement.SORTIE);
            Long mouvementsAujourdhui = entreesAujourdhui + sortiesAujourdhui;

            // Statistiques des fournisseurs
            Long totalFournisseurs = fournisseurRepository.count();
            Long fournisseursActifs = fournisseurRepository.countByActif(true);

            stats.setTotalArticles(totalArticles.intValue());
            stats.setTotalArticlesActifs(articlesActifs.intValue());
            stats.setValeurTotaleStock(valeurTotaleStock);
            stats.setMouvementsAujourdhui(mouvementsAujourdhui.intValue());
            stats.setEntreesAujourdhui(entreesAujourdhui.intValue());
            stats.setSortiesAujourdhui(sortiesAujourdhui.intValue());
            stats.setArticlesCritiques(articlesCritiques);
            stats.setArticlesFaibles(articlesFaibles);
            stats.setTotalFournisseurs(totalFournisseurs.intValue());
            stats.setFournisseursActifs(fournisseursActifs.intValue());

            return stats;
        }

        private List<ChartDataDTO> generateStockByCategory() {
            List<Object[]> categoryStats = stockRepository.getStockValueByCategory();

            return categoryStats.stream().map(stat -> {
                String categorie = (String) stat[0];
                BigDecimal valeur = (BigDecimal) stat[1];
                Long count = (Long) stat[2];
                Long quantite = (Long) stat[3];

                ChartDataDTO chartData = new ChartDataDTO();
                chartData.setLabel(categorie != null ? categorie : "Sans catégorie");
                chartData.setValue(valeur);
                chartData.setColor(generateRandomColor());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("nombreArticles", count);
                metadata.put("quantiteTotale", quantite);
                chartData.setMetadata(metadata);

                return chartData;
            }).collect(Collectors.toList());
        }

        private List<ChartDataDTO> generateMovementsTrend(int days) {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);

            List<Object[]> trendData = stockMovementRepository.getMovementTrend(startDate, endDate);

            // Regroupement par date
            Map<LocalDate, Map<String, Long>> dailyData = new LinkedHashMap<>();

            for (Object[] data : trendData) {
                LocalDate date = ((java.sql.Date) data[0]).toLocalDate();
                String type = data[1].toString();
                Long count = ((Number) data[2]).longValue();

                dailyData.computeIfAbsent(date, k -> new HashMap<>()).put(type, count);
            }

            List<ChartDataDTO> chartData = new ArrayList<>();

            for (Map.Entry<LocalDate, Map<String, Long>> entry : dailyData.entrySet()) {
                LocalDate date = entry.getKey();
                Map<String, Long> movements = entry.getValue();

                Long entrees = movements.getOrDefault("ENTREE", 0L);
                Long sorties = movements.getOrDefault("SORTIE", 0L);

                // Point pour les entrées
                ChartDataDTO entreeData = new ChartDataDTO();
                entreeData.setLabel(date.toString());
                entreeData.setValue(entrees);
                entreeData.setColor("#2ecc71"); // Vert pour entrées

                Map<String, Object> entreeMetadata = new HashMap<>();
                entreeMetadata.put("type", "ENTREE");
                entreeMetadata.put("date", date);
                entreeData.setMetadata(entreeMetadata);

                chartData.add(entreeData);

                // Point pour les sorties
                ChartDataDTO sortieData = new ChartDataDTO();
                sortieData.setLabel(date.toString());
                sortieData.setValue(sorties);
                sortieData.setColor("#e74c3c"); // Rouge pour sorties

                Map<String, Object> sortieMetadata = new HashMap<>();
                sortieMetadata.put("type", "SORTIE");
                sortieMetadata.put("date", date);
                sortieData.setMetadata(sortieMetadata);

                chartData.add(sortieData);
            }

            return chartData;
        }

        private List<ChartDataDTO> generateTopConsumedArticles(int days, int limit) {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            LocalDateTime endDate = LocalDateTime.now();
            Pageable pageable = PageRequest.of(0, limit);

            List<Object[]> topArticles = stockMovementRepository.getTopConsumedArticles(startDate, endDate, pageable);

            return topArticles.stream().map(data -> {
                Article article = (Article) data[0];
                Long quantiteConsommee = ((Number) data[1]).longValue();

                ChartDataDTO chartData = new ChartDataDTO();
                chartData.setValue(quantiteConsommee);
                chartData.setColor(generateRandomColor());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("articleId", article.getId());
                metadata.put("articleNom", article.getNom());
                metadata.put("unite", article.getUnite());
                chartData.setMetadata(metadata);

                return chartData;
            }).collect(Collectors.toList());
        }

        private List<AlerteStockDTO> generateStockAlerts() {
            List<AlerteStockDTO> alertes = new ArrayList<>();

            // Alertes de stock critique
            List<Stock> stocksCritiques = stockRepository.findCriticalStocks();
            for (Stock stock : stocksCritiques) {
                Article article = stock.getArticle();
                AlerteStockDTO alerte = new AlerteStockDTO(
                        article.getId(),
                        article.getNom(),
                        "CRITIQUE",
                        stock.getQuantiteActuelle(),
                        article.getStockMin()
                );
                alertes.add(alerte);
            }

            // Alertes de stock faible
            List<Stock> stocksFaibles = stockRepository.findLowStocks();
            for (Stock stock : stocksFaibles) {
                Article article = stock.getArticle();
                AlerteStockDTO alerte = new AlerteStockDTO(
                        article.getId(),
                        article.getNom(),
                        "FAIBLE",
                        stock.getQuantiteActuelle(),
                        article.getStockMin()
                );
                alertes.add(alerte);
            }

            // Alertes de stock excessif
            List<Stock> stocksExcessifs = stockRepository.findExcessiveStocks();
            for (Stock stock : stocksExcessifs) {
                Article article = stock.getArticle();
                AlerteStockDTO alerte = new AlerteStockDTO();
                alerte.setArticleId(article.getId());
                alerte.setArticleCode(article.getNom());
                alerte.setTypeAlerte("EXCESSIF");
                alerte.setQuantiteActuelle(stock.getQuantiteActuelle());
                alerte.setStockMax(article.getStockMax());
                alerte.setDateAlerte(LocalDate.now());
                alerte.setMessage("Stock excessif : " + stock.getQuantiteActuelle() + " unités (max: " + article.getStockMax() + ")");
                alerte.setPriorite("BASSE");
                alertes.add(alerte);
            }

            // Trier les alertes par priorité
            alertes.sort((a1, a2) -> {
                Map<String, Integer> priorityOrder = Map.of("HAUTE", 1, "MOYENNE", 2, "BASSE", 3);
                return priorityOrder.get(a1.getPriorite()).compareTo(priorityOrder.get(a2.getPriorite()));
            });

            return alertes;
        }

        private List<ActiviteRecenteDTO> generateRecentActivities(int limit) {
            List<ActiviteRecenteDTO> activites = new ArrayList<>();

            // Récupération des mouvements récents
            Pageable pageable = PageRequest.of(0, limit);
            List<StockMovement> recentMovements = stockMovementRepository
                    .findAllByOrderByDateMouvementDesc(pageable)
                    .getContent();

            for (StockMovement movement : recentMovements) {
                String type = movement.getTypeMouvement().isEntree() ? "ENTREE" : "SORTIE";
                String description = String.format("%s - %s: %d %s",
                        movement.getArticle().getNom(),
                        movement.getQuantite(),
                        movement.getArticle().getUnite() != null ? movement.getArticle().getUnite() : "unités"
                );

                ActiviteRecenteDTO activite = new ActiviteRecenteDTO(
                        type,
                        description,
                        movement.getUtilisateur()
                );
                activite.setDate(movement.getDateMouvement().toLocalDate());

                activites.add(activite);
            }

            return activites;
        }

        // ===============================
        // MÉTHODES PRIVÉES - CALCULS KPI
        // ===============================

        private Double calculateStockRotationRate() {
            // Taux de rotation = Coût des marchandises vendues / Stock moyen
            // Simulation simplifiée
            LocalDateTime startMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endMonth = LocalDate.now().atTime(23, 59, 59);

            BigDecimal valeurSorties = stockMovementRepository.getTotalExitValueBetween(startMonth, endMonth);
            BigDecimal valeurStock = stockRepository.getTotalStockValue();

            if (valeurStock.compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }

            return valeurSorties.divide(valeurStock, 2, java.math.RoundingMode.HALF_UP).doubleValue();
        }

        private Double calculateStockCoverageRate() {
            // Taux de couverture = Stock actuel / Consommation moyenne quotidienne
            // Simulation simplifiée sur 30 jours
            LocalDateTime start30Days = LocalDateTime.now().minusDays(30);
            LocalDateTime now = LocalDateTime.now();

            BigDecimal valeurSorties30j = stockMovementRepository.getTotalExitValueBetween(start30Days, now);
            BigDecimal consommationMoyenneJour = valeurSorties30j.divide(BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal valeurStock = stockRepository.getTotalStockValue();

            if (consommationMoyenneJour.compareTo(BigDecimal.ZERO) == 0) {
                return 999.0; // Stock très élevé par rapport à la consommation
            }

            return valeurStock.divide(consommationMoyenneJour, 2, java.math.RoundingMode.HALF_UP).doubleValue();
        }

        private Double calculateStockoutRate() {
            // Taux de rupture = Nombre d'articles en rupture / Total articles
            Long totalArticles = articleRepository.countByActif(true);
            Long articlesEnRupture = (long) stockRepository.findEmptyStocks().size();

            if (totalArticles == 0) {
                return 0.0;
            }

            return (articlesEnRupture.doubleValue() / totalArticles.doubleValue()) * 100;
        }

        // ===============================
        // MÉTHODES UTILITAIRES
        // ===============================

        private String generateRandomColor() {
            String[] colors = {
                    "#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6",
                    "#1abc9c", "#34495e", "#e67e22", "#95a5a6", "#16a085"
            };
            Random random = new Random();
            return colors[random.nextInt(colors.length)];
        }
    }
