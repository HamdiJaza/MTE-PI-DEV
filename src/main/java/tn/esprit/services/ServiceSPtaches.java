package tn.esprit.services;

import tn.esprit.interfaces.ISuivieSP;
import tn.esprit.models.SPtaches;
import tn.esprit.utils.MyDataBase;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceSPtaches implements ISuivieSP<SPtaches> {
    private Connection cnx = MyDataBase.getInstance().getCnx();

    @Override
    public void add(SPtaches task) {
        if (task.getNom() == null || task.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la tâche ne peut pas être vide");
        }
        if (task.getCategorie() == null || task.getCategorie().trim().isEmpty()) {
            throw new IllegalArgumentException("La catégorie de la tâche ne peut pas être vide");
        }
        if (task.getProjetId() <= 0) {
            throw new IllegalArgumentException("L'ID du projet doit être positif");
        }
        // Si la date de création est nulle, on utilise la date actuelle
        if (task.getDateCreation() == null) {
            task.setDateCreation(LocalDate.now());
        }

        String query = "INSERT INTO taches (nom, categorie, projet_id, date_creation) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, task.getNom());
            pst.setString(2, task.getCategorie());
            pst.setInt(3, task.getProjetId());
            // Gérer les dates nulles ou invalides
            LocalDate dateCreation = task.getDateCreation();
            if (dateCreation == null) {
                dateCreation = LocalDate.now();
            }
            pst.setDate(4, Date.valueOf(dateCreation));
            pst.executeUpdate();
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                task.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout de la tâche: " + e.getMessage());
        }
    }

    @Override
    public List<SPtaches> getAll() {
        List<SPtaches> tasks = new ArrayList<>();
        String query = "SELECT * FROM taches";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                LocalDate localDate;
                try {
                    Date dateCreation = rs.getDate("date_creation");
                    localDate = (dateCreation != null) ? dateCreation.toLocalDate() : LocalDate.now();
                } catch (SQLException e) {
                    // En cas d'erreur avec la date (comme "Zero date value prohibited")
                    System.out.println("Erreur de date pour la tâche ID " + rs.getInt("id") + ": " + e.getMessage());
                    localDate = LocalDate.now();
                }

                tasks.add(new SPtaches(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("categorie"),
                        rs.getInt("projet_id"),
                        localDate
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tâches: " + e.getMessage());
        }
        return tasks;
    }

    @Override
    public void update(SPtaches task) {
        if (task.getNom() == null || task.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la tâche ne peut pas être vide");
        }
        if (task.getCategorie() == null || task.getCategorie().trim().isEmpty()) {
            throw new IllegalArgumentException("La catégorie de la tâche ne peut pas être vide");
        }
        if (task.getProjetId() <= 0) {
            throw new IllegalArgumentException("L'ID du projet doit être positif");
        }
        // Si la date de création est nulle, on utilise la date actuelle
        if (task.getDateCreation() == null) {
            task.setDateCreation(LocalDate.now());
        }

        String query = "UPDATE taches SET nom = ?, categorie = ?, projet_id = ?, date_creation = ? WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, task.getNom());
            pst.setString(2, task.getCategorie());
            pst.setInt(3, task.getProjetId());
            // Gérer les dates nulles ou invalides
            LocalDate dateCreation = task.getDateCreation();
            if (dateCreation == null) {
                dateCreation = LocalDate.now();
            }
            pst.setDate(4, Date.valueOf(dateCreation));
            pst.setInt(5, task.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la tâche: " + e.getMessage());
        }
    }

    @Override
    public void delete(SPtaches task) {
        String query = "DELETE FROM taches WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, task.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la tâche: " + e.getMessage());
        }
    }

    public List<SPtaches> getByCategory(String category) {
        List<SPtaches> tasks = new ArrayList<>();
        String query = "SELECT * FROM taches WHERE categorie = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setString(1, category);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    LocalDate localDate;
                    try {
                        Date dateCreation = rs.getDate("date_creation");
                        localDate = (dateCreation != null) ? dateCreation.toLocalDate() : LocalDate.now();
                    } catch (SQLException e) {
                        // En cas d'erreur avec la date (comme "Zero date value prohibited")
                        System.out.println("Erreur de date pour la tâche ID " + rs.getInt("id") + ": " + e.getMessage());
                        localDate = LocalDate.now();
                    }

                    tasks.add(new SPtaches(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("categorie"),
                            rs.getInt("projet_id"),
                            localDate
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération par catégorie: " + e.getMessage());
        }
        return tasks;
    }

    public List<SPtaches> getByProjetId(int projetId) {
        List<SPtaches> tasks = new ArrayList<>();
        String query = "SELECT * FROM taches WHERE projet_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, projetId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    LocalDate localDate;
                    try {
                        Date dateCreation = rs.getDate("date_creation");
                        localDate = (dateCreation != null) ? dateCreation.toLocalDate() : LocalDate.now();
                    } catch (SQLException e) {
                        // En cas d'erreur avec la date (comme "Zero date value prohibited")
                        System.out.println("Erreur de date pour la tâche ID " + rs.getInt("id") + ": " + e.getMessage());
                        localDate = LocalDate.now();
                    }

                    tasks.add(new SPtaches(
                            rs.getInt("id"),
                            rs.getString("nom"),
                            rs.getString("categorie"),
                            rs.getInt("projet_id"),
                            localDate
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération par projet ID: " + e.getMessage());
        }
        return tasks;
    }

    public void deleteByProjetId(int projetId) {
        String query = "DELETE FROM taches WHERE projet_id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, projetId);
            pst.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression des tâches par projet ID: " + e.getMessage());
        }
    }

    public void exportToCSV(String filePath, int projetId) {
        String query = "SELECT * FROM taches WHERE projet_id = ?";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, projetId);
            try (ResultSet rs = pst.executeQuery()) {
                writer.write("ID,Nom,Categorie,Projet ID,Date Creation\n");
                while (rs.next()) {
                    writer.write(String.format("%d,%s,%s,%d,%s\n",
                            rs.getInt("id"),
                            rs.getString("nom").replace(",", ""),
                            rs.getString("categorie"),
                            rs.getInt("projet_id"),
                            getDateString(rs)));
                }
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Erreur lors de l'exportation CSV: " + e.getMessage());
        }
    }

    // Méthode utilitaire pour récupérer une date de manière sécurisée
    private String getDateString(ResultSet rs) {
        try {
            Date date = rs.getDate("date_creation");
            return (date != null) ? date.toString() : LocalDate.now().toString();
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de la date: " + e.getMessage());
            return LocalDate.now().toString();
        }
    }
}