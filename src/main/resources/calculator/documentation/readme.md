# DegreeCalculator – Dokumentation

## Vorwort
Der **DegreeCalculator** ist ein Werkzeug, das den Prozess der Anerkennung von Studienleistungen zwischen der Hochschule Heilbronn (HHN) und der Vietnamese-German University (VGU) vereinfacht.  
Die Anwendung liest die Notenübersicht *(Transcript.pdf)* sowie die Themenliste *(Thema.xlsx)*, ermöglicht die Zuordnung von VGU-Modulen zu ihren entsprechenden HHN-Moduläquivalenten und erstellt anschließend standardisierte Ausgabedokumente (Zeugnis, Urkunde, Diploma Supplement).

**Warum gibt es HHN- und VGU-Module?**  
Die Ausbildungspläne von HHN und VGU unterscheiden sich in Umfang, Nummerierung und teilweise auch in den Modulbezeichnungen. Damit dennoch eine transparente, nachvollziehbare Anerkennung erfolgen kann, werden:

* **HHN-Module** (»Main«-Module in der Software) als Referenz verwendet – sie bilden den offiziellen Rahmen des Abschlusszeugnisses.
* **VGU-Module** (»Compare«-Module) werden den HHN-Modulen zugeordnet, um Leistungen abzubilden, die Studierende an der VGU erbracht haben.

**Wozu dienen Kategorien?**  
Mit *Kategorien* lassen sich Module thematisch gruppieren (z.&nbsp;B. *Grundlagen*, *Wahlpflicht*, *Praxissemester*). Das erleichtert:

* die automatische Befüllung der Dokumentvorlagen.

---

