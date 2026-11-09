package calculator.doubledegreehelper;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Geführte Einführung: Hebt jeweils ein Bedienelement hervor, dunkelt den Rest ab
 * und erklärt das Element in einer Sprechblase daneben.
 *
 * Die Szene wird dafür kurz in ein StackPane gepackt (Original-Root + Overlay)
 * und beim Beenden wieder hergestellt.
 */
public final class OnboardingTour {

  /** Ein Schritt der Tour. Ohne Ziele wird die Sprechblase mittig angezeigt. */
  public record Step(String title, String text, List<Node> targets) {}

  public static Step step(String title, String text, Node... targets) {
    return new Step(title, text, Arrays.stream(targets).filter(Objects::nonNull).toList());
  }

  private static final double PAD = 6;
  private static final double GAP = 16;
  private static final double MARGIN = 12;
  private static final double CARD_WIDTH = 340;

  private static OnboardingTour running;

  private final Scene scene;
  private final Parent originalRoot;
  private final List<Step> steps;
  private final Runnable onFinish;

  private final StackPane wrapper = new StackPane();
  private final Pane overlay = new Pane();
  private final Rectangle ring = new Rectangle();
  private Shape dim;

  private final VBox card = new VBox(8);
  private final Label counter = new Label();
  private final Label title = new Label();
  private final Label text = new Label();
  private final Button backButton = new Button("Zurück");
  private final Button nextButton = new Button("Weiter");

  private final EventHandler<KeyEvent> keyFilter = this::onKey;
  private int index = -1;

  private OnboardingTour(Scene scene, List<Step> steps, Runnable onFinish) {
    this.scene = scene;
    this.originalRoot = scene.getRoot();
    this.steps = new ArrayList<>(steps);
    this.onFinish = onFinish;
    buildUi();
  }

  /** Startet die Tour in der Szene von {@code anyNode}. Läuft bereits eine, passiert nichts. */
  public static void start(Node anyNode, List<Step> steps, Runnable onFinish) {
    if (running != null || anyNode == null || anyNode.getScene() == null || steps.isEmpty()) return;
    running = new OnboardingTour(anyNode.getScene(), steps, onFinish);
    running.show();
  }

  public static boolean isRunning() {
    return running != null;
  }

  // ------------------------------------------------------------------ Tour nach Szenenwechsel

  /** Welche Tour nach dem nächsten Szenenwechsel starten soll (z. B. aus dem Hilfe-Fenster heraus). */
  private static String pendingTour;

  public static void requestAfterSwitch(String tourId) {
    pendingTour = tourId;
  }

  /** true, wenn für diese Ansicht eine Tour angefordert wurde (wird dabei zurückgesetzt). */
  public static boolean consumeRequest(String tourId) {
    if (tourId.equals(pendingTour)) {
      pendingTour = null;
      return true;
    }
    return false;
  }

