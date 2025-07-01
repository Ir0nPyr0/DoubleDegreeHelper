package calculator.doubledegreehelper.student;

import calculator.doubledegreehelper.module.ModuleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Student {
  private static final Logger logger = LoggerFactory.getLogger(Student.class);
  private Title title;
  private Gender gender;
  private Date birthDate, grantedAt, deliverDate;
  private String firstName, middleName, lastName, birthPlace, theme;
  private String fullName;
  private int number, semesterTotal, totalGrade;
  private List<StudentModule> modules;

  public Student(Title title, String firstname, String lastname, String bornIn,
                 int number, int semesterTotal, int totalGrade,
                 Date bornDate, Date grantedAt) {
    modules = new ArrayList<>();
    this.title = title;
    this.firstName = firstname;
    this.lastName = lastname;
    this.fullName = (firstname == null ? "" : firstname) + (lastname==null||lastname.isBlank()?"":" "+lastname);
    this.number = number;
    this.semesterTotal = semesterTotal;
    this.birthPlace = bornIn;
    this.totalGrade = totalGrade;
    this.birthDate = bornDate;
    this.grantedAt = grantedAt;
  }
  private static List<ModuleItem> hnModules = new ArrayList<>();

  public Title getTitle() {
    if (title != null) return title;
    if (gender == Gender.MALE)   return Title.MR;
    if (gender == Gender.FEMALE) return Title.MRS;
    return null;
  }
  public void setTitle(Title title) {
    this.title = title;
  }

  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }

  public String getMiddleName() { return middleName; }
  public void setMiddleName(String middleName) { this.middleName = middleName; }

  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }

  public int getNumber() { return number; }
  public void setNumber(int number) { this.number = number; }

  public int getSemesterTotal() { return semesterTotal; }
  public void setSemesterTotal(int semesterTotal) { this.semesterTotal = semesterTotal; }

  public Date getBirthDate() { return birthDate; }
  public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }

  public String getBirthPlace() { return birthPlace; }
  public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }

  public String getTheme() { return theme; }
  public void setTheme(String theme) { this.theme = theme; }

  public Date getGrantedAt() { return grantedAt; }
  public void setGrantedAt(Date grantedAt) { this.grantedAt = grantedAt; }

  public Date getDeliverDate() {return deliverDate;}
  public void setDeliverDate(String date) {
    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    sdf.setLenient(false);
    try {
      this.deliverDate = sdf.parse(date);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Ungültiges Datum: " + date, e);
    }
  }


  public Gender getGender() { return gender; }
  public void setGender(Gender gender) {
    this.gender = gender;
    if (gender == Gender.MALE) {
      this.title = Title.MR;
    } else if (gender == Gender.FEMALE) {
      this.title = Title.MRS;
    }
  }

  public int getTotalGrade() { return totalGrade; }
  public void setTotalGrade(int totalGrade) { this.totalGrade = totalGrade; }


  public void addModule(StudentModule module) {
    if (!modules.contains(module)) modules.add(module);
  }
  public void removeModule(StudentModule module) {
    modules.remove(module);
  }

  public void setHnModules(List<ModuleItem> modules) {
    this.hnModules = new ArrayList<>(modules);
  }
  public static List<ModuleItem> getHnModules() {
    return Collections.unmodifiableList(hnModules);
  }

  public List<StudentModule> getModules() { return modules; }
  public void setModules(List<StudentModule> modules ) { this.modules = modules; }

  public float getCalculateTotalGrande() {
    int counter = 0;
    float grade = 0;
    for (StudentModule module : getModules()) {
      counter++;
      grade += module.getGrade();
    }
    return grade / counter;
  }

  public String getFullName() {
    if (fullName != null && !fullName.isBlank()) return fullName;

    StringBuilder sb = new StringBuilder();
    if (firstName!=null && !firstName.isBlank()) sb.append(firstName);
    if (middleName!=null && !middleName.isBlank()) {
      if(sb.length()>0) sb.append(" ");
      sb.append(middleName);
    }
    if (lastName!=null && !lastName.isBlank()) {
      if(sb.length()>0) sb.append(" ");
      sb.append(lastName);
    }
    return sb.toString();
  }

  public void setFullName(String fullName) { this.fullName = fullName; }
}
