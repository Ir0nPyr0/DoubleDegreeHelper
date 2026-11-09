package calculator.doubledegreehelper;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private final SaveHandler saveHandler = new SaveHandler();
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage stage) {
        boolean loaded = saveHandler.loadData();
        StudentRegistry.load();
        SceneManager.init(stage);
        SceneManager.switchTo(SceneManager.View.HOME);
        if (!loaded) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(stage);
            alert.setTitle("Moduldaten konnten nicht geladen werden");
            alert.setHeaderText("Die modules.dat ist beschädigt oder nicht kompatibel.");
            alert.setContentText("Datei: " + SaveHandler.getDataFile()
                + "\n\nSpeichern ist gesperrt, damit die vorhandene Datei nicht überschrieben wird."
                + "\nDetails stehen in modules-load-error.log im selben Ordner."
                + "\n\nBitte die Anwendung schließen und eine passende Sicherung einspielen.");
            alert.showAndWait();
        }
    }
    @Override
    public void stop() throws Exception {
        super.stop();
        saveHandler.saveData();
        StudentRegistry.save();
    }
}
