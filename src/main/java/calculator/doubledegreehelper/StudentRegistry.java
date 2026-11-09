package calculator.doubledegreehelper;

import calculator.doubledegreehelper.student.Student;
import calculator.doubledegreehelper.student.StudentRecord;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

/**
 * Verwaltet die Studentenliste (students.dat neben modules.dat).
 * <p>
 * PDFs und XLSX-Dateien werden über die VGU-ID (Matrikelnummer) einander zugeordnet.
 * Wird dieselbe Datei erneut eingelesen, werden nur die Verknüpfungen aktualisiert.
 * XLSX-Einträge, zu denen noch kein Transkript existiert, werden vorgemerkt und
 * verknüpft, sobald das passende PDF auftaucht.
 */
public final class StudentRegistry {
  private static final Logger logger = LoggerFactory.getLogger(StudentRegistry.class);

  private static final Path STUDENTS_FILE = SaveHandler.getDataDir().resolve("students.dat");
  private static final Preferences PREFS = Preferences.userNodeForPackage(StudentRegistry.class);
  private static final String PREF_PDF_DIR = "studentPdfFolder";
  private static final String PREF_XLSX_DIR = "studentXlsxFolder";
  private static final int SCAN_DEPTH = 3;

  private static final ObservableList<StudentRecord> records = FXCollections.observableArrayList();
  /** Pfad -> lastModified der bereits eingelesenen Dateien, damit der Ordner-Scan nur Neues parst. */
  private static final Map<String, Long> knownFiles = new ConcurrentHashMap<>();
  /** XLSX-Zuordnungen ohne passendes PDF (VGU-ID -> Datei). */
  private static final Map<Integer, PendingXlsx> pendingXlsx = new HashMap<>();
  /** Neu hinzugekommene Studenten, deren Listeneintrag einmal animiert werden soll. */
  private static final Set<Integer> highlight = new HashSet<>();

