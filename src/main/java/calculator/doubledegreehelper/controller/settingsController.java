package calculator.doubledegreehelper.controller;

import calculator.doubledegreehelper.AppManager;
import calculator.doubledegreehelper.Category;
import calculator.doubledegreehelper.module.ModuleItem;
import calculator.doubledegreehelper.module.Semester;
import calculator.doubledegreehelper.SaveHandler;
import calculator.doubledegreehelper.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextFormatter.Change;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;
import javafx.scene.layout.Region;

import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class settingsController {
    @FXML
    private Button backButton, saveButton, addVguButton, addHhnButton, addCategoryButton;
    @FXML
    private Label vguCount, hhnCount, categoryCount, saveStatus;
    @FXML
    private TextField vguSearch, hhnSearch;
    @FXML
    private ListView<ModuleItem> hhnModuleContainer, compareModuleContainer;
    @FXML
    private ListView<Category> categoryContainer;

    private final SaveHandler saveHandler = new SaveHandler();
    private static settingsController INSTANCE;

    @FXML
    public void initialize() {
        INSTANCE = this;
        ObservableList<ModuleItem> hhnList = AppManager.getHhnModules();
        ObservableList<ModuleItem> cmpList = AppManager.getCompareModules();
        FilteredList<ModuleItem> hhnFiltered = new FilteredList<>(hhnList, m -> true);
        FilteredList<ModuleItem> cmpFiltered = new FilteredList<>(cmpList, m -> true);
        hhnModuleContainer.setItems(hhnFiltered);
        compareModuleContainer.setItems(cmpFiltered);
        hhnSearch.textProperty().addListener((o, a, q) -> hhnFiltered.setPredicate(m -> matches(m, q)));
        vguSearch.textProperty().addListener((o, a, q) -> cmpFiltered.setPredicate(m -> matches(m, q)));
        hhnList.addListener((ListChangeListener<ModuleItem>) c -> updateCounts());
        cmpList.addListener((ListChangeListener<ModuleItem>) c -> updateCounts());
        AppManager.getCategories().addListener((ListChangeListener<Category>) c -> updateCounts());
        updateCounts();

        hhnList.addListener((ListChangeListener<ModuleItem>) ch -> {
            while (ch.next()) {
                if (ch.wasRemoved()) {
                    for (ModuleItem rem : ch.getRemoved()) {
                        cmpList.forEach(cmp -> AppManager.removeComparableModule(cmp, rem));
                    }
                    compareModuleContainer.refresh();
                }
            }
        });

        hhnModuleContainer.setCellFactory(lv -> new ModuleCell(null, false));
        compareModuleContainer.setCellFactory(lv -> new ModuleCell(hhnList, true));

        AppManager.getCategories().addListener((ListChangeListener<Category>) c -> {
            hhnModuleContainer.refresh();
            compareModuleContainer.refresh();
        });

        categoryContainer.setItems(AppManager.getCategories());
        categoryContainer.setCellFactory(lv -> new ListCell<>() {
            private final TextField nameField = new TextField();
            private final Button delBtn = new Button("✕");
            private final TextField priority = new TextField();
            private final HBox box = new HBox(6, nameField, priority, delBtn);

            {
                nameField.setPromptText("Kategoriename");
                priority.setPromptText("Prio");
                priority.setPrefWidth(52);
                priority.setTooltip(new Tooltip("Priorität: bestimmt die Reihenfolge im Zeugnis"));
                delBtn.getStyleClass().add("delete-button");
                delBtn.setFocusTraversable(false);
                box.getStyleClass().add("card");
                setPrefWidth(0);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                HBox.setHgrow(nameField, Priority.ALWAYS);
                delBtn.setOnAction(e -> {
                    Category c = getItem();
                    if (c != null && confirmDelete("Kategorie", c.getName())) {
                        AppManager.removeCategory(c);
                    }
                });
                nameField.textProperty().addListener((obs, o, n) -> {
                    Category c = getItem();
                    if (c != null) {
                        c.setName(n);
                        hhnModuleContainer.refresh();
                        compareModuleContainer.refresh();
                    }
                });
                priority.textProperty().addListener((obs, o, n) -> {
                    Category c = getItem();
                    if (c != null) {
                        if (!n.isEmpty()) {
                            c.setPriority(Integer.parseInt(n));
                        }
                    }
                });
                UnaryOperator<Change> priorityDigits = ch -> ch.getControlNewText().matches("\\d*") ? ch : null;
                priority.setTextFormatter(new TextFormatter<>(priorityDigits));
            }

            @Override
            protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                } else {
                    nameField.setText(c.getName());
                    priority.setText(String.valueOf(c.getPriority()));
                    setGraphic(box);
                }
            }
        });

        backButton.setOnAction(this::handleBack);
        saveButton.setOnAction(this::handleSave);
        addVguButton.setOnAction(e -> {
            vguSearch.clear();
            ModuleItem m = new ModuleItem(false, Semester.WINTER, "", 0, "");
            AppManager.addCompareModule(m);
            reveal(compareModuleContainer, m);
        });
        addHhnButton.setOnAction(e -> {
            hhnSearch.clear();
            ModuleItem m = new ModuleItem(true, Semester.WINTER, "", 0, "");
            AppManager.addHhnModule(m);
            reveal(hhnModuleContainer, m);
        });
        addCategoryButton.setOnAction(e -> {
            Category neu = new Category("");
            AppManager.addCategory(neu);
            reveal(categoryContainer, neu);
        });
    }

    private static <T> void reveal(ListView<T> list, T item) {
        list.refresh();
        int idx = list.getItems().indexOf(item);
        if (idx >= 0) {
            list.scrollTo(idx);
            list.getSelectionModel().select(idx);
        }
    }

    private static boolean matches(ModuleItem m, String q) {
        if (q == null || q.isBlank()) return true;
        String needle = q.trim().toLowerCase(java.util.Locale.ROOT);
        String name = m.getModulName() == null ? "" : m.getModulName().toLowerCase(java.util.Locale.ROOT);
        String nr = m.getModulNumber() == null ? "" : m.getModulNumber().toLowerCase(java.util.Locale.ROOT);
        return name.contains(needle) || nr.contains(needle);
    }

    private void updateCounts() {
        vguCount.setText(String.valueOf(AppManager.getCompareModules().size()));
        hhnCount.setText(String.valueOf(AppManager.getHhnModules().size()));
        categoryCount.setText(String.valueOf(AppManager.getCategories().size()));
        long unmapped = AppManager.getCompareModules().stream()
            .filter(m -> m.getComparableNumber().isEmpty()).count();
        vguCount.setText(AppManager.getCompareModules().size()
            + (unmapped > 0 ? " · " + unmapped + " ohne Zuordnung" : ""));
    }

    static boolean confirmDelete(String what, String name) {
        ButtonType delete = new ButtonType("Löschen", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        String label = name == null || name.isBlank() ? "(ohne Namen)" : name;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Die Änderung wird erst mit „Speichern“ in die modules.dat geschrieben.", cancel, delete);
        a.setTitle(what + " löschen");
        a.setHeaderText(what + " „" + label + "“ löschen?");
        ((Button) a.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        ((Button) a.getDialogPane().lookupButton(delete)).setDefaultButton(false);
        return a.showAndWait().orElse(cancel) == delete;
    }

    static void refreshCompareList() {
        if (INSTANCE != null) {
            INSTANCE.compareModuleContainer.refresh();
            INSTANCE.updateCounts();
        }
    }

    private void handleBack(ActionEvent e) {
        SceneManager.switchTo(SceneManager.View.HOME);
    }

    private void handleSave(ActionEvent e) {
        saveHandler.saveData(true);
        saveStatus.setText("Gespeichert um "
            + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            + " · Backup angelegt");

        StringBuilder sb = new StringBuilder();

        sb.append("Compare-Module und ihre Zuordnungen:\n");
        for (ModuleItem cmp : AppManager.getCompareModules()) {
            sb.append(cmp.getModulNumber())
                .append(" → ")
                .append(cmp.getComparableNumber().stream()
                    .map(m -> String.valueOf(m.getModulNumber()))
                    .collect(Collectors.joining(", ", "[", "]")))
                .append("\n");
        }

        sb.append("\nHHN-Module und ihre Kategorien:\n");
        for (ModuleItem hhn : AppManager.getHhnModules()) {
            String catName = hhn.getCategory() != null
                ? hhn.getCategory().getName()
                : "<keine Kategorie>";
            sb.append(hhn.getModulNumber())
                .append(" ")
                .append(hhn.getModulName())
                .append(" → ")
                .append(catName)
                .append("\n");
        }
    }
    private static class ModuleCell extends ListCell<ModuleItem> {
        private final boolean isHhn;
        private final ComboBox<Semester> semesterBox = new ComboBox<>();
        private final TextField numberField = new TextField();
        private final TextField nameField = new TextField();
        private final TextField ectsField = new TextField();
        private final CheckComboBox<ModuleItem> checkCombo;
        private final Button removeBtn = new Button("✕");
        private final VBox container;
        private final ObservableList<ModuleItem> otherList;
        private boolean updating = false;
        private final ComboBox<Category> categoryBox = new ComboBox<>();

        ModuleCell(ObservableList<ModuleItem> otherList, boolean multiSelect) {
            this.otherList = otherList;
            this.isHhn = (otherList == null);

            semesterBox.setItems(FXCollections.observableArrayList(Semester.values()));

            categoryBox.setItems(AppManager.getCategories());
            categoryBox.setPrefWidth(200);
            categoryBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Category c) { return c == null ? "" : c.getName(); }
                @Override
                public Category fromString(String s) {
                    return AppManager.getCategories().stream()
                        .filter(c -> s.equals(c.getName()))
                        .findFirst().orElse(null);
                }
            });
            categoryBox.setCellFactory(new Callback<ListView<Category>, ListCell<Category>>() {
                @Override
                public ListCell<Category> call(ListView<Category> list) {
                    return new ListCell<>() {
                        @Override
                        protected void updateItem(Category c, boolean empty) {
                            super.updateItem(c, empty);
                            setText(empty || c == null ? null : c.getName());
                        }
                    };
                }
            });
            categoryBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Category c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : c.getName());
                }
            });
            categoryBox.valueProperty().addListener((o, oldV, newV) -> {
                ModuleItem it = getItem();
                if (it != null) it.setCategory(newV);
            });

            semesterBox.setPrefWidth(140);
            semesterBox.valueProperty().addListener((o, oldS, newS) -> {
                ModuleItem it = getItem();
                if (it != null && newS != null) it.setSemester(newS);
            });

            UnaryOperator<Change> alnum = ch -> ch.getControlNewText().matches("[A-Za-z0-9]*") ? ch : null;
            UnaryOperator<Change> digitsOnly = ch -> ch.getControlNewText().matches("\\d*") ? ch : null;
            numberField.setPromptText("Modul-Nr.");
            numberField.setPrefWidth(90);
            numberField.setTextFormatter(new TextFormatter<>(alnum));
            nameField.setPromptText("Modul-Name");
            ectsField.setPromptText("ECTS");
            ectsField.setTextFormatter(new TextFormatter<>(digitsOnly));
            ectsField.setPrefWidth(60);

            numberField.textProperty().addListener((o, oldV, newV) -> {
                ModuleItem it = getItem();
                if (it != null) {
                    it.changeNumber(newV);
                    if (isHhn) refreshCompareList();
                }
            });
            nameField.textProperty().addListener((o, oldV, newV) -> {
                ModuleItem it = getItem();
                if (it != null) {
                    it.changeName(newV);
                    if (isHhn) refreshCompareList();
                }
            });
            ectsField.textProperty().addListener((o, oldV, newV) -> {
                ModuleItem it = getItem();
                if (it != null && !newV.isEmpty()) it.changeEcts(Integer.parseInt(newV));
            });

            if (!isHhn) {
                checkCombo = new CheckComboBox<>(otherList);
                checkCombo.setConverter(new StringConverter<ModuleItem>() {
                    @Override
                    public String toString(ModuleItem item) {
                        return item == null ? "" : item.getModulName();
                    }

                    @Override
                    public ModuleItem fromString(String s) {
                        return null;
                    }
                });
                checkCombo.getCheckModel().getCheckedItems().addListener((ListChangeListener<ModuleItem>) ch -> {
                    if (updating) return;
                    ModuleItem it = getItem();
                    if (it != null) {
                        while (ch.next()) {
                            ch.getAddedSubList().forEach(m -> AppManager.addComparableModule(it, m));
                            ch.getRemoved().forEach(m -> AppManager.removeComparableModule(it, m));
                        }
                    }
                    checkCombo.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    checkCombo.requestLayout();
                });
                checkCombo.setMaxWidth(Double.MAX_VALUE);
                checkCombo.setPrefWidth(120);
                checkCombo.setTooltip(new Tooltip("HHN-Module auswählen, die dieses VGU-Modul ersetzt"));
                HBox.setHgrow(checkCombo, Priority.ALWAYS);
                Label arrow = new Label("→");
                arrow.getStyleClass().add("arrow");
                checkCombo.getCheckModel().getCheckedItems().addListener((ListChangeListener<ModuleItem>) ch -> updateMappedStyle());
                container = new VBox(6,
                    row(numberField, nameField, removeBtn),
                    row(semesterBox, ectsField, arrow, checkCombo));
            } else {
                checkCombo = null;
                categoryBox.setPromptText("Kategorie");
                categoryBox.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(categoryBox, Priority.ALWAYS);
                container = new VBox(6,
                    row(numberField, nameField, removeBtn),
                    row(semesterBox, ectsField, categoryBox));
            }
            container.getStyleClass().add("card");
            // Zellen an die Listenbreite anpassen statt horizontal zu scrollen
            setPrefWidth(0);

            removeBtn.setFocusTraversable(false);
            removeBtn.getStyleClass().add("delete-button");
            removeBtn.setTooltip(new Tooltip("Modul löschen"));
            removeBtn.setOnAction(e -> {
                ModuleItem it = getItem();
                if (it != null && confirmDelete(isHhn ? "HHN-Modul" : "VGU-Modul", it.getModulName())) {
                    if (isHhn) AppManager.removeHhnModule(it);
                    else AppManager.removeCompareModule(it);
                }
            });

            HBox.setHgrow(nameField, Priority.ALWAYS);
            semesterBox.setPrefWidth(120);
            semesterBox.setConverter(new StringConverter<>() {
                @Override public String toString(Semester s) {
                    return s == null ? "" : (s == Semester.SUMMER ? "Sommer" : "Winter");
                }
                @Override public Semester fromString(String s) { return null; }
            });
        }

        private static HBox row(javafx.scene.Node... nodes) {
            HBox h = new HBox(6, nodes);
            h.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            return h;
        }

        private void updateMappedStyle() {
            if (isHhn) return;
            ModuleItem it = getItem();
            container.getStyleClass().remove("unmapped");
            if (it != null && it.getComparableNumber().isEmpty()) container.getStyleClass().add("unmapped");
            if (!updating && INSTANCE != null) INSTANCE.updateCounts();
        }

        @Override
        protected void updateItem(ModuleItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                semesterBox.setValue(item.getSemester());
                numberField.setText(item.getModulNumber() == null ? "" : item.getModulNumber());
                nameField.setText(item.getModulName());
                ectsField.setText(item.getEcts() == 0 ? "" : String.valueOf(item.getEcts()));
                categoryBox.setValue(item.getCategory());

                if (!isHhn) {
                    updating = true;
                    checkCombo.getCheckModel().clearChecks();
                    item.getComparableNumber().forEach(checkCombo.getCheckModel()::check);
                    updating = false;
                    updateMappedStyle();
                }
                setGraphic(container);
            }
        }
    }
}
