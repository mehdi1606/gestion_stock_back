package com.elamal.stockmanagement.exception;

import lombok.Getter;

/**
 * Exception personnalisée pour la gestion des erreurs du système de gestion de stock
 * Supporte l'internationalisation avec codes de messages et paramètres
 */
@Getter
public class StockManagementException extends RuntimeException {

    private final String messageCode;
    private final Object[] messageArgs;
    private final String defaultMessage;

    /**
     * Constructeur avec code de message seulement
     * @param messageCode Code du message à traduire
     */
    public StockManagementException(String messageCode) {
        super(messageCode);
        this.messageCode = messageCode;
        this.messageArgs = null;
        this.defaultMessage = null;
    }

    /**
     * Constructeur avec code de message et arguments
     * @param messageCode Code du message à traduire
     * @param messageArgs Arguments à insérer dans le message
     */
    public StockManagementException(String messageCode, Object... messageArgs) {
        super(messageCode);
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
        this.defaultMessage = null;
    }

    /**
     * Constructeur avec code de message et cause
     * @param messageCode Code du message à traduire
     * @param cause Cause de l'exception
     */
    public StockManagementException(String messageCode, Throwable cause) {
        super(messageCode, cause);
        this.messageCode = messageCode;
        this.messageArgs = null;
        this.defaultMessage = null;
    }

    /**
     * Constructeur avec code de message, cause et arguments
     * @param messageCode Code du message à traduire
     * @param cause Cause de l'exception
     * @param messageArgs Arguments à insérer dans le message
     */
    public StockManagementException(String messageCode, Throwable cause, Object... messageArgs) {
        super(messageCode, cause);
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
        this.defaultMessage = null;
    }

    /**
     * Constructeur complet avec message par défaut
     * @param messageCode Code du message à traduire
     * @param defaultMessage Message par défaut si traduction non disponible
     * @param cause Cause de l'exception
     * @param messageArgs Arguments à insérer dans le message
     */
    public StockManagementException(String messageCode, String defaultMessage, Throwable cause, Object... messageArgs) {
        super(messageCode, cause);
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Constructeur avec message par défaut sans cause
     * @param messageCode Code du message à traduire
     * @param defaultMessage Message par défaut si traduction non disponible
     * @param messageArgs Arguments à insérer dans le message
     */
    public StockManagementException(String messageCode, String defaultMessage, Object... messageArgs) {
        super(messageCode);
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
        this.defaultMessage = defaultMessage;
    }

    // ===============================
    // MÉTHODES UTILITAIRES
    // ===============================

    /**
     * Vérifie si l'exception a des arguments de message
     * @return true si des arguments sont présents
     */
    public boolean hasMessageArgs() {
        return messageArgs != null && messageArgs.length > 0;
    }

    /**
     * Retourne le nombre d'arguments de message
     * @return nombre d'arguments
     */
    public int getMessageArgsCount() {
        return messageArgs != null ? messageArgs.length : 0;
    }

    /**
     * Vérifie si un message par défaut est défini
     * @return true si un message par défaut existe
     */
    public boolean hasDefaultMessage() {
        return defaultMessage != null && !defaultMessage.trim().isEmpty();
    }

    // ===============================
    // MÉTHODES STATIQUES DE CRÉATION
    // ===============================

    /**
     * Crée une exception pour article non trouvé
     * @param articleId ID de l'article
     * @return StockManagementException
     */
    public static StockManagementException articleNotFound(Long articleId) {
        return new StockManagementException("error.article.not.found", articleId);
    }

    /**
     * Crée une exception pour fournisseur non trouvé
     * @param fournisseurId ID du fournisseur
     * @return StockManagementException
     */
    public static StockManagementException supplierNotFound(Long fournisseurId) {
        return new StockManagementException("error.supplier.not.found", fournisseurId);
    }

    /**
     * Crée une exception pour mouvement non trouvé
     * @param movementId ID du mouvement
     * @return StockManagementException
     */
    public static StockManagementException movementNotFound(Long movementId) {
        return new StockManagementException("error.movement.not.found", movementId);
    }

    /**
     * Crée une exception pour stock insuffisant
     * @param available Quantité disponible
     * @param requested Quantité demandée
     * @return StockManagementException
     */
    public static StockManagementException insufficientStock(Integer available, Integer requested) {
        return new StockManagementException("error.stock.insufficient", available, requested);
    }

    /**
     * Crée une exception pour stock non trouvé
     * @param articleId ID de l'article
     * @return StockManagementException
     */
    public static StockManagementException stockNotFound(Long articleId) {
        return new StockManagementException("error.stock.not.found", articleId);
    }

    /**
     * Crée une exception pour validation échouée
     * @param field Champ en erreur
     * @param value Valeur invalide
     * @return StockManagementException
     */
    public static StockManagementException validationFailed(String field, Object value) {
        return new StockManagementException("error.validation.failed", field, value);
    }

    /**
     * Crée une exception pour erreur de traitement
     * @param operation Type d'opération
     * @param cause Cause de l'erreur
     * @return StockManagementException
     */
    public static StockManagementException processingError(String operation, Throwable cause) {
        return new StockManagementException("error.processing.failed", cause, operation);
    }

    /**
     * Crée une exception pour erreur de conversion de données
     * @param sourceType Type source
     * @param targetType Type cible
     * @param cause Cause de l'erreur
     * @return StockManagementException
     */
    public static StockManagementException conversionError(String sourceType, String targetType, Throwable cause) {
        return new StockManagementException("error.conversion.failed", cause, sourceType, targetType);
    }

    /**
     * Crée une exception pour format de date invalide
     * @param dateString Chaîne de date invalide
     * @return StockManagementException
     */
    public static StockManagementException invalidDateFormat(String dateString) {
        return new StockManagementException("error.date.format.invalid", dateString);
    }

    /**
     * Crée une exception pour opération non autorisée
     * @param operation Opération tentée
     * @param reason Raison du refus
     * @return StockManagementException
     */
    public static StockManagementException operationNotAllowed(String operation, String reason) {
        return new StockManagementException("error.operation.not.allowed", operation, reason);
    }

    // ===============================
    // SURCHARGE DE TOSTRING
    // ===============================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StockManagementException{");
        sb.append("messageCode='").append(messageCode).append('\'');

        if (hasMessageArgs()) {
            sb.append(", messageArgs=[");
            for (int i = 0; i < messageArgs.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(messageArgs[i]);
            }
            sb.append("]");
        }

        if (hasDefaultMessage()) {
            sb.append(", defaultMessage='").append(defaultMessage).append('\'');
        }

        if (getCause() != null) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName());
            sb.append(": ").append(getCause().getMessage());
        }

        sb.append('}');
        return sb.toString();
    }
}
