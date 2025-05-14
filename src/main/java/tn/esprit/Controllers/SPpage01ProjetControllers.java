package tn.esprit.Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.models.SPprojets;
import tn.esprit.services.ServiceSPprojet;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SPpage01ProjetControllers implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(SPpage01ProjetControllers.class.getName());
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s-]{1,100}$");

    @FXML private ListView<SPprojets> listProjets;
    @FXML private Button btnNouveau;
    @FXML private Button btnExporter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnRetour;
    @FXML private TextField searchField;
    @FXML private Label currentDateLabel;
    @FXML private PieChart chartStats;

    private final ServiceSPprojet service = new ServiceSPprojet();
    private ObservableList<SPprojets> projetsList = FXCollections.observableArrayList();
    private ObservableList<SPprojets> filteredProjetsList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configure ListView
        listProjets.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(SPprojets projet, boolean empty) {
                super.updateItem(projet, empty);
                if (empty || projet == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(5);
                    container.setPadding(new Insets(10));
                    container.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5;");

                    Label nameLabel = new Label("Nom: " + projet.getNom());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                    Label etatLabel = new Label("État: " + projet.getEtatActuel());
                    etatLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

                    Label datesLabel = new Label("Du " + projet.getDateDebut() + " au " + projet.getDateFin());
                    datesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

                    Button kanbanButton = new Button("Kanban");
                    kanbanButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-background-radius: 4px;");
                    kanbanButton.setOnAction(event -> openKanbanBoard(projet));

                    HBox buttonBox = new HBox(kanbanButton);
                    buttonBox.setSpacing(10);

                    container.getChildren().addAll(nameLabel, etatLabel, datesLabel, buttonBox);
                    setGraphic(container);
                }
            }
        });

        // Load data
        loadProjets();
        listProjets.setItems(filteredProjetsList);
        currentDateLabel.setText("Date: " + LocalDate.now().toString());

        updatePieChart();

        btnNouveau.setOnAction(this::handleNouveau);
        btnExporter.setOnAction(this::handleExporter);
        btnModifier.setOnAction(this::handleModifier);
        btnSupprimer.setOnAction(this::handleSupprimer);
        //btnRetour.setOnAction(this::handleRetour);
    }

    private void loadProjets() {
        try {
            projetsList.clear();
            projetsList.addAll(service.getAll());
            filteredProjetsList.setAll(projetsList);
        } catch (Exception e) {
            LOGGER.severe("Failed to load projects: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les projets: " + e.getMessage());
        }
    }

    private void updatePieChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        long enCoursCount = service.getByEtatActuel("En cours").size();
        long termineCount = service.getByEtatActuel("Terminé").size();
        long autresCount = service.getByEtatActuel("En pause").size();

        pieChartData.add(new PieChart.Data("En cours", enCoursCount));
        pieChartData.add(new PieChart.Data("Terminé", termineCount));
        pieChartData.add(new PieChart.Data("En pause", autresCount));

        chartStats.setData(pieChartData);
        chartStats.setTitle("Répartition des Projets");
        chartStats.setLabelLineLength(10);
        chartStats.setLegendSide(javafx.geometry.Side.BOTTOM);

        int index = 0;
        for (PieChart.Data data : chartStats.getData()) {
            String color = switch (index++) {
                case 0 -> "#4169e1";
                case 1 -> "#27ae60";
                default -> "#db4495";
            };
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
    }

    private void handleNouveau(ActionEvent event) {
        Dialog<SPprojets> dialog = new Dialog<>();
        dialog.setTitle("Nouveau Projet");
        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomField = new TextField();
        nomField.setPromptText("Nom du projet");
        ComboBox<String> etatActuelCombo = new ComboBox<>(FXCollections.observableArrayList("En cours", "Terminé", "En pause"));
        etatActuelCombo.setPromptText("État actuel");
        DatePicker dateDebutPicker = new DatePicker();
        dateDebutPicker.setPromptText("Date Début");
        DatePicker dateFinPicker = new DatePicker();
        dateFinPicker.setPromptText("Date Fin");

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("État actuel:"), 0, 1);
        grid.add(etatActuelCombo, 1, 1);
        grid.add(new Label("Date Début:"), 0, 2);
        grid.add(dateDebutPicker, 1, 2);
        grid.add(new Label("Date Fin:"), 0, 3);
        grid.add(dateFinPicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        nomField.textProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));
        etatActuelCombo.valueProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));
        dateDebutPicker.valueProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));
        dateFinPicker.valueProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String nom = sanitizeInput(nomField.getText());
                    return new SPprojets(
                            0,
                            nom,
                            etatActuelCombo.getValue(),
                            dateDebutPicker.getValue(),
                            dateFinPicker.getValue()
                    );
                } catch (IllegalArgumentException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(projet -> {
            try {
                service.add(projet);
                loadProjets();
                updatePieChart();
            } catch (Exception e) {
                LOGGER.severe("Failed to add project: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter le projet: " + e.getMessage());
            }
        });
    }

    private void handleExporter(ActionEvent event) {
        try {
            btnExporter.setDisable(true);
            btnExporter.setText("Exportation...");
            service.exportToCSV("projets.csv");
            showAlert(Alert.AlertType.INFORMATION, "Exporter", "Projets exportés vers projets.csv");
        } catch (Exception e) {
            LOGGER.severe("Failed to export projects: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'exporter les projets: " + e.getMessage());
        } finally {
            btnExporter.setDisable(false);
            btnExporter.setText("Exporter");
        }
    }

    private void handleModifier(ActionEvent event) {
        SPprojets selectedProjet = listProjets.getSelectionModel().getSelectedItem();
        if (selectedProjet == null) {
            showAlert(Alert.AlertType.WARNING, "Aucun projet sélectionné", "Veuillez sélectionner un projet à modifier.");
            return;
        }

        Dialog<SPprojets> dialog = new Dialog<>();
        dialog.setTitle("Modifier Projet");
        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomField = new TextField(selectedProjet.getNom());
        ComboBox<String> etatActuelCombo = new ComboBox<>(FXCollections.observableArrayList("En cours", "Terminé", "En pause"));
        etatActuelCombo.setValue(selectedProjet.getEtatActuel());
        DatePicker dateDebutPicker = new DatePicker(selectedProjet.getDateDebut());
        DatePicker dateFinPicker = new DatePicker(selectedProjet.getDateFin());

        grid.add(new Label("Nom:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("État actuel:"), 0, 1);
        grid.add(etatActuelCombo, 1, 1);
        grid.add(new Label("Date Début:"), 0, 2);
        grid.add(dateDebutPicker, 1, 2);
        grid.add(new Label("Date Fin:"), 0, 3);
        grid.add(dateFinPicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        nomField.textProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));
        etatActuelCombo.valueProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));
        dateDebutPicker.valueProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));
        dateFinPicker.valueProperty().addListener((obs, old, newValue) -> validateDialog(saveButton, nomField, etatActuelCombo, dateDebutPicker, dateFinPicker));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String nom = sanitizeInput(nomField.getText());
                    selectedProjet.setNom(nom);
                    selectedProjet.setEtatActuel(etatActuelCombo.getValue());
                    selectedProjet.setDateDebut(dateDebutPicker.getValue());
                    selectedProjet.setDateFin(dateFinPicker.getValue());
                    return selectedProjet;
                } catch (IllegalArgumentException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(projet -> {
            try {
                service.update(projet);
                loadProjets();
                updatePieChart();
            } catch (Exception e) {
                LOGGER.severe("Failed to update project: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier le projet: " + e.getMessage());
            }
        });
    }

    private void handleSupprimer(ActionEvent event) {
        SPprojets selectedProjet = listProjets.getSelectionModel().getSelectedItem();
        if (selectedProjet == null) {
            showAlert(Alert.AlertType.WARNING, "Aucun projet sélectionné", "Veuillez sélectionner un projet à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer ce projet ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(selectedProjet);
                    loadProjets();
                    updatePieChart();
                } catch (Exception e) {
                    LOGGER.severe("Failed to delete project: " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le projet: " + e.getMessage());
                }
            }
        });
    }

    private void handleRetour(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/MainMenu.fxml"));
            Scene mainMenuScene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(mainMenuScene);
            stage.setTitle("Menu Principal");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate to main menu: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner au menu principal: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch(KeyEvent event) {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            filteredProjetsList.setAll(projetsList);
        } else {
            filteredProjetsList.setAll(projetsList.stream()
                    .filter(projet ->
                            projet.getNom().toLowerCase().contains(query) ||
                                    projet.getEtatActuel().toLowerCase().contains(query) ||
                                    projet.getDateDebut().toString().contains(query) ||
                                    projet.getDateFin().toString().contains(query)
                    )
                    .collect(Collectors.toList()));
        }
    }

    private void openKanbanBoard(SPprojets projet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/SPpage02_Kanban.fxml"));
            Scene kanbanScene = new Scene(loader.load());
            SPpage02KanbanController controller = loader.getController();
            controller.setProjetId(projet.getId());
            Stage stage = (Stage) listProjets.getScene().getWindow();
            stage.setScene(kanbanScene);
            stage.setTitle("Kanban - " + projet.getNom());
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to open Kanban board: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le tableau Kanban: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void validateDialog(Button saveButton, TextField nomField, ComboBox<String> etatActuelCombo,
                                DatePicker dateDebutPicker, DatePicker dateFinPicker) {
        saveButton.setDisable(
                nomField.getText().trim().isEmpty() ||
                        !NAME_PATTERN.matcher(nomField.getText().trim()).matches() ||
                        etatActuelCombo.getValue() == null ||
                        dateDebutPicker.getValue() == null ||
                        dateFinPicker.getValue() == null ||
                        dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())
        );
    }

    private String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du projet ne peut pas être vide.");
        }
        if (input.length() > 100) {
            throw new IllegalArgumentException("Le nom du projet ne peut pas dépasser 100 caractères.");
        }
        if (!NAME_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("Le nom du projet ne peut contenir que des lettres, chiffres, espaces ou tirets.");
        }
        return input.trim();
    }

    public void handleDemandes(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Demandes.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gestion des Demandes");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate to Demandes: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la gestion des demandes: " + e.getMessage());
        }
    }

    public void handleReservations(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Reservations.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gestion des Réservations");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate to Reservations: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la gestion des réservations: " + e.getMessage());
        }
    }

    public void handleEvenements(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Evenements.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gestion des Evènements");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate to Evenements: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers la gestion des évènements: " + e.getMessage());
        }
    }

    public void handleProjets(ActionEvent actionEvent) {
        showAlert(Alert.AlertType.INFORMATION, "Projets", "Vous êtes déjà sur la page des projets.");
    }

    public void handleParametres(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Parametres.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Paramètres");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate to Parametres: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers les paramètres: " + e.getMessage());
        }
    }

    public void logout(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Login.fxml"));
            Scene loginScene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(loginScene);
            stage.setTitle("Connexion");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to logout: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de se déconnecter: " + e.getMessage());
        }
    }

    public void showProfile(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Profile.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Mon Profil");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate to Profile: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de naviguer vers le profil: " + e.getMessage());
        }
    }
}