package calculator.doubledegreehelper;

import calculator.doubledegreehelper.module.ModuleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SaveHandler {
  private static final Logger logger = LoggerFactory.getLogger(SaveHandler.class);

  private static final Path DATA_DIR;
  private static final Path DATA_FILE;

  static {
    String base = System.getProperty("user.home");
    DATA_DIR  = Paths.get(base, "DoubleDegreeHelperData");
    DATA_FILE = DATA_DIR.resolve("modules.dat");
  }

  public void saveData() {
    try {
      Files.createDirectories(DATA_DIR);
    } catch (IOException io) {
      logger.error("Kann Datenverzeichnis '{}' nicht anlegen: {}", DATA_DIR, io.toString());
      return;
    }

    List<ModuleItem> hhnCopy = new ArrayList<>(AppManager.getHhnModules());
    List<ModuleItem> cmpCopy = new ArrayList<>(AppManager.getCompareModules());

    try (ObjectOutputStream out = new ObjectOutputStream(
        new BufferedOutputStream(Files.newOutputStream(DATA_FILE)))) {
      out.writeObject(hhnCopy);
      out.writeObject(cmpCopy);
      out.writeObject(new ArrayList<>(AppManager.getCategories()));
      logger.info("Daten gespeichert nach {} ({} HHN, {} CMP, {} Kategorien)",
          DATA_FILE, hhnCopy.size(), cmpCopy.size(), AppManager.getCategories().size());
    } catch (IOException e) {
      logger.error("Fehler beim Speichern der Module: {}", e.toString(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public void loadData() {
    if (!Files.exists(DATA_FILE)) return;

    try (ObjectInputStream in = new ObjectInputStream(
        new BufferedInputStream(Files.newInputStream(DATA_FILE)))) {

      List<ModuleItem> hhnList = (List<ModuleItem>) in.readObject();
      List<ModuleItem> cmpList = (List<ModuleItem>) in.readObject();

      AppManager.clearAll();
      AppManager.clearCategories();

      hhnList.forEach(AppManager::addHhnModule);
      cmpList.forEach(AppManager::addCompareModule);

      @SuppressWarnings("unchecked")
      List<Category> catList = (List<Category>) in.readObject();
      catList.forEach(AppManager::addCategory);

    } catch (IOException | ClassNotFoundException ex) {
      logger.error("Fehler beim Laden der Module: {}", ex.toString(), ex);
    }
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
