package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.ModuleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class SaveHandler {
  private static final Logger logger = LoggerFactory.getLogger(SaveHandler.class);

  private static final Path DATA_DIR;
  private static final Path DATA_FILE;
  private static boolean saveBlocked;

  static {
    String base = System.getProperty("user.home");
    DATA_DIR  = Paths.get(base, "DoubleDegreeHelperData");
    DATA_FILE = DATA_DIR.resolve("modules.dat");
  }

  public void saveData() {
    saveData(false);
  }

  /**
   * Speichert die Module. Mit {@code createBackup} wird die bisherige modules.dat
   * vorher nach DoubleDegreeHelperData/backups kopiert (die neuesten MAX_BACKUPS bleiben).
   */
  public void saveData(boolean createBackup) {
    if (saveBlocked) {
      logger.error("Speichern gesperrt: '{}' konnte nicht geladen werden. Die Datei bleibt erhalten.", DATA_FILE);
      return;
    }
    try {
      Files.createDirectories(DATA_DIR);
    } catch (IOException io) {
      logger.error("Kann Datenverzeichnis '{}' nicht anlegen: {}", DATA_DIR, io.toString());
      return;
    }

    if (createBackup) {
      backupDataFile();
    }

    List<ModuleItem> hhnCopy = new ArrayList<>(AppManager.getHhnModules());
    List<ModuleItem> cmpCopy = new ArrayList<>(AppManager.getCompareModules());

    Path temporaryFile = null;
    try {
      temporaryFile = Files.createTempFile(DATA_DIR, "modules-", ".tmp");
      try (ObjectOutputStream out = new ObjectOutputStream(
          new BufferedOutputStream(Files.newOutputStream(temporaryFile)))) {
        out.writeObject(hhnCopy);
        out.writeObject(cmpCopy);
        out.writeObject(new ArrayList<>(AppManager.getCategories()));
      }
      try {
        Files.move(temporaryFile, DATA_FILE,
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(temporaryFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
      }
      logger.info("Daten gespeichert nach {} ({} HHN, {} CMP, {} Kategorien)",
          DATA_FILE, hhnCopy.size(), cmpCopy.size(), AppManager.getCategories().size());
    } catch (IOException e) {
      logger.error("Fehler beim Speichern der Module: {}", e.toString(), e);
    } finally {
      if (temporaryFile != null) {
        try {
          Files.deleteIfExists(temporaryFile);
        } catch (IOException e) {
          logger.warn("Temporäre Moduldatei konnte nicht entfernt werden: {}", temporaryFile, e);
        }
      }
    }
  }

  public boolean loadData() {
    if (!Files.exists(DATA_FILE)) return !saveBlocked;

    List<ModuleItem> hhnList;
    List<ModuleItem> cmpList;
    List<Category> catList;
    try (ObjectInputStream in = new ObjectInputStream(
        new BufferedInputStream(Files.newInputStream(DATA_FILE)))) {
      hhnList = readList(in, ModuleItem.class);
      cmpList = readList(in, ModuleItem.class);
      catList = readList(in, Category.class);
    } catch (IOException | ClassNotFoundException | ClassCastException ex) {
      saveBlocked = true;
      logger.error("Fehler beim Laden der Module: {}", ex.toString(), ex);
      try {
        StringWriter details = new StringWriter();
        ex.printStackTrace(new PrintWriter(details));
        Files.writeString(DATA_DIR.resolve("modules-load-error.log"),
            "Datei: " + DATA_FILE + System.lineSeparator() + details);
      } catch (IOException logError) {
        logger.error("Ladefehler konnte nicht in eine Datei geschrieben werden", logError);
      }
      return false;
    }

    AppManager.clearAll();
    AppManager.clearCategories();
    hhnList.forEach(AppManager::addHhnModule);
    cmpList.forEach(AppManager::addCompareModule);
    catList.forEach(AppManager::addCategory);
    saveBlocked = false;
    logger.info("Daten geladen aus {} ({} HHN, {} CMP, {} Kategorien)",
        DATA_FILE, hhnList.size(), cmpList.size(), catList.size());
    return true;
  }

  private static final int MAX_BACKUPS = 30;
  private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private static void backupDataFile() {
    if (!Files.exists(DATA_FILE)) return;
    Path backupDir = DATA_DIR.resolve("backups");
    try {
      Files.createDirectories(backupDir);
      Path target = backupDir.resolve("modules-" + LocalDateTime.now().format(BACKUP_STAMP) + ".dat");
      Files.copy(DATA_FILE, target, StandardCopyOption.REPLACE_EXISTING);
      logger.info("Backup der Module erstellt: {}", target);

      List<Path> backups;
      try (Stream<Path> files = Files.list(backupDir)) {
        backups = files
            .filter(f -> f.getFileName().toString().matches("modules-\\d{8}-\\d{6}\\.dat"))
            .sorted(Comparator.comparing((Path f) -> f.getFileName().toString()).reversed())
            .toList();
      }
      for (int i = MAX_BACKUPS; i < backups.size(); i++) {
        Files.deleteIfExists(backups.get(i));
      }
    } catch (IOException e) {
      logger.error("Backup der Module fehlgeschlagen: {}", e.toString(), e);
    }
  }

  public static Path getBackupDir() {
    return DATA_DIR.resolve("backups");
  }

  public static Path getDataDir() {
    return DATA_DIR;
  }

  public static Path getDataFile() {
    return DATA_FILE;
  }

  private static <T> List<T> readList(ObjectInputStream in, Class<T> type)
      throws IOException, ClassNotFoundException {
    if (!(in.readObject() instanceof List<?> entries)) {
      throw new InvalidObjectException("Erwartete Modulliste fehlt");
    }
    List<T> result = new ArrayList<>();
    for (Object entry : entries) {
      if (!type.isInstance(entry)) {
        throw new InvalidObjectException("Ungültiger Eintrag in " + type.getSimpleName() + "-Liste");
      }
      result.add(type.cast(entry));
    }
    return result;
  }

  private static class CompatObjectInputStream extends ObjectInputStream {
    CompatObjectInputStream(InputStream in) throws IOException { super(in); }

    @Override protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
      String name = desc.getName();
      if (name.startsWith("calculator.degreecalculator.")) {
        name = name.replace("calculator.degreecalculator.", "calculator.doubledegreehelper.");
      }
      return Class.forName(name);
    }
  }
}
