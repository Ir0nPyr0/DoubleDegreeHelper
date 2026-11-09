package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.AppManager;
import calculator.doubledegreehelper.SceneManager;
import calculator.doubledegreehelper.TranscriptPdfReader;
import calculator.doubledegreehelper.student.Student;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class gradeFileController {
    @FXML private Button backButton, continueButton, fileChooserButton;
    private final Preferences prefs = Preferences.userNodeForPackage(gradeFileController.class);

    @FXML
    public void initialize() {
        continueButton.setDisable(true);
        fileChooserButton.setOnAction(this::handleFileChoose);
        backButton       .setOnAction(e -> SceneManager.switchTo(SceneManager.View.HOME));
        continueButton   .setOnAction(e -> SceneManager.switchTo(SceneManager.View.THEMEFILE));
    }

    private void handleFileChoose(ActionEvent ev) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Transkript-PDF wählen");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        File lastDir = new File(prefs.get("lastPdfDir", ""));
        if (lastDir.isDirectory()) chooser.setInitialDirectory(lastDir);
        Window w = ((Node) ev.getSource()).getScene().getWindow();
        File file = chooser.showOpenDialog(w);
        if (file != null) {
            if (file.getParentFile() != null) prefs.put("lastPdfDir", file.getParentFile().getAbsolutePath());
            try {
                Student s = TranscriptPdfReader.read(file.toPath());
                AppManager.setStudent(s);
                AppManager.setFilePath(file.getAbsolutePath());
                continueButton.setDisable(false);
                SceneManager.switchTo(SceneManager.View.THEMEFILE);
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Fehler beim Einlesen der PDF:\n" + ex.getMessage())
                    .showAndWait();
                continueButton.setDisable(true);
            }
        }
    }
}
