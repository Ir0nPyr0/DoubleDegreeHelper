package calculator.doubledegreehelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Wandelt die erzeugten DOCX-Dateien in PDF um.
 * Unter Windows über Microsoft Word (beste Treue zur Vorlage), sonst bzw. als
 * Fallback über LibreOffice.
 */
public final class PdfConverter {
  private static final Logger logger = LoggerFactory.getLogger(PdfConverter.class);
  private static final long TIMEOUT_SECONDS = 180;

  private static final String WORD_SCRIPT = String.join("\r\n",
      "$ErrorActionPreference = 'Stop'",
      "$word = New-Object -ComObject Word.Application",
      "$word.Visible = $false",
      "$word.DisplayAlerts = 0",
      "try {",
      "  foreach ($f in $args) {",
      "    $doc = $word.Documents.Open($f, $false, $true)",
      "    try { $doc.ExportAsFixedFormat([IO.Path]::ChangeExtension($f, '.pdf'), 17) }",
      "    finally { $doc.Close(0) }",
      "  }",
      "} finally { $word.Quit() }");

  private PdfConverter() {}

  /** Erzeugt zu jeder DOCX eine PDF im selben Ordner und liefert die PDF-Pfade. */
  public static List<Path> convert(List<Path> docxFiles) throws IOException {
    if (docxFiles.isEmpty()) return List.of();
    List<String> problems = new ArrayList<>();

    if (isWindows()) {
      try {
        convertWithWord(docxFiles);
        if (allExist(docxFiles)) return pdfPaths(docxFiles);
        problems.add("Word hat keine PDF erzeugt");
      } catch (Exception ex) {
        logger.warn("PDF über Word fehlgeschlagen: {}", ex.toString());
        problems.add("Word: " + ex.getMessage());
      }
    }

    String soffice = findLibreOffice();
    if (soffice != null) {
      try {
        convertWithLibreOffice(soffice, docxFiles);
        if (allExist(docxFiles)) return pdfPaths(docxFiles);
        problems.add("LibreOffice hat keine PDF erzeugt");
      } catch (Exception ex) {
        logger.warn("PDF über LibreOffice fehlgeschlagen: {}", ex.toString());
        problems.add("LibreOffice: " + ex.getMessage());
      }
    } else {
      problems.add("LibreOffice nicht gefunden");
    }

    throw new IOException("PDF konnte nicht erstellt werden. Dafür wird Microsoft Word oder LibreOffice benötigt.\n"
        + String.join("\n", problems));
  }

  private static void convertWithWord(List<Path> files) throws IOException, InterruptedException {
    Path script = Files.createTempFile("ddh-docx2pdf-", ".ps1");
    try {
      // BOM, damit PowerShell 5 das Skript als UTF-8 liest
      Files.writeString(script, "﻿" + WORD_SCRIPT, StandardCharsets.UTF_8);
      List<String> cmd = new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-NonInteractive",
          "-ExecutionPolicy", "Bypass", "-File", script.toString()));
      files.forEach(f -> cmd.add(f.toAbsolutePath().toString()));
      run(cmd);
    } finally {
      Files.deleteIfExists(script);
    }
  }

  private static void convertWithLibreOffice(String soffice, List<Path> files) throws IOException, InterruptedException {
    Path outDir = files.get(0).toAbsolutePath().getParent();
    List<String> cmd = new ArrayList<>(List.of(soffice, "--headless", "--norestore",
        "--convert-to", "pdf", "--outdir", outDir.toString()));
    files.forEach(f -> cmd.add(f.toAbsolutePath().toString()));
    run(cmd);
  }

  private static void run(List<String> cmd) throws IOException, InterruptedException {
    Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
    p.getOutputStream().close();
    // Ausgabe lesen, damit der Prozess nicht an einem vollen Puffer hängt
    Thread drain = new Thread(() -> {
      try {
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!out.isEmpty()) logger.info("PDF-Konvertierung: {}", out);
      } catch (IOException ignored) { }
    });
    drain.setDaemon(true);
    drain.start();
    if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
      p.destroyForcibly();
      throw new IOException("Zeitüberschreitung nach " + TIMEOUT_SECONDS + " s");
    }
    drain.join(2000);
    if (p.exitValue() != 0) {
      throw new IOException("Prozess beendet mit Code " + p.exitValue());
    }
  }

  private static String findLibreOffice() {
    List<String> candidates = new ArrayList<>();
    if (isWindows()) {
      for (String env : List.of("ProgramFiles", "ProgramFiles(x86)")) {
        String base = System.getenv(env);
        if (base != null) candidates.add(base + "\\LibreOffice\\program\\soffice.exe");
      }
    } else {
      candidates.addAll(List.of("/usr/bin/soffice", "/usr/local/bin/soffice",
          "/Applications/LibreOffice.app/Contents/MacOS/soffice"));
    }
    for (String c : candidates) {
      if (Files.isRegularFile(Path.of(c))) return c;
    }
    return null;
  }

  private static List<Path> pdfPaths(List<Path> docx) {
    return docx.stream().map(PdfConverter::pdfFor).toList();
  }

  private static boolean allExist(List<Path> docx) {
    return docx.stream().map(PdfConverter::pdfFor).allMatch(Files::isRegularFile);
  }

  private static Path pdfFor(Path docx) {
    String name = docx.getFileName().toString();
    int dot = name.lastIndexOf('.');
    return docx.resolveSibling((dot > 0 ? name.substring(0, dot) : name) + ".pdf");
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }
}
