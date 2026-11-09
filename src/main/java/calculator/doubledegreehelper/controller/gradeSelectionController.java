package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.AppManager;
import calculator.doubledegreehelper.SceneManager;
import calculator.doubledegreehelper.ExportSettings;
import calculator.doubledegreehelper.PdfConverter;
import calculator.doubledegreehelper.SelectorAlgorithm;
import calculator.doubledegreehelper.StudentRegistry;
import calculator.doubledegreehelper.WordFileCreator;
import calculator.doubledegreehelper.module.ModuleItem;
import calculator.doubledegreehelper.module.Semester;
import calculator.doubledegreehelper.student.Gender;
import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentModule;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;


public class gradeSelectionController {
    @FXML private DatePicker              birthDatePicker, completionDatePicker, deliverDatePicker;
    @FXML private Text                    ectsCounter, safePathText;
    @FXML private TextField               birthPlaceField, chairpersonText, presidentText, matriculationField;
    @FXML private TextArea                themeField;
    @FXML private TextField               nameField, middleNameField, lastNameField;
    @FXML private Label                   studentTitle, studentSubtitle, moduleHint;
    @FXML private Text                    moduleCountText, avgGradeText;
    @FXML private VBox                    ectsChip;
    @FXML private TitledPane              unknownPane;
    @FXML private ListView<StudentModule> unknownListView;
    @FXML private ListView<StudentModule> listViewContainer;
    @FXML private ComboBox<Gender>        genderComboBox;
    @FXML private Button                  backToHomeButton, createButton, safePathButton, charDropdownButton, autoSelectionButton;

    private final Popup             charPopup        = new Popup();
    private final DateTimeFormatter df               = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private TextInputControl        lastTextInput;
    private int                     lastCaretPos;
    private final Preferences       prefs            = Preferences.userNodeForPackage(gradeSelectionController.class);

    @FXML
    public void initialize() {
        String saved = ExportSettings.getSavePath();
        createButton.setText(ExportSettings.getFormat().getActionText());

        java.io.File defaultDir = new java.io.File(saved);
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }

        if (!saved.isBlank() && defaultDir.isDirectory()) {
            AppManager.setSavePath(saved);
            safePathText.setText(saved);
            safePathText.setVisible(true);
            createButton.setDisable(false);
        } else {
            safePathText.setVisible(false);
            createButton.setDisable(true);
        }

        List<TextInputControl> inputs = List.of(
            nameField, middleNameField, lastNameField, themeField,
            birthPlaceField, matriculationField,
            chairpersonText, presidentText
        );
        for (TextInputControl tic : inputs) {
            tic.focusedProperty().addListener((obs, old, focused) -> {
                if (focused) {
                    lastTextInput = tic;
                    lastCaretPos = tic.getCaretPosition();
                }
            });
            tic.caretPositionProperty().addListener((obs, old, pos) -> {
                if (tic.equals(lastTextInput)) {
                    lastCaretPos = pos.intValue();
                }
            });
        }

