package calculator.doubledegreehelper;

import javafx.application.Application;
import javafx.stage.Stage;
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
        saveHandler.loadData();
        SceneManager.init(stage);
        SceneManager.switchTo(SceneManager.View.HOME);
    }
    @Override
    public void stop() throws Exception {
        super.stop();
        saveHandler.saveData();
    }
}
