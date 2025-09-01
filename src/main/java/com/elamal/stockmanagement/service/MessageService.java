package com.elamal.stockmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service pour la gestion des messages internationalisés
 * Permet de récupérer les messages traduits selon la locale de l'utilisateur
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Récupère un message traduit selon la locale courante
     * @param code Code du message
     * @return Message traduit
     */
    public String getMessage(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, null, locale);
        } catch (Exception e) {
            log.warn("Message non trouvé pour le code: {} - Locale: {}", code, LocaleContextHolder.getLocale());
            return code; // Retourne le code si message non trouvé
        }
    }

    /**
     * Récupère un message traduit avec arguments selon la locale courante
     * @param code Code du message
     * @param args Arguments à insérer dans le message
     * @return Message traduit avec arguments
     */
    public String getMessage(String code, Object... args) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Message non trouvé pour le code: {} - Locale: {} - Args: {}",
                    code, LocaleContextHolder.getLocale(), args);
            return code; // Retourne le code si message non trouvé
        }
    }

    /**
     * Récupère un message traduit avec message par défaut
     * @param code Code du message
     * @param defaultMessage Message par défaut si code non trouvé
     * @return Message traduit ou message par défaut
     */
    public String getMessage(String code, String defaultMessage) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, null, defaultMessage, locale);
        } catch (Exception e) {
            log.warn("Message non trouvé pour le code: {} - Utilisation du message par défaut", code);
            return defaultMessage != null ? defaultMessage : code;
        }
    }

    /**
     * Récupère un message traduit avec arguments et message par défaut
     * @param code Code du message
     * @param args Arguments à insérer dans le message
     * @param defaultMessage Message par défaut si code non trouvé
     * @return Message traduit avec arguments ou message par défaut
     */
    public String getMessage(String code, Object[] args, String defaultMessage) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(code, args, defaultMessage, locale);
        } catch (Exception e) {
            log.warn("Message non trouvé pour le code: {} - Utilisation du message par défaut", code);
            return defaultMessage != null ? defaultMessage : code;
        }
    }

    /**
     * Récupère un message traduit pour une locale spécifique
     * @param code Code du message
     * @param locale Locale spécifique
     * @return Message traduit
     */
    public String getMessage(String code, Locale locale) {
        try {
            return messageSource.getMessage(code, null, locale);
        } catch (Exception e) {
            log.warn("Message non trouvé pour le code: {} - Locale: {}", code, locale);
            return code;
        }
    }

    /**
     * Récupère un message traduit avec arguments pour une locale spécifique
     * @param code Code du message
     * @param args Arguments à insérer dans le message
     * @param locale Locale spécifique
     * @return Message traduit avec arguments
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            log.warn("Message non trouvé pour le code: {} - Locale: {} - Args: {}", code, locale, args);
            return code;
        }
    }

    /**
     * Vérifie si un message existe pour un code donné
     * @param code Code du message
     * @return true si le message existe
     */
    public boolean messageExists(String code) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            messageSource.getMessage(code, null, locale);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère la locale courante de l'utilisateur
     * @return Locale courante
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * Récupère le code de langue de la locale courante
     * @return Code de langue (ex: "fr", "en", "ar")
     */
    public String getCurrentLanguageCode() {
        return LocaleContextHolder.getLocale().getLanguage();
    }

    // ===============================
    // MÉTHODES UTILITAIRES SPÉCIALISÉES
    // ===============================

    /**
     * Message de succès générique
     * @return Message de succès traduit
     */
    public String getSuccessMessage() {
        return getMessage("success.operation.completed", "Opération réussie");
    }

    /**
     * Message d'erreur générique
     * @return Message d'erreur traduit
     */
    public String getErrorMessage() {
        return getMessage("error.generic", "Une erreur s'est produite");
    }

    /**
     * Message de validation échouée
     * @param field Champ en erreur
     * @param value Valeur invalide
     * @return Message de validation traduit
     */
    public String getValidationError(String field, Object value) {
        return getMessage("error.validation.failed", new Object[]{field, value},
                "Erreur de validation pour " + field + ": " + value);
    }

    /**
     * Message d'entité non trouvée
     * @param entityName Nom de l'entité
     * @param id ID de l'entité
     * @return Message traduit
     */
    public String getEntityNotFoundMessage(String entityName, Object id) {
        return getMessage("error.entity.not.found", new Object[]{entityName, id},
                entityName + " non trouvé avec l'ID: " + id);
    }

    /**
     * Message de stock insuffisant
     * @param available Quantité disponible
     * @param requested Quantité demandée
     * @return Message traduit
     */
    public String getInsufficientStockMessage(Integer available, Integer requested) {
        return getMessage("error.stock.insufficient", new Object[]{available, requested},
                "Stock insuffisant. Disponible: " + available + ", Demandé: " + requested);
    }

    /**
     * Message d'opération de stock réussie
     * @param operation Type d'opération (entrée/sortie)
     * @param quantity Quantité
     * @param articleName Nom de l'article
     * @return Message traduit
     */
    public String getStockOperationSuccess(String operation, Integer quantity, String articleName) {
        return getMessage("success.stock.operation", new Object[]{operation, quantity, articleName},
                operation + " de " + quantity + " unités pour " + articleName + " effectuée avec succès");
    }

    // ===============================
    // MÉTHODES DE DEBUG
    // ===============================

    /**
     * Log les informations de locale courante
     */
    public void logCurrentLocaleInfo() {
        Locale locale = getCurrentLocale();
        log.debug("Locale courante - Langue: {}, Pays: {}, Variante: {}",
                locale.getLanguage(), locale.getCountry(), locale.getVariant());
    }

    /**
     * Test si les messages de base sont disponibles
     * @return true si les messages de base sont chargés
     */
    public boolean areBasicMessagesAvailable() {
        return messageExists("success.operation.completed") &&
                messageExists("error.generic") &&
                messageExists("error.validation.failed");
    }

    /**
     * Récupère des informations sur l'état du service de messages
     * @return Informations de diagnostic
     */
    public java.util.Map<String, Object> getDiagnosticInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();

        Locale locale = getCurrentLocale();
        info.put("currentLocale", locale.toString());
        info.put("languageCode", locale.getLanguage());
        info.put("countryCode", locale.getCountry());
        info.put("displayLanguage", locale.getDisplayLanguage());
        info.put("basicMessagesAvailable", areBasicMessagesAvailable());

        // Test de quelques messages clés
        String[] testCodes = {
                "success.operation.completed",
                "error.generic",
                "error.article.not.found",
                "success.stock.entry"
        };

        java.util.Map<String, Boolean> messageTests = new java.util.HashMap<>();
        for (String code : testCodes) {
            messageTests.put(code, messageExists(code));
        }
        info.put("messageTests", messageTests);

        return info;
    }
}
