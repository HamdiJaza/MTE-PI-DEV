package tn.esprit.Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.models.SPtaches;
import tn.esprit.models.SPprojets;
import tn.esprit.services.ServiceSPprojet;
import tn.esprit.services.ServiceSPtaches;
import javafx.animation.PauseTransition;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SPpage03UserController implements Initializable {
    @FXML private ListView<SPprojets> listProjets;
    @FXML private ChoiceBox<String> projectChoiceBox;
    @FXML private TextField searchField;
    @FXML private Button btnDemandes;
    @FXML private Button btnReservations;
    @FXML private Button btnEvenements;
    @FXML private Button btnProjets;
    @FXML private Button btnParametres;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;
    @FXML private ImageView imgDemandes;
    @FXML private ImageView imgReservations;
    @FXML private ImageView imgEvenements;
    @FXML private ImageView imgProjets;
    @FXML private ImageView imgParametres;
    @FXML private ImageView imgProfile;
    @FXML private Label userNameLabel;
    @FXML private Label currentDateLabel;
    @FXML private Label tasksDoneLabel;
    @FXML private Label tasksNotDoneLabel;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private PieChart chartStats;

    private ObservableList<SPprojets> projectList = FXCollections.observableArrayList();
    private ServiceSPprojet projectService = new ServiceSPprojet();
    private ServiceSPtaches taskService = new ServiceSPtaches();
    private PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    private static final String ACCESS_DENIED_MESSAGE = "Statut: Accès non autorisé";
    private static final String INITIALIZED_STATUS = "Statut: Initialisé";
    private static final String NO_PROJECTS_STATUS = "Statut: Aucun projet disponible";
    private static final String NO_PROJECT_SELECTED = "Aucun projet sélectionné";
    private static final String GUEST_USER = "Guest";
    private static final String SELECTED_PROJECT_STATUS = "Statut: Projet '%s' sélectionné";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize ListView
        listProjets.setCellFactory(param -> new ProjectListCell());
        listProjets.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listProjets.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                updateTaskProgress(newValue.getId());
                projectChoiceBox.getSelectionModel().select(newValue.getNom());
                statusLabel.setText(String.format(SELECTED_PROJECT_STATUS, newValue.getNom()));
            } else {
                clearTaskProgress();
                projectChoiceBox.getSelectionModel().select(NO_PROJECT_SELECTED);
                statusLabel.setText(NO_PROJECT_SELECTED);
            }
        });

        // Initialize choice box
        ObservableList<String> choiceBoxItems = FXCollections.observableArrayList(NO_PROJECT_SELECTED);
        projectChoiceBox.setItems(choiceBoxItems);
        projectChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                if (newValue.equals(NO_PROJECT_SELECTED)) {
                    listProjets.getSelectionModel().clearSelection();
                    clearTaskProgress();
                    statusLabel.setText(NO_PROJECT_SELECTED);
                } else {
                    SPprojets selected = projectList.stream()
                            .filter(p -> p.getNom().equals(newValue))
                            .findFirst()
                            .orElse(null);
                    if (selected != null) {
                        listProjets.getSelectionModel().select(selected);
                        updateTaskProgress(selected.getId());
                        statusLabel.setText(String.format(SELECTED_PROJECT_STATUS, selected.getNom()));
                    }
                }
            }
        });

        // Load projects asynchronously
        loadProjectsAsync();

        // Set user and date (uncomment if LoginController is available)
        /*
        userNameLabel.setText(LoginController.getCurrentUser() != null
                ? LoginController.getCurrentUser().getUsername()
                : GUEST_USER);
        currentDateLabel.setText("Date: " + LocalDate.now());
        */
        userNameLabel.setText(GUEST_USER); // Placeholder
        currentDateLabel.setText("Date: " + LocalDate.now());

        // Debounced search functionality
        searchDebounce.setOnFinished(event -> {
            String searchText = searchField.getText().toLowerCase();
            listProjets.setItems(projectList.filtered(project ->
                    String.valueOf(project.getId()).contains(searchText) ||
                            project.getNom().toLowerCase().contains(searchText) ||
                            project.getEtatActuel().toLowerCase().contains(searchText))); // Updated to status
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> searchDebounce.playFromStart());

        // Disable navigation buttons for read-only user
        btnDemandes.setDisable(true);
        btnReservations.setDisable(true);
        btnEvenements.setDisable(true);
        btnProjets.setDisable(true);
        btnParametres.setDisable(true);
        profileButton.setDisable(true);

        // Set initial state
        projectChoiceBox.getSelectionModel().select(NO_PROJECT_SELECTED);
        clearTaskProgress();
    }

    /**
     * Custom ListCell for displaying project details.
     */
    private class ProjectListCell extends ListCell<SPprojets> {
        private HBox content;
        private Label idLabel;
        private Label nameLabel;
        private Label vetLabel; // Renamed from categoryLabel
        private Label statusLabel; // Renamed from etatActuelLabel
        private Label dateDebutLabel;
        private Label dateFinLabel;

        public ProjectListCell() {
            super();
            idLabel = new Label();
            nameLabel = new Label();
            vetLabel = new Label();
            statusLabel = new Label();
            dateDebutLabel = new Label();
            dateFinLabel = new Label();

            idLabel.getStyleClass().add("project-id");
            nameLabel.getStyleClass().add("project-name");
            vetLabel.getStyleClass().add("project-vet");
            statusLabel.getStyleClass().add("project-status");
            dateDebutLabel.getStyleClass().add("project-dates");
            dateFinLabel.getStyleClass().add("project-dates");

            VBox idBox = new VBox(idLabel);
            idBox.setPrefWidth(60);
            VBox nameBox = new VBox(nameLabel);
            nameBox.setPrefWidth(150);
            VBox vetBox = new VBox(vetLabel);
            vetBox.setPrefWidth(100);
            VBox statusBox = new VBox(statusLabel);
            statusBox.setPrefWidth(100);
            VBox datesBox = new VBox(dateDebutLabel, dateFinLabel);
            datesBox.setPrefWidth(120);

            content = new HBox(idBox, nameBox, vetBox, statusBox, datesBox);
            content.setSpacing(10);
            content.getStyleClass().add("project-cell");
        }

        @Override
        protected void updateItem(SPprojets project, boolean empty) {
            super.updateItem(project, empty);
            if (empty || project == null) {
                setGraphic(null);
            } else {
                idLabel.setText(String.valueOf(project.getId()));
                nameLabel.setText(project.getNom());
                statusLabel.setText(project.getEtatActuel());
                dateDebutLabel.setText(project.getDateDebut() != null ? project.getDateDebut().toString() : "");
                dateFinLabel.setText(project.getDateFin() != null ? project.getDateFin().toString() : "");
                setGraphic(content);
            }
        }
    }

    /**
     * Loads projects from the database asynchronously to prevent UI freezing.
     */
    private void loadProjectsAsync() {
        Task<List<SPprojets>> loadProjectsTask = new Task<>() {
            @Override
            protected List<SPprojets> call() {
                return projectService.getAll();
            }
        };

        loadProjectsTask.setOnSucceeded(event -> {
            projectList.addAll(loadProjectsTask.getValue());
            listProjets.setItems(projectList);
            if (projectList.isEmpty()) {
                statusLabel.setText(NO_PROJECTS_STATUS);
            } else {
                statusLabel.setText(INITIALIZED_STATUS);
                projectChoiceBox.getItems().addAll(
                        projectList.stream().map(SPprojets::getNom).collect(Collectors.toList()));
            }
        });

        loadProjectsTask.setOnFailed(event -> {
            statusLabel.setText("Erreur lors du chargement des projets: " + loadProjectsTask.getException().getMessage());
        });

        new Thread(loadProjectsTask).start();
    }

    /**
     * Updates the task progress statistics for the given project ID.
     */
    private void updateTaskProgress(int projectId) {
        Task<List<SPtaches>> loadTasksTask = new Task<>() {
            @Override
            protected List<SPtaches> call() {
                return taskService.getByProjetId(projectId);
            }
        };

        loadTasksTask.setOnSucceeded(event -> {
            List<SPtaches> tasks = loadTasksTask.getValue();
            long done = tasks.stream().filter(t -> "Terminé".equals(t.getCategorie())).count();
            long notDone = tasks.size() - done;
            tasksDoneLabel.setText("Tâches Terminées: " + done);
            tasksNotDoneLabel.setText("Tâches Non Terminées: " + notDone);
            double progress = tasks.isEmpty() ? 0 : (done * 100.0 / tasks.size());
            progressLabel.setText(String.format("Avancement: %.2f%%", progress));

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    new PieChart.Data("Terminées", done),
                    new PieChart.Data("Non Terminées", notDone)
            );
            chartStats.setData(pieChartData);
        });

        loadTasksTask.setOnFailed(event -> {
            statusLabel.setText("Erreur lors du chargement des statistiques: " + loadTasksTask.getException().getMessage());
            clearTaskProgress();
        });

        new Thread(loadTasksTask).start();
    }

    private void clearTaskProgress() {
        tasksDoneLabel.setText("Tâches Terminées: 0");
        tasksNotDoneLabel.setText("Tâches Non Terminées: 0");
        progressLabel.setText("Avancement: 0%");
        chartStats.setData(FXCollections.observableArrayList());
    }

    @FXML
    private void handleDemandes() {
        statusLabel.setText(ACCESS_DENIED_MESSAGE + " aux Demandes");
    }

    @FXML
    private void handleReservations() {
        statusLabel.setText(ACCESS_DENIED_MESSAGE + " aux Réservations");
    }

    @FXML
    private void handleEvenements() {
        statusLabel.setText(ACCESS_DENIED_MESSAGE + " aux Evènements");
    }

    @FXML
    private void handleProjets() {
        statusLabel.setText("Statut: Déjà sur Projets");
    }

    @FXML
    private void handleParametres() {
        statusLabel.setText(ACCESS_DENIED_MESSAGE + " aux Paramètres");
    }

    @FXML
    private void showProfile() {
        statusLabel.setText(ACCESS_DENIED_MESSAGE + " au Profil");
    }

    @FXML
    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) listProjets.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
}