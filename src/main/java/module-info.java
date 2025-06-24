module calculator.doubledegreehelper {

    /* Java-FX & Logging */
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires javafx.media;
    requires org.slf4j;

    /* Apache-POI und seine weiteren automatischen Module */
    requires org.apache.poi.ooxml;      // zieht poi-ooxml-lite/full
    requires org.apache.poi.poi;        // Basis-POI
    requires commons.math3; // Apache Commons Math (Originalname)
    requires org.apache.commons.compress; // Pack-/Entpackroutinen
    requires org.apache.xmlbeans;
    requires java.prefs;
    requires org.apache.pdfbox;
    requires org.apache.commons.logging;
    requires java.desktop;

    /* eigene Packages für FXML öffnen */
    opens calculator.doubledegreehelper           to javafx.fxml;
    opens calculator.doubledegreehelper.controller to javafx.fxml;

    /* falls andere Pakete außerhalb verwendet werden */
    exports calculator.doubledegreehelper;
}