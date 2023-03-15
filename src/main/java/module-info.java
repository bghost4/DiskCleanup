open module DiskCleanup.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.slf4j;
    requires org.apache.tika.core;
    requires org.controlsfx.controls;
    requires java.desktop;
    requires sevenzipjbinding;

    exports org.example to javafx.graphics,javafx.fxml;
}
