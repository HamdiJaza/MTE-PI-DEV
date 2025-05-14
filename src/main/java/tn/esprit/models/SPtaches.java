package tn.esprit.models;

import java.time.LocalDate;
import java.util.Objects;

public class SPtaches {
    private int id;
    private String nom;
    private String categorie;
    private int projetId;
    private LocalDate dateCreation;

    public SPtaches() {}

    public SPtaches(int id, String nom, String categorie, int projetId, LocalDate dateCreation) {
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.projetId = projetId;
        this.dateCreation = dateCreation;
    }

    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getCategorie() { return categorie; }
    public int getProjetId() { return projetId; }
    public LocalDate getDateCreation() { return dateCreation; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public void setProjetId(int projetId) { this.projetId = projetId; }
    public void setDateCreation(LocalDate dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() { return nom; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SPtaches sPtaches = (SPtaches) o;
        return id == sPtaches.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}