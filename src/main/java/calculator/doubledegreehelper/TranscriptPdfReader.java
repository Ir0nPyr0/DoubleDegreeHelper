package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.Semester;
import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentModule;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscriptPdfReader {
  private static final Logger logger = LoggerFactory.getLogger(TranscriptPdfReader.class);
  private static final Pattern MODULE_P = Pattern.compile(
      "^(.+?)\\s+(Winter|Summer|WiSe|SoSe)\\s+" +
          "(\\d{4})\\s+([0-9]+(?:\\.[0-9])?)\\s+([0-9]+(?:\\.[0-9])?)$"
  );
  private static final Pattern NAME_P = Pattern.compile("^Name[:]?\\s*(.+)$", Pattern.CASE_INSENSITIVE);

  public static Student read(Path pdfPath) throws IOException, ParseException {
    PDFTextStripper stripper = new PDFTextStripper();
    stripper.setSortByPosition(true);
    String text;
    try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())) {
      text = stripper.getText(pdf);
    }

    String[] lines = text.split("\\r?\\n");
    SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");

    String firstname = "", lastname = "", bornIn = "";
    Date bornDate = null, grantedAt = null;
    int studentId = 0, totalGrade = 0;

    String fullName = "";
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].trim();
      Matcher nameM = NAME_P.matcher(line);
      if (nameM.find()) {
        StringBuilder sb = new StringBuilder(nameM.group(1).trim());
        for (int j = i + 1; j < lines.length; j++) {
          String nxt = lines[j].trim();
          if (nxt.isEmpty()) break;
          if (nxt.matches("^(Date of Birth|Place of Birth|Student ID|Intake|Program|Gender)\\b.*"))
            break;
          sb.append("").append(nxt);
        }
        fullName = sb.toString()
            .replaceAll("(?i)Gender\\s+(Female|Male)", "")
            .trim();
        break;
      }
    }

    String extractedFirst = fullName;
    String extractedLast  = "";
    if (fullName.contains(",")) {
      
      String[] parts = fullName.split(",", 2);
      extractedLast  = parts[0].trim();
      extractedFirst = parts[1].trim();
    } else {
      String[] tokens = fullName.split("\\s+");
      if (tokens.length >= 2) {
        extractedLast = tokens[0];
        extractedFirst = String.join(" ", java.util.Arrays.copyOfRange(tokens, 1, tokens.length));
      }
    }

    firstname = extractedFirst;
    lastname  = extractedLast;

    String fullOriginalName = fullName.trim();

    String[] tokensAll = fullName.trim().split("\\s+");
    String middleName = "";

    for (String line : lines) {
      line = line.trim();
      if (line.startsWith("Date of Birth")) {
        bornDate = df.parse(line.substring("Date of Birth".length()).trim());
        break;
      }
    }

    for (String line : lines) {
      line = line.trim();
      if (line.startsWith("Place of Birth")) {
        bornIn = line.substring("Place of Birth".length()).trim();
        break;
      }
    }

    for (String line : lines) {
      line = line.trim();
      if (line.startsWith("Student ID")) {
        String[] partsId = line.substring("Student ID".length()).trim().split("\\s+", 2);
        studentId = Integer.parseInt(partsId[0]);
        break;
      }
    }

    for (String line : lines) {
      line = line.trim();
      if (line.startsWith("Grade Point Average")) {
        String g = line.substring(line.indexOf(':') + 1).trim();
        totalGrade = Math.round(Float.parseFloat(g) * 10);
        break;
      }
    }

    Matcher foot = Pattern.compile("\\b(\\d{2}\\.\\d{2}\\.\\d{4})\\b").matcher(text);
    String lastDate = null;
    while (foot.find()) lastDate = foot.group(1);
    if (lastDate != null) {
      grantedAt = df.parse(lastDate);
    }

    List<StudentModule> modules = new ArrayList<>();

    boolean inModules = false;
    StudentModule lastModule = null;

    for (String raw : lines) {
      String line = raw.trim();
      if (line.isEmpty()) continue;

      if (!inModules && line.startsWith("Course") && line.contains("Grade")) {
        inModules = true;
        continue;
      }
      if (inModules) {
        if (line.startsWith("Total ECTS")) break;

        Matcher m = MODULE_P.matcher(line);
        if (m.matches()) {
          String course = m.group(1).trim();
          String semStr = m.group(2);
          Semester sem = (semStr.equalsIgnoreCase("Summer") || semStr.equalsIgnoreCase("SoSe"))
              ? Semester.SUMMER
              : Semester.WINTER;
          int year  = Integer.parseInt(m.group(3));
          int ects  = (int) Float.parseFloat(m.group(4));
          float grd = Float.parseFloat(m.group(5));

          lastModule = new StudentModule(course, sem, year, ects, grd, null);
          modules.add(lastModule);
        } else if (lastModule != null) {
          if (line.matches("^[A-Za-zÄÖÜäöüß].*")
              && !line.contains("Page")
              && !line.toUpperCase().contains("ACADEMIC TRANSCRIPT")
              && !line.matches(".*\\b\\d{2}\\.\\d{2}\\.\\d{4}\\b.*")
              && !line.startsWith("Course")
              && !line.contains("Semester")
              && !line.contains("ECTS")
              && !line.contains("Grade")) {
            lastModule.setName(lastModule.getName() + " " + line);
          }
        }
      }
    }

    Student student = new Student(
        null, firstname, lastname, bornIn,
        studentId, modules.size(), totalGrade,
        bornDate, grantedAt
    );

    student.setFullName(fullOriginalName);
    student.setMiddleName(middleName.trim());

    modules.forEach(student::addModule);
    printInfo(firstname, studentId, df, bornDate, bornIn, totalGrade, modules);
    return student;
  }
  public static void printInfo(String firstname, int studentId, SimpleDateFormat df, Date bornDate, String bornIn, float totalGrade, List<StudentModule> studentModule) {
    logger.info("=== Student aus PDF geladen ===");
    logger.info("Name (voll):    {}", firstname);
    logger.info("Student ID:     " + studentId);
    logger.info("Geboren:        {} in {}", df.format(bornDate), bornIn);
    logger.info("GPA:            {}", String.format("%.2f", totalGrade / 10.0));
    logger.info("Module gesamt:  " + studentModule.size());
  }
}
