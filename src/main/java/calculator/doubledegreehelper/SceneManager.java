package calculator.doubledegreehelper;

import calculator.doubledegreehelper.controller.gradeSelectionController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public final class SceneManager {
  private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);

  public enum View {
    HOME("/calculator/doubledegreehelper/home-view.fxml", "Startseite"),
    GRADEFILE("/calculator/doubledegreehelper/gradeFile-view.fxml", "Transkript"),
    THEMEFILE("/calculator/doubledegreehelper/themeFile-view.fxml", "Thema"),
    SETTINGS("/calculator/doubledegreehelper/settings-view.fxml", "Einstellungen"),
    SELECTION("/calculator/doubledegreehelper/gradeSelection-view.fxml", "Auswahl");

    private final String fxmlPath, title;
    View(String fxmlPath, String title) {
      this.fxmlPath = fxmlPath; this.title = title;
    }
    public String getFxmlPath() { return fxmlPath; }
    public String getTitle()   { return title; }
  }

  private static Stage primaryStage;
  private static final double WIDTH = 800, HEIGHT = 550;

  private SceneManager() {}

  public static void init(Stage stage) {
    primaryStage = stage;
  }

  public static void switchTo(View view) {
    if (primaryStage == null) {
      throw new IllegalStateException("SceneManager ist nicht initialisiert");
    }
    URL url = SceneManager.class.getResource(view.getFxmlPath());
    if (url == null) {
      throw new IllegalArgumentException("FXML nicht gefunden: " + view.getFxmlPath());
    }

    try {
      FXMLLoader loader = new FXMLLoader(url);
      Parent root = loader.load();

      if (view == View.SELECTION) {
        Object ctrl = loader.getController();
        if (ctrl instanceof gradeSelectionController selCtrl) {
          selCtrl.refresh();
        }
      }

      Scene scene = primaryStage.getScene();
      if (scene == null) {
        scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
      } else {
        scene.setRoot(root);
      }
      primaryStage.setTitle(view.getTitle());
      primaryStage.show();
      logger.info("Scene gewechselt " + view.getTitle());
    } catch (IOException e) {
      throw new RuntimeException("Fehler beim Laden der Szene: " + view.getFxmlPath(), e);
    }
  }
}
