package calculator.doubledegreehelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

/** Hilfe-Fenster: Tour starten, letzte Fehlermeldungen, häufige Probleme mit Lösung. */
public final class HelpDialog {

  /** Problem, wie es in der App erscheint, und was zu tun ist. */
  private record Problem(String message, String cause, String... steps) {}

  private static final List<Problem> PROBLEMS = List.of(
      new Problem("„… Dateien übersprungen“ nach dem Einlesen",
          "Einige Dateien konnten nicht gelesen werden. Die anderen wurden trotzdem übernommen.",
          "Maus über den Statustext unter „Studenten aktualisieren“ halten: dort steht, welche Datei warum übersprungen wurde.",
          "Ist die Datei gerade in Excel oder einem PDF-Programm geöffnet? Schließen und erneut hineinziehen.",
          "Die Einzelmeldungen findest du auch oben in diesem Fenster unter „Letzte Fehlermeldungen“."),
      new Problem("„keine Student ID gefunden“ (PDF)",
          "In der PDF wurde keine Matrikelnummer erkannt.",
          "Prüfen, ob es wirklich das Transkript der VGU ist (nicht Urkunde o. Ä.).",
          "Eingescannte PDFs (nur Bild, kein markierbarer Text) können nicht gelesen werden. Bitte die digitale Original-PDF verwenden."),
      new Problem("„keine VGU ID gefunden“ / „Spalte 'VGU ID' nicht gefunden“ (XLSX)",
          "Die Excel-Datei hat keine Spalte mit der Überschrift „VGU ID“ oder sie ist leer.",
          "Excel-Datei öffnen und prüfen, ob es eine Spalte „VGU ID“ gibt (Schreibweise genau so).",
          "Die Datei speichern, schließen und erneut in das Feld ziehen."),
      new Problem("Student erscheint nicht in der Liste",
          "Die Datei wurde noch nicht eingelesen oder die Suche filtert die Liste.",
          "Suchfeld über der Liste leeren.",
          "PDF erneut in das große Feld ziehen oder „Studenten aktualisieren“ klicken.",
          "Bei festgelegten Quellordnern: liegt die Datei wirklich in diesem Ordner (oder einem Unterordner)?"),
      new Problem("Badge „XLSX fehlt“ / Hinweis „XLSX fehlt“ beim Öffnen",
          "Zu diesem Studenten wurde noch keine Excel-Datei mit seiner VGU ID gefunden.",
          "Die passende XLSX (Themenliste) in das Feld ziehen. Die Zuordnung passiert automatisch über die VGU ID.",
          "Alternativ beim Öffnen Thema, Geschlecht und Abgabedatum manuell eintragen."),
      new Problem("„Das Transkript wurde nicht gefunden“",
          "Die PDF wurde verschoben, umbenannt oder gelöscht, seit sie eingelesen wurde.",
          "Die PDF vom neuen Ort erneut in das Feld ziehen. Die Verknüpfung wird dann aktualisiert.",
          "Wenn ein Quellordner festgelegt ist: „Studenten aktualisieren“ klicken."),
      new Problem("„Ungültiger Speicherordner“",
          "Der Export-Ordner existiert nicht mehr (z. B. USB-Stick oder Netzlaufwerk nicht verbunden).",
          "Im Hauptmenü rechts unter „Export“ auf „Speicherort festlegen“ klicken und einen vorhandenen Ordner wählen."),
      new Problem("„PDF konnte nicht erstellt werden“",
          "Für PDFs wird Microsoft Word (oder LibreOffice) im Hintergrund benutzt.",
          "Prüfen, ob Word installiert ist und sich normal öffnen lässt (kein Lizenz- oder Anmeldedialog offen).",
          "Offene Word-Fenster mit Dialogen schließen und erneut exportieren.",
          "Notlösung: „Word“ als Format wählen und die Datei selbst in Word als PDF speichern."),
      new Problem("„Fehler beim Erstellen der Word-Datei“",
          "Die Vorlage oder die Zieldatei ist nicht in Ordnung.",
          "Ist eine Datei mit gleichem Namen im Zielordner gerade in Word geöffnet? Schließen und erneut exportieren.",
          "Wurde eine Vorlage bearbeitet („Zeugnis/Diploma/Zertifikat bearbeiten“)? Platzhalter in geschweiften Klammern (z. B. {BDATE}) oder die Kategoriezeilen dürfen nicht gelöscht werden.",
          "Kaputte Vorlage zurücksetzen: Datei im Datenordner (Button unten) löschen, beim nächsten Klick auf „… bearbeiten“ wird die Originalvorlage neu angelegt."),
      new Problem("Module stehen unter „Nicht zugeordnet“",
          "Für diese VGU-Module ist noch kein HHN-Modul hinterlegt.",
          "Unter „Einstellungen“ das VGU-Modul dem passenden HHN-Modul zuordnen und speichern.",
          "Danach den Studenten erneut öffnen."),
      new Problem("„Moduldaten konnten nicht geladen werden“ beim Start",
          "Die Datei modules.dat ist beschädigt. Speichern ist gesperrt, damit nichts überschrieben wird.",
          "App schließen. Im Datenordner unter „backups“ die neueste Sicherung als modules.dat zurückkopieren.",
          "Details stehen in modules-load-error.log im Datenordner."),
      new Problem("„Datei konnte nicht geöffnet werden“ (Vorlagen, Ordner)",
          "Windows kennt kein Programm für die Datei oder der Ordner ist nicht erreichbar.",
          "Prüfen, ob Word installiert ist und .docx-Dateien damit verknüpft sind.",
          "Den Datenordner über den Button unten öffnen und die Datei von Hand öffnen.")
  );

  private HelpDialog() {}

