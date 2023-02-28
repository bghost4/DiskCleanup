package org.example.searchStrategy;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeTableView;
import org.example.StatItem;
import org.example.TreeMap;

import java.nio.file.attribute.UserPrincipal;
import java.util.Optional;

public interface DataSupplier {

    ObservableList<String> getStrategies();
    Optional<StrategyBase> getStrategyByName(String name);
    ObservableList<String> fileExtensions();
    ObservableList<String> fileTypes();

    ObservableList<UserPrincipal> fileOwners();

    TreeTableView<StatItem> getTreeView();
    TreeMap getTreeMap();

}
