package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.AppManager;
import calculator.doubledegreehelper.AppVersion;
import calculator.doubledegreehelper.ExportSettings;
import calculator.doubledegreehelper.HelpDialog;
import calculator.doubledegreehelper.HelpLog;
import calculator.doubledegreehelper.OnboardingTour;
import calculator.doubledegreehelper.SceneManager;
import calculator.doubledegreehelper.StudentRegistry;
import calculator.doubledegreehelper.ThemeReader;
import calculator.doubledegreehelper.TranscriptPdfReader;
import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentRecord;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

public class homeController {
    private static final Logger logger = LoggerFactory.getLogger(homeController.class);

    @FXML private BorderPane root;
    @FXML private Button settingsButton;
    @FXML private Button transcriptEditButton, diplomaEditButton, certificateEditButton;
    @FXML private Button pdfFolderButton, xlsxFolderButton, refreshButton;
    @FXML private Label pdfFolderLabel, xlsxFolderLabel, statusLabel, countLabel, versionLabel, dropTitle;
    @FXML private ProgressIndicator scanIndicator;
    @FXML private TextField searchField;
    @FXML private ListView<StudentRecord> studentList;
    @FXML private StackPane dropZone;
    @FXML private Button saveFolderButton;
    @FXML private Label saveFolderLabel;
    @FXML private RadioButton wordRadio, pdfRadio, bothRadio;
    @FXML private CheckBox openFolderCheck;
    @FXML private Button helpButton;

    private static final String PREF_TOUR_DONE = "onboardingTourDone";
    private static boolean tourOfferedThisSession;

    private final Label placeholder = new Label();
    private FilteredList<StudentRecord> filtered;
    private ListChangeListener<StudentRecord> countListener;

    @FXML
    public void initialize() {
        settingsButton.setOnAction(this::handleSettingsButtonAction);
        transcriptEditButton.setOnAction(this::handleTranscriptButtonAction);
        diplomaEditButton   .setOnAction(this::handleDiplomaButtonAction);
        certificateEditButton.setOnAction(this::handleCertificateButtonAction);

        versionLabel.setText("Version " + AppVersion.get());

        setupStudentList();
        setupFolders();
        setupDragAndDrop();
        setupExport();
        setupHelp();

        if (StudentRegistry.claimStartupScan() && hasAnyFolder()) {
            runFolderScan(true);
        }
    }

    // ------------------------------------------------------------------ Liste

