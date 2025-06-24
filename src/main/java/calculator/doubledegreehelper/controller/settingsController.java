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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;

import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class settingsController {
    @FXML
    private TabPane moduleTabPane;
    @FXML
    private Button backButton, saveButton, addButton;
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
        hhnModuleContainer.setItems(hhnList);
        compareModuleContainer.setItems(cmpList);

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

        moduleTabPane.getSelectionModel().selectedIndexProperty().addListener((o, oldI, newI) -> {
            hhnModuleContainer.refresh();
            compareModuleContainer.refresh();
        });

        categoryContainer.setItems(AppManager.getCategories());
        categoryContainer.setCellFactory(lv -> new ListCell<>() {
            private final TextField nameField = new TextField();
            private final Button delBtn = new Button("X");
            private final TextField priority = new TextField();
            private final HBox box = new HBox(5, nameField, priority, delBtn);

            {
                nameField.setPromptText("Kategorien name");

                HBox.setHgrow(nameField, Priority.ALWAYS);
                delBtn.setOnAction(e -> {
                    Category c = getItem();
                    if (c != null) {
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
        addButton.setOnAction(this::handleAdd);
    }

    static void refreshCompareList() {
        if (INSTANCE != null) INSTANCE.compareModuleContainer.refresh();
    }

    private void handleBack(ActionEvent e) {
        SceneManager.switchTo(SceneManager.View.HOME);
    }

    private void handleSave(ActionEvent e) {
        saveHandler.saveData();

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
    private void handleAdd(ActionEvent e) {
        int idx = moduleTabPane.getSelectionModel().getSelectedIndex();
        if (idx == 2) {
            Category neu = new Category("");
            AppManager.addCategory(neu);
            categoryContainer.refresh();
        } else {
            boolean isMain = (idx == 0);
            ModuleItem newItem = new ModuleItem(isMain, Semester.WINTER, "", 0, "");
            if (isMain) {
                AppManager.addHhnModule(newItem);
                hhnModuleContainer.refresh();
            } else {
                AppManager.addCompareModule(newItem);
                compareModuleContainer.refresh();
            }
        }
    }

    private static class ModuleCell extends ListCell<ModuleItem> {
        private final boolean isHhn;
        private final ComboBox<Semester> semesterBox = new ComboBox<>();
        private final TextField numberField = new TextField();
        private final TextField nameField = new TextField();
        private final TextField ectsField = new TextField();
        private final CheckComboBox<ModuleItem> checkCombo;
        private final Button removeBtn = new Button("X");
        private final HBox container;
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
                    @Override public String toString(ModuleItem item) { return item == null ? "" : item.toString(); }
                    @Override public ModuleItem fromString(String s) { return null; }
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
                });
                container = new HBox(5, semesterBox, numberField, nameField, ectsField, checkCombo, removeBtn);
            } else {
                checkCombo = null;
                container = new HBox(5, semesterBox, numberField, nameField, ectsField, categoryBox, removeBtn);
            }

            removeBtn.setFocusTraversable(false);
            removeBtn.setStyle("-fx-padding:2 5; -fx-font-weight:bold;");
            removeBtn.setOnAction(e -> {
                ModuleItem it = getItem();
                if (it != null) {
                    if (isHhn) AppManager.removeHhnModule(it);
                    else AppManager.removeCompareModule(it);
                }
            });

            container.setFillHeight(true);
            HBox.setHgrow(nameField, Priority.ALWAYS);
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
                }
                setGraphic(container);
            }
        }
    }
}
