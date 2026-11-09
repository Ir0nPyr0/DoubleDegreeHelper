package calculator.doubledegreehelper;

import calculator.doubledegreehelper.controller.gradeSelectionController;

import java.util.prefs.Preferences;

/** Export-Einstellungen, die im Hauptmenü und in der Auswahl gemeinsam genutzt werden. */
public final class ExportSettings {
  /** Gleicher Knoten/Schlüssel wie bisher in der Auswahl, damit der gespeicherte Pfad erhalten bleibt. */
  private static final Preferences PREFS = Preferences.userNodeForPackage(gradeSelectionController.class);
  private static final String KEY_SAVE_PATH = "savePath";
  private static final String KEY_FORMAT = "exportFormat";
  private static final String KEY_OPEN_FOLDER = "openFolderAfterExport";

  public enum Format {
    WORD("Word", "Word erstellen"),
    PDF("PDF", "PDF erstellen"),
    BOTH("Word + PDF", "Word + PDF erstellen");

    private final String label, action;
    Format(String label, String action) { this.label = label; this.action = action; }
    public String getLabel() { return label; }
    public String getActionText() { return action; }
    public boolean wantsPdf() { return this != WORD; }
    public boolean keepsWord() { return this != PDF; }
  }

  private ExportSettings() {}

  public static String getSavePath() {
    String p = PREFS.get(KEY_SAVE_PATH, "");
    return p.isBlank() ? AppManager.getDefaultWordDir() : p;
  }

  public static void setSavePath(String path) {
    PREFS.put(KEY_SAVE_PATH, path);
    AppManager.setSavePath(path);
  }

  public static Format getFormat() {
    try {
      return Format.valueOf(PREFS.get(KEY_FORMAT, Format.WORD.name()));
    } catch (IllegalArgumentException e) {
      return Format.WORD;
    }
  }

  public static void setFormat(Format format) {
    PREFS.put(KEY_FORMAT, format.name());
  }

  public static boolean isOpenFolderAfterExport() {
    return PREFS.getBoolean(KEY_OPEN_FOLDER, false);
  }

  public static void setOpenFolderAfterExport(boolean open) {
    PREFS.putBoolean(KEY_OPEN_FOLDER, open);
  }
}
