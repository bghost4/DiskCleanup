package org.example.searchStrategy;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.LongSpinnerValueFactory;
import org.example.StatItem;

import java.util.function.Predicate;

public class ExactNameAndSize extends CompositeAndStrategy {

    FileNameStrategy fns = new FileNameStrategy();
    FileSizeStrategy fss = new FileSizeStrategy();

    public StringProperty fileNameProperty() { return fns.fileNamePatternProperty(); }
    public ObjectProperty<Long> fileSizeProperty() { return fss.fileSizeProperty(); }

    public ExactNameAndSize(DataSupplier dataSupplier) {
        super(dataSupplier);

        fss.operatorProperty().set(FileSizeStrategy.Operator.EQUAL);
        fns.setNameMatchStrategy(FileNameStrategy.NameMatchStrategy.CASE_SENSITIVE);

        a.set(fns);
        b.set(fss);
    }

    @Override
    public String getName() {
        return "File Name and Size";
    }
}
