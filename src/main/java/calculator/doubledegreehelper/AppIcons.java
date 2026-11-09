package calculator.doubledegreehelper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Setzt das App-Icon (Titelleiste, Taskleiste, Alt+Tab) für alle Fenster der App. */
public final class AppIcons {
  private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};
  private static List<Image> icons;

  private AppIcons() {}

  public static synchronized List<Image> get() {
    if (icons == null) {
      icons = new ArrayList<>();
      for (int s : SIZES) {
        try (InputStream in = AppIcons.class.getResourceAsStream("/calculator/appicon/icon-" + s + ".png")) {
          if (in != null) icons.add(new Image(in));
        } catch (Exception ignored) {
          // fehlendes Icon ist kein Grund, die App nicht zu starten
        }
      }
    }
    return icons;
  }

  /** Icon für das Hauptfenster und automatisch für jedes später geöffnete Fenster (Dialoge, Hilfe …). */
  public static void installGlobally(Stage primary) {
    apply(primary);
    Window.getWindows().addListener((ListChangeListener<Window>) c -> {
      while (c.next()) {
        for (Window w : c.getAddedSubList()) apply(w);
      }
    });
  }

  private static void apply(Window w) {
    if (w instanceof Stage s && s.getIcons().isEmpty()) s.getIcons().setAll(get());
  }
}
