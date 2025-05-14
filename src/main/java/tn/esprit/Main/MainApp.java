package tn.esprit.Main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        Parent root = null;
        try {
            // Corrected the file extension to .fxml and verified path
            java.net.URL fxmlLocation = getClass().getResource("/Projet_Nefzi_FXML/FXML/SPpage03_User.fxml");
            if (fxmlLocation == null) {
                throw new IOException("FXML file not found at Projet_Nefzi_FXML/FXML/SPpage03_User.fxml");
            }

            root = FXMLLoader.load(fxmlLocation);

            primaryStage.setTitle("Gestion des Projets");
            primaryStage.setScene(new Scene(root));
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            System.out.println("Error loading FXML file: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}