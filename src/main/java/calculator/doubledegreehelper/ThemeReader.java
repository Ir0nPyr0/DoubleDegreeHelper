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
        break;
      }
    }
  }
}
