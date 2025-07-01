package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.Semester;
import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentModule;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

public class WordFileCreator {
  private static final Logger logger = LoggerFactory.getLogger(WordFileCreator.class);
  private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM/yyyy");
  private static Path destination;
  private static Student student;
  private static List<StudentModule> modules = new ArrayList<>();

  public static void createWord(Path dest, Student stud, List<StudentModule> studentModule) throws IOException {
    // Create a sub-directory inside the chosen destination that is named after the student number.
    // All generated Word files will be placed inside this folder.
    Path studentDir = dest.resolve(String.valueOf(stud.getNumber()));
    Files.createDirectories(studentDir);

    destination = studentDir;
    student     = stud;
    modules     = studentModule;
    createDiploma();
    createCertificate();
    createTranscript();
  }

  private static Map<String,String> buildPlaceholderMap(Student s, String number) {
    Map<String,String> m = new HashMap<>();
    
    String pres = AppManager.getPresident();
    String chair = AppManager.getChairperson();

    m.put("{NUMBER}",    number);
    m.put("{NR}",        number);
    m.put("{FIRSTN}",    s.getFirstName());
    m.put("{LASTN}",     s.getLastName());
    m.put("{FIRSTNAME}", s.getFirstName());
    m.put("{LASTNAME}",  s.getLastName());
    m.put("{NAME}",      s.getFirstName() + " " + s.getLastName());
    m.put("{TITLE}",     s.getTitle()==null?"":s.getTitle().getDisplayName());
    m.put("{BDATE}",     DATE.format(s.getBirthDate()));
    m.put("{BPLACE}",    s.getBirthPlace());
    m.put("{DELIVERDATE}", DATE.format(s.getDeliverDate()));
    m.put("{GRANT}", DATE.format(s.getGrantedAt()));
    m.put("{TEXTGRADE}", getFullGrade(s.getTotalGrade(), true));
    m.put("{GRADETEXT}", getFullGrade(s.getTotalGrade(), false));
    m.put("{PRESIDENT}",   pres == null || pres.isBlank() ? "Prof. Dr. Oliver Lenzen" : pres);
    m.put("{CHAIRPERSON}", chair == null || chair.isBlank() ? "Prof. Dr. Thomas Fankhauser" : chair);
    return m;
  }

  private static void replaceInParagraphs(List<XWPFParagraph> paras, Map<String,String> repl) {
    for (XWPFParagraph p : paras) {
      List<XWPFRun> runs = p.getRuns();
      if (runs.isEmpty()) continue;

      // 1) Gesamten Absatztext und Längen der Runs bestimmen
      StringBuilder fullText = new StringBuilder();
      int[] runLens = new int[runs.size()];
      for (int i = 0; i < runs.size(); i++) {
        String txt = runs.get(i).getText(0);
        txt = (txt == null) ? "" : txt;
        fullText.append(txt);
        runLens[i] = txt.length();
      }
      String paragraphText = fullText.toString();

      // 2) Jeden Platzhalter nacheinander ersetzen
      for (Map.Entry<String,String> entry : repl.entrySet()) {
        String placeholder = entry.getKey();
        String replacement = entry.getValue();

        int idx = paragraphText.indexOf(placeholder);
        if (idx < 0) continue;

        // 3) Errechne Start-Run und Offset
        int charCount = 0, startRun = 0, startOff = 0;
        for (int i = 0; i < runLens.length; i++) {
          if (charCount + runLens[i] > idx) {
            startRun = i;
            startOff = idx - charCount;
            break;
          }
          charCount += runLens[i];
        }

        // 4) Errechne End-Run und Offset
        int endIdx = idx + placeholder.length();
        charCount = 0;
        int endRun = 0, endOff = 0;
        for (int i = 0; i < runLens.length; i++) {
          if (charCount + runLens[i] >= endIdx) {
            endRun = i;
            endOff = endIdx - charCount;
            break;
          }
          charCount += runLens[i];
        }

        // 5) Texte aus Start- und End-Run holen
        String startText = runs.get(startRun).getText(0);
        String before    = startText.substring(0, startOff);

        String endText = runs.get(endRun).getText(0);
        String after   = endText.substring(endOff);

        // 6) Ersetze im ersten Run und behalte dessen Formatierung
        XWPFRun firstRun = runs.get(startRun);
        firstRun.setText(before + replacement + after, 0);

        // 7) Lösche alle Runs zwischen startRun+1 und endRun
        for (int i = endRun; i > startRun; i--) {
          p.removeRun(i);
        }

        // 8) Aktualisiere paragraphText, falls Du mehrfach ersetzen willst
        paragraphText = paragraphText.substring(0, idx)
            + replacement
            + paragraphText.substring(endIdx);
        // und passe runLens[startRun] an (optional bei mehreren Vorkommen pro Absatz)
        runLens[startRun] = (before + replacement + after).length();
      }
    }
  }

