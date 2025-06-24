package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.AppManager;
import calculator.doubledegreehelper.SceneManager;
import calculator.doubledegreehelper.ThemeReader;
import calculator.doubledegreehelper.TranscriptPdfReader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class themeFileController {
    @FXML private Button backButton, continueButton, fileChooserButton;

    @FXML
    public void initialize() {
        continueButton.setDisable(true);
        fileChooserButton.setOnAction(this::handleFileChoose);
        backButton       .setOnAction(e -> SceneManager.switchTo(SceneManager.View.GRADEFILE));
        continueButton   .setOnAction(e -> SceneManager.switchTo(SceneManager.View.SELECTION));
    }

    private void handleFileChoose(ActionEvent ev) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Theme-Excel wählen");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx")
        );
        String gradePath = AppManager.getFilePath();
        if (gradePath != null) {
            java.io.File initDir = new java.io.File(gradePath).getParentFile();
            if (initDir != null && initDir.exists()) {
                chooser.setInitialDirectory(initDir);
            }
        }
        Window w = ((Node) ev.getSource()).getScene().getWindow();
        File file = chooser.showOpenDialog(w);
        if (file != null) {
            try {
                ThemeReader.fillThemeAndGenderFromExcel(file.toPath(), AppManager.getStudent());
                AppManager.setThemeFilePath(file.getAbsolutePath());
                continueButton.setDisable(false);
                SceneManager.switchTo(SceneManager.View.SELECTION);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Fehler beim Einlesen der XLSX:\n" + ex.getMessage())
                    .showAndWait();
                continueButton.setDisable(true);
            }
        }
    }
}