  /**
   * Zeigt das Hilfe-Fenster.
   * @param homeTour     startet die Tour durch das Hauptmenü (Fenster ist dann schon zu)
   * @param settingsTour startet die Tour durch die Einstellungen
   */
  public static void show(Window owner, Runnable homeTour, Runnable settingsTour) {
    Stage stage = new Stage();
    if (owner != null) stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle("Hilfe");

    VBox content = new VBox(18);
    content.setPadding(new Insets(20, 22, 20, 22));
    content.getStyleClass().add("help-content");

    // Einführung
    Label introTitle = new Label("Einführung");
    introTitle.getStyleClass().add("help-section-title");
    Label introText = new Label("Die Touren heben die wichtigsten Bedienelemente nacheinander hervor "
        + "und erklären, was sie tun. Je etwa eine Minute. Navigieren mit den Pfeiltasten, Esc beendet.");
    introText.setWrapText(true);
    introText.getStyleClass().add("help-muted");
    Button tourButton = new Button("Tour: Hauptmenü");
    tourButton.getStyleClass().add("primary");
    tourButton.setDefaultButton(true);
    tourButton.setOnAction(e -> {
      stage.close();
      if (homeTour != null) javafx.application.Platform.runLater(homeTour);
    });
    Button settingsTourButton = new Button("Tour: Einstellungen");
    settingsTourButton.setOnAction(e -> {
      stage.close();
      if (settingsTour != null) javafx.application.Platform.runLater(settingsTour);
    });
    HBox tourButtons = new HBox(8, tourButton, settingsTourButton);
    VBox intro = new VBox(8, introTitle, introText, tourButtons);
    intro.getStyleClass().add("help-card");

    content.getChildren().addAll(intro, recentErrorsSection(), problemsSection());

    ScrollPane scroll = new ScrollPane(content);
    scroll.setFitToWidth(true);
    scroll.getStyleClass().add("help-scroll");

    // Fußleiste
    Button dataButton = new Button("Datenordner öffnen");
    dataButton.setOnAction(e -> openDataFolder());
    Button closeButton = new Button("Schließen");
    closeButton.setCancelButton(true);
    closeButton.setOnAction(e -> stage.close());
    Label version = new Label("Version " + AppVersion.get());
    version.getStyleClass().add("help-muted");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox footer = new HBox(8, version, spacer, dataButton, closeButton);
    footer.setAlignment(Pos.CENTER_LEFT);
    footer.setPadding(new Insets(10, 16, 12, 16));
    footer.getStyleClass().add("help-footer");

    BorderPane root = new BorderPane(scroll);
    root.setBottom(footer);
    root.getStyleClass().add("help-root");

    Scene scene = new Scene(root, 660, 640);
    var css = HelpDialog.class.getResource("/calculator/doubledegreehelper/help.css");
    if (css != null) scene.getStylesheets().add(css.toExternalForm());
    scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.F1) stage.close(); });
    stage.setScene(scene);
    stage.show();
    tourButton.requestFocus();
  }

  private static VBox recentErrorsSection() {
    Label head = new Label("Letzte Fehlermeldungen");
    head.getStyleClass().add("help-section-title");
    VBox box = new VBox(8, head);

    List<String> recent = HelpLog.recent();
    if (recent.isEmpty()) {
      Label none = new Label("In dieser Sitzung ist noch kein Fehler aufgetreten.");
      none.getStyleClass().add("help-muted");
      box.getChildren().add(none);
      return box;
    }
    TextArea area = new TextArea(String.join("\n", recent));
    area.setEditable(false);
    area.setWrapText(true);
    area.setPrefRowCount(Math.min(6, recent.size() + 1));
    area.getStyleClass().add("help-log");
    Button copy = new Button("Meldungen kopieren");
    copy.setOnAction(e -> {
      ClipboardContent c = new ClipboardContent();
      c.putString("DoubleDegreeHelper " + AppVersion.get() + "\n" + area.getText());
      Clipboard.getSystemClipboard().setContent(c);
      copy.setText("Kopiert ✓");
    });
    Label hint = new Label("Neueste zuerst. Zum Nachfragen einfach kopieren und mitschicken.");
    hint.getStyleClass().add("help-muted");
    hint.setWrapText(true);
    HBox actions = new HBox(10, copy, hint);
    actions.setAlignment(Pos.CENTER_LEFT);
    box.getChildren().addAll(area, actions);
    return box;
  }

  private static VBox problemsSection() {
    Label head = new Label("Häufige Probleme und was zu tun ist");
    head.getStyleClass().add("help-section-title");

    Accordion acc = new Accordion();
    for (Problem p : PROBLEMS) {
      VBox body = new VBox(6);
      Label cause = new Label(p.cause());
      cause.setWrapText(true);
      cause.getStyleClass().add("help-cause");
      body.getChildren().add(cause);
      int i = 1;
      for (String s : p.steps()) {
        Label num = new Label(i++ + ".");
        num.getStyleClass().add("help-step-num");
        num.setMinWidth(18);
        Label txt = new Label(s);
        txt.setWrapText(true);
        HBox.setHgrow(txt, Priority.ALWAYS);
        HBox row = new HBox(4, num, txt);
        body.getChildren().add(row);
      }
      TitledPane tp = new TitledPane(p.message(), body);
      tp.getStyleClass().add("help-problem");
      acc.getPanes().add(tp);
    }
    return new VBox(8, head, acc);
  }

  private static void openDataFolder() {
    File dir = new File(AppManager.getDefaultWordDir());
    new Thread(() -> {
      try {
        if (!dir.isDirectory()) dir.mkdirs();
        Desktop.getDesktop().open(dir);
      } catch (Exception ex) {
        HelpLog.add("Datenordner konnte nicht geöffnet werden: " + dir);
      }
    }, "open-data-folder").start();
  }
}
