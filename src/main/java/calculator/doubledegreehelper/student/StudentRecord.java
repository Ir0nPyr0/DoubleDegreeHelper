package calculator.doubledegreehelper.student;

import java.io.Serial;
import java.io.Serializable;

/**
 * Eintrag in students.dat: ein Student mit den Pfaden zu seinem Transkript (PDF)
 * und seiner Theme-Datei (XLSX). Die eigentlichen Daten werden beim Öffnen
 * immer frisch aus diesen Dateien gelesen.
 */
public class StudentRecord implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private final int number;
  private String name;
  private String pdfPath;
  private String xlsxPath;
  private boolean isNew;
  private final long addedAt;
  private long exportedAt;
  /** Manuell oder per Export abgehakt; null in alten students.dat-Dateien. */
  private Boolean done;

  public StudentRecord(int number, String name) {
    this.number = number;
    this.name = name == null ? "" : name;
    this.isNew = true;
    this.addedAt = System.currentTimeMillis();
  }

  public int getNumber() { return number; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name == null ? "" : name; }

  public String getPdfPath() { return pdfPath; }
  public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }

  public String getXlsxPath() { return xlsxPath; }
  public void setXlsxPath(String xlsxPath) { this.xlsxPath = xlsxPath; }

  public boolean isNew() { return isNew; }
  public void setNew(boolean isNew) { this.isNew = isNew; }

  public long getAddedAt() { return addedAt; }

  public long getExportedAt() { return exportedAt; }
  public void setExportedAt(long exportedAt) { this.exportedAt = exportedAt; }

  /** Erledigt: manuell abgehakt oder (bei alten Einträgen) schon einmal exportiert. */
  public boolean isDone() { return done != null ? done : exportedAt > 0; }
  public void setDone(boolean done) { this.done = done; }

  public boolean hasPdf() { return pdfPath != null && !pdfPath.isBlank(); }
  public boolean hasXlsx() { return xlsxPath != null && !xlsxPath.isBlank(); }

  @Override
  public String toString() {
    return number + " - " + name;
  }
}
