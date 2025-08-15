package com.elamal.stockmanagement.dto;

import com.elamal.stockmanagement.entity.TypeMouvement;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDTO {

    private Long id;

    @NotNull(message = "L'article est obligatoire")
    private Long articleId;

    // Informations de l'article (lecture seule)
    private String articleCode;
    private String articleDesignation;
    private String articleUnite;
    private String articleCategorie;

    @NotNull(message = "Le type de mouvement est obligatoire")
    private TypeMouvement typeMouvement;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être positive")
    private Integer quantite;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix unitaire doit être positif")
    @Digits(integer = 8, fraction = 2, message = "Format de prix invalide")
    private BigDecimal prixUnitaire;

    private BigDecimal valeurTotale; // Calculé automatiquement

    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String motif;

    @Size(max = 100, message = "Le numéro de bon ne peut pas dépasser 100 caractères")
    private String numeroBon;

    @Size(max = 100, message = "Le numéro de facture ne peut pas dépasser 100 caractères")
    private String numeroFacture;

    // Pour les entrées de stock
    private Long fournisseurId;
    private String fournisseurNom;
    private String fournisseurCode;

    // Pour les sorties de stock
    @Size(max = 200, message = "Le nom du client ne peut pas dépasser 200 caractères")
    private String client;

    @NotNull(message = "La date du mouvement est obligatoire")
    private LocalDateTime dateMouvement;

    @Size(max = 100, message = "Le nom d'utilisateur ne peut pas dépasser 100 caractères")
    private String utilisateur;

    @Size(max = 1000, message = "Les observations ne peuvent pas dépasser 1000 caractères")
    private String observations;

    // Informations de stock (lecture seule)
    private Integer stockAvant;
    private Integer stockApres;

    private LocalDateTime dateCreation;

    // Champs additionnels pour l'affichage et les rapports
    private String typeMouvementDescription;
    private String statusMouvement; // "VALIDE", "EN_ATTENTE", "ANNULE"
    private Boolean mouvementValide = true;

    // Constructeurs pour différents types de mouvements

    // Constructeur pour entrée de stock
    public StockMovementDTO(Long articleId, TypeMouvement typeMouvement, Integer quantite,
                            BigDecimal prixUnitaire, Long fournisseurId, String motif, String utilisateur) {
        this.articleId = articleId;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.fournisseurId = fournisseurId;
        this.motif = motif;
        this.utilisateur = utilisateur;
        this.dateMouvement = LocalDateTime.now();
        this.mouvementValide = true;
        this.statusMouvement = "VALIDE";
        calculateValeurTotale();
        setTypeMouvementDescription();
    }

    // Constructeur pour sortie de stock
    public StockMovementDTO(Long articleId, TypeMouvement typeMouvement, Integer quantite,
                            String client, String motif, String utilisateur) {
        this.articleId = articleId;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.client = client;
        this.motif = motif;
        this.utilisateur = utilisateur;
        this.dateMouvement = LocalDateTime.now();
        this.mouvementValide = true;
        this.statusMouvement = "VALIDE";
        setTypeMouvementDescription();
    }

    // Constructeur pour inventaire/correction
    public StockMovementDTO(Long articleId, TypeMouvement typeMouvement, Integer quantite,
                            String motif, String utilisateur, Integer stockAvant, Integer stockApres) {
        this.articleId = articleId;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.motif = motif;
        this.utilisateur = utilisateur;
        this.stockAvant = stockAvant;
        this.stockApres = stockApres;
        this.dateMouvement = LocalDateTime.now();
        this.mouvementValide = true;
        this.statusMouvement = "VALIDE";
        setTypeMouvementDescription();
    }

    // Méthodes utilitaires
    public boolean isEntree() {
        return this.typeMouvement != null && this.typeMouvement.isEntree();
    }

    public boolean isSortie() {
        return this.typeMouvement != null && this.typeMouvement.isSortie();
    }

    public boolean isInventaire() {
        return this.typeMouvement != null &&
                (this.typeMouvement == TypeMouvement.INVENTAIRE ||
                        this.typeMouvement == TypeMouvement.CORRECTION);
    }

    // Calcul automatique de la valeur totale
    public void calculateValeurTotale() {
        if (this.quantite != null && this.prixUnitaire != null) {
            this.valeurTotale = this.prixUnitaire.multiply(BigDecimal.valueOf(this.quantite));
        }
    }

    // Attribution de la description du type de mouvement
    public void setTypeMouvementDescription() {
        if (this.typeMouvement != null) {
            this.typeMouvementDescription = this.typeMouvement.getDescription();
        }
    }

    // Validation métier
    public boolean isValidForEntree() {
        return this.articleId != null &&
                this.quantite != null && this.quantite > 0 &&
                this.prixUnitaire != null && this.prixUnitaire.compareTo(BigDecimal.ZERO) > 0 &&
                this.fournisseurId != null &&
                this.utilisateur != null && !this.utilisateur.trim().isEmpty() &&
                this.isEntree();
    }

    public boolean isValidForSortie() {
        return this.articleId != null &&
                this.quantite != null && this.quantite > 0 &&
                this.client != null && !this.client.trim().isEmpty() &&
                this.utilisateur != null && !this.utilisateur.trim().isEmpty() &&
                this.isSortie();
    }

    public boolean isValidForInventaire() {
        return this.articleId != null &&
                this.quantite != null &&
                this.utilisateur != null && !this.utilisateur.trim().isEmpty() &&
                this.isInventaire();
    }

    // Méthodes pour l'affichage
    public String getFormattedValeurTotale() {
        if (this.valeurTotale != null) {
            return String.format("%.2f DH", this.valeurTotale);
        }
        return "0.00 DH";
    }

    public String getFormattedPrixUnitaire() {
        if (this.prixUnitaire != null) {
            return String.format("%.2f DH", this.prixUnitaire);
        }
        return "0.00 DH";
    }

    public String getFormattedQuantite() {
        if (this.quantite != null) {
            String unite = this.articleUnite != null ? " " + this.articleUnite : "";
            return this.quantite + unite;
        }
        return "0";
    }

    public String getDirectionIcon() {
        if (this.isEntree()) {
            return "⬆️"; // Flèche vers le haut pour entrée
        } else if (this.isSortie()) {
            return "⬇️"; // Flèche vers le bas pour sortie
        } else {
            return "🔄"; // Icône rotation pour inventaire/correction
        }
    }

    public String getDirectionColor() {
        if (this.isEntree()) {
            return "green";
        } else if (this.isSortie()) {
            return "red";
        } else {
            return "blue";
        }
    }

    // Méthode pour créer un mouvement d'entrée rapide
    public static StockMovementDTO createEntreeRapide(Long articleId, Integer quantite,
                                                      BigDecimal prixUnitaire, Long fournisseurId,
                                                      String utilisateur) {
        return new StockMovementDTO(
                articleId,
                TypeMouvement.ENTREE,
                quantite,
                prixUnitaire,
                fournisseurId,
                "Entrée de stock",
                utilisateur
        );
    }

    // Méthode pour créer un mouvement de sortie rapide
    public static StockMovementDTO createSortieRapide(Long articleId, Integer quantite,
                                                      String client, String utilisateur) {
        return new StockMovementDTO(
                articleId,
                TypeMouvement.SORTIE,
                quantite,
                client,
                "Sortie de stock",
                utilisateur
        );
    }

    // Méthode pour créer un ajustement d'inventaire
    public static StockMovementDTO createAjustementInventaire(Long articleId, Integer quantiteReel,
                                                              Integer stockSysteme, String utilisateur) {
        int ecart = quantiteReel - stockSysteme;
        TypeMouvement type = ecart >= 0 ? TypeMouvement.INVENTAIRE : TypeMouvement.CORRECTION;

        return new StockMovementDTO(
                articleId,
                type,
                Math.abs(ecart),
                "Ajustement inventaire - Écart: " + ecart,
                utilisateur,
                stockSysteme,
                quantiteReel
        );
    }

    // Validation globale
    public boolean isValid() {
        if (this.isEntree()) {
            return this.isValidForEntree();
        } else if (this.isSortie()) {
            return this.isValidForSortie();
        } else if (this.isInventaire()) {
            return this.isValidForInventaire();
        }
        return false;
    }

    // Méthode toString personnalisée pour les logs
    @Override
    public String toString() {
        return String.format("StockMovement{id=%d, article=%s, type=%s, quantite=%d, utilisateur='%s', date=%s}",
                this.id, this.articleCode, this.typeMouvement, this.quantite, this.utilisateur, this.dateMouvement);
    }
}
