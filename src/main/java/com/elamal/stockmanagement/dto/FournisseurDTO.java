package com.elamal.stockmanagement.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurDTO {

    private Long id;

    @NotBlank(message = "Le code fournisseur est obligatoire")
    @Size(max = 50, message = "Le code ne peut pas dépasser 50 caractères")
    private String code;

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    @Size(max = 200, message = "Le nom ne peut pas dépasser 200 caractères")
    private String nom;

    @Size(max = 250, message = "La raison sociale ne peut pas dépasser 250 caractères")
    private String raisonSociale;

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
    private String adresse;

    @Size(max = 100, message = "La ville ne peut pas dépasser 100 caractères")
    private String ville;

    @Size(max = 20, message = "Le code postal ne peut pas dépasser 20 caractères")
    private String codePostal;

    @Size(max = 100, message = "Le pays ne peut pas dépasser 100 caractères")
    private String pays;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\.\\(\\)]*$", message = "Format de téléphone invalide")
    @Size(max = 20, message = "Le téléphone ne peut pas dépasser 20 caractères")
    private String telephone;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\.\\(\\)]*$", message = "Format de fax invalide")
    @Size(max = 20, message = "Le fax ne peut pas dépasser 20 caractères")
    private String fax;

    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères")
    private String email;

    @Size(max = 200, message = "Le site web ne peut pas dépasser 200 caractères")
    private String siteWeb;

    @Size(max = 150, message = "Le contact principal ne peut pas dépasser 150 caractères")
    private String contactPrincipal;

    @Pattern(regexp = "^[+]?[0-9\\s\\-\\.\\(\\)]*$", message = "Format de téléphone de contact invalide")
    @Size(max = 20, message = "Le téléphone de contact ne peut pas dépasser 20 caractères")
    private String telephoneContact;

    @Email(message = "Format d'email de contact invalide")
    @Size(max = 150, message = "L'email de contact ne peut pas dépasser 150 caractères")
    private String emailContact;

    @Size(max = 100, message = "Les conditions de paiement ne peuvent pas dépasser 100 caractères")
    private String conditionsPaiement;

    @Min(value = 0, message = "Le délai de livraison ne peut pas être négatif")
    @Max(value = 365, message = "Le délai de livraison ne peut pas dépasser 365 jours")
    private Integer delaiLivraison;

    private Boolean actif = true;

    @Size(max = 1000, message = "Les notes ne peuvent pas dépasser 1000 caractères")
    private String notes;

    // Statistiques (lecture seule)
    private Integer nombreArticles;
    private Integer nombreMouvements;
    private LocalDateTime derniereMouvement;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Constructeur pour création simple
    public FournisseurDTO(String code, String nom, String telephone, String email) {
        this.code = code;
        this.nom = nom;
        this.telephone = telephone;
        this.email = email;
        this.actif = true;
    }

    // Constructeur complet pour création
    public FournisseurDTO(String code, String nom, String adresse, String ville,
                          String telephone, String email, String contactPrincipal) {
        this.code = code;
        this.nom = nom;
        this.adresse = adresse;
        this.ville = ville;
        this.telephone = telephone;
        this.email = email;
        this.contactPrincipal = contactPrincipal;
        this.actif = true;
    }
}
