package org.example.searchStrategy;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

public class ExactNameAndSize extends CompositeAndStrategy {

    final FileNameStrategy fns = new FileNameStrategy();
    final FileSizeStrategy fss = new FileSizeStrategy();

    public StringProperty fileNameProperty() { return fns.fileNamePatternProperty(); }
    public ObjectProperty<Long> fileSizeProperty() { return fss.fileSizeProperty(); }

    public ExactNameAndSize(DataSupplier dataSupplier) {
        super(dataSupplier);

        fss.operatorProperty().set(Comparison.EQUAL);
        fns.setNameMatchStrategy(FileNameStrategy.NameMatchStrategy.CASE_SENSITIVE);

        a.set(fns);
        b.set(fss);
    }

    @Override
    public String getName() {
        return "File Name and Size";
    }
}
