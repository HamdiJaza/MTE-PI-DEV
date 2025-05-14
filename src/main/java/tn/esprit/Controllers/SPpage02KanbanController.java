package tn.esprit.Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.models.SPprojets;
import tn.esprit.models.SPtaches;
import tn.esprit.services.ServiceSPprojet;
import tn.esprit.services.ServiceSPtaches;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SPpage02KanbanController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(SPpage02KanbanController.class.getName());
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s-]{1,100}$");

    @FXML private ChoiceBox<SPprojets> projectChoiceBox;
    @FXML private TextField searchField;
    @FXML private ListView<SPtaches> todoListView;
    @FXML private ListView<SPtaches> inProgressListView;
    @FXML private ListView<SPtaches> doneListView;
    @FXML private Label statusLabel;
    @FXML private Label todoColumnTitle;
    @FXML private Label inProgressColumnTitle;
    @FXML private Label doneColumnTitle;
    @FXML private Button btnBack;

    private final ObservableList<SPtaches> todoTasks = FXCollections.observableArrayList();
    private final ObservableList<SPtaches> inProgressTasks = FXCollections.observableArrayList();
    private final ObservableList<SPtaches> doneTasks = FXCollections.observableArrayList();
    private final ServiceSPtaches taskService = new ServiceSPtaches();
    private final ServiceSPprojet projectService = new ServiceSPprojet();
    private int projetId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            LOGGER.info("Initializing Kanban controller");

            // Configurer les ListViews avec leurs ObservableLists respectives
            todoListView.setItems(todoTasks);
            inProgressListView.setItems(inProgressTasks);
            doneListView.setItems(doneTasks);

            // Configurer le drag and drop et les cell factories
            setupDragAndDrop();
            setupCellFactories();

            // Configurer les écouteurs d'événements
            btnBack.setOnAction(event -> handleBack());
            searchField.textProperty().addListener((obs, old, newValue) -> handleFilter());
            projectChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
                if (newValue != null) {
                    LOGGER.info("Project selected: " + newValue.getNom() + " (ID: " + newValue.getId() + ")");
                    setProjetId(newValue.getId());
                }
            });

            // Charger les projets et initialiser les titres des colonnes
            loadProjects();
            updateColumnTitles();

            LOGGER.info("Kanban controller initialized successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to initialize Kanban controller: " + e.getMessage());
            updateStatus("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    public void setProjetId(int projetId) {
        if (projetId <= 0) {
            LOGGER.warning("Invalid project ID: " + projetId);
            return;
        }

        this.projetId = projetId;
        LOGGER.info("Setting project ID to: " + projetId);

        // Sélectionner le projet correspondant dans le ChoiceBox
        boolean found = false;
        for (SPprojets projet : projectChoiceBox.getItems()) {
            if (projet.getId() == projetId) {
                projectChoiceBox.setValue(projet);
                found = true;
                break;
            }
        }

        if (!found) {
            LOGGER.warning("Project with ID " + projetId + " not found in ChoiceBox");
        }

        // Charger les tâches pour ce projet
        loadTasks();
    }

    private void setupCellFactories() {
        todoListView.setCellFactory(lv -> new TaskListCell());
        inProgressListView.setCellFactory(lv -> new TaskListCell());
        doneListView.setCellFactory(lv -> new TaskListCell());
    }

    private void setupDragAndDrop() {
        todoListView.setOnDragDetected(event -> handleDragDetected(event, todoListView));
        inProgressListView.setOnDragDetected(event -> handleDragDetected(event, inProgressListView));
        doneListView.setOnDragDetected(event -> handleDragDetected(event, doneListView));

        todoListView.setOnDragOver(this::handleDragOver);
        inProgressListView.setOnDragOver(this::handleDragOver);
        doneListView.setOnDragOver(this::handleDragOver);

        todoListView.setOnDragDropped(event -> handleDrop(event, todoTasks, "À faire"));
        inProgressListView.setOnDragDropped(event -> handleDrop(event, inProgressTasks, "En cours"));
        doneListView.setOnDragDropped(event -> handleDrop(event, doneTasks, "Terminé"));
    }

    private void loadProjects() {
        try {
            LOGGER.info("Loading projects");

            // Récupérer tous les projets
            List<SPprojets> allProjects = projectService.getAll();
            LOGGER.info("Found " + allProjects.size() + " projects");

            // Créer une ObservableList pour le ChoiceBox
            ObservableList<SPprojets> projects = FXCollections.observableArrayList(allProjects);
            projectChoiceBox.setItems(projects);

            // Sélectionner le premier projet s'il y en a
            if (!projects.isEmpty()) {
                SPprojets firstProject = projects.get(0);
                LOGGER.info("Selecting first project: " + firstProject.getNom() + " (ID: " + firstProject.getId() + ")");
                projectChoiceBox.setValue(firstProject);
                setProjetId(firstProject.getId());
            } else {
                LOGGER.warning("No projects found");
                updateStatus("Aucun projet trouvé");
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load projects: " + e.getMessage());
            updateStatus("Erreur lors du chargement des projets: " + e.getMessage());
        }
    }

    private void loadTasks() {
        try {
            // Vider les listes existantes
            todoTasks.clear();
            inProgressTasks.clear();
            doneTasks.clear();

            // Récupérer toutes les tâches pour ce projet
            List<SPtaches> tasks = taskService.getByProjetId(projetId);
            LOGGER.info("Loaded " + tasks.size() + " tasks for project ID: " + projetId);

            // Trier les tâches par catégorie
            for (SPtaches task : tasks) {
                // Vérifier que la tâche a une date de création, sinon définir la date actuelle
                if (task.getDateCreation() == null) {
                    LOGGER.warning("Task with ID " + task.getId() + " has null date, setting to current date");
                    task.setDateCreation(LocalDate.now());
                }

                String categorie = task.getCategorie();
                if ("À faire".equals(categorie)) {
                    todoTasks.add(task);
                } else if ("En cours".equals(categorie)) {
                    inProgressTasks.add(task);
                } else if ("Terminé".equals(categorie)) {
                    doneTasks.add(task);
                } else {
                    LOGGER.warning("Task with ID " + task.getId() + " has unknown category: " + categorie);
                }
            }

            // Mettre à jour les titres des colonnes et le statut
            updateColumnTitles();
            updateStatus("Tableau Kanban chargé pour le projet ID: " + projetId);
        } catch (Exception e) {
            LOGGER.severe("Failed to load tasks: " + e.getMessage());
            updateStatus("Erreur lors du chargement des tâches: " + e.getMessage());
        }
    }

    private void handleDragDetected(MouseEvent event, ListView<SPtaches> sourceList) {
        SPtaches selected = sourceList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Dragboard db = sourceList.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(selected.getId()));
            db.setContent(content);
            event.consume();
        }
    }

    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != event.getSource() && event.getDragboard().hasString()) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
        event.consume();
    }

    private void handleDrop(DragEvent event, ObservableList<SPtaches> targetList, String category) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasString()) {
            try {
                int taskId = Integer.parseInt(db.getString());
                SPtaches task = findTaskById(taskId);
                if (task != null) {
                    LOGGER.info("Moving task ID " + taskId + " to category: " + category);

                    // Vérifier que la tâche a une date de création, sinon définir la date actuelle
                    if (task.getDateCreation() == null) {
                        LOGGER.warning("Task with ID " + task.getId() + " has null date, setting to current date");
                        task.setDateCreation(LocalDate.now());
                    }

                    // Retirer la tâche de toutes les listes
                    todoTasks.remove(task);
                    inProgressTasks.remove(task);
                    doneTasks.remove(task);

                    // Mettre à jour la catégorie de la tâche
                    task.setCategorie(category);

                    // Ajouter la tâche à la liste cible
                    targetList.add(task);

                    // Mettre à jour la tâche dans la base de données
                    taskService.update(task);

                    success = true;
                    updateColumnTitles();
                    updateStatus("Tâche déplacée vers " + category);
                    LOGGER.info("Successfully moved task ID " + taskId + " to category: " + category);
                } else {
                    LOGGER.warning("Could not find task with ID: " + taskId);
                    updateStatus("Tâche introuvable");
                }
            } catch (NumberFormatException e) {
                LOGGER.severe("Failed to parse task ID: " + e.getMessage());
                updateStatus("Erreur lors du déplacement de la tâche");
            } catch (Exception e) {
                LOGGER.severe("Failed to move task: " + e.getMessage());
                updateStatus("Erreur lors du déplacement de la tâche: " + e.getMessage());
            }
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private SPtaches findTaskById(int id) {
        return Stream.of(todoTasks, inProgressTasks, doneTasks)
                .flatMap(List::stream)
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }

    @FXML
    private void handleNewProject() {
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
                    showAlert("Erreur", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(projet -> {
            try {
                projectService.add(projet);
                loadProjects();
                projectChoiceBox.setValue(projet);
                updateStatus("Nouveau projet créé: " + projet.getNom());
            } catch (Exception e) {
                LOGGER.severe("Failed to add project: " + e.getMessage());
                updateStatus("Erreur lors de la création du projet: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleExport() {
        try {
            taskService.exportToCSV("taches_projet_" + projetId + ".csv", projetId);
            updateStatus("Tableau exporté vers taches_projet_" + projetId + ".csv");
        } catch (Exception e) {
            LOGGER.severe("Failed to export tasks: " + e.getMessage());
            updateStatus("Erreur lors de l'exportation: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilter() {
        try {
            String searchText = searchField.getText().trim().toLowerCase();
            LOGGER.info("Filtering tasks with text: '" + searchText + "'");

            if (searchText.isEmpty()) {
                LOGGER.info("Empty search text, loading all tasks");
                loadTasks();
            } else {
                LOGGER.info("Applying filter");
                filterLists(searchText);
            }

            updateStatus("Filtre appliqué: " + searchText);
            updateColumnTitles();
        } catch (Exception e) {
            LOGGER.severe("Failed to filter tasks: " + e.getMessage());
            updateStatus("Erreur lors du filtrage: " + e.getMessage());
        }
    }

    private void filterLists(String searchText) {
        try {
            // Vider les listes existantes
            todoTasks.clear();
            inProgressTasks.clear();
            doneTasks.clear();

            // Récupérer toutes les tâches pour ce projet
            List<SPtaches> tasks = taskService.getByProjetId(projetId);
            String searchLower = searchText.toLowerCase();
            LOGGER.info("Filtering " + tasks.size() + " tasks with search text: '" + searchLower + "'");

            // Filtrer et trier les tâches par catégorie
            for (SPtaches task : tasks) {
                // Vérifier que la tâche a une date de création, sinon définir la date actuelle
                if (task.getDateCreation() == null) {
                    LOGGER.warning("Task with ID " + task.getId() + " has null date, setting to current date");
                    task.setDateCreation(LocalDate.now());
                }

                if (task.getNom() != null && task.getNom().toLowerCase().contains(searchLower)) {
                    String categorie = task.getCategorie();
                    if ("À faire".equals(categorie)) {
                        todoTasks.add(task);
                    } else if ("En cours".equals(categorie)) {
                        inProgressTasks.add(task);
                    } else if ("Terminé".equals(categorie)) {
                        doneTasks.add(task);
                    }
                }
            }

            LOGGER.info("Filtered results - Todo: " + todoTasks.size() + ", In Progress: " + inProgressTasks.size() + ", Done: " + doneTasks.size());
        } catch (Exception e) {
            LOGGER.severe("Failed to filter tasks: " + e.getMessage());
            updateStatus("Erreur lors du filtrage: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddTodo() {
        addNewTask(todoTasks, "Nouvelle tâche À faire", "À faire");
    }

    @FXML
    private void handleAddInProgress() {
        addNewTask(inProgressTasks, "Nouvelle tâche En cours", "En cours");
    }

    @FXML
    private void handleAddDone() {
        addNewTask(doneTasks, "Nouvelle tâche Terminée", "Terminé");
    }

    private void addNewTask(ObservableList<SPtaches> list, String defaultText, String category) {
        Dialog<SPtaches> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle Tâche");
        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomField = new TextField(defaultText);
        Label dateLabel = new Label(LocalDate.now().toString());
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        grid.add(new Label("Description:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Date de création:"), 0, 1);
        grid.add(dateLabel, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        nomField.textProperty().addListener((obs, old, newValue) -> saveButton.setDisable(newValue.trim().isEmpty() || !NAME_PATTERN.matcher(newValue.trim()).matches()));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String nom = sanitizeInput(nomField.getText());
                    return new SPtaches(0, nom, category, projetId, LocalDate.now());
                } catch (IllegalArgumentException e) {
                    showAlert("Erreur", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(task -> {
            try {
                list.add(task);
                taskService.add(task);
                updateStatus("Tâche ajoutée à " + category);
                updateColumnTitles();
            } catch (Exception e) {
                LOGGER.severe("Failed to add task: " + e.getMessage());
                updateStatus("Erreur lors de l'ajout de la tâche: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleArchive() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Archiver Tâches");
        confirmDialog.setHeaderText("Confirmer l'archivage");
        confirmDialog.setContentText("Voulez-vous archiver toutes les tâches terminées ?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    doneTasks.forEach(taskService::delete);
                    doneTasks.clear();
                    updateColumnTitles();
                    updateStatus("Tâches terminées archivées");
                } catch (Exception e) {
                    LOGGER.severe("Failed to archive tasks: " + e.getMessage());
                    updateStatus("Erreur lors de l'archivage: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleReset() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Réinitialiser Tableau");
        confirmDialog.setHeaderText("Confirmer la réinitialisation");
        confirmDialog.setContentText("Voulez-vous réinitialiser le tableau pour ce projet ?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    taskService.deleteByProjetId(projetId);
                    todoTasks.clear();
                    inProgressTasks.clear();
                    doneTasks.clear();
                    updateColumnTitles();
                    updateStatus("Tableau réinitialisé pour le projet ID: " + projetId);
                } catch (Exception e) {
                    LOGGER.severe("Failed to reset tasks: " + e.getMessage());
                    updateStatus("Erreur lors de la réinitialisation: " + e.getMessage());
                }
            }
        });
    }

    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Projet_Nefzi_FXML/FXML/SPpage01_projet.fxml"));
            Scene projectScene = new Scene(loader.load());
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setScene(projectScene);
            stage.setTitle("Gestion des Projets");
            stage.show();
        } catch (IOException e) {
            LOGGER.severe("Failed to navigate back: " + e.getMessage());
            updateStatus("Erreur lors du retour: " + e.getMessage());
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText("Statut: " + message);
    }

    private void updateColumnTitles() {
        try {
            int todoCount = todoTasks.size();
            int inProgressCount = inProgressTasks.size();
            int doneCount = doneTasks.size();

            todoColumnTitle.setText("📋 À FAIRE (" + todoCount + ")");
            inProgressColumnTitle.setText("⚙ EN COURS (" + inProgressCount + ")");
            doneColumnTitle.setText("✅ TERMINÉ (" + doneCount + ")");

            LOGGER.info("Updated column titles - Todo: " + todoCount + ", In Progress: " + inProgressCount + ", Done: " + doneCount);
        } catch (Exception e) {
            LOGGER.severe("Failed to update column titles: " + e.getMessage());
        }
    }

    private void handleEditTask(SPtaches task, ObservableList<SPtaches> list) {
        Dialog<SPtaches> dialog = new Dialog<>();
        dialog.setTitle("Modifier Tâche");
        ButtonType saveButtonType = new ButtonType("Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomField = new TextField(task.getNom());
        DatePicker datePicker = new DatePicker(task.getDateCreation());

        grid.add(new Label("Description:"), 0, 0);
        grid.add(nomField, 1, 0);
        grid.add(new Label("Date de création:"), 0, 1);
        grid.add(datePicker, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        nomField.textProperty().addListener((obs, old, newValue) -> saveButton.setDisable(newValue.trim().isEmpty() || !NAME_PATTERN.matcher(newValue.trim()).matches() || datePicker.getValue() == null));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String nom = sanitizeInput(nomField.getText());
                    task.setNom(nom);
                    task.setDateCreation(datePicker.getValue());
                    return task;
                } catch (IllegalArgumentException e) {
                    showAlert("Erreur", e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTask -> {
            try {
                taskService.update(updatedTask);
                int index = list.indexOf(updatedTask);
                if (index >= 0) {
                    list.set(index, updatedTask);
                }
                updateStatus("Tâche modifiée: " + updatedTask.getNom());
            } catch (Exception e) {
                LOGGER.severe("Failed to update task: " + e.getMessage());
                updateStatus("Erreur lors de la modification: " + e.getMessage());
            }
        });
    }

    private void handleDeleteTask(SPtaches task, ObservableList<SPtaches> list) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Supprimer Tâche");
        confirmDialog.setHeaderText("Confirmer la suppression");
        confirmDialog.setContentText("Voulez-vous vraiment supprimer la tâche: " + task.getNom() + " ?");

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    list.remove(task);
                    taskService.delete(task);
                    updateStatus("Tâche supprimée: " + task.getNom());
                    updateColumnTitles();
                } catch (Exception e) {
                    LOGGER.severe("Failed to delete task: " + e.getMessage());
                    updateStatus("Erreur lors de la suppression: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void validateDialog(Button saveButton, TextField nomField, ComboBox<String> etatActuelCombo, DatePicker dateDebutPicker, DatePicker dateFinPicker) {
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
            throw new IllegalArgumentException("Le nom ne peut pas être vide.");
        }
        if (input.length() > 100) {
            throw new IllegalArgumentException("Le nom ne peut pas dépasser 100 caractères.");
        }
        if (!NAME_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException("Le nom ne peut contenir que des lettres, chiffres, espaces ou tirets.");
        }
        return input.trim();
    }

    private class TaskListCell extends ListCell<SPtaches> {
        private final VBox vBox = new VBox(5);
        private final Label nameLabel = new Label();
        private final Label dateLabel = new Label();
        private final Button editButton = new Button("Modifier");
        private final Button deleteButton = new Button("Supprimer");
        private final HBox buttonBox = new HBox(10);

        public TaskListCell() {
            vBox.setPadding(new Insets(10));
            vBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5;");
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            editButton.setStyle("-fx-font-size: 12px; -fx-background-color: #4169e1; -fx-text-fill: white; -fx-background-radius: 4px;");
            deleteButton.setStyle("-fx-font-size: 12px; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-background-radius: 4px;");
            buttonBox.getChildren().addAll(editButton, deleteButton);
            vBox.getChildren().addAll(nameLabel, dateLabel, buttonBox);
            setOnMouseEntered(e -> vBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5;"));
            setOnMouseExited(e -> vBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ccc; -fx-border-radius: 5; -fx-background-radius: 5;"));
        }

        @Override
        protected void updateItem(SPtaches task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(task.getNom());
                dateLabel.setText("Créé le: " + task.getDateCreation());
                editButton.setOnAction(e -> handleEditTask(task, getListView().getItems()));
                deleteButton.setOnAction(e -> handleDeleteTask(task, getListView().getItems()));
                setGraphic(vBox);
            }
        }
    }
}