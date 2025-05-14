package tn.esprit.models;

import java.time.LocalDate;

public class SPprojets {
    private int id;
    private String nom;
    private String etatActuel;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Default constructor
    public SPprojets() {}

    // Parameterized constructor
    public SPprojets(int id, String nom, String etatActuel, LocalDate dateDebut, LocalDate dateFin) {
        this.id = id;
        this.nom = nom;
        this.etatActuel = etatActuel;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getEtatActuel() { return etatActuel; }
    public LocalDate getDateDebut() { return dateDebut; }
    public LocalDate getDateFin() { return dateFin; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setEtatActuel(String etatActuel) { this.etatActuel = etatActuel; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    @Override
    public String toString() {
        return "SPprojets{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", etatActuel='" + etatActuel + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }
}