  /** Führt {@code action} einmal kurz aus, nachdem {@code root} in einer Szene angezeigt wird. */
  public static void runWhenShown(Node root, Runnable action) {
    Runnable delayed = () -> {
      javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(Duration.millis(450));
      wait.setOnFinished(e -> { if (root.getScene() != null) action.run(); });
      wait.play();
    };
    if (root.getScene() != null) {
      delayed.run();
      return;
    }
    root.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
      @Override
      public void changed(javafx.beans.value.ObservableValue<? extends Scene> obs, Scene old, Scene sc) {
        if (sc == null) return;
        obs.removeListener(this); // nur einmal
        delayed.run();
      }
    });
  }

  // ------------------------------------------------------------------ Aufbau

  private void buildUi() {
    var css = OnboardingTour.class.getResource("/calculator/doubledegreehelper/help.css");
    if (css != null) wrapper.getStylesheets().add(css.toExternalForm());

    overlay.getStyleClass().add("tour-overlay");
    overlay.setPickOnBounds(true);
    // Klicks auf den abgedunkelten Bereich nicht an die App weitergeben
    overlay.setOnMouseClicked(javafx.event.Event::consume);
    overlay.setOnMousePressed(javafx.event.Event::consume);
    overlay.setOnMouseReleased(javafx.event.Event::consume);

    ring.getStyleClass().add("tour-ring");
    ring.setArcWidth(16);
    ring.setArcHeight(16);
    ring.setFill(Color.TRANSPARENT);
    ring.setMouseTransparent(true);

    counter.getStyleClass().add("tour-counter");
    title.getStyleClass().add("tour-title");
    title.setWrapText(true);
    text.getStyleClass().add("tour-text");
    text.setWrapText(true);
    text.setMinHeight(Region.USE_PREF_SIZE);

    Button skip = new Button("Tour beenden");
    skip.getStyleClass().add("tour-link");
    skip.setOnAction(e -> finish());
    backButton.setOnAction(e -> go(index - 1));
    nextButton.getStyleClass().add("primary");
    nextButton.setDefaultButton(true);
    nextButton.setOnAction(e -> {
      if (index >= steps.size() - 1) finish(); else go(index + 1);
    });
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox buttons = new HBox(8, skip, spacer, backButton, nextButton);
    buttons.setAlignment(Pos.CENTER_LEFT);

    card.getStyleClass().add("tour-card");
    card.getChildren().addAll(counter, title, text, buttons);
    card.setPrefWidth(CARD_WIDTH);
    card.setMaxWidth(CARD_WIDTH);
    card.setManaged(false);

    overlay.getChildren().addAll(ring, card);
    overlay.widthProperty().addListener((o, a, b) -> layoutStep(false));
    overlay.heightProperty().addListener((o, a, b) -> layoutStep(false));
  }

  private void show() {
    scene.setRoot(wrapper);
    wrapper.getChildren().setAll(originalRoot, overlay);
    scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);

    overlay.setOpacity(0);
    FadeTransition fade = new FadeTransition(Duration.millis(200), overlay);
    fade.setToValue(1);
    fade.play();

    go(0);
    nextButton.requestFocus();
  }

  private void finish() {
    if (running != this) return;
    running = null;
    scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
    FadeTransition fade = new FadeTransition(Duration.millis(150), overlay);
    fade.setToValue(0);
    fade.setOnFinished(e -> {
      wrapper.getChildren().clear();
      if (scene.getRoot() == wrapper) scene.setRoot(originalRoot);
      if (onFinish != null) onFinish.run();
    });
    fade.play();
  }

  private void onKey(KeyEvent e) {
    switch (e.getCode()) {
      case ESCAPE -> finish();
      case RIGHT, PAGE_DOWN -> { if (index < steps.size() - 1) go(index + 1); else finish(); }
      case LEFT, PAGE_UP -> { if (index > 0) go(index - 1); }
      case ENTER, SPACE -> nextButton.fire();
      default -> { return; }
    }
    e.consume();
  }

  // ------------------------------------------------------------------ Schritte

  private void go(int i) {
    if (i < 0 || i >= steps.size()) return;
    index = i;
    Step s = steps.get(i);
    counter.setText("Schritt " + (i + 1) + " von " + steps.size());
    title.setText(s.title());
    text.setText(s.text());
    backButton.setDisable(i == 0);
    nextButton.setText(i == steps.size() - 1 ? "Fertig" : "Weiter");
    layoutStep(true);
    nextButton.requestFocus();
  }

  private void layoutStep(boolean animate) {
    if (index < 0) return;
    double w = overlay.getWidth(), h = overlay.getHeight();
    if (w <= 0 || h <= 0) {
      // Erste Anzeige: Größe steht erst nach dem Layout fest
      javafx.application.Platform.runLater(() -> layoutStep(animate));
      return;
    }

    Bounds target = targetBounds(steps.get(index), w, h);

    // Abdunkeln mit "Loch" an der hervorgehobenen Stelle
    Rectangle full = new Rectangle(0, 0, w, h);
    Shape newDim;
    if (target != null) {
      Rectangle hole = new Rectangle(target.getMinX(), target.getMinY(), target.getWidth(), target.getHeight());
      hole.setArcWidth(16);
      hole.setArcHeight(16);
      newDim = Shape.subtract(full, hole);
      ring.setX(target.getMinX());
      ring.setY(target.getMinY());
      ring.setWidth(target.getWidth());
      ring.setHeight(target.getHeight());
      ring.setVisible(true);
    } else {
      newDim = full;
      ring.setVisible(false);
    }
    newDim.getStyleClass().add("tour-dim");
    newDim.setFill(Color.rgb(15, 23, 42, 0.62));
    if (dim != null) overlay.getChildren().remove(dim);
    dim = newDim;
    overlay.getChildren().add(0, dim);

    // Sprechblase platzieren
    card.applyCss();
    card.layout();
    double cw = CARD_WIDTH;
    double ch = card.prefHeight(cw);
    card.resize(cw, ch);

    double x, y;
    if (target == null) {
      x = (w - cw) / 2;
      y = (h - ch) / 2;
    } else if (target.getMaxX() + GAP + cw <= w - MARGIN) {          // rechts daneben
      x = target.getMaxX() + GAP;
      y = target.getMinY();
    } else if (target.getMinX() - GAP - cw >= MARGIN) {              // links daneben
      x = target.getMinX() - GAP - cw;
      y = target.getMinY();
    } else if (target.getMaxY() + GAP + ch <= h - MARGIN) {          // darunter
      x = target.getMinX() + (target.getWidth() - cw) / 2;
      y = target.getMaxY() + GAP;
    } else if (target.getMinY() - GAP - ch >= MARGIN) {              // darüber
      x = target.getMinX() + (target.getWidth() - cw) / 2;
      y = target.getMinY() - GAP - ch;
    } else {                                                         // großes Element: mittig hinein
      x = target.getMinX() + (target.getWidth() - cw) / 2;
      y = target.getMinY() + (target.getHeight() - ch) / 2;
    }
    x = clamp(x, MARGIN, w - cw - MARGIN);
    y = clamp(y, MARGIN, h - ch - MARGIN);
    card.relocate(x, y);

    if (animate) {
      card.setOpacity(0);
      card.setTranslateY(6);
      FadeTransition fade = new FadeTransition(Duration.millis(180), card);
      fade.setToValue(1);
      TranslateTransition slide = new TranslateTransition(Duration.millis(180), card);
      slide.setToY(0);
      slide.setInterpolator(Interpolator.EASE_OUT);
      new ParallelTransition(fade, slide).play();
    }
  }

  /** Umschließendes Rechteck aller sichtbaren Ziele, in Overlay-Koordinaten. */
  private Bounds targetBounds(Step s, double w, double h) {
    double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
    boolean any = false;
    for (Node n : s.targets()) {
      if (n.getScene() != scene || !n.isVisible()) continue;
      Bounds b = overlay.sceneToLocal(n.localToScene(n.getBoundsInLocal()));
      if (b == null || b.getWidth() <= 0 || b.getHeight() <= 0) continue;
      minX = Math.min(minX, b.getMinX());
      minY = Math.min(minY, b.getMinY());
      maxX = Math.max(maxX, b.getMaxX());
      maxY = Math.max(maxY, b.getMaxY());
      any = true;
    }
    if (!any) return null;
    minX = Math.max(2, minX - PAD);
    minY = Math.max(2, minY - PAD);
    maxX = Math.min(w - 2, maxX + PAD);
    maxY = Math.min(h - 2, maxY + PAD);
    return new javafx.geometry.BoundingBox(minX, minY, maxX - minX, maxY - minY);
  }

  private static double clamp(double v, double lo, double hi) {
    return hi < lo ? lo : Math.max(lo, Math.min(hi, v));
  }
}