  /* Find a template file in the user's DoubleDegreeHelper dir. Returns null if not found */
  private static Path findTemplatePath(String baseName) {
    Path dir = java.nio.file.Paths.get(System.getProperty("user.home"), "DoubleDegreeHelperData");
    try {
      // 1) Placeholder variant {NUMBER}-<baseName>
      Path p1 = dir.resolve("{NUMBER}" + baseName);
      if (java.nio.file.Files.exists(p1)) return p1;

      // 2) Any file that ends with baseName (e.g. 215021-<baseName>)
      try (java.util.stream.Stream<Path> s = java.nio.file.Files.list(dir)) {
        return s.filter(p -> p.getFileName().toString().endsWith(baseName))
                .findFirst()
                .orElse(null);
      }
    } catch (Exception ex) {
      return null;
    }
  }

  private static void createDiploma() throws IOException {
    String num = String.valueOf(student.getNumber());
    Path diskTpl = findTemplatePath("-DiplomaSupplement-1.docx");

    if (diskTpl != null && Files.exists(diskTpl)) {
      replaceAttribute(diskTpl, "-DiplomaSupplement-1.docx", num);
    } else {
      // Fallback to classpath resource (works in JAR/EXE)
      var tplStream = WordFileCreator.class.getResourceAsStream("/calculator/outputTemplate/{NUMBER}-DiplomaSupplement-1.docx");
      if (tplStream == null) {
        throw new FileNotFoundException("Template '/calculator/outputTemplate/{NUMBER}-DiplomaSupplement-1.docx' not found in classpath");
      }
      replaceAttribute(tplStream, "-DiplomaSupplement-1.docx", num);
    }
  }

  private static void createTranscript() throws IOException {
    String num = String.valueOf(student.getNumber());
    Path diskTpl = findTemplatePath("-Zeugnis-En-1.docx");

    if (diskTpl != null && Files.exists(diskTpl)) {
      replaceAttribute(diskTpl, "-Zeugnis-En-1.docx", num);
    } else {
      var tplStream = WordFileCreator.class.getResourceAsStream("/calculator/outputTemplate/{NUMBER}-Zeugnis-En-1.docx");
      if (tplStream == null) {
        throw new FileNotFoundException("Template '/calculator/outputTemplate/{NUMBER}-Zeugnis-En-1.docx' not found in classpath");
      }
      replaceAttribute(tplStream, "-Zeugnis-En-1.docx", num);
    }
    replaceTable();
  }

  private static void createCertificate() throws IOException {
    String num = String.valueOf(student.getNumber());
    Path diskTpl = findTemplatePath("-Urkunde-En-1.docx");

    if (diskTpl != null && Files.exists(diskTpl)) {
      replaceAttribute(diskTpl, "-Urkunde-En-1.docx", num);
    } else {
      var tplStream = WordFileCreator.class.getResourceAsStream("/calculator/outputTemplate/{NUMBER}-Urkunde-En-1.docx");
      if (tplStream == null) {
        throw new FileNotFoundException("Template '/calculator/outputTemplate/{NUMBER}-Urkunde-En-1.docx' not found in classpath");
      }
      replaceAttribute(tplStream, "-Urkunde-En-1.docx", num);
    }
  }

  public static void replaceAttribute(Path tpl, String fileName, String num) throws IOException {
    try (FileInputStream in = new FileInputStream(tpl.toFile())) {
      replaceAttribute(in, fileName, num);
    }
  }

