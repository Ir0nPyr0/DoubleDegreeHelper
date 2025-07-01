package calculator.doubledegreehelper;

import calculator.doubledegreehelper.student.Gender;
import calculator.doubledegreehelper.student.Student;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ThemeReader {
  private static final Logger logger = LoggerFactory.getLogger(ThemeReader.class);
  public static void fillThemeAndGenderFromExcel(Path excelPath, Student student)
      throws IOException
  {
    try (FileInputStream fis = new FileInputStream(excelPath.toFile());
         Workbook wb = new XSSFWorkbook(fis))
    {
      Sheet sheet = wb.getSheetAt(0);
      Row header = sheet.getRow(0);
      if (header == null) return;

      Map<String,Integer> col = new HashMap<>();
      for (Cell c : header) {
        String h = c.getStringCellValue().trim();
        col.put(h, c.getColumnIndex());
      }

      Integer idxVguId      = col.get("VGU ID");
      Integer idxTopic      = col.get("Thesis Topic");
      Integer idxGender     = col.get("Gender");
      Integer idxDeliver     = col.get("Deliver date");
      Integer idxBirthPlace = col.get("Birth place (in English) (City, Country)");
      Integer idxFullNameVN = col.get("Full name in\nVietnamese\n(Family name - Middle name - First name)");
      if (idxBirthPlace == null) {
        for (Map.Entry<String,Integer> e : col.entrySet()) {
          String key = e.getKey().replace("\n", " ").toLowerCase();
          if (key.contains("birth place")) {
            idxBirthPlace = e.getValue();
            break;
          }
        }
      }
      if (idxFullNameVN == null) {
        for (Map.Entry<String,Integer> e : col.entrySet()) {
          String key = e.getKey().replace("\n", " ").toLowerCase();
          if (key.contains("full name") && key.contains("vietnamese")) {
            idxFullNameVN = e.getValue();
            break;
          }
        }
      }

      if (idxVguId == null) {
        throw new IOException("Spalte 'VGU ID' nicht gefunden");
      }

      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;

        Cell idCell = row.getCell(idxVguId);
        if (idCell == null) continue;

        int vguId;
        if (idCell.getCellType() == CellType.NUMERIC) {
          vguId = (int) idCell.getNumericCellValue();
        } else {
          try {
            vguId = Integer.parseInt(idCell.getStringCellValue().trim());
          } catch (NumberFormatException e) {
            continue;
          }
        }
        if (vguId != student.getNumber()) {
          continue;
        }

        if (idxTopic != null) {
          Cell topicCell = row.getCell(idxTopic);
          if (topicCell != null && topicCell.getCellType() == CellType.STRING) {
            student.setTheme(topicCell.getStringCellValue().trim());
          }
          logger.info("Thesis Topic: {}", student.getTheme());
        }

        if (idxGender != null) {
          Cell genderCell = row.getCell(idxGender);
          if (genderCell != null && genderCell.getCellType() == CellType.STRING) {
            String g = genderCell.getStringCellValue().trim().toLowerCase();
            student.setGender(g.startsWith("m") ? Gender.MALE : Gender.FEMALE);
            logger.info("Gender:       {}", student.getGender());
          }
        }

        if (idxDeliver != null) {
          Cell deliverCell = row.getCell(idxDeliver);
          if (deliverCell != null && deliverCell.getCellType() == CellType.STRING) {
            String g = deliverCell.getStringCellValue();
            student.setDeliverDate(g);
            logger.info("DeliverDate: " + student.getDeliverDate());
          }
        }

        if (idxBirthPlace != null) {
          Cell birthPlaceCell = row.getCell(idxBirthPlace);
          if (birthPlaceCell != null && birthPlaceCell.getCellType() == CellType.STRING) {
            String bpRaw = birthPlaceCell.getStringCellValue().trim();
            if (!bpRaw.isEmpty()) {
              String bp = bpRaw;
              int idx = bpRaw.indexOf(',');
              if (idx >= 0) bp = bpRaw.substring(0, idx).trim();

              student.setBirthPlace(bp);
              logger.info("Birth place:   {}", student.getBirthPlace());
            }
          }
        }

        if (idxFullNameVN != null) {
          Cell nameCell = row.getCell(idxFullNameVN);
          if (nameCell != null && nameCell.getCellType() == CellType.STRING) {
            String rawName = nameCell.getStringCellValue().trim();
            if (!rawName.isEmpty()) {
              String capitalised = java.util.Arrays.stream(rawName.split("\\s+"))
                  .map(part -> part.isEmpty() ? part :
                      part.substring(0,1).toUpperCase(java.util.Locale.ROOT) +
                      part.substring(1).toLowerCase(java.util.Locale.ROOT))
                  .collect(java.util.stream.Collectors.joining(" "));

              String[] nameParts = capitalised.split("\\s+");
              if (nameParts.length >= 2) {
                student.setLastName(nameParts[0]);

                student.setFirstName(String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length)));
                student.setMiddleName("");
              } else {
                student.setFirstName(capitalised);
                student.setLastName("");
                student.setMiddleName("");
              }
              
              student.setFullName(capitalised);

              logger.info("Full name (VN): {} {}", student.getFirstName(), student.getLastName());
            }
          }
        }
        break;
      }
    }
  }
}
