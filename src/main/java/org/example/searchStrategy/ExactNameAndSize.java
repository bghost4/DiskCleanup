package org.example.searchStrategy;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.LongSpinnerValueFactory;
import org.example.StatItem;

import java.util.function.Predicate;

public class ExactNameAndSize extends StrategyBase {

    private final TextField txtName = new TextField();
    private final LongSpinnerValueFactory spnFactory = new LongSpinnerValueFactory();
    private final Spinner<Long> spnSize = new Spinner<>(spnFactory);
    private final Node settings;

    public StringProperty fileNameProperty() { return txtName.textProperty(); }
    public ObjectProperty<Long> fileSizeProperty() { return spnFactory.valueProperty(); }
    public String getFileName() { return fileNameProperty().getName(); }
    public void setFileName(String fileName) { fileNameProperty().set(fileName); }
    public long getFileSize() { return spnFactory.getValue(); }
    public void setFileSize(long l) { spnFactory.setValue(l); }

    public ExactNameAndSize() {
        spnSize.setEditable(true);

        VBox vb = new VBox();
        HBox hb1 = new HBox();
        hb1.getChildren().addAll(new Label("File Name: "),txtName);
        HBox hb2 = new HBox(new Label("File Size: "),spnSize);
        vb.getChildren().addAll(hb1,hb2);

        settings = vb;
    }

    public Node getSettings() {
        return settings;
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        return ti -> ti.getValue().p().getFileName().equals(txtName.getText()) && ti.getValue().length() == spnFactory.getValue().longValue();
    }

    @Override
    public String getName() {
        return "File Name and Size";
    }
}
