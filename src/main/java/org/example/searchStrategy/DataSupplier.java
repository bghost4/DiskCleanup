package org.example.searchStrategy;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeTableView;
import org.example.StatItem;
import org.example.TreeMap;

import java.nio.file.attribute.UserPrincipal;

public interface DataSupplier {

    ObservableList<StrategyBase> getStrategies();
    ObservableList<String> fileExtensions();
    ObservableList<String> fileTypes();

    ObservableList<UserPrincipal> fileOwners();

    TreeTableView<StatItem> getTreeView();
    TreeMap getTreeMap();

}