## Inhaltsverzeichnis
1. [Wie installiere ich die neueste Version?](#wie-installiere-ich-die-neueste-version)
2. [Wie starte ich das Programm?](#wie-starte-ich-das-programm)
3. [Ersteinrichtung](#ersteinrichtung)
4. [Zuordnung zwischen HHN-Modulen und VGU-Modulen](#zuordnung-zwischen-hhn-modulen-und-vgu-modulen)
5. [Kategorien verwalten](#kategorien-verwalten)
6. [Studierende bearbeiten (Transcript.pdf & Thema.xlsx)](#studierende-bearbeiten-transcriptpdf--themaxlsx)
7. [Felder bearbeiten](#felder-bearbeiten)
8. [Fehlermeldungen](#fehlermeldungen)

---

## Wie installiere ich die neueste Version?

### Systemvoraussetzungen
* **Betriebssystem:** Windows 10/11 (getestet), macOS 11+ oder Linux (glibc 2.28+)
* **Festplattenspeicher:** ≥ 100 MB
* **Internetzugang:** zum Herunterladen des Installers

### Installation für Endanwender
1. **Installer herunterladen**  
   Laden Sie die aktuelle `DegreeCalculator-Setup.exe` von der Projekt-Release-Seite herunter.
2. **Setup ausführen**  
   Doppelklicken Sie auf die EXE und folgen Sie dem Assistenten.  
   Nach Abschluss befindet sich die Anwendung unter  
   `C:\Program Files\DegreeCalculator\DegreeCalculator.exe` und ist im **Startmenü** registriert.
3. **Programm starten**  
   Öffnen Sie das Windows-Startmenü, geben Sie "DegreeCalculator" ein und drücken Sie **Enter**.  
   Alternativ können Sie per Rechtsklick *Mehr → Dateipfad öffnen* auswählen und eine **Desktop-Verknüpfung** erstellen.

Beim ersten Start erscheint ein **Einstellungs-Dialog**, in dem Sie die wichtigsten Speicherorte definieren.

---

## Wie starte ich das Programm?

### Windows-Nutzer
1. Drücken Sie die **Windows-Taste** und tippen Sie "DegreeCalculator".
2. Klicken Sie auf das gefundene Programm-Icon – die Anwendung öffnet sich.

### Desktop-Verknüpfung erstellen
* Öffnen Sie den Installationsordner (`C:\Program Files\DegreeCalculator`).
* Ziehen Sie `DegreeCalculator.exe` mit der rechten Maustaste auf den Desktop und wählen Sie "Verknüpfungen hier erstellen".

---

## Ersteinrichtung

Nach dem ersten Start klicken Sie auf den **Settings-Button** (Zahnrad-Symbol) und folgen diesen Schritten:

### 1. HHN-Module anlegen (Tab »HHN-Module«)
1. Unten – mittig – auf **Hinzufügen** klicken. Es erscheint eine neue Zeile.
2. Füllen Sie die Spalten der Zeile wie folgt aus:
   | Spalte | Eingabe | Bedeutung |
   |-------|---------|-----------|
   | 1 | `S`, `W` oder `S/W` | Semester: Sommer, Winter bzw. beide |
   | 2 | Modulnummer | Offizielle HHN-Modulnummer |
   | 3 | Modulname   | Vollständiger Titel des Moduls |
   | 4 | ECTS        | Kreditpunkte |
   | 5 | Kategorie  | Wird erst später gewählt (siehe Schritt 2) |

> **Hinweis:** Spalte 5 bleibt zunächst leer, da noch keine Kategorien existieren.

### 2. Kategorien definieren (Tab »Kategorien«)
1. Unten auf **Hinzufügen** klicken, es erscheint eine neue Zeile.
2. Spalten ausfüllen:
   | Spalte | Eingabe | Bedeutung |
   |-------|---------|-----------|
   | 1 | Kategoriename | z.&nbsp;B. *Grundlagen* |
   | 2 | Wichtigkeit  | Ganzzahl – je **kleiner** die Zahl, desto **höher** wird die Kategorie im Zeugnis gelistet |

Legen Sie alle benötigten Kategorien an und kehren Sie anschließend zum Tab **HHN-Module** zurück, um in Spalte 5 die passende Kategorie für jedes Modul auszuwählen.

### 3. Vergleichs-Module anlegen (Tab »Vergleichs-Module«)
1. Unten auf **Hinzufügen** klicken, es erscheint wieder eine neue Zeile.
2. Spalten ausfüllen:
   | Spalte | Eingabe | Bedeutung |
   |-------|---------|-----------|
   | 1 | `S`, `W` oder `S/W` | Semesterangabe |
   | 2 | Modulnummer | VGU-Modulnummer |
   | 3 | Modulname   | Titel des VGU-Moduls |
   | 4 | ECTS        | Kreditpunkte |
   | 5 | HHN-Modul(e) | Wählen Sie **ein oder mehrere** passende HHN-Module aus (gemäß Mapping-Dokument) |

### 4. Speichern
Nachdem alle Module und Kategorien eingetragen wurden, klicken Sie auf **Speichern**.  
Die Konfiguration wird in Ihrer Projektdatei gesichert – Sie können jetzt mit dem normalen Arbeitsablauf (PDF/Excel importieren usw.) fortfahren.

---

## Kategorien verwalten

1. **Kategorie hinzufügen**  
   *Kategorien → Neu* → Namen eingeben → Priorität festlegen
2. **Priorität ändern**  
   Klicken Sie auf das Prioritätsfeld einer Kategorie, um die Reihenfolge in den Ausgabedokumenten zu beeinflussen. Je kleiner die Zahl ist, desto höher ist der eintrag beim zeugnis. so sollte bsp.("Master Thesis with Colloquium") immer eine hohe zahl haben da sie ganz unten sein sollte.
3. **Kategorie löschen**  
   *Kategorien → Löschen*. Bereits zugeordnete Module verlieren dadurch ihre Kategorie.

---

## Studierende bearbeiten / Arbeitsablauf nach einer erfolgreichen Einrichtung(Transcript.pdf & Thema.xlsx)

1. **Transcript importieren**  
   *Datei → Transcript laden* und die PDF auswählen.
2. **Thema importieren**  
   *Datei → Thema laden* und die Excel-Datei wählen.
3. **Daten prüfen & ergänzen**  
   Es Öffnet sich ein fenster mit vielen auswahl möglichkeiten. der Großteil wird vom Programm übernommen. Es sollte trotzdem noch alles einmal händisch Überprüft werden.
   Was auf jedenfall gepürft werden muss sind die Vietnamesischen Zeichen. Auch sollte der Nachname noch eingetragen werden der sich aktuell noch bei dem Vornamen liegt.

> ⚠️ **Hinweis:** Bei fehlerhaften PDFs kann es vorkommen, dass einzelne Module nicht erkannt werden. Nutzen Sie in diesem Fall die manuelle Moduleingabe.

---

## Word Vorlagen Bearbeiten

Die generierten Word-/PDF-Vorlagen enthalten Platzhalterklammern (`{}`), z. bsp. `{FIRST_NAME}`. Diese werden beim Export automatisch ersetzt.  

Es gibt folgende Platzhalter:
    {NUMBER} oder {NR} -> Matrikelnummer
    {FIRSTN} oder {FIRSTNAME} -> Vorname
    {LASTN} oder {LASTNAME} -> Nachname
    {NAME} ->  Vor und Nachname
    {TITLE} -> Titel "Mr" oder "Mrs"
    {BDATE} ->  Geburtsdatum
    {BPLACE} -> Geburtsort
    {DELIVERDATE} -> Deliver Datum
    {GRANT} ->  bestanden Datum
    {TEXTGRADE} -> bsp. very good (1.4)
    {GRADETEXT} ->  bsp 1.4 (very good)
    {PRESIDENT} -> President
    {CHAIRPERSON} ->  Chairperson

Für die Tabelle im Zeugnis gibt es folgende Platzhalter:

    {CATEGORY} oder {CAT} oder {CATNAME} -> Kategorienname
    {CAVG} oder {AVG} -> Durschnitt eines modulenkategorie
    {CECT} oder {ECTS} -> Alle Ects einer Kategorie
    {SW} oder {SEM} -> Semester also st oder wt
    {Y} oder {YEAR}-> Year des moduls / kategorie abgeschlossen

---

## Fehlermeldungen

| Meldung (Alert) | Typische Ursache | Lösung |
|-----------------|-----------------|--------|
| **Fehler beim Einlesen der PDF** | Defektes oder nicht unterstütztes `Transcript.pdf` | PDF prüfen; ggf. neue Version exportieren. |
| **Fehler beim Einlesen der XLSX** | Excel-Datei `Thema.xlsx` enthält unerwartetes Format | Spaltenüberschriften und Dateiformat kontrollieren. |
| **Ungültiger Speicherordner** | Pfad für die Ausgabe-Dokumente wurde nicht gesetzt oder ist nicht erreichbar | In den Einstellungen einen gültigen Ordner auswählen. |
| **Fehler beim Erstellen der Word-Datei** | Word-Vorlage beschädigt oder Zielordner schreibgeschützt | Vorlage ersetzen bzw. Schreibrechte prüfen. |

---

© Hochschule Heilbronn & Vietnamese-German University – 2024
