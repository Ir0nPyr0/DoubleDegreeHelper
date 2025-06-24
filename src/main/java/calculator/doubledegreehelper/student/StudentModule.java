package calculator.doubledegreehelper.student;

import calculator.doubledegreehelper.Category;
import calculator.doubledegreehelper.StringSimilarity;
import calculator.doubledegreehelper.module.Semester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class StudentModule {
  private static final Logger logger = LoggerFactory.getLogger(StudentModule.class);
  private String name;
  private Semester semester;
  private int year, ects;
  private float grade;
  private boolean active = false;
  private boolean placeholder = false;
  private Category category;

  public StudentModule(String name, Semester sem, int year, int ects, float grade, Category category) {
    this.name = name;
    this.semester = sem;
    this.year = year;
    this.ects = ects;
    this.grade = grade;
    this.category = category;
  }

  public boolean isActive()      { return active; }
  public void setName(String name) {this.name = name;}
  public void setActive(boolean a){ this.active = a; }
  public boolean isPlaceholder(){ return placeholder; }
  public void setPlaceholder(boolean p){ this.placeholder = p; }

  public String getName()        { return name; }
  public Semester getSemester()  { return semester; }
  public int getYear()           { return year; }
  public void setYear(int year)           { this.year = year;}
  public int getEcts()           { return ects; }
  public float getGrade()        { return grade; }
  public Category getCategory()        { return category; }
  public void setCategory(Category category)        { this.category = category; }
  @Override public boolean equals(Object o){
    if(this==o) return true;
    if(!(o instanceof StudentModule)) return false;
    StudentModule m = (StudentModule)o;
    return year==m.year && name.equalsIgnoreCase(m.name);
  }
  @Override public int hashCode(){
    return Objects.hash(name.toLowerCase(), year);
  }
}
