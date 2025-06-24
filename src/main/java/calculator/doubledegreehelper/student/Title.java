package calculator.doubledegreehelper.student;

public enum Title {
  MR("Mr"),
  MRS("Mrs");

  private final String displayName;

  Title(String displayName) {this.displayName = displayName;}
  public String getDisplayName() {return displayName;}
}
