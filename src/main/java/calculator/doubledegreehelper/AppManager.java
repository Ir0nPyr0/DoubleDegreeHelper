package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.ModuleItem;
import calculator.doubledegreehelper.student.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppManager {
  private static final Logger logger = LoggerFactory.getLogger(AppManager.class);
  private static final StringSimilarity stringSimilarity = new StringSimilarity();
  private static final ObservableList<ModuleItem> hhnModules = FXCollections.observableArrayList();
  private static final ObservableList<ModuleItem> compareModules = FXCollections.observableArrayList();

  private static String gradeFilePath;
  private static String themeFilePath;
  private static String savePath;
  private static String filePath;
  private static String chairperson = "Prof. Dr. Thomas Fankhauser";
  private static String president   = "Prof. Dr. Oliver Lenzen";
  private static Student student;

  private static final ObservableList<Category> categories = FXCollections.observableArrayList();

  static {
    // Die Reihenfolge der Liste ist die Reihenfolge im Zeugnis: oben = Priorität 0.
    categories.addListener((javafx.collections.ListChangeListener<Category>) c -> renumberCategories());
  }

  private static void renumberCategories() {
    for (int i = 0; i < categories.size(); i++) categories.get(i).setPriority(i);
  }

  /** Verschiebt eine Kategorie (Drag and Drop in den Einstellungen). */
  public static void moveCategory(int from, int to) {
    if (from < 0 || from >= categories.size() || from == to) return;
    Category c = categories.remove(from);
    categories.add(Math.max(0, Math.min(to, categories.size())), c);
  }

  public static ObservableList<Category> getCategories() {
    return categories;
    }
  public static void addCategory(Category c) {
    if (c != null && !categories.contains(c)) categories.add(c);
    }
  public static void removeCategory(Category c) {
    categories.remove(c);
    }
  public static void clearCategories() {
    categories.clear();
  }

  public static void setGradeFilePath(String path) {
    gradeFilePath = path;
  }
  public static String getGradeFilePath() {
    return gradeFilePath;
  }

  public static void setThemeFilePath(String path) {
    themeFilePath = path;
  }
  public static String getThemeFilePath() {
    return themeFilePath;
  }

  public static void setSavePath(String path) {
    savePath = path;
  }
  public static String getSavePath() {
    return savePath;
  }

  public static void setFilePath(String path) {
    filePath = path;
  }
  public static String getFilePath() {
    return filePath;
  }

  public static void addComparableModule(ModuleItem compareModule, ModuleItem hhnModule) {
    if (compareModule != null && hhnModule != null
        && compareModules.contains(compareModule)
        && hhnModules.contains(hhnModule)) {
      compareModule.addModule(hhnModule);
    }
  }
  public static void removeComparableModule(ModuleItem compareModule, ModuleItem hhnModule) {
    if (compareModule != null && hhnModule != null
        && compareModules.contains(compareModule)
        && hhnModules.contains(hhnModule)) {
      compareModule.removeModule(hhnModule);
    }
  }

  public static void addCompareModule(ModuleItem module) {
    if (module != null && !module.getIsMain()) {
      compareModules.add(module);
    }
  }
  public static void removeCompareModule(ModuleItem module) {
    compareModules.remove(module);
  }
  public static ObservableList<ModuleItem> getCompareModules() {
    return compareModules;
  }

  public static void addHhnModule(ModuleItem module) {
    if (module != null && module.getIsMain()) {
      hhnModules.add(module);
    }
  }
  public static void removeHhnModule(ModuleItem module) {
    if (module != null && module.getIsMain()) {
      for (ModuleItem cmp : compareModules) {
        cmp.removeModule(module);
      }
      hhnModules.remove(module);
    }
  }
  public static ObservableList<ModuleItem> getHhnModules() {
    return hhnModules;
  }

  public static void setStudent(Student student) {AppManager.student = student;}
  public static Student  getStudent() {return student;}

  public static boolean checkSimilarity(String s1, String s2) {
    if (stringSimilarity != null) {
      return stringSimilarity.isSimilar(s1,s2);
    }
    else {
      return false;
    }
  }

  public static void clearAll() {
    hhnModules.clear();
    compareModules.clear();
  }

  public static void setChairperson(String chair) {
    chairperson = chair;
  }
  public static String getChairperson() {
    return chairperson;
  }

  public static void setPresident(String pres) {
    president = pres;
  }
  public static String getPresident() {
    return president;
  }

  public static String getDefaultWordDir() {
    return java.nio.file.Paths.get(System.getProperty("user.home"), "DoubleDegreeHelperData").toString();
  }
}