    private void setupStudentList() {
        filtered = new FilteredList<>(StudentRegistry.getRecords(), r -> true);
        SortedList<StudentRecord> sorted = new SortedList<>(filtered,
            Comparator.comparing((StudentRecord r) -> !r.isNew())
                .thenComparingInt(StudentRecord::getNumber));
        studentList.setItems(sorted);
        studentList.setCellFactory(lv -> new StudentCell());

        placeholder.getStyleClass().add("muted");
        placeholder.setWrapText(true);
        placeholder.setMaxWidth(280);
        placeholder.setAlignment(Pos.CENTER);
        studentList.setPlaceholder(placeholder);

        searchField.textProperty().addListener((obs, old, text) -> {
            String q = normalize(text);
            filtered.setPredicate(r -> q.isEmpty()
                || String.valueOf(r.getNumber()).contains(q)
                || normalize(r.getName()).contains(q));
            updatePlaceholder();
        });

        studentList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                openStudent(studentList.getSelectionModel().getSelectedItem());
            } else if (e.getCode() == KeyCode.DELETE) {
                confirmDelete(studentList.getSelectionModel().getSelectedItem());
            }
        });

        countListener = c -> updateCount();
        StudentRegistry.getRecords().addListener(countListener);
        // Listener entfernen, sobald die Szene gewechselt wird (Controller wird neu erzeugt)
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) StudentRegistry.getRecords().removeListener(countListener);
        });
        updateCount();
        updatePlaceholder();
    }

    private void updateCount() {
        List<StudentRecord> all = StudentRegistry.getRecords();
        long fresh = all.stream().filter(StudentRecord::isNew).count();
        countLabel.setText(all.size() + (fresh > 0 ? " · " + fresh + " neu" : ""));
    }

    private void updatePlaceholder() {
        String q = searchField.getText();
        if (q != null && !q.isBlank()) {
            placeholder.setText("Keine Treffer für „" + q.trim() + "“.");
        } else {
            placeholder.setText("Noch keine Studenten.\nZieh PDF- und XLSX-Dateien in das Feld "
                + "oder lege rechts die Quellordner fest.");
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }

    private final class StudentCell extends ListCell<StudentRecord> {
        private final Label number = new Label();
        private final Label name = new Label();
        private final Label newBadge = new Label("NEU");
        private final Label missingBadge = new Label();
        private final Button deleteButton = new Button("✕");
        private final HBox box;

        StudentCell() {
            number.getStyleClass().add("student-number");
            name.getStyleClass().add("student-name");
            newBadge.getStyleClass().addAll("badge", "badge-new");
            missingBadge.getStyleClass().addAll("badge", "badge-muted");
            name.setMinWidth(0);
            HBox.setHgrow(name, Priority.ALWAYS);
            name.setMaxWidth(Double.MAX_VALUE);
            deleteButton.getStyleClass().add("delete-button");
            deleteButton.setFocusTraversable(false);
            deleteButton.setTooltip(new Tooltip("Student aus der Liste löschen"));
            deleteButton.setOnAction(e -> confirmDelete(getItem()));
            // Klick auf den Button soll den Studenten nicht öffnen
            deleteButton.setOnMouseClicked(javafx.event.Event::consume);

            MenuItem deleteItem = new MenuItem("Löschen…");
            deleteItem.setOnAction(e -> confirmDelete(getItem()));
            MenuItem openItem = new MenuItem("Öffnen");
            openItem.setOnAction(e -> openStudent(getItem()));
            ContextMenu menu = new ContextMenu(openItem, deleteItem);
            emptyProperty().addListener((obs, was, isEmpty) -> setContextMenu(isEmpty ? null : menu));

            box = new HBox(6, number, new Label("-"), name, missingBadge, newBadge, deleteButton);
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().get(1).getStyleClass().add("student-name");

            setOnMouseClicked(e -> {
                if (!isEmpty() && getItem() != null && e.getButton() == MouseButton.PRIMARY) {
                    openStudent(getItem());
                }
            });
        }

        @Override
        protected void updateItem(StudentRecord item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setTooltip(null);
                return;
            }
            number.setText(String.valueOf(item.getNumber()));
            name.setText(item.getName().isBlank() ? "(ohne Namen)" : item.getName());

            newBadge.setVisible(item.isNew());
            newBadge.setManaged(item.isNew());

            String missing = !item.hasXlsx() ? "XLSX fehlt" : null;
            missingBadge.setText(missing);
            missingBadge.setVisible(missing != null);
            missingBadge.setManaged(missing != null);

            setTooltip(new Tooltip("PDF:  " + (item.hasPdf() ? item.getPdfPath() : "–")
                + "\nXLSX: " + (item.hasXlsx() ? item.getXlsxPath() : "–")));
            setText(null);
            setGraphic(box);

            if (StudentRegistry.consumeHighlight(item)) {
                animateIn(box);
            } else {
                box.setOpacity(1);
                box.setTranslateX(0);
            }
        }
    }

    private static void animateIn(HBox node) {
        node.setOpacity(0);
        node.setTranslateX(-24);
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(320), node);
        slide.setFromX(-24);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition anim = new ParallelTransition(fade, slide);
        anim.play();
    }

    /** Löschen nur nach Bestätigung; die PDF/XLSX-Dateien selbst bleiben erhalten. */
    private void confirmDelete(StudentRecord rec) {
        if (rec == null) return;
        ButtonType delete = new ButtonType("Löschen", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Der Student wird aus der Liste entfernt. Die PDF- und XLSX-Dateien bleiben erhalten.\n"
                + "Mit „Studenten aktualisieren“ oder erneutem Hineinziehen der PDF erscheint er wieder.",
            cancel, delete);
        alert.initOwner(root.getScene().getWindow());
        alert.setTitle("Student löschen");
        alert.setHeaderText(rec + " löschen?");
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        ((Button) alert.getDialogPane().lookupButton(delete)).setDefaultButton(false);
        if (alert.showAndWait().orElse(cancel) == delete) {
            StudentRegistry.remove(rec);
            statusLabel.setText(rec + " gelöscht.");
            statusLabel.setTooltip(null);
        }
    }

    /** Klick auf einen Studenten: Daten aus PDF + XLSX laden und direkt zur Auswahl springen. */
    private void openStudent(StudentRecord rec) {
        if (rec == null) return;
        if (!rec.hasPdf() || !Files.isRegularFile(Path.of(rec.getPdfPath()))) {
            HelpLog.add("Transkript nicht gefunden: " + rec.getPdfPath());
            new Alert(Alert.AlertType.ERROR,
                "Das Transkript wurde nicht gefunden:\n" + rec.getPdfPath()
                    + "\n\nZieh die PDF bitte erneut in das Feld, dann wird die Verknüpfung aktualisiert.")
                .showAndWait();
            return;
        }
        try {
            Student s = TranscriptPdfReader.read(Path.of(rec.getPdfPath()));
            AppManager.setStudent(s);
            AppManager.setFilePath(rec.getPdfPath());
            AppManager.setGradeFilePath(rec.getPdfPath());
            AppManager.setThemeFilePath(null);

            boolean xlsxOk = rec.hasXlsx() && Files.isRegularFile(Path.of(rec.getXlsxPath()));
            if (xlsxOk) {
                ThemeReader.fillThemeAndGenderFromExcel(Path.of(rec.getXlsxPath()), s);
                AppManager.setThemeFilePath(rec.getXlsxPath());
            } else {
                Alert info = new Alert(Alert.AlertType.WARNING,
                    "Für " + rec + " ist keine XLSX verknüpft.\n"
                        + "Thema, Geschlecht und Abgabedatum bitte im nächsten Schritt manuell eintragen.");
                info.setHeaderText("XLSX fehlt");
                info.showAndWait();
            }
            SceneManager.switchTo(SceneManager.View.SELECTION);
        } catch (Exception ex) {
            logger.error("Student {} konnte nicht geöffnet werden", rec.getNumber(), ex);
            HelpLog.add("Fehler beim Öffnen von " + rec + ": " + ex.getMessage());
            new Alert(Alert.AlertType.ERROR, "Fehler beim Öffnen von " + rec + ":\n" + ex.getMessage())
                .showAndWait();
        }
    }

    // ------------------------------------------------------------------ Ordner

    private void setupFolders() {
        updateFolderLabels();
        pdfFolderButton.setOnAction(e -> chooseFolder(true));
        xlsxFolderButton.setOnAction(e -> chooseFolder(false));
        refreshButton.setOnAction(e -> runFolderScan(false));

        refreshButton.disableProperty().bind(StudentRegistry.busyProperty());
        scanIndicator.visibleProperty().bind(StudentRegistry.busyProperty());
        scanIndicator.managedProperty().bind(StudentRegistry.busyProperty());
        if (StudentRegistry.busyProperty().get()) statusLabel.setText("Suche nach neuen Dateien…");
    }

    private void updateFolderLabels() {
        setPathLabel(pdfFolderLabel, StudentRegistry.getPdfFolder());
        setPathLabel(xlsxFolderLabel, StudentRegistry.getXlsxFolder());
    }

    private static void setPathLabel(Label label, String path) {
        boolean set = path != null && !path.isBlank();
        label.setText(set ? path : "Nicht festgelegt");
        label.setTooltip(set ? new Tooltip(path) : null);
    }

    private boolean hasAnyFolder() {
        return !StudentRegistry.getPdfFolder().isBlank() || !StudentRegistry.getXlsxFolder().isBlank();
    }

    private void chooseFolder(boolean pdf) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(pdf ? "Ordner mit Transkript-PDFs wählen" : "Ordner mit Theme-XLSX wählen");
        String current = pdf ? StudentRegistry.getPdfFolder() : StudentRegistry.getXlsxFolder();
        if (current.isBlank()) {
            // Fallback: zuletzt im Einzel-Ablauf benutzter Ordner
            Class<?> c = pdf ? gradeFileController.class : themeFileController.class;
            current = Preferences.userNodeForPackage(c).get(pdf ? "lastPdfDir" : "lastXlsxDir", "");
        }
        File init = new File(current);
        if (init.isDirectory()) dc.setInitialDirectory(init);

        File dir = dc.showDialog(root.getScene().getWindow());
        if (dir == null) return;
        if (pdf) StudentRegistry.setPdfFolder(dir.getAbsolutePath());
        else     StudentRegistry.setXlsxFolder(dir.getAbsolutePath());
        updateFolderLabels();
        runFolderScan(true);
    }

    private void runFolderScan(boolean onlyChanged) {
        if (!hasAnyFolder()) {
            statusLabel.setText("Bitte zuerst einen PDF- oder XLSX-Ordner festlegen.");
            HelpLog.add("Aktualisieren ohne Quellordner: bitte zuerst einen PDF- oder XLSX-Ordner festlegen.");
            return;
        }
        statusLabel.setText("Suche nach neuen Dateien…");
        statusLabel.setTooltip(null);
        StudentRegistry.scanFoldersAsync(onlyChanged, this::showResult);
    }

    private void showResult(StudentRegistry.Result r) {
        StringBuilder sb = new StringBuilder();
        if (r.added == 0 && r.updated == 0) {
            sb.append("Keine neuen Studenten.");
        } else {
            if (r.added > 0) sb.append(r.added).append(r.added == 1 ? " neuer Student" : " neue Studenten");
            if (r.updated > 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(r.updated).append(r.updated == 1 ? " Verknüpfung aktualisiert" : " Verknüpfungen aktualisiert");
            }
            sb.append('.');
        }
        if (!r.errors.isEmpty()) {
            sb.append(' ').append(r.errors.size())
              .append(r.errors.size() == 1 ? " Datei übersprungen." : " Dateien übersprungen.");
            statusLabel.setTooltip(new Tooltip(String.join("\n", r.errors)));
            HelpLog.addAll("Übersprungen: ", r.errors);
        } else {
            statusLabel.setTooltip(null);
        }
        statusLabel.setText(sb.toString());
    }

    // ------------------------------------------------------------------ Export

    private void setupExport() {
        setPathLabel(saveFolderLabel, ExportSettings.getSavePath());
        saveFolderButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Speicherort für die erstellten Dateien wählen");
            File init = new File(ExportSettings.getSavePath());
            if (init.isDirectory()) dc.setInitialDirectory(init);
            File dir = dc.showDialog(root.getScene().getWindow());
            if (dir != null) {
                ExportSettings.setSavePath(dir.getAbsolutePath());
                setPathLabel(saveFolderLabel, dir.getAbsolutePath());
            }
        });

        ToggleGroup group = new ToggleGroup();
        wordRadio.setToggleGroup(group);
        pdfRadio.setToggleGroup(group);
        bothRadio.setToggleGroup(group);
        wordRadio.setUserData(ExportSettings.Format.WORD);
        pdfRadio.setUserData(ExportSettings.Format.PDF);
        bothRadio.setUserData(ExportSettings.Format.BOTH);
        switch (ExportSettings.getFormat()) {
            case PDF -> pdfRadio.setSelected(true);
            case BOTH -> bothRadio.setSelected(true);
            default -> wordRadio.setSelected(true);
        }
        Tooltip pdfHint = new Tooltip("PDF wird über Microsoft Word erstellt (alternativ LibreOffice).");
        pdfRadio.setTooltip(pdfHint);
        bothRadio.setTooltip(pdfHint);
        group.selectedToggleProperty().addListener((obs, old, sel) -> {
            if (sel != null) ExportSettings.setFormat((ExportSettings.Format) sel.getUserData());
        });

        openFolderCheck.setSelected(ExportSettings.isOpenFolderAfterExport());
        openFolderCheck.selectedProperty().addListener((obs, old, on) -> ExportSettings.setOpenFolderAfterExport(on));
    }

    // ------------------------------------------------------------------ Drag & Drop

    private void setupDragAndDrop() {
        root.setOnDragOver(this::handleDragOver);
        root.setOnDragExited(e -> setDropActive(false));
        root.setOnDragDropped(this::handleDragDropped);
    }

    private void handleDragOver(DragEvent e) {
        Dragboard db = e.getDragboard();
        if (db.hasFiles() && db.getFiles().stream().anyMatch(StudentRegistry::isSupported)) {
            e.acceptTransferModes(TransferMode.COPY);
            setDropActive(true);
        }
        e.consume();
    }

    private void handleDragDropped(DragEvent e) {
        Dragboard db = e.getDragboard();
        boolean ok = false;
        if (db.hasFiles()) {
            List<File> files = db.getFiles().stream().filter(StudentRegistry::isSupported).toList();
            if (!files.isEmpty()) {
                statusLabel.setText(files.size() == 1 ? "Lese 1 Datei ein…" : "Lese " + files.size() + " Dateien ein…");
                statusLabel.setTooltip(null);
                StudentRegistry.ingestAsync(files, this::showResult);
                ok = true;
            }
        }
        setDropActive(false);
        e.setDropCompleted(ok);
        e.consume();
    }

    private void setDropActive(boolean active) {
        dropZone.getStyleClass().remove("drop-active");
        if (active) dropZone.getStyleClass().add("drop-active");
        dropTitle.setText(active ? "Loslassen zum Einlesen" : "PDF- und XLSX-Dateien hierher ziehen");
    }

    // ------------------------------------------------------------------ Hilfe & Einführung

    private void setupHelp() {
        Label icon = new Label("?");
        icon.getStyleClass().add("help-icon");
        helpButton.setGraphic(icon);
        helpButton.setGraphicTextGap(6);
        helpButton.setTooltip(new Tooltip("Hilfe, Fehlermeldungen und Einführung (F1)"));
        helpButton.setOnAction(e -> openHelp());
        root.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F1 && !OnboardingTour.isRunning()) {
                e.consume();
                openHelp();
            }
        });

        // Beim allerersten Start die Tour automatisch zeigen
        Preferences prefs = Preferences.userNodeForPackage(homeController.class);
        if (!prefs.getBoolean(PREF_TOUR_DONE, false) && !tourOfferedThisSession) {
            tourOfferedThisSession = true;
            root.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> obs,
                                    javafx.scene.Scene old, javafx.scene.Scene scene) {
                    if (scene == null) return;
                    obs.removeListener(this); // nur einmal
                    PauseTransition wait = new PauseTransition(Duration.millis(500));
                    wait.setOnFinished(e -> {
                        if (root.getScene() != null) startTour();
                    });
                    wait.play();
                }
            });
        }
    }

    private void openHelp() {
        HelpDialog.show(root.getScene() != null ? root.getScene().getWindow() : null, this::startTour);
    }

    private void startTour() {
        Node exportFormat = wordRadio.getParent();
        Node statusRow = statusLabel.getParent();
        List<OnboardingTour.Step> steps = List.of(
            OnboardingTour.step("Willkommen beim DoubleDegreeHelper",
                "Diese kurze Tour zeigt dir, wo du was findest. Das jeweils erklärte Element ist "
                    + "hervorgehoben, der Rest abgedunkelt.\n\n"
                    + "Weiter mit „Weiter“ oder den Pfeiltasten, Esc beendet die Tour. "
                    + "Du kannst sie jederzeit über „Hilfe“ unten links erneut starten."),
            OnboardingTour.step("Dateien einlesen",
                "Zieh hier die PDF-Transkripte der VGU und die XLSX-Themenlisten hinein, auch mehrere auf einmal. "
                    + "PDF und XLSX werden automatisch über die Matrikelnummer (VGU ID) zusammengeführt. "
                    + "Eine XLSX darf mehrere Studenten enthalten.",
                dropZone),
            OnboardingTour.step("Studentenliste",
                "Alle eingelesenen Studenten. „NEU“ heißt: noch nicht exportiert. "
                    + "„XLSX fehlt“ heißt: Thema und Abgabedatum fehlen noch.\n\n"
                    + "Ein Klick öffnet den Studenten direkt in der Notenauswahl. "
                    + "Mit ✕ (beim Überfahren sichtbar) entfernst du ihn aus der Liste, die Dateien bleiben erhalten.",
                studentList),
            OnboardingTour.step("Suche",
                "Filtert die Liste nach Matrikelnummer oder Name. Vietnamesische Akzente musst du nicht mittippen.",
                searchField),
            OnboardingTour.step("Quellordner",
                "Optional: Lege fest, in welchen Ordnern deine PDFs und XLSX-Dateien liegen. "
                    + "Neue Dateien darin werden dann bei jedem Start automatisch gefunden.",
                pdfFolderButton, pdfFolderLabel, xlsxFolderButton, xlsxFolderLabel),
            OnboardingTour.step("Studenten aktualisieren",
                "Durchsucht die Quellordner sofort nach neuen oder geänderten Dateien. "
                    + "Darunter steht das Ergebnis. Wurden Dateien übersprungen, halte die Maus auf den Text, "
                    + "um den Grund zu sehen.",
                refreshButton, statusRow),
            OnboardingTour.step("Speicherort",
                "Hier landen die erstellten Zeugnisse, Diploma Supplements und Urkunden.",
                saveFolderButton, saveFolderLabel),
            OnboardingTour.step("Format",
                "Wähle, ob Word-Dateien, PDFs oder beides erstellt werden. Für PDF wird Microsoft Word im "
                    + "Hintergrund benutzt. Optional öffnet sich nach dem Export direkt der Ordner.",
                exportFormat, openFolderCheck),
            OnboardingTour.step("Einstellungen",
                "Hier pflegst du die HHN-Module und ordnest die VGU-Module zu. "
                    + "Module ohne Zuordnung erscheinen später unter „Nicht zugeordnet“.",
                settingsButton),
            OnboardingTour.step("Vorlagen bearbeiten",
                "Öffnet die Word-Vorlagen für Zeugnis, Diploma Supplement und Urkunde. "
                    + "Texte darfst du ändern, Platzhalter in geschweiften Klammern wie {BDATE} aber nicht löschen.",
                transcriptEditButton, diplomaEditButton, certificateEditButton),
            OnboardingTour.step("Hilfe",
                "Wenn etwas schiefgeht: Hier findest du die letzten Fehlermeldungen, Lösungen für häufige "
                    + "Probleme und diese Tour. Tastenkürzel: F1.",
                helpButton)
        );
        OnboardingTour.start(root, steps, () ->
            Preferences.userNodeForPackage(homeController.class).putBoolean(PREF_TOUR_DONE, true));
    }

    // ------------------------------------------------------------------ bisherige Aktionen

    private void handleSettingsButtonAction(ActionEvent event) {
        SceneManager.switchTo(SceneManager.View.SETTINGS);
    }

    private static final Path TEMPLATE_DIR = Path.of(AppManager.getDefaultWordDir());

    private void openTemplate(String resourcePath, String fileName) {
        try {
            Files.createDirectories(TEMPLATE_DIR);

            Path dest = TEMPLATE_DIR.resolve(fileName);

            if (Files.notExists(dest)) {
                try (InputStream in = homeController.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        HelpLog.add("Ressource nicht gefunden: " + resourcePath);
                        new Alert(Alert.AlertType.ERROR, "Ressource nicht gefunden:\n" + resourcePath)
                            .showAndWait();
                        return;
                    }
                    Files.copy(in, dest);
                }
            }
            Desktop.getDesktop().open(dest.toFile());
        } catch (IOException ex) {
            HelpLog.add("Vorlage konnte nicht geöffnet werden: " + ex.getMessage());
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
