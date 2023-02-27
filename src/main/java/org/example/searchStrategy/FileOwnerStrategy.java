package org.example.searchStrategy;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.example.StatItem;

import java.nio.file.attribute.UserPrincipal;
import java.util.function.Predicate;

public class FileOwnerStrategy extends StrategyBase {

    private final ComboBox<UserPrincipal> cboUserSelect = new ComboBox<>();
    private final HBox settingsNode = new HBox();

    public FileOwnerStrategy(DataSupplier ds) {
        super();

        cboUserSelect.setItems(ds.fileOwners());
        settingsNode.getChildren().addAll(new Label("File Owner: "),cboUserSelect);

    }

    @Override
    public Node getSettings() {
        return settingsNode;
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        return ti -> ti.getValue().owner().equals(cboUserSelect.getValue());
    }

    @Override
    public String getName() {
        return "File Owner";
    }
}
