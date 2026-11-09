package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.ModuleItem;
import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class SelectorAlgorithm {
  private static final Logger logger = LoggerFactory.getLogger(SelectorAlgorithm.class);

  public static List<StudentModule> calculate(Student student, List<ModuleItem> compareModules) {
    return calculate(student, compareModules, 90);
  }

  public static List<StudentModule> calculate(Student student,
                                              List<ModuleItem> compareModules,
                                              int target) {
    List<StudentModule> hnModules = getComparableModules(student, compareModules);

    List<StudentModule> mandatory = hnModules.stream()
        .filter(m -> {
          String n = m.getName().toLowerCase(java.util.Locale.ROOT);
          return n.contains("thesis") || n.contains("colloquium");
        })
        .collect(Collectors.toList());
    hnModules.removeAll(mandatory);

    int mandatoryEcts = mandatory.stream().mapToInt(StudentModule::getEcts).sum();
    int remainingTarget = Math.max(0, target - mandatoryEcts);

    // -----------------------------------------------------------------------------------------
    class Entry {
      List<StudentModule> combo;
      double cost;
    }

    Entry[] dp = new Entry[remainingTarget + 1];
    dp[0] = new Entry();
    dp[0].combo = new ArrayList<>();
    dp[0].cost  = 0.0;

    for (StudentModule mod : hnModules) {
      int w       = mod.getEcts();
      double cost = mod.getGrade() * w;
      for (int e = remainingTarget; e >= w; e--) {
        if (dp[e - w] != null) {
          double newCost = dp[e - w].cost + cost;
          if (dp[e] == null || newCost < dp[e].cost) {
            Entry ne = new Entry();
            ne.combo = new ArrayList<>(dp[e - w].combo);
            ne.combo.add(mod);
            ne.cost  = newCost;
            dp[e]    = ne;
          }
        }
      }
    }

    for (int e = remainingTarget; e >= 0; e--) {
      if (dp[e] != null) {
        List<StudentModule> result = new ArrayList<>(mandatory);
        result.addAll(dp[e].combo);
        return result;
      }
    }
    return new ArrayList<>(mandatory);
  }

  public static List<StudentModule> getComparableModules(Student student,
                                                         List<ModuleItem> compareModules) {
    List<StudentModule> hn = new ArrayList<>();
    for (ModuleItem comp : compareModules) {
      for (StudentModule sm : student.getModules()) {
        if (AppManager.checkSimilarity(sm.getName(), comp.getModulName())) {
          for (ModuleItem found : comp.getComparableNumber()) {
            hn.add(new StudentModule(
                found.getModulName(),
                found.getSemester(),
                sm.getYear(),
                found.getEcts(),
                sm.getGrade(),
                comp.getCategory()
            ));
          }
        }
      }
    }
    return hn;
  }

  public static List<StudentModule> getExcludedModules(Student student,
                                                       List<ModuleItem> compareModules) {
    List<StudentModule> possible = getComparableModules(student, compareModules);
    Set<StudentModule> chosen = new HashSet<>(calculate(student, compareModules));
    return possible.stream()
        .filter(m -> !chosen.contains(m))
        .collect(Collectors.toList());
  }
}
