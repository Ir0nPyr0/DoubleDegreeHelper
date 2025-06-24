package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import java.awt.Desktop;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.io.IOException;
import calculator.doubledegreehelper.AppManager;

public class homeController {
    @FXML
    private Button startButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button transcriptEditButton;
    @FXML
    private Button diplomaEditButton;
    @FXML
    private Button certificateEditButton;

    @FXML
    public void initialize() {
        startButton.setOnAction(this::handleStartButtonAction);
        settingsButton.setOnAction(this::handleSettingsButtonAction);

        /* Buttons öffnen jetzt die Word-Templates in C:/Programme/DegreeCalculator */
        transcriptEditButton.setOnAction(this::handleTranscriptButtonAction);
        diplomaEditButton   .setOnAction(this::handleDiplomaButtonAction);
        certificateEditButton.setOnAction(this::handleCertificateButtonAction);
    }

    private void handleStartButtonAction(ActionEvent event) {
        SceneManager.switchTo(SceneManager.View.GRADEFILE);
    }

    private void handleSettingsButtonAction(ActionEvent event) {
        SceneManager.switchTo(SceneManager.View.SETTINGS);
    }

    private static final Path TEMPLATE_DIR = Path.of(AppManager.getDefaultWordDir());

    // Kopiert (falls nötig) die Vorlage in das feste Verzeichnis und öffnet sie dort.
    private void openTemplate(String resourcePath, String fileName) {
        try {
            // Stelle sicher, dass Zielordner existiert
            Files.createDirectories(TEMPLATE_DIR);

            Path dest = TEMPLATE_DIR.resolve(fileName);

            // Falls Datei noch nicht vorhanden, aus der Ressource kopieren
            if (Files.notExists(dest)) {
                try (InputStream in = homeController.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        new Alert(Alert.AlertType.ERROR, "Ressource nicht gefunden:\n" + resourcePath)
                            .showAndWait();
                        return;
                    }
                    Files.copy(in, dest);
                }
            }

            // Öffne die Datei mit dem Standardprogramm
            Desktop.getDesktop().open(dest.toFile());

        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Datei konnte nicht geöffnet werden:\n" + ex.getMessage())
                .showAndWait();
        }
    }

    private void handleTranscriptButtonAction(ActionEvent ev) {
        openTemplate("/calculator/outputTemplate/{NUMBER}-Zeugnis-En-1.docx", "{NUMBER}-Zeugnis-En-1.docx");
    }

    private void handleDiplomaButtonAction(ActionEvent ev) {
        openTemplate("/calculator/outputTemplate/{NUMBER}-DiplomaSupplement-1.docx", "{NUMBER}-DiplomaSupplement-1.docx");
    }

    private void handleCertificateButtonAction(ActionEvent ev) {
        openTemplate("/calculator/outputTemplate/{NUMBER}-Urkunde-En-1.docx", "{NUMBER}-Urkunde-En-1.docx");
    }
}