  // New overload that works directly with an InputStream (templates loaded from resources)
  public static void replaceAttribute(InputStream tplStream, String fileName, String num) throws IOException {
    Files.createDirectories(destination);
    try (InputStream in = tplStream; XWPFDocument doc = new XWPFDocument(in)) {
      
      var repl = buildPlaceholderMap(student, num);
      replaceInParagraphs(doc.getParagraphs(), repl);
      replaceInTables(doc.getTables(),     repl);

      for (XWPFHeader h : doc.getHeaderList()) {
        replaceInParagraphs(h.getParagraphs(), repl);
        replaceInTables(h.getTables(),       repl);
      }
      for (XWPFFooter f : doc.getFooterList()) {
        replaceInParagraphs(f.getParagraphs(), repl);
        replaceInTables(f.getTables(),       repl);
      }

      try (FileOutputStream out = new FileOutputStream(destination.resolve(num + fileName).toFile())) {
        doc.write(out);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static void replaceInTables(List<XWPFTable> tables, Map<String,String> repl) {
    for (XWPFTable tbl : tables) {
      for (XWPFTableRow row : tbl.getRows()) {
        for (XWPFTableCell cell : row.getTableCells()) {
          replaceInParagraphs(cell.getParagraphs(), repl);
          replaceInTables(cell.getTables(),     repl);
          logger.debug("Cell content: '{}'", cell.getText());
        }
      }
    }
  }

  public static String getFullGrade(float grade, boolean mode) {
    double g;
    if (modules != null && !modules.isEmpty()) {
      int ectsSum = modules.stream().mapToInt(StudentModule::getEcts).sum();
      if (ectsSum > 0) {
        double weighted = modules.stream()
            .mapToDouble(m -> m.getGrade() * m.getEcts())
            .sum();
        g = weighted / ectsSum;
      } else {
        g = 0.0;
      }
    } else {
      g = grade / 10.0;
    }
    String desc;
    if      (g <= 1.5) desc = "very good";
    else if (g <= 2.5) desc = "good";
    else if (g <= 3.5) desc = "satisfactory";
    else if (g <= 4.0) desc = "sufficient";
    else               desc = "non sufficient";

    // Round DOWN (Abschneiden) statt normales Runden, damit z. B. 1.387 -> 1.3 bzw. 1.38
    double g1d = Math.floor(g * 10.0)  / 10.0;   // eine Nachkommastelle
    double g2d = Math.floor(g * 100.0) / 100.0;  // zwei Nachkommastellen

    if (mode) {
      return String.format(Locale.US, "%s (%.1f)", desc, g1d);
    } else {
      return String.format(Locale.US, "%.2f (%s)", g2d, desc);
    }
  }

  public static void replaceTable() {
    Path certPath = destination.resolve(student.getNumber() + "-Zeugnis-En-1.docx");
    if (!Files.exists(certPath)) {
      logger.warn("Certificate file not found at {}", certPath);
      return;
    }

    try (FileInputStream in = new FileInputStream(certPath.toFile());
         XWPFDocument doc = new XWPFDocument(in)) {

      XWPFTable templateTable = null;
      int catRowIdx = -1;
      int modRowIdx = -1;
      outer:
      for (XWPFTable tbl : doc.getTables()) {
        List<XWPFTableRow> rows = tbl.getRows();
        for (int i = 0; i < rows.size(); i++) {
          String rowText = rows.get(i).getTableCells().stream()
                             .map(XWPFTableCell::getText)
                             .collect(Collectors.joining(" "));
          if (rowText != null && rowText.contains("{CATEGORY}")) {
            templateTable = tbl;
            catRowIdx = i;
            if (i + 1 < rows.size()) {
              modRowIdx = i + 1;
            }
            break outer;
          }
        }
      }

      if (templateTable == null || catRowIdx == -1 || modRowIdx == -1) {
        logger.warn("No template table with placeholders {CATEGORY} / module row found – skipping table replacement.");
        throw new IllegalStateException("Kategoriezeile nicht gefunden!");
      }

      XWPFTableRow catTemplateRow = templateTable.getRow(catRowIdx);
      XWPFTableRow modTemplateRow = templateTable.getRow(modRowIdx);

      CTRow catCtRow = (CTRow) catTemplateRow.getCtRow().copy();
      CTRow modCtRow = (CTRow) modTemplateRow.getCtRow().copy();

      templateTable.removeRow(modRowIdx); // remove module row first (lower index afterwards!)
      templateTable.removeRow(catRowIdx);

      // 2) Group the student modules by category.
      Map<Category, List<StudentModule>> byCategory = new LinkedHashMap<>();
      for (StudentModule m : modules) {
        byCategory.computeIfAbsent(m.getCategory(), k -> new ArrayList<>()).add(m);
      }

      // 3) Sort the categories by priority (0 = top, higher numbers below)
      List<Map.Entry<Category, List<StudentModule>>> sortedCats = byCategory.entrySet()
          .stream()
          .sorted(Comparator.comparingInt(e -> {
            Category c = e.getKey();
            return c == null ? Integer.MAX_VALUE : c.getPriority();
          }))
          .toList();

      // 4) Iterate over categories and create rows.
      for (Map.Entry<Category, List<StudentModule>> entry : sortedCats) {
        Category cat = entry.getKey();
        List<StudentModule> catModules = entry.getValue();

        // Compute statistics required for placeholders.
        double avgGrade = catModules.stream().mapToDouble(StudentModule::getGrade).average().orElse(0.0);
        int totalEcts  = catModules.stream().mapToInt(StudentModule::getEcts).sum();

        // Determine the highest year present (ignore year 0 which indicates 'not set').
        int year = catModules.stream()
            .mapToInt(StudentModule::getYear)
            .filter(y -> y > 0)
            .max()
            .orElse(0);

        // For the highest year, decide semester label with priority: winter (wt) > summer (st).
        String sw;
        if (year > 0) {
          boolean hasWinter = catModules.stream()
              .anyMatch(m -> m.getYear() == year && (m.getSemester() == Semester.WINTER || m.getSemester() == Semester.SUMWIN));
          boolean hasSummer = catModules.stream()
              .anyMatch(m -> m.getYear() == year && (m.getSemester() == Semester.SUMMER || m.getSemester() == Semester.SUMWIN));
          if (hasWinter)      sw = "wt";
          else if (hasSummer) sw = "st";
          else                sw = "s/w";  // Fallback: unbekanntes Semester -> Winter
        } else {
          // No year information – fall back to mixed logic
          boolean hasWinter = catModules.stream()
              .anyMatch(m -> m.getSemester() == Semester.WINTER || m.getSemester() == Semester.SUMWIN);
          boolean hasSummer = catModules.stream()
              .anyMatch(m -> m.getSemester() == Semester.SUMMER || m.getSemester() == Semester.SUMWIN);

          /*
           * Wenn sowohl Winter- als auch Sommersemester vertreten sind, soll standardmäßig
           * "wt" (Winter) ausgegeben werden. Dadurch wird vermieden, dass in der Word-Vorlage
           * "s/w" erscheint, obwohl der Wintersemester-Eintrag in der Regel dominieren soll.
           */
          if (hasWinter)      sw = "wt";  // Winter hat Priorität
          else if (hasSummer) sw = "st";  // nur Sommer vorhanden
          else                sw = "s/w"; // kein Semester gesetzt
        }

        // Build replacement map for category row – include several alias placeholders
        Map<String, String> catMap = new HashMap<>();
        String catName = cat == null ? "" : cat.getName();
        String avgStr  = String.format(Locale.US, "%.1f", avgGrade);
        String ectsStr = String.valueOf(totalEcts);
        String yearStr = year == 0 ? "" : String.valueOf(year);

        catMap.put("{CATEGORY}", catName);
        catMap.put("{CAT}", catName);
        catMap.put("{CATNAME}", catName);
        catMap.put("{CAVG}", avgStr);
        catMap.put("{AVG}", avgStr);
        catMap.put("{CECT}", ectsStr);
        catMap.put("{ECTS}", ectsStr);
        catMap.put("{SW}", sw);
        catMap.put("{SEM}", sw);
        catMap.put("{Y}",  yearStr);
        catMap.put("{YEAR}", yearStr);

        // Duplicate template row, REPLACE placeholders, then insert.
        XWPFTableRow newCatRow = new XWPFTableRow((CTRow) catCtRow.copy(), templateTable);
        replaceRowPlaceholders(newCatRow, catMap);
        templateTable.addRow(newCatRow);

        // 3b) Sort modules so that Colloquium first, Thesis second, others afterwards.
        List<StudentModule> sortedModules = new ArrayList<>(catModules);
        sortedModules.sort(Comparator.comparingInt(m -> {
            String n = m.getName().toLowerCase(Locale.ROOT);
            if (n.contains("colloquium")) return 0;
            if (n.contains("thesis"))     return 1;
            return 2;
        }));

        // 3c) Add module rows.
        for (StudentModule mod : sortedModules) {
          Map<String, String> modMap = new HashMap<>();
          String gradeStr = String.format(Locale.US, "%.1f", mod.getGrade());
          String ectsModule = String.valueOf(mod.getEcts());
          String semDisplay;
          switch (mod.getSemester()) {
            case SUMMER -> semDisplay = "st";
            case WINTER  -> semDisplay = "wt";
            /*
             * Fallback für unbekanntes Semester (z. B. SUMWIN oder null). Anstatt "s/w"
             * wird nun "wt" ausgegeben, damit die Vorlage konsistent bleibt.
             */
            default       -> semDisplay = "wt";
          }
          String yearMod = mod.getYear()==0? "" : String.valueOf(mod.getYear());

          modMap.put("{MODULNAME}", mod.getName());
          modMap.put("{MODULE}", mod.getName());
          modMap.put("{NAME}",   mod.getName());
          modMap.put("{MGRA}", gradeStr);
          modMap.put("{GRADE}", gradeStr);
          modMap.put("{NOTE}", gradeStr);
          modMap.put("{MECTS}", ectsModule);
          modMap.put("{ECTS}", ectsModule);
          modMap.put("{SW}", semDisplay);
          modMap.put("{SEM}", semDisplay);
          modMap.put("{Y}", yearMod);

          XWPFTableRow newModRow = new XWPFTableRow((CTRow) modCtRow.copy(), templateTable);
          replaceRowPlaceholders(newModRow, modMap);

          // If this is the thesis row, append topic paragraph inside the same first cell.
          if (mod.getName().toLowerCase(Locale.ROOT).contains("thesis") && student.getTheme() != null) {
            XWPFTableCell firstCell = newModRow.getCell(0);
            if (firstCell != null) {
              XWPFParagraph topicPara = firstCell.addParagraph();
              topicPara.setIndentationLeft(550); // slight indent (~0.5 cm)
              XWPFRun run = topicPara.createRun();
              run.setItalic(true);
              run.setText(student.getTheme());
            }
          }

          templateTable.addRow(newModRow);
        }

        // 3d) (Removed separate thesis topic row – now included inside thesis module row.)
      }

      // 4) Write changes back to the document.
      try (FileOutputStream out = new FileOutputStream(certPath.toFile())) {
        doc.write(out);
      }

    } catch (IOException e) {
      logger.error("Error while replacing table", e);
      throw new RuntimeException(e);
    }
  }

  // Helper that replaces placeholders inside a single table row.
  private static void replaceRowPlaceholders(XWPFTableRow row, Map<String, String> repl) {
    for (XWPFTableCell cell : row.getTableCells()) {
      // --- 1) Versuche zunächst die Run-basierte Ersetzung wie bisher ---
      replaceInParagraphs(cell.getParagraphs(), repl);
      replaceInTables(cell.getTables(), repl);
      String joined = cell.getText();
      if (joined == null) continue;
      boolean hasPlaceholder = false;
      for (String key : repl.keySet()) {
        if (joined.contains(key)) {
          hasPlaceholder = true;
          break;
        }
      }
      if (hasPlaceholder) {
        String newText = joined;
        for (Map.Entry<String, String> e : repl.entrySet()) {
          newText = newText.replace(e.getKey(), Objects.toString(e.getValue(), ""));
        }
        // Inhalt der Zelle überschreiben
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        para.createRun().setText(newText, 0);
      }
    }
  }
}
