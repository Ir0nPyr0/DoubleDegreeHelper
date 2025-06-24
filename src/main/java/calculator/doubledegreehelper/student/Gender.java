package calculator.doubledegreehelper.student;

public enum Gender {
  FEMALE("Female"),
  MALE("Male");

  private final String displayName;

  Gender(String displayName) {this.displayName = displayName;}
  public String getDisplayName() {return displayName;}
}
