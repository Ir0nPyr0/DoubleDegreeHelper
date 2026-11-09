  package calculator.doubledegreehelper.module;

  import calculator.doubledegreehelper.Category;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  import java.io.ObjectStreamException;
  import java.io.Serializable;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Locale;

  public class ModuleItem implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ModuleItem.class);
    private static final long serialVersionUID = 1L;

    private final boolean isMain;
    private String modulNumber;
    private int ects;
    private String modulName;
    private Semester semester;
    private final List<ModuleItem> comparableNumber = new ArrayList<>();
    private Category category;
    private Double grade;
    private int    year;

    public ModuleItem(boolean isMain, Semester semester, int year,
                      String modulNumber, int ects, String modulName, Double grade) {
      this.isMain       = isMain;
      this.semester     = semester;
      this.year         = year;
      this.modulNumber  = modulNumber != null ? modulNumber : "";
      this.ects         = ects;
      this.modulName    = modulName;
      this.grade        = grade;
    }
    public ModuleItem(boolean isMain, Semester semester, String modulNumber, int ects, String modulName) {
      this(isMain, semester, 0, modulNumber, ects, modulName, null);
    }
    public Category getCategory() {
      return category;
    }
    public void setCategory(Category category) {
      this.category = category;
    }

    public boolean getIsMain() { return isMain; }
    public String getSeasonShort() {
      return semester == Semester.SUMMER ? "st" : "wt";
    }
    public int getYear() { return year; }
    public String getGradeFormatted() {
      return grade == null ? "-" : String.format(Locale.GERMAN, "%.1f", grade);
    }
    public double getGrand() {
      return grade != null ? grade : 0.0;
    }
    public int getCredits() { return ects; }
    public String getModulNumber() { return modulNumber; }
    public void changeNumber(String newNumber) { modulNumber = newNumber != null ? newNumber : ""; }

    public String getModulName() { return modulName; }
    public void changeName(String newName) { modulName = newName; }

    public int getEcts() { return ects; }
    public void changeEcts(int newEcts) { ects = newEcts; }

    public Semester getSemester() { return semester; }
    public void setSemester(Semester semester) { this.semester = semester; }

    public List<ModuleItem> getComparableNumber() { return comparableNumber; }

    public void addModule(ModuleItem module) {
      if (!comparableNumber.contains(module)) {
        comparableNumber.add(module);
      } else {
        logger.info("ModulItem konnte nicht hinzugefügt werden (bereits vorhanden)!");
      }
    }
    public void removeModule(ModuleItem module) {
      if (!comparableNumber.remove(module)) {
        logger.info("ModulItem konnte nicht entfernt werden (nicht gefunden)!");
      }
    }

    private Object readResolve() throws ObjectStreamException {
      if (semester == null) semester = Semester.WINTER;
      return this;
    }

    @Override public String toString() {
      return modulNumber == null || modulNumber.isEmpty() ? "" : modulNumber;
    }
  }