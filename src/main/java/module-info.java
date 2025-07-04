module calculator.doubledegreehelper {

    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires javafx.media;
    requires org.slf4j;

    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    requires commons.math3;
    requires org.apache.commons.compress;
    requires org.apache.xmlbeans;
    requires java.prefs;
    requires org.apache.pdfbox;
    requires org.apache.commons.logging;
    requires java.desktop;

    opens calculator.doubledegreehelper           to javafx.fxml;
    opens calculator.doubledegreehelper.controller to javafx.fxml;

    exports calculator.doubledegreehelper;
}