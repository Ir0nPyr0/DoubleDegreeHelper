package calculator.doubledegreehelper;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prüft beim Start, ob auf GitHub Releases eine neuere Version liegt, und bietet an,
 * den Installer herunterzuladen und zu starten. Ohne Internet oder bei Fehlern passiert
 * einfach nichts (nur ein Eintrag im Hilfe-Log).
 */
public final class UpdateChecker {
  private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);

  /** GitHub-Repo, dessen Releases geprüft werden ("besitzer/repo"). Muss öffentlich sein. */
  public static final String RELEASE_REPO =
      System.getProperty("ddh.releaseRepo", "Ir0nPyr0/DoubleDegreeHelper");

  private static final String SKIP_KEY = "skipUpdateVersion";
  private static final Preferences PREFS = Preferences.userNodeForPackage(UpdateChecker.class);
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .followRedirects(HttpClient.Redirect.NORMAL)
      .connectTimeout(Duration.ofSeconds(8))
      .build();

  private UpdateChecker() {}

  /** Neueste Version mit Download-Link für den Installer. */
  record Release(String version, String installerUrl, String installerName, String pageUrl, String notes) {}

  /**
   * Startet die Prüfung im Hintergrund. Nur in der installierten App (jpackage setzt
   * jpackage.app-version), nicht bei gradlew run. Zum Testen: -Dddh.forceUpdateCheck=true
   */
  public static void checkInBackground(Stage owner) {
    String current = AppVersion.get();
    boolean installed = System.getProperty("jpackage.app-version") != null;
    if (!installed && !Boolean.getBoolean("ddh.forceUpdateCheck")) return;
    if ("dev".equals(current)) return;
    Thread t = new Thread(() -> {
      try {
        Optional<Release> latest = fetchLatest();
        if (latest.isEmpty()) return;
        Release r = latest.get();
        if (compareVersions(r.version(), current) <= 0) return;
        if (r.version().equals(PREFS.get(SKIP_KEY, ""))) return;
        Platform.runLater(() -> offerUpdate(owner, current, r));
      } catch (Exception e) {
        logger.info("Update-Prüfung fehlgeschlagen: {}", e.toString());
        HelpLog.add("Update-Prüfung nicht möglich: " + e.getMessage());
      }
    }, "update-check");
    t.setDaemon(true);
    t.start();
  }

  static Optional<Release> fetchLatest() throws IOException, InterruptedException {
    HttpRequest req = HttpRequest.newBuilder(
            URI.create("https://api.github.com/repos/" + RELEASE_REPO + "/releases/latest"))
        .header("Accept", "application/vnd.github+json")
        .header("User-Agent", "DoubleDegreeHelper-Updater")
        .timeout(Duration.ofSeconds(15))
        .GET().build();
    HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() == 404) {
      HelpLog.add("Update-Prüfung: keine öffentlichen Releases unter " + RELEASE_REPO + " gefunden.");
      return Optional.empty();
    }
    if (resp.statusCode() != 200) {
      throw new IOException("GitHub antwortet mit HTTP " + resp.statusCode());
    }
    return parseRelease(resp.body());
  }

  /** Liest die nötigen Felder aus der GitHub-Antwort (ohne extra JSON-Bibliothek). */
  static Optional<Release> parseRelease(String json) {
    String tag = stringField(json, "tag_name");
    if (tag == null) return Optional.empty();
    String version = tag.replaceFirst("^[^0-9]*", "");
    String page = stringField(json, "html_url");
    String notes = Optional.ofNullable(stringField(json, "body")).orElse("");

    String installerUrl = null;
    String installerName = null;
    Matcher m = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
    while (m.find()) {
      String url = m.group(1);
      String name = url.substring(url.lastIndexOf('/') + 1);
      String lower = name.toLowerCase();
      if (lower.endsWith(".exe") || lower.endsWith(".msi")) {
        installerUrl = url;
        installerName = name;
        if (lower.endsWith(".exe")) break;
      }
    }
    // Releases ohne Installer (z.B. alte ZIP-Releases oder gerade erst angelegt) ignorieren.
    if (installerUrl == null || version.isEmpty()) return Optional.empty();
    return Optional.of(new Release(version, installerUrl, installerName, page, notes));
  }

  private static String stringField(String json, String name) {
    Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
    if (!m.find()) return null;
    return unescape(m.group(1));
  }

  private static String unescape(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c != '\\' || i + 1 >= s.length()) { sb.append(c); continue; }
      char n = s.charAt(++i);
      switch (n) {
        case 'n' -> sb.append('\n');
        case 'r' -> { }
        case 't' -> sb.append('\t');
        case 'u' -> {
          if (i + 4 < s.length()) {
            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
            i += 4;
          }
        }
        default -> sb.append(n);
      }
    }
    return sb.toString();
  }

  /** Vergleicht "1.0.10" mit "1.0.9" numerisch. Positiv, wenn a neuer ist. */
  static int compareVersions(String a, String b) {
    List<Integer> pa = parts(a);
    List<Integer> pb = parts(b);
    for (int i = 0; i < Math.max(pa.size(), pb.size()); i++) {
      int x = i < pa.size() ? pa.get(i) : 0;
      int y = i < pb.size() ? pb.get(i) : 0;
      if (x != y) return Integer.compare(x, y);
    }
    return 0;
  }

  private static List<Integer> parts(String v) {
    List<Integer> out = new ArrayList<>();
    Matcher m = Pattern.compile("\\d+").matcher(v);
    while (m.find()) {
      try { out.add(Integer.parseInt(m.group())); } catch (NumberFormatException e) { out.add(0); }
    }
    return out;
  }

  private static void offerUpdate(Stage owner, String current, Release r) {
    ButtonType update = new ButtonType("Jetzt aktualisieren", ButtonBar.ButtonData.OK_DONE);
    ButtonType later = new ButtonType("Später", ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType skip = new ButtonType("Diese Version überspringen", ButtonBar.ButtonData.OTHER);

    Alert alert = new Alert(Alert.AlertType.INFORMATION, "", update, later, skip);
    alert.initOwner(owner);
    alert.setTitle("Update verfügbar");
    alert.setHeaderText("Version " + r.version() + " ist verfügbar (installiert: " + current + ").");
    String notes = r.notes().strip();
    if (notes.length() > 800) notes = notes.substring(0, 800) + " …";
    alert.setContentText((notes.isEmpty() ? "" : "Neu:\n" + notes + "\n\n")
        + "Beim Aktualisieren wird der Installer heruntergeladen und gestartet. "
        + "Die App schließt sich dabei, deine Daten bleiben erhalten.");
    alert.getDialogPane().setMinWidth(480);

    Optional<ButtonType> choice = alert.showAndWait();
    if (choice.isEmpty() || choice.get() == later) return;
    if (choice.get() == skip) {
      PREFS.put(SKIP_KEY, r.version());
      return;
    }
    downloadAndInstall(owner, r);
  }

  private static void downloadAndInstall(Stage owner, Release r) {
    ProgressBar bar = new ProgressBar();
    bar.setPrefWidth(360);
    Label label = new Label("Lade Version " + r.version() + " herunter …");
    VBox box = new VBox(10, label, bar);
    box.setPadding(new Insets(16));

    Stage progress = new Stage();
    progress.initOwner(owner);
    progress.initModality(Modality.WINDOW_MODAL);
    progress.setTitle("Update");
    progress.setResizable(false);
    progress.setScene(new javafx.scene.Scene(box));

    Task<Path> task = new Task<>() {
      @Override
      protected Path call() throws Exception {
        Path dir = Files.createTempDirectory("ddh-update-");
        Path target = dir.resolve(r.installerName());
        HttpRequest req = HttpRequest.newBuilder(URI.create(r.installerUrl()))
            .header("User-Agent", "DoubleDegreeHelper-Updater")
            .header("Accept", "application/octet-stream")
            .GET().build();
        HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) throw new IOException("Download fehlgeschlagen (HTTP " + resp.statusCode() + ")");
        long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
        Path part = dir.resolve(r.installerName() + ".part");
        try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(part)) {
          byte[] buf = new byte[64 * 1024];
          long done = 0;
          int n;
          while ((n = in.read(buf)) > 0) {
            if (isCancelled()) throw new IOException("abgebrochen");
            out.write(buf, 0, n);
            done += n;
            if (total > 0) updateProgress(done, total);
          }
        }
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
      }
    };
    bar.progressProperty().bind(task.progressProperty());

    task.setOnSucceeded(e -> {
      progress.close();
      Path installer = task.getValue();
      try {
        launchInstaller(installer);
        HelpLog.add("Update auf " + r.version() + " gestartet: " + installer);
        // App beenden, damit der Installer die Dateien ersetzen kann (stop() speichert vorher).
        Platform.exit();
      } catch (IOException ex) {
        showError(owner, r, "Der Installer konnte nicht gestartet werden: " + ex.getMessage()
            + "\n\nDatei: " + installer);
      }
    });
    task.setOnFailed(e -> {
      progress.close();
      Throwable ex = task.getException();
      logger.warn("Update-Download fehlgeschlagen", ex);
      showError(owner, r, "Der Download ist fehlgeschlagen: " + (ex != null ? ex.getMessage() : "unbekannt"));
    });

    progress.show();
    Thread t = new Thread(task, "update-download");
    t.setDaemon(true);
    t.start();
  }

  private static void launchInstaller(Path installer) throws IOException {
    // Desktop.open nutzt unter Windows ShellExecute, dadurch kann der Installer nach Admin-Rechten fragen.
    if (!java.awt.Desktop.isDesktopSupported()
        || !java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
      throw new IOException("Dateien können auf diesem System nicht geöffnet werden");
    }
    java.awt.Desktop.getDesktop().open(installer.toFile());
  }

  private static void showError(Stage owner, Release r, String message) {
    HelpLog.add("Update fehlgeschlagen: " + message);
    Alert a = new Alert(Alert.AlertType.ERROR);
    a.initOwner(owner);
    a.setTitle("Update fehlgeschlagen");
    a.setHeaderText("Update auf Version " + r.version() + " hat nicht geklappt.");
    a.setContentText(message + (r.pageUrl() != null
        ? "\n\nDu kannst den Installer auch selbst herunterladen:\n" + r.pageUrl() : ""));
    a.showAndWait();
  }
}
