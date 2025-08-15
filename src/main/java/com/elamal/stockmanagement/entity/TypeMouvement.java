package com.elamal.stockmanagement.entity;

public enum TypeMouvement {
    ENTREE("Entrée de stock"),
    SORTIE("Sortie de stock"),
    INVENTAIRE("Ajustement inventaire"),
    RETOUR_CLIENT("Retour client"),
    RETOUR_FOURNISSEUR("Retour fournisseur"),
    PERTE("Perte/Casse"),
    TRANSFERT_ENTREE("Transfert entrant"),
    TRANSFERT_SORTIE("Transfert sortant"),
    CORRECTION("Correction manuelle");

    private final String description;

    TypeMouvement(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEntree() {
        return this == ENTREE || this == RETOUR_CLIENT || this == TRANSFERT_ENTREE ||
                (this == INVENTAIRE) || (this == CORRECTION);
    }

    public boolean isSortie() {
        return this == SORTIE || this == RETOUR_FOURNISSEUR || this == PERTE ||
                this == TRANSFERT_SORTIE;
    }
}
