package org.example.searchStrategy;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.example.StatItem;
import org.example.TreeItemUtils;

import java.util.List;
import java.util.function.Predicate;

public class FileTypeStrategy extends StrategyBase {

    ComboBox<String> cboFileTypes = new ComboBox<>();
    HBox hb = new HBox();

    @Override
    public Node getSettings() {
        return hb;
    }

    public FileTypeStrategy() {
        hb.getChildren().addAll(new Label("File Type"),cboFileTypes);
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        return ti -> TreeItemUtils.getType(ti).equals(cboFileTypes.getValue());
    }

    @Override
    public String getName() {
        return "File Type";
    }

    public List<String> getTypes() { return cboFileTypes.getItems(); }
    public void setTypes(List<String> types) { cboFileTypes.getItems().setAll(types); }
    public String getSelectedType() { return cboFileTypes.getValue(); }
    public void setSelectedType(String type) { cboFileTypes.setValue(type); }


}
