package tn.esprit.services;

import tn.esprit.interfaces.ISuivieSP;
import tn.esprit.models.SPprojets;
import tn.esprit.utils.MyDataBase;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSPprojet implements ISuivieSP<SPprojets> {
    private Connection cnx = MyDataBase.getInstance().getCnx();

    @Override
    public void add(SPprojets projet) {
        if (projet.getNom() == null || projet.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du projet ne peut pas être vide");
        }
        if (projet.getEtatActuel() == null || projet.getEtatActuel().trim().isEmpty()) {
            throw new IllegalArgumentException("L'état actuel ne peut pas être vide");
        }
        if (projet.getDateDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire");
        }
        if (projet.getDateFin() == null) {
            throw new IllegalArgumentException("La date de fin est obligatoire");
        }
        if (projet.getDateFin().isBefore(projet.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }

        String query = "INSERT INTO projets (nom, etatActuel, date_debut, date_fin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, projet.getNom());
            pst.setString(2, projet.getEtatActuel());
            pst.setDate(3, Date.valueOf(projet.getDateDebut()));
            pst.setDate(4, Date.valueOf(projet.getDateFin()));
            pst.executeUpdate();
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                projet.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du projet: " + e.getMessage());
        }
    }

    @Override
    public List<SPprojets> getAll() {
        List<SPprojets> projets = new ArrayList<>();
        String query = "SELECT * FROM projets";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                projets.add(new SPprojets(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("etatActuel"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des projets: " + e.getMessage());
        }
        return projets;
    }

    @Override
    public void update(SPprojets projet) {
        String query = "UPDATE projets SET nom = ?, etatActuel = ?, date_debut = ?, date_fin = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, projet.getNom());
            pst.setString(2, projet.getEtatActuel());
            pst.setDate(3, Date.valueOf(projet.getDateDebut()));
            pst.setDate(4, Date.valueOf(projet.getDateFin()));
            pst.setInt(5, projet.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour du projet: " + e.getMessage());
        }
    }

    @Override
    public void delete(SPprojets projet) {
        String query = "DELETE FROM projets WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, projet.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du projet: " + e.getMessage());
        }
    }

    public List<SPprojets> getByEtatActuel(String etatActuel) {
        List<SPprojets> projets = new ArrayList<>();
        String query = "SELECT * FROM projets WHERE etatActuel = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, etatActuel);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                projets.add(new SPprojets(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("etatActuel"),
                        rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération par état: " + e.getMessage());
        }
        return projets;
    }

    public void exportToCSV(String filePath) {
        String query = "SELECT * FROM projets";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
             Statement stmt = cnx.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            writer.write("ID,Nom,EtatActuel,Date Debut,Date Fin\n");
            while (rs.next()) {
                writer.write(String.format("%d,%s,%s,%s,%s\n",
                        rs.getInt("id"),
                        rs.getString("nom").replace(",", ""),
                        rs.getString("etatActuel"),
                        rs.getDate("date_debut").toString(),
                        rs.getDate("date_fin").toString()));
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Erreur lors de l'exportation CSV: " + e.getMessage());
        }
    }
}