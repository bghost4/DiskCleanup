open module DiskCleanup.main {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.tika.core;
    requires org.controlsfx.controls;
    requires java.datatransfer;
    requires java.desktop;
    requires org.slf4j;

    exports org.example to javafx.graphics,javafx.fxml;
}