        nameField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null) s.setFirstName(n.trim());
            updateHeader();
        });
        themeField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null && n != null) s.setTheme(n.trim());
            updateHeader();
        });
        middleNameField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null) s.setMiddleName(n.trim());
            updateHeader();
        });
        lastNameField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null) s.setLastName(n.trim());
            updateHeader();
        });
        birthPlaceField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null) s.setBirthPlace(n.trim());
        });
        chairpersonText.textProperty().addListener((obs, o, n) -> {
            AppManager.setChairperson(n.trim());
        });
        presidentText.textProperty().addListener((obs, o, n) -> {
            AppManager.setPresident(n.trim());
        });

        genderComboBox.setItems(FXCollections.observableArrayList(Gender.MALE, Gender.FEMALE));
        genderComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(Gender g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty||g==null?"":(g==Gender.MALE?"Männlich":"Weiblich"));
            }
        });
        genderComboBox.setButtonCell(genderComboBox.getCellFactory().call(null));
        genderComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Gender g) {
                return g==null?"":(g==Gender.MALE?"Männlich":"Weiblich");
            }
            @Override public Gender fromString(String s) {
                return "Männlich".equals(s)? Gender.MALE
                    : "Weiblich".equals(s)? Gender.FEMALE
                    : null;
            }
        });
        genderComboBox.setPromptText("Geschlecht");

        backToHomeButton.setOnAction(this::handleBackToHome);
        createButton   .setOnAction(this::handleCreateWordFile);
        safePathButton .setOnAction(this::handleSafePath);
        autoSelectionButton.setOnAction(this::handleAutoSelection);

        birthDatePicker.setPromptText("dd/MM/yyyy");
        completionDatePicker.setPromptText("dd/MM/yyyy");
        deliverDatePicker.setPromptText("dd/MM/yyyy");
        var conv = new StringConverter<LocalDate>() {
            @Override public String toString(LocalDate d) { return d!=null?d.format(df):""; }
            @Override public LocalDate fromString(String s) {
                return (s==null||s.isEmpty())?null:LocalDate.parse(s,df);
            }
        };
        birthDatePicker.setConverter(conv);
        completionDatePicker.setConverter(conv);
        deliverDatePicker.setConverter(conv);

        buildCharPopup();
    }

    public void refresh() {
        Student s = AppManager.getStudent();
        List<ModuleItem> comps = AppManager.getCompareModules();
        if (s==null || comps==null) return;

        nameField.setText(s.getFirstName());
        middleNameField.setText(s.getMiddleName());
        lastNameField.setText(s.getLastName());
        genderComboBox.setValue(s.getGender());
        themeField.setText(s.getTheme());
        birthPlaceField.setText(s.getBirthPlace());
        matriculationField.setText(String.valueOf(s.getNumber()));
        chairpersonText.setText(AppManager.getChairperson()==null?"":AppManager.getChairperson());
        presidentText.setText(AppManager.getPresident()==null?"":AppManager.getPresident());

        if (s.getBirthDate()!=null) {
            LocalDate bd = Instant.ofEpochMilli(s.getBirthDate().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
            birthDatePicker.setValue(bd);
        }
        if (s.getGrantedAt()!=null) {
            LocalDate cd = Instant.ofEpochMilli(s.getGrantedAt().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
            completionDatePicker.setValue(cd);
        }
        if (s.getDeliverDate()!=null) {
            LocalDate ad = Instant.ofEpochMilli(s.getDeliverDate().getTime())
                .atZone(ZoneId.systemDefault()).toLocalDate();
            deliverDatePicker.setValue(ad);
        }

        List<StudentModule> validHhn = SelectorAlgorithm.getComparableModules(s, comps);

        Set<String> matched = s.getModules().stream()
            .filter(sm->comps.stream()
                .anyMatch(ci->AppManager.checkSimilarity(sm.getName(),ci.getModulName())))
            .map(sm->normalize(sm.getName()))
            .collect(Collectors.toSet());
        List<StudentModule> unknown = s.getModules().stream()
            .filter(sm->!matched.contains(normalize(sm.getName())))
            .peek(sm->sm.setPlaceholder(true))
            .toList();

        List<StudentModule> best = SelectorAlgorithm.calculate(s, comps);
        Set<String> bestNames = best.stream()
            .map(m->normalize(m.getName()))
            .collect(Collectors.toSet());
        validHhn.forEach(m->m.setActive(bestNames.contains(normalize(m.getName()))));

        Comparator<StudentModule> byGrade = Comparator
            .comparingDouble(StudentModule::getGrade)
            .thenComparing(StudentModule::getName);
        ObservableList<StudentModule> items = FXCollections.observableArrayList(validHhn);
        items.sort(byGrade);
        listViewContainer.setItems(items);

        ObservableList<StudentModule> unknownItems = FXCollections.observableArrayList(unknown);
        unknownItems.sort(byGrade);
        unknownListView.setItems(unknownItems);
        unknownListView.setCellFactory(lv -> new ModuleCell());
        unknownPane.setText("Nicht zugeordnet (" + unknownItems.size() + ") · in den Einstellungen keinem HHN-Modul zugewiesen");
        unknownPane.setVisible(!unknownItems.isEmpty());
        unknownPane.setManaged(!unknownItems.isEmpty());
        unknownListView.setPrefHeight(Math.min(6, Math.max(1, unknownItems.size())) * 36 + 4);

        listViewContainer.setCellFactory(lv -> new ModuleCell());
        // Leertaste schaltet das markierte Modul um
        listViewContainer.setOnKeyPressed(e -> {
            StudentModule m = listViewContainer.getSelectionModel().getSelectedItem();
            if (e.getCode() == javafx.scene.input.KeyCode.SPACE && m != null && !m.isPlaceholder()) {
                m.setActive(!m.isActive());
                listViewContainer.refresh();
                updateEctsCounter();
                updateStudentHhnModules();
                e.consume();
            }
        });
        updateHeader();

        updateEctsCounter();
        updateStudentHhnModules();
    }

    private void updateStudentHhnModules() {
        Student s = AppManager.getStudent();
        if (s == null) return;

        List<ModuleItem> active = AppManager.getHhnModules().stream()
            .filter(item ->
                listViewContainer.getItems().stream()
                    .filter(m -> !m.isPlaceholder() && m.isActive())
                    .anyMatch(m -> normalize(m.getName())
                        .equals(normalize(item.getModulName())))
            )
            .collect(Collectors.toList());

        s.setHnModules(active);
    }

    /** Eine Zeile der Modulliste: Haken, Name, Semester/ECTS, Notenpille. */
    private final class ModuleCell extends ListCell<StudentModule> {
        private final CheckBox cb    = new CheckBox();
        private final Label    name  = new Label();
        private final Label    meta  = new Label();
        private final Label    grade = new Label();
        private final HBox     row;

        ModuleCell() {
            name.getStyleClass().add("module-name");
            meta.getStyleClass().add("module-meta");
            grade.getStyleClass().add("grade");
            name.setMinWidth(0);
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            meta.setMinWidth(Region.USE_PREF_SIZE);
            grade.setMinWidth(Region.USE_PREF_SIZE);
            row = new HBox(12, cb, name, meta, grade);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getStyleClass().add("module-row");

            // Ganze Zeile ist klickbar; der Haken selbst nimmt keine Klicks an
            cb.setMouseTransparent(true);
            cb.setFocusTraversable(false);
            row.setOnMouseClicked(e -> {
                StudentModule m = getItem();
                if (e.getButton() == MouseButton.PRIMARY && m != null && !m.isPlaceholder()) {
                    cb.setSelected(!cb.isSelected());
                    toggle(cb.isSelected());
                }
            });
        }

        private void toggle(boolean sel) {
            StudentModule m = getItem();
            if (m == null || m.isPlaceholder()) return;
            m.setActive(sel);
            row.getStyleClass().remove("inactive");
            if (!sel) row.getStyleClass().add("inactive");
            updateEctsCounter();
            updateStudentHhnModules();
        }

        @Override protected void updateItem(StudentModule m, boolean empty) {
            super.updateItem(m, empty);
            if (empty || m == null) {
                setGraphic(null);
                setTooltip(null);
                return;
            }
            name.setText(m.getName());
            String sem = m.getSemester() == Semester.SUMMER ? "SoSe" : "WiSe";
            meta.setText((m.getYear() > 0 ? sem + " " + m.getYear() + "  ·  " : "") + m.getEcts() + " ECTS");
            grade.setText(String.format(Locale.GERMANY, "%.1f", m.getGrade()));
            grade.getStyleClass().removeIf(c -> c.startsWith("grade-"));
            row.getStyleClass().removeAll("inactive", "placeholder");
            if (m.isPlaceholder()) {
                cb.setVisible(false);
                cb.setManaged(true);
                row.getStyleClass().add("placeholder");
                grade.getStyleClass().add("grade-none");
                setTooltip(new Tooltip("Dieses Modul ist in den Einstellungen keinem HHN-Modul zugeordnet."));
            } else {
                cb.setVisible(true);
                cb.setSelected(m.isActive());
                if (!m.isActive()) row.getStyleClass().add("inactive");
                grade.getStyleClass().add(gradeClass(m.getGrade()));
                setTooltip(null);
            }
            setGraphic(row);
        }
    }

    private static String gradeClass(double g) {
        return g < 1.5 ? "grade-1" : g < 2.5 ? "grade-2" : g < 3.5 ? "grade-3" : g < 4.5 ? "grade-4" : "grade-5";
    }

    private void updateHeader() {
        if (studentTitle == null) return;
        String full = String.join(" ", java.util.stream.Stream.of(
                nameField.getText(), middleNameField.getText(), lastNameField.getText())
            .filter(t -> t != null && !t.isBlank()).map(String::trim).toList());
        studentTitle.setText(full.isBlank() ? "Student" : full);
        String nr = matriculationField.getText();
        String theme = themeField.getText();
        StringBuilder sub = new StringBuilder();
        if (nr != null && !nr.isBlank()) sub.append("Matrikelnr. ").append(nr.trim());
        if (theme != null && !theme.isBlank()) {
            if (sub.length() > 0) sub.append("   ·   ");
            sub.append(theme.trim());
        }
        studentSubtitle.setText(sub.toString());
    }

    private void updateEctsCounter(){
        int sum = listViewContainer.getItems().stream()
            .filter(m->!m.isPlaceholder()&&m.isActive())
            .mapToInt(StudentModule::getEcts)
            .sum();
        ectsCounter.setText(String.valueOf(sum));

        List<StudentModule> active = listViewContainer.getItems().stream()
            .filter(m -> !m.isPlaceholder() && m.isActive()).toList();
        moduleCountText.setText(String.valueOf(active.size()));
        double weighted = active.stream().mapToDouble(m -> m.getGrade() * m.getEcts()).sum();
        avgGradeText.setText(sum > 0 ? String.format(Locale.GERMANY, "%.1f", weighted / sum) : "–");

        ectsChip.getStyleClass().removeAll("ok", "warn");
        ectsChip.getStyleClass().add(sum == 90 ? "ok" : "warn");
        moduleHint.setText(sum == 90 ? "Genau 90 ECTS ausgewählt"
            : sum < 90 ? "Noch " + (90 - sum) + " ECTS bis 90"
            : (sum - 90) + " ECTS über 90");
    }

    private void handleBackToHome(ActionEvent ev){
        SceneManager.switchTo(SceneManager.View.HOME);
    }

    private void handleCreateWordFile(ActionEvent ev) {
        String out = AppManager.getSavePath();
        if (out == null || out.isBlank() || !new File(out).isDirectory()) {
            new Alert(Alert.AlertType.ERROR, "Ungültiger Speicherordner:\n" + out)
                .showAndWait();
            return;
        }

        List<StudentModule> active = listViewContainer.getItems().stream()
            .filter(m -> !m.isPlaceholder() && m.isActive())
            .toList();

        System.out.println("=== Alle aktiven HHN-Module ===");
        List<StudentModule> activeModule = new ArrayList<>();
        for (StudentModule m : active) {
            for (ModuleItem moduleItem : AppManager.getHhnModules()) {
                if (Objects.equals(m.getName(), moduleItem.getModulName())) {
                    m.setCategory(moduleItem.getCategory());
                }
            }
            String sem = m.getSemester() == Semester.SUMMER ? "SoSe" : "WiSe";
            System.out.printf(
                "%s | %d ECTS | %s | %s | %d | %s%n",
                m.getName(),
                m.getEcts(),
                m.getGrade(),
                sem,
                m.getYear(),
                m.getCategory() != null ? m.getCategory().getName() : "<keine Kategorie>");
            activeModule.add(new StudentModule(m.getName(),
                m.getSemester(),
                m.getYear(),
                m.getEcts(),
                m.getGrade(),
                m.getCategory()));
        }
        System.out.println("================================");

        ExportSettings.Format format = ExportSettings.getFormat();
        boolean openFolder = ExportSettings.isOpenFolderAfterExport();
        Student student = AppManager.getStudent();
        String idleText = createButton.getText();
        createButton.setDisable(true);
        createButton.setText(format.wantsPdf() ? "Erstelle PDF…" : "Erstelle Word…");

        Task<Path> job = new Task<>() {
            @Override
            protected Path call() throws Exception {
                WordFileCreator.createWord(Path.of(out), student, activeModule);
                Path dir = WordFileCreator.getLastOutputDir();
                if (format.wantsPdf()) {
                    String prefix = student.getNumber() + "-";
                    List<Path> docx;
                    try (var files = Files.list(dir)) {
                        docx = files.filter(f -> {
                            String n = f.getFileName().toString();
                            return n.startsWith(prefix) && n.toLowerCase(Locale.ROOT).endsWith(".docx");
                        }).toList();
                    }
                    PdfConverter.convert(docx);
                    if (!format.keepsWord()) {
                        for (Path f : docx) Files.deleteIfExists(f);
                    }
                }
                return dir;
            }
        };
        job.setOnSucceeded(e -> {
            createButton.setText(idleText);
            createButton.setDisable(false);
            Path dir = job.getValue();
            StudentRegistry.markExported(student);
            if (openFolder) {
                openFolder(dir);
            } else {
                new Alert(Alert.AlertType.INFORMATION,
                    (format == ExportSettings.Format.WORD ? "Die Word-Dateien wurden" :
                     format == ExportSettings.Format.PDF ? "Die PDF-Dateien wurden" : "Die Word- und PDF-Dateien wurden")
                        + " erfolgreich erstellt:\n" + dir).showAndWait();
            }
        });
        job.setOnFailed(e -> {
            createButton.setText(idleText);
            createButton.setDisable(false);
            Throwable ex = job.getException();
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText(format.wantsPdf() ? "Fehler beim Erstellen der Dateien" : "Fehler beim Erstellen der Word-Datei");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        });
        Thread t = new Thread(job, "export");
        t.setDaemon(true);
        t.start();
    }

    private static void openFolder(Path dir) {
        if (dir == null) return;
        // Desktop.open blockiert unter Windows teils kurz, daher eigener Thread
        Thread t = new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().open(dir.toFile());
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                    new Alert(Alert.AlertType.WARNING, "Ordner konnte nicht geöffnet werden:\n" + dir).showAndWait());
            }
        }, "open-folder");
        t.setDaemon(true);
        t.start();
    }

    private void handleSafePath(ActionEvent ev){
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Speicherordner auswählen");
        Window w = safePathButton.getScene().getWindow();
        File dir = dc.showDialog(w);
        if(dir!=null){
            ExportSettings.setSavePath(dir.getAbsolutePath());
            safePathText.setText(dir.getAbsolutePath());
            safePathText.setVisible(true);
            createButton.setDisable(false);
        }
    }

    private void handleAutoSelection(ActionEvent ev) {
        refresh();
    }

    private void buildCharPopup(){
        String upper = "ÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬ" +
            "ÉÈẺẼẸÊẾỀỂỄỆ" +
            "ÍÌỈĨỊ" +
            "ÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢ" +
            "ÚÙỦŨỤƯỨỪỬỮỰ" +
            "ÝỲỶỸỴ" +
            "Đ";

        String lower = "áàảãạăắằẳẵặâấầẩẫậ" +
            "éèẻẽẹêếềểễệ" +
            "íìỉĩị" +
            "óòỏõọôốồổỗộơớờởỡợ" +
            "úùủũụưứừửữự" +
            "ýỳỷỹỵ" +
            "đ";

        charPopup.getContent().clear();

        GridPane gridUpper = createCharGrid(upper);
        GridPane gridLower = createCharGrid(lower);

        HBox container = new HBox(16, gridUpper, gridLower);
        container.setPadding(new Insets(10));

        charPopup.getContent().add(container);
        charPopup.setAutoHide(true);
        charDropdownButton.setOnAction(e -> {
            if (charPopup.isShowing()) {
                charPopup.hide();
            } else {
                Bounds b = charDropdownButton.localToScreen(charDropdownButton.getBoundsInLocal());
                charPopup.show(charDropdownButton, b.getMinX(), b.getMaxY() + 4);
            }
        });
    }

    private GridPane createCharGrid(String chars){
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int cols = 7;
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            Label lbl = new Label(String.valueOf(c));
            lbl.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-border-color:#666;" +
                "-fx-background-color:#f9f9f9;-fx-padding:6 10;");
            lbl.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    var owner = charDropdownButton.getScene().getFocusOwner();
                    if (owner instanceof TextInputControl tic) {
                        int p = tic.getCaretPosition();
                        tic.insertText(p, String.valueOf(c));
                        tic.positionCaret(p + 1);
                    }
                    charPopup.hide();
                }
            });
            grid.add(lbl, i % cols, i / cols);
        }
        return grid;
    }

    private String normalize(String s){
        return Normalizer.normalize(s,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","")
            .toLowerCase(Locale.ROOT)
            .trim();
    }
}
