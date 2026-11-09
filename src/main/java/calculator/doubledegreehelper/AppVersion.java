package calculator.doubledegreehelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Versionsnummer aus build.gradle (wird beim Build in version.properties eingesetzt). */
public final class AppVersion {
  private static final String VERSION = load();

  private AppVersion() {}

  public static String get() {
    return VERSION;
  }

  private static String load() {
    try (InputStream in = AppVersion.class.getResourceAsStream("/calculator/version.properties")) {
      if (in != null) {
        Properties p = new Properties();
        p.load(in);
        String v = p.getProperty("version", "").trim();
        if (!v.isEmpty() && !v.contains("$")) return v;
      }
    } catch (IOException ignored) {
      // Fallback unten
    }
    Package pkg = AppVersion.class.getPackage();
    String impl = pkg != null ? pkg.getImplementationVersion() : null;
    return impl != null ? impl : "dev";
  }
}
