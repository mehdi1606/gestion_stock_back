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
import java.time.format.DateTimeFormatter;
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

        log.info("Dashboard généré avec {} catégories, {} mouvements, {} articles top",
                stockParCategorie.size(), mouvementsTendance.size(), topArticlesConsommes.size());

        return dashboard;
    }

    /**
     * Récupérer les statistiques générales - CRITICAL FOR FRONTEND
     */
    public StatsGeneralesDTO getGeneralStatistics() {
        log.debug("Génération des statistiques générales");
        return generateGeneralStats();
    }

    // ===============================
    // DONNÉES POUR GRAPHIQUES - REQUIRED BY FRONTEND
    // ===============================

    /**
     * Stock par catégorie - REQUIRED
     */
    public List<ChartDataDTO> getStockByCategory() {
        log.debug("Récupération des données de stock par catégorie");
        return generateStockByCategory();
    }

    /**
     * Tendance des mouvements - REQUIRED
     */
    public List<ChartDataDTO> getMovementsTrend(int days) {
        log.debug("Récupération de la tendance des mouvements sur {} jours", days);
        return generateMovementsTrend(days);
    }

    /**
     * Top articles consommés - REQUIRED
     */
    public List<ChartDataDTO> getTopConsumedArticles(int days, int limit) {
        log.debug("Récupération des {} top articles consommés sur {} jours", limit, days);
        return generateTopConsumedArticles(days, limit);
    }

    /**
     * Évolution de la valeur du stock - REQUIRED
     */
    public List<ChartDataDTO> getStockValueEvolution(int days) {
        log.debug("Récupération de l'évolution de la valeur du stock sur {} jours", days);
        return generateStockValueEvolution(days);
    }

    /**
     * Statistiques des mouvements d'aujourd'hui - REQUIRED
     */
    public Map<String, Object> getTodayMovementStats() {
        log.debug("Récupération des statistiques des mouvements d'aujourd'hui");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        Map<String, Object> stats = new HashMap<>();

        try {
            Long entreesAujourdhui = stockMovementRepository.countMovementsByTypeAndDateRange(
                    TypeMouvement.ENTREE, startOfDay, endOfDay);
            Long sortiesAujourdhui = stockMovementRepository.countMovementsByTypeAndDateRange(
                    TypeMouvement.SORTIE, startOfDay, endOfDay);

            BigDecimal valeurEntrees = stockMovementRepository.getTotalEntryValueBetween(startOfDay, endOfDay);
            BigDecimal valeurSorties = stockMovementRepository.getTotalExitValueBetween(startOfDay, endOfDay);

            stats.put("entreesAujourdhui", entreesAujourdhui != null ? entreesAujourdhui : 0L);
            stats.put("sortiesAujourdhui", sortiesAujourdhui != null ? sortiesAujourdhui : 0L);
            stats.put("mouvementsTotaux", (entreesAujourdhui != null ? entreesAujourdhui : 0L) +
                    (sortiesAujourdhui != null ? sortiesAujourdhui : 0L));
            stats.put("valeurEntrees", valeurEntrees != null ? valeurEntrees : BigDecimal.ZERO);
            stats.put("valeurSorties", valeurSorties != null ? valeurSorties : BigDecimal.ZERO);
            stats.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des stats d'aujourd'hui", e);
            // Valeurs par défaut en cas d'erreur
            stats.put("entreesAujourdhui", 0L);
            stats.put("sortiesAujourdhui", 0L);
            stats.put("mouvementsTotaux", 0L);
            stats.put("valeurEntrees", BigDecimal.ZERO);
            stats.put("valeurSorties", BigDecimal.ZERO);
            stats.put("timestamp", LocalDateTime.now());
        }

        return stats;
    }

    /**
     * Performance des fournisseurs - REQUIRED
     */
    public List<Map<String, Object>> getSupplierPerformance(int days, int limit) {
        log.debug("Récupération des performances des fournisseurs sur {} jours, limit {}", days, limit);

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);

        List<Map<String, Object>> performance = new ArrayList<>();

        try {
            // Récupérer les top fournisseurs par valeur d'achats
            List<Object[]> topSuppliers = fournisseurRepository.findTopFournisseursByPurchaseValue(since, pageable);

            for (Object[] supplier : topSuppliers) {
                Map<String, Object> supplierPerf = new HashMap<>();
                Fournisseur fournisseur = (Fournisseur) supplier[0];
                BigDecimal totalAchats = (BigDecimal) supplier[1];

                supplierPerf.put("id", fournisseur.getId());
                supplierPerf.put("nomFournisseur", fournisseur.getNom());
                supplierPerf.put("performance", totalAchats);
                supplierPerf.put("valeur", totalAchats); // Pour compatibilité avec le frontend
                supplierPerf.put("ville", fournisseur.getVille());
                supplierPerf.put("pays", fournisseur.getPays());

                performance.add(supplierPerf);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des performances fournisseurs", e);
            // Retourner une liste vide en cas d'erreur
        }

        return performance;
    }

    // ===============================
    // MÉTHODES PRIVÉES - GÉNÉRATION DONNÉES
    // ===============================

    private StatsGeneralesDTO generateGeneralStats() {
        StatsGeneralesDTO stats = new StatsGeneralesDTO();

        try {
            // Statistiques des articles
            Long totalArticles = articleRepository.count();
            Long articlesActifs = articleRepository.countByActif(true);

            // Statistiques du stock
            BigDecimal valeurTotaleStock = stockRepository.getTotalStockValue();
            if (valeurTotaleStock == null) valeurTotaleStock = BigDecimal.ZERO;

            // Récupérer les alertes de stock
            Object stockStatsObj = stockRepository.getDetailedStockStatusCount();
            Integer articlesCritiques = 0;
            Integer articlesFaibles = 0;

            if (stockStatsObj != null && stockStatsObj instanceof Object[]) {
                Object[] stockStats = (Object[]) stockStatsObj;
                if (stockStats.length >= 3) {
                    articlesCritiques = stockStats[1] != null ? ((Number) stockStats[1]).intValue() : 0;
                    articlesFaibles = stockStats[2] != null ? ((Number) stockStats[2]).intValue() : 0;
                }
            }

            // Statistiques des mouvements d'aujourd'hui
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

            Long entreesAujourdhui = stockMovementRepository.countMovementsByTypeAndDateRange(
                    TypeMouvement.ENTREE, startOfDay, endOfDay);
            Long sortiesAujourdhui = stockMovementRepository.countMovementsByTypeAndDateRange(
                    TypeMouvement.SORTIE, startOfDay, endOfDay);

            // Statistiques des fournisseurs
            Long totalFournisseurs = fournisseurRepository.count();
            Long fournisseursActifs = fournisseurRepository.countByActif(true);

            // Remplir le DTO
            stats.setTotalArticles(totalArticles != null ? totalArticles.intValue() : 0);
            stats.setTotalArticlesActifs(articlesActifs != null ? articlesActifs.intValue() : 0);
            stats.setValeurTotaleStock(valeurTotaleStock);
            stats.setMouvementsAujourdhui((entreesAujourdhui != null ? entreesAujourdhui.intValue() : 0) +
                    (sortiesAujourdhui != null ? sortiesAujourdhui.intValue() : 0));
            stats.setEntreesAujourdhui(entreesAujourdhui != null ? entreesAujourdhui.intValue() : 0);
            stats.setSortiesAujourdhui(sortiesAujourdhui != null ? sortiesAujourdhui.intValue() : 0);
            stats.setArticlesCritiques(articlesCritiques);
            stats.setArticlesFaibles(articlesFaibles);
            stats.setTotalFournisseurs(totalFournisseurs != null ? totalFournisseurs.intValue() : 0);
            stats.setFournisseursActifs(fournisseursActifs != null ? fournisseursActifs.intValue() : 0);

            log.debug("Stats générées: {} articles, {} MAD valeur stock, {} critiques",
                    stats.getTotalArticles(), stats.getValeurTotaleStock(), stats.getArticlesCritiques());

        } catch (Exception e) {
            log.error("Erreur lors de la génération des statistiques générales", e);
            // Valeurs par défaut en cas d'erreur
            stats.setTotalArticles(0);
            stats.setTotalArticlesActifs(0);
            stats.setValeurTotaleStock(BigDecimal.ZERO);
            stats.setMouvementsAujourdhui(0);
            stats.setEntreesAujourdhui(0);
            stats.setSortiesAujourdhui(0);
            stats.setArticlesCritiques(0);
            stats.setArticlesFaibles(0);
            stats.setTotalFournisseurs(0);
            stats.setFournisseursActifs(0);
        }

        return stats;
    }

    private List<ChartDataDTO> generateStockByCategory() {
        try {
            List<Object[]> categoryStats = stockRepository.getStockValueByCategory();

            return categoryStats.stream().map(stat -> {
                String categorie = (String) stat[0];
                BigDecimal valeur = (BigDecimal) stat[1];
                Long count = (Long) stat[2];

                ChartDataDTO chartData = new ChartDataDTO();
                chartData.setLabel(categorie != null ? categorie : "Non classé");
                chartData.setValue(count != null ? count : 0L);

                // Métadonnées supplémentaires
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("valeur", valeur != null ? valeur : BigDecimal.ZERO);
                metadata.put("nombreArticles", count != null ? count : 0L);
                chartData.setMetadata(metadata);

                return chartData;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la génération du stock par catégorie", e);
            return new ArrayList<>();
        }
    }

    private List<ChartDataDTO> generateMovementsTrend(int days) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);

            List<Object[]> movementTrend = stockMovementRepository.getMovementTrend(startDate, endDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

            // Créer une map pour grouper par date
            Map<LocalDate, Integer> dailyMovements = new HashMap<>();

            for (Object[] trend : movementTrend) {
                LocalDate date = (LocalDate) trend[0];
                Long count = ((Number) trend[2]).longValue();
                dailyMovements.merge(date, count.intValue(), Integer::sum);
            }

            // Convertir en ChartDataDTO
            return dailyMovements.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> {
                        ChartDataDTO chartData = new ChartDataDTO();
                        chartData.setLabel(entry.getKey().format(formatter));
                        chartData.setValue(entry.getValue());
                        return chartData;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la génération de la tendance des mouvements", e);
            return new ArrayList<>();
        }
    }

    private List<ChartDataDTO> generateTopConsumedArticles(int days, int limit) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            Pageable pageable = PageRequest.of(0, limit);

            List<Object[]> topConsumed = stockMovementRepository.getTopConsumedArticles(startDate, endDate, pageable);

            return topConsumed.stream().map(consumed -> {
                Article article = (Article) consumed[0];
                Long totalQuantite = ((Number) consumed[1]).longValue();

                ChartDataDTO chartData = new ChartDataDTO();
                chartData.setValue(totalQuantite);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("articleId", article.getId());
                metadata.put("articleCode", article.getNom());
                metadata.put("unite", article.getUnite());
                chartData.setMetadata(metadata);

                return chartData;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la génération des top articles consommés", e);
            return new ArrayList<>();
        }
    }

    private List<ChartDataDTO> generateStockValueEvolution(int days) {
        try {
            List<ChartDataDTO> evolution = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

            // Simuler l'évolution sur les derniers jours
            // Dans un cas réel, vous devriez avoir un historique des valeurs de stock
            BigDecimal currentValue = stockRepository.getTotalStockValue();
            if (currentValue == null) currentValue = BigDecimal.ZERO;

            LocalDate today = LocalDate.now();
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);

                // Simulation de variation (à remplacer par des données réelles)
                double variation = 1.0 + (Math.random() - 0.5) * 0.1; // ±5% de variation
                BigDecimal dailyValue = currentValue.multiply(BigDecimal.valueOf(variation));

                ChartDataDTO chartData = new ChartDataDTO();
                chartData.setLabel(date.format(formatter));
                chartData.setValue(dailyValue);

                evolution.add(chartData);
            }

            return evolution;

        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'évolution de la valeur du stock", e);
            return new ArrayList<>();
        }
    }

    private List<AlerteStockDTO> generateStockAlerts() {
        List<AlerteStockDTO> alertes = new ArrayList<>();

        try {
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
                AlerteStockDTO alerte = new AlerteStockDTO(
                        article.getId(),
                        article.getNom(),
                        "EXCESSIF",
                        stock.getQuantiteActuelle(),
                        article.getStockMax()
                );
                alertes.add(alerte);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la génération des alertes de stock", e);
        }

        return alertes;
    }

    private List<ActiviteRecenteDTO> generateRecentActivities(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            List<StockMovement> recentMovements = stockMovementRepository.findAllByOrderByDateMouvementDesc(pageable).getContent();

            return recentMovements.stream().map(movement -> {
                ActiviteRecenteDTO activite = new ActiviteRecenteDTO();
                activite.setId(movement.getId());
                activite.setType(movement.getTypeMouvement().name());
                activite.setDescription(String.format("%s - %s (%d %s)",
                        movement.getTypeMouvement().name(),
                        movement.getQuantite(),
                        movement.getArticle().getUnite()));
                activite.setDateActivite(movement.getDateMouvement());
                activite.setUtilisateur(movement.getUtilisateur());

                return activite;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erreur lors de la génération des activités récentes", e);
            return new ArrayList<>();
        }
    }
}