  private static final BooleanProperty busy = new SimpleBooleanProperty(false);
  private static final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "student-scan");
    t.setDaemon(true);
    return t;
  });
  private static int runningJobs;
  private static boolean startupScanDone;

  private StudentRegistry() {}

  // ---------------------------------------------------------------- Zugriff

  public static ObservableList<StudentRecord> getRecords() { return records; }

  public static ReadOnlyBooleanProperty busyProperty() { return busy; }

  public static Path getStudentsFile() { return STUDENTS_FILE; }

  public static String getPdfFolder() { return PREFS.get(PREF_PDF_DIR, ""); }
  public static void setPdfFolder(String folder) { PREFS.put(PREF_PDF_DIR, folder == null ? "" : folder); }

  public static String getXlsxFolder() { return PREFS.get(PREF_XLSX_DIR, ""); }
  public static void setXlsxFolder(String folder) { PREFS.put(PREF_XLSX_DIR, folder == null ? "" : folder); }

  /** Liefert true genau einmal pro Programmstart (für den automatischen Scan beim Start). */
  public static boolean claimStartupScan() {
    if (startupScanDone) return false;
    startupScanDone = true;
    return true;
  }

  /** Liefert true, wenn der Eintrag frisch hinzugekommen ist und noch nicht animiert wurde. */
  public static boolean consumeHighlight(StudentRecord record) {
    return record != null && highlight.remove(record.getNumber());
  }

  public static Optional<StudentRecord> find(int number) {
    return records.stream().filter(r -> r.getNumber() == number).findFirst();
  }

  // ---------------------------------------------------------------- Speichern / Laden

  public static void load() {
    if (!Files.exists(STUDENTS_FILE)) return;
    try (ObjectInputStream in = new ObjectInputStream(
        new BufferedInputStream(Files.newInputStream(STUDENTS_FILE)))) {
      Object obj = in.readObject();
      if (!(obj instanceof Store store)) {
        throw new InvalidObjectException("Unbekanntes Format");
      }
      records.setAll(store.records);
      knownFiles.clear();
      knownFiles.putAll(store.knownFiles);
      pendingXlsx.clear();
      pendingXlsx.putAll(store.pendingXlsx);
      logger.info("Studenten geladen aus {} ({} Einträge)", STUDENTS_FILE, records.size());
    } catch (IOException | ClassNotFoundException | ClassCastException ex) {
      logger.error("students.dat konnte nicht geladen werden: {}", ex.toString(), ex);
      Path broken = STUDENTS_FILE.resolveSibling("students.dat.broken-"
          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
      try {
        Files.move(STUDENTS_FILE, broken);
        logger.warn("Beschädigte students.dat verschoben nach {}", broken);
      } catch (IOException moveError) {
        logger.error("Beschädigte students.dat konnte nicht verschoben werden", moveError);
      }
    }
  }

  public static synchronized void save() {
    Store store = new Store();
    store.records = new ArrayList<>(records);
    store.knownFiles = new HashMap<>(knownFiles);
    store.pendingXlsx = new HashMap<>(pendingXlsx);

    Path tmp = null;
    try {
      Files.createDirectories(STUDENTS_FILE.getParent());
      tmp = Files.createTempFile(STUDENTS_FILE.getParent(), "students-", ".tmp");
      try (ObjectOutputStream out = new ObjectOutputStream(
          new BufferedOutputStream(Files.newOutputStream(tmp)))) {
        out.writeObject(store);
      }
      try {
        Files.move(tmp, STUDENTS_FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(tmp, STUDENTS_FILE, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      logger.error("Fehler beim Speichern der Studenten: {}", e.toString(), e);
    } finally {
      if (tmp != null) {
        try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
      }
    }
  }

  // ---------------------------------------------------------------- Export-Flag

  /** Nach erfolgreichem Export: "Neu"-Markierung entfernen (bzw. Studenten aufnehmen). */
  public static void markExported(Student student) {
    if (student == null || student.getNumber() <= 0) return;
    StudentRecord rec = find(student.getNumber()).orElse(null);
    if (rec == null) {
      rec = new StudentRecord(student.getNumber(), student.getFullName());
      rec.setPdfPath(AppManager.getFilePath());
      rec.setXlsxPath(AppManager.getThemeFilePath());
      rec.setNew(false);
      rec.setExportedAt(System.currentTimeMillis());
      records.add(rec);
    } else {
      rec.setNew(false);
      rec.setExportedAt(System.currentTimeMillis());
      touch(rec);
    }
    save();
  }

  // ---------------------------------------------------------------- Löschen

  /**
   * Entfernt einen Studenten aus der Liste. Die Dateien selbst bleiben unangetastet.
   * Der automatische Scan beim Start nimmt ihn nicht wieder auf; „Studenten aktualisieren“
   * oder erneutes Hineinziehen der PDF fügt ihn wieder hinzu. Eine verknüpfte XLSX wird
   * vorgemerkt, damit sie dann automatisch wieder zugeordnet wird.
   */
  public static void remove(StudentRecord rec) {
    if (rec == null || !records.remove(rec)) return;
    highlight.remove(rec.getNumber());
    if (rec.hasXlsx()) {
      pendingXlsx.put(rec.getNumber(), new PendingXlsx(rec.getXlsxPath(), rec.getName()));
    }
    logger.info("Student {} aus der Liste entfernt", rec);
    save();
  }

  // ---------------------------------------------------------------- Einlesen

  /**
   * PDF/XLSX in den konfigurierten Ordnern einlesen.
   * @param onlyChanged true beim Programmstart (nur neue/geänderte Dateien),
   *                    false bei „Studenten aktualisieren“ (alles, auch gelöschte Studenten kommen zurück)
   */
  public static void scanFoldersAsync(boolean onlyChanged, Consumer<Result> onDone) {
    List<String> folders = List.of(getPdfFolder(), getXlsxFolder());
    submit(() -> {
      List<Path> files = new ArrayList<>();
      for (String folder : new LinkedHashSet<>(folders)) {
        if (folder.isBlank()) continue;
        files.addAll(collect(List.of(Path.of(folder))));
      }
      return onlyChanged ? files.stream().filter(StudentRegistry::isChanged).toList() : files;
    }, onDone);
  }

  /** Per Drag &amp; Drop übergebene Dateien/Ordner einlesen (immer, auch wenn bekannt). */
  public static void ingestAsync(List<File> dropped, Consumer<Result> onDone) {
    List<Path> roots = dropped.stream().map(File::toPath).toList();
    submit(() -> collect(roots), onDone);
  }

  public static boolean isSupported(File f) {
    return f.isDirectory() || isSupported(f.toPath());
  }

  private static boolean isSupported(Path p) {
    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
    return !n.startsWith("~$") && (n.endsWith(".pdf") || n.endsWith(".xlsx"));
  }

  private static boolean isChanged(Path p) {
    Long known = knownFiles.get(key(p));
    return known == null || known != lastModified(p);
  }

  private interface FileSource { List<Path> get() throws IOException; }

  private static void submit(FileSource source, Consumer<Result> onDone) {
    runningJobs++;
    busy.set(true);
    worker.submit(() -> {
      List<Parsed> parsed = new ArrayList<>();
      String fatal = null;
      try {
        for (Path p : source.get()) parsed.add(parse(p));
      } catch (Exception ex) {
        logger.error("Einlesen der Studentendateien fehlgeschlagen", ex);
        fatal = ex.getMessage();
      }
      String fatalMsg = fatal;
      Platform.runLater(() -> {
        Result result = apply(parsed);
        if (fatalMsg != null) result.errors.add(fatalMsg);
        runningJobs--;
        busy.set(runningJobs > 0);
        if (onDone != null) onDone.accept(result);
      });
    });
  }

  private static List<Path> collect(List<Path> roots) throws IOException {
    List<Path> out = new ArrayList<>();
    for (Path root : roots) {
      if (Files.isDirectory(root)) {
        try (Stream<Path> s = Files.walk(root, SCAN_DEPTH)) {
          s.filter(Files::isRegularFile).filter(StudentRegistry::isSupported).forEach(out::add);
        }
      } else if (Files.isRegularFile(root) && isSupported(root)) {
        out.add(root);
      }
    }
    return out;
  }

  /** Läuft im Hintergrund: liest Matrikelnummer und Namen aus einer Datei. */
  private static Parsed parse(Path p) {
    Parsed r = new Parsed();
    r.path = p.toAbsolutePath().normalize();
    r.lastModified = lastModified(p);
    r.pdf = p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf");
    try {
      if (r.pdf) {
        Student s = TranscriptPdfReader.read(p);
        if (s.getNumber() > 0) {
          r.students.put(s.getNumber(), s.getFullName());
        } else {
          r.error = p.getFileName() + ": keine Student ID gefunden";
        }
      } else {
        r.students.putAll(ThemeReader.readStudents(p));
        if (r.students.isEmpty()) r.error = p.getFileName() + ": keine VGU ID gefunden";
      }
    } catch (Exception ex) {
      logger.warn("Datei konnte nicht gelesen werden: {} ({})", p, ex.toString());
      r.error = p.getFileName() + ": " + ex.getMessage();
    }
    return r;
  }

  /** Läuft im FX-Thread: Ergebnisse in die Liste übernehmen. PDFs zuerst, dann XLSX. */
  private static Result apply(List<Parsed> parsed) {
    Result result = new Result();
    Set<Integer> addedNow = new HashSet<>();
    List<Parsed> ordered = new ArrayList<>(parsed);
    ordered.sort(Comparator.comparing((Parsed p) -> !p.pdf));

    for (Parsed f : ordered) {
      knownFiles.put(key(f.path), f.lastModified);
      if (f.error != null) {
        result.errors.add(f.error);
        continue;
      }
      String path = f.path.toString();
      for (Map.Entry<Integer, String> e : f.students.entrySet()) {
        int number = e.getKey();
        String name = e.getValue();
        StudentRecord rec = find(number).orElse(null);
        if (f.pdf) {
          if (rec == null) {
            rec = new StudentRecord(number, name);
            rec.setPdfPath(path);
            PendingXlsx pending = pendingXlsx.remove(number);
            if (pending != null) {
              rec.setXlsxPath(pending.path);
              if (!pending.name.isBlank()) rec.setName(pending.name);
            }
            highlight.add(number);
            addedNow.add(number);
            records.add(rec);
            result.added++;
          } else if (!path.equals(rec.getPdfPath())) {
            rec.setPdfPath(path);
            if (!rec.hasXlsx() && name != null && !name.isBlank()) rec.setName(name);
            touch(rec);
            if (!addedNow.contains(number)) result.updated++;
          }
        } else {
          if (rec == null) {
            pendingXlsx.put(number, new PendingXlsx(path, name));
          } else if (!path.equals(rec.getXlsxPath())) {
            rec.setXlsxPath(path);
            // Name aus der XLSX ist sauberer (vietnamesische Zeichen) als aus dem PDF
            if (name != null && !name.isBlank()) rec.setName(name);
            touch(rec);
            if (!addedNow.contains(number)) result.updated++;
          }
        }
      }
    }
    if (!parsed.isEmpty()) save();
    logger.info("Studenten eingelesen: {} Dateien, {} neu, {} aktualisiert, {} Fehler",
        parsed.size(), result.added, result.updated, result.errors.size());
    return result;
  }

  /** Ersetzt den Eintrag in der Liste durch sich selbst, damit die Liste neu zeichnet. */
  private static void touch(StudentRecord rec) {
    int idx = records.indexOf(rec);
    if (idx >= 0) records.set(idx, rec);
  }

  private static String key(Path p) {
    return p.toAbsolutePath().normalize().toString();
  }

  private static long lastModified(Path p) {
    try {
      return Files.getLastModifiedTime(p).toMillis();
    } catch (IOException e) {
      return -1;
    }
  }

  // ---------------------------------------------------------------- Typen

  public static final class Result {
    public int added;
    public int updated;
    public final List<String> errors = new ArrayList<>();
  }

  private static final class Parsed {
    Path path;
    long lastModified;
    boolean pdf;
    final Map<Integer, String> students = new LinkedHashMap<>();
    String error;
  }

  private static final class PendingXlsx implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    final String path;
    final String name;
    PendingXlsx(String path, String name) {
      this.path = path;
      this.name = name == null ? "" : name;
    }
  }

  private static final class Store implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    ArrayList<StudentRecord> records = new ArrayList<>();
    HashMap<String, Long> knownFiles = new HashMap<>();
    HashMap<Integer, PendingXlsx> pendingXlsx = new HashMap<>();
  }
}
