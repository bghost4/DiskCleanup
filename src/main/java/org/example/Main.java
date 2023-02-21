package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MainWindow mw = new MainWindow();
        primaryStage.setScene(new Scene(mw));
        primaryStage.setTitle("Disk Cleanup Tool");
        primaryStage.show();
    }
}
