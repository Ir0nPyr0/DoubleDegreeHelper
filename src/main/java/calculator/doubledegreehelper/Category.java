package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.ModuleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

public class Category implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(Category.class);
  private static final long serialVersionUID = 1L;
  private String name;
  private int priority;
  public Category(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public int getPriority() {return priority;}
  public void setPriority(int newPriority) {priority = newPriority;}

  @Override
  public String toString() {
    return name == null ? "" : name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Category)) return false;
    return Objects.equals(name, ((Category) o).name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
