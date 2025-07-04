package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.AppManager;
import calculator.doubledegreehelper.SceneManager;
import calculator.doubledegreehelper.SelectorAlgorithm;
import calculator.doubledegreehelper.WordFileCreator;
import calculator.doubledegreehelper.module.ModuleItem;
import calculator.doubledegreehelper.module.Semester;
import calculator.doubledegreehelper.student.Gender;
import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentModule;
import javafx.collections.FXCollections;
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
    @FXML private TextArea                themeField, nameField, middleNameField, lastNameField;
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
        String saved = prefs.get("savePath", "");
        if (saved.isBlank()) {
            saved = AppManager.getDefaultWordDir();
        }

        java.io.File defaultDir = new java.io.File(saved);
        if (!defaultDir.exists()) {
            defaultDir.mkdirs();
        }

        if (!saved.isBlank() && defaultDir.isDirectory()) {
            AppManager.setSavePath(saved);
            safePathText.setText("Pfad festgelegt: " + saved);
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
        });
        middleNameField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null) s.setMiddleName(n.trim());
        });
        lastNameField.textProperty().addListener((obs, o, n) -> {
            Student s = AppManager.getStudent();
            if (s != null) s.setLastName(n.trim());
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

        ObservableList<StudentModule> items = FXCollections.observableArrayList();
        items.addAll(validHhn);
        items.addAll(unknown);
        items.sort(Comparator
            .comparing(StudentModule::isPlaceholder)
            .thenComparingDouble(StudentModule::getGrade)
            .thenComparing(StudentModule::getName)
        );
        listViewContainer.setItems(items);

        listViewContainer.setCellFactory(lv->new ListCell<>() {
            private final CheckBox cb     = new CheckBox();
            private final Label    lblN   = new Label();
            private final Label    lblE   = new Label();
            private final Label    lblG   = new Label();
            private final Region   spacer = new Region();
            private final HBox     hbox   = new HBox(8, cb, lblN, spacer, lblE, lblG);
            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                cb.setOnAction(e->{
                    StudentModule m = getItem();
                    if (m!=null && !m.isPlaceholder()) {
                        boolean sel = cb.isSelected();
                        m.setActive(sel);
                        System.out.println((sel?"Modul aktiviert:   ":"Modul deaktiviert: ") + m.getName());
                        updateEctsCounter();
                        updateStudentHhnModules();
                    }
                });
            }
            @Override protected void updateItem(StudentModule m, boolean empty){
                super.updateItem(m,empty);
                if(empty||m==null) {
                    setGraphic(null);
                } else {
                    lblN.setText(m.getName());
                    lblE.setText(String.valueOf(m.getEcts()));
                    lblG.setText(String.format(" (%.1f)",m.getGrade()));
                    if(m.isPlaceholder()){
                        cb.setVisible(false);
                        lblN.setTextFill(Color.GRAY);
                        lblE.setTextFill(Color.GRAY);
                        lblG.setTextFill(Color.GRAY);
                        hbox.setStyle("-fx-background-color:#e0e0e0;-fx-padding:4;");
                    } else {
                        cb.setVisible(true);
                        cb.setSelected(m.isActive());
                        lblN.setTextFill(Color.BLACK);
                        lblE.setTextFill(Color.BLACK);
                        lblG.setTextFill(Color.BLACK);
                        double g = m.getGrade();
                        String bg = g<1.5?"#d4edda"
                            :g<2.5?"#a3d5a1"
                            :g<3.5?"#fff3cd"
                            :g<4.5?"#ffe5a1"
                            :"#f8d7da";
                        hbox.setStyle("-fx-background-color:"+bg+";-fx-padding:4;");
                    }
                    setGraphic(hbox);
                }
            }
        });

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

    private void updateEctsCounter(){
        int sum = listViewContainer.getItems().stream()
            .filter(m->!m.isPlaceholder()&&m.isActive())
            .mapToInt(StudentModule::getEcts)
            .sum();
        ectsCounter.setText(String.valueOf(sum));
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

        try {
            WordFileCreator.createWord(Path.of(out), AppManager.getStudent(), activeModule);
            new Alert(
                Alert.AlertType.INFORMATION,
                "Die Word-Dateien wurden erfolgreich erstellt:\n" + out
            ).showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setHeaderText("Fehler beim Erstellen der Word-Datei");
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    private void handleSafePath(ActionEvent ev){
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Speicherordner auswählen");
        Window w = safePathButton.getScene().getWindow();
        File dir = dc.showDialog(w);
        if(dir!=null){
            AppManager.setSavePath(dir.getAbsolutePath());
            prefs.put("savePath", dir.getAbsolutePath());
            safePathText.setText("Pfad festgelegt: "+dir.getAbsolutePath());
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
