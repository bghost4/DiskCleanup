package org.example.searchStrategy;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.example.LongSpinnerValueFactory;
import org.example.StatItem;

import java.util.function.Predicate;

public class FileSizeStrategy extends StrategyBase {

    private final LongSpinnerValueFactory svf = new LongSpinnerValueFactory();
    private final Spinner<Long> spnFileSize = new Spinner<>(svf);

    enum Operator { EQUAL,LESS_THAN,GREATER_THAN,LESS_THAN_EQUAL,GREATER_THAN_EQUAL }
    public ObjectProperty<Long> fileSizeProperty() { return svf.valueProperty(); }

    ComboBox<Operator> cboOperator = new ComboBox<>();
    public ObjectProperty<Operator> operatorProperty() { return cboOperator.valueProperty(); }

    HBox settings = new HBox();

    @Override
    public Node getSettings() {
        return settings;
    }

    public FileSizeStrategy() {
        super();

        cboOperator.getItems().setAll((Operator.values()));
        settings.getChildren().addAll(new Label("File Size: "),cboOperator,spnFileSize);
        spnFileSize.setEditable(true);
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        Operator o = cboOperator.getValue();
        switch(o) {
            case EQUAL:
                return ti -> ti.getValue().length() == svf.getValue();
            case LESS_THAN:
                return ti -> ti.getValue().length() < svf.getValue();
            case LESS_THAN_EQUAL:
                return ti -> ti.getValue().length() <= svf.getValue();
            case GREATER_THAN:
                return ti -> ti.getValue().length() > svf.getValue();
            case GREATER_THAN_EQUAL:
                return ti -> ti.getValue().length() >= svf.getValue();
            default:
                System.err.println("Error Unable to Figure Out Operator");
                return ti -> false;
        }
    }

    @Override
    public String getName() {
        return "File Size";
    }
}
