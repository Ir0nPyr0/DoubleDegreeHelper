package calculator.doubledegreehelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Merkt sich die letzten Fehlermeldungen dieser Sitzung, damit sie im Hilfe-Fenster
 * noch einmal nachgelesen (und kopiert) werden können.
 */
public final class HelpLog {
  private static final int MAX = 30;
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final Deque<String> entries = new ArrayDeque<>();

  private HelpLog() {}

  public static synchronized void add(String message) {
    if (message == null || message.isBlank()) return;
    entries.addFirst(LocalTime.now().format(TIME) + "  " + message.strip());
    while (entries.size() > MAX) entries.removeLast();
  }

  public static synchronized void addAll(String prefix, List<String> messages) {
    for (String m : messages) add(prefix == null ? m : prefix + m);
  }

  /** Neueste zuerst. */
  public static synchronized List<String> recent() {
    return new ArrayList<>(entries);
  }

  public static synchronized void clear() {
    entries.clear();
  }
}
