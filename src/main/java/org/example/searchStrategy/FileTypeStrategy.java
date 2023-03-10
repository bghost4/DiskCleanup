package org.example.searchStrategy;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.example.StatItem;

import java.util.List;
import java.util.function.Predicate;

public class FileTypeStrategy extends StrategyBase {
    final ComboBox<String> cboFileTypes = new ComboBox<>();
    final HBox hb = new HBox();

    private final DataSupplier dataSupplier;

    public FileTypeStrategy(DataSupplier ds) {
        super();
        this.dataSupplier = ds;
        hb.getChildren().addAll(new Label("File Type"),cboFileTypes);
        cboFileTypes.setItems(ds.fileTypes());
    }

    @Override
    public Node getSettings() {
        return hb;
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        return ti -> ti.getValue().type().equals(cboFileTypes.getValue());
    }

    @Override
    public String getName() {
        return "File Type";
    }

    public List<String> getTypes() { return cboFileTypes.getItems(); }
    public void setTypes(ObservableList<String> types) { cboFileTypes.setItems(types); }
    public String getSelectedType() { return cboFileTypes.getValue(); }
    public void setSelectedType(String type) { cboFileTypes.setValue(type); }
}
