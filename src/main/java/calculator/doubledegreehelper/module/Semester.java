package calculator.doubledegreehelper.module;

public enum Semester {
  WINTER("W"),
  SUMMER("S"),
  SUMWIN("S/W");

  private final String display;
  Semester(String display) { this.display = display; }

  @Override public String toString() { return display; }
}