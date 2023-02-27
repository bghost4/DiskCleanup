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

public class FileExtStrategy extends StrategyBase {

    ComboBox<String> cboFileExt = new ComboBox<>();
    HBox hb = new HBox();

    @Override
    public Node getSettings() {
        return hb;
    }

    public FileExtStrategy(ObservableList<String> ext) {
        hb.getChildren().addAll(new Label("File Extension"),cboFileExt);
        setExtensions(ext);
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        return ti -> ti.getValue().ext().equals(cboFileExt.getValue());
    }

    @Override
    public String getName() {
        return "File Extension";
    }

    public List<String> getExtensions() { return cboFileExt.getItems(); }
    public void setExtensions(ObservableList<String> extensions) { cboFileExt.setItems(extensions); }
    public String getSelectedExtension() { return cboFileExt.getValue(); }
    public void setSelectedExtension(String extension) { cboFileExt.setValue(extension); }


}
