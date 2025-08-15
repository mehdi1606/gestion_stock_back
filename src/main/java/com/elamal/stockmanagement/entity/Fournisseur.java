package com.elamal.stockmanagement.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "fournisseurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "nom", nullable = false, length = 200)
    private String nom;

    @Column(name = "raison_sociale", length = 250)
    private String raisonSociale;

    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column(name = "pays", length = 100)
    private String pays;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "fax", length = 20)
    private String fax;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "site_web", length = 200)
    private String siteWeb;

    @Column(name = "contact_principal", length = 150)
    private String contactPrincipal;

    @Column(name = "telephone_contact", length = 20)
    private String telephoneContact;

    @Column(name = "email_contact", length = 150)
    private String emailContact;

    @Column(name = "conditions_paiement", length = 100)
    private String conditionsPaiement; // 30 jours, comptant, etc.

    @Column(name = "delai_livraison")
    private Integer delaiLivraison; // en jours

    @Column(name = "actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "fournisseurPrincipal", fetch = FetchType.LAZY)
    private List<Article> articles;

    @OneToMany(mappedBy = "fournisseur", fetch = FetchType.LAZY)
    private List<StockMovement> mouvements;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

}
