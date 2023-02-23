package org.example;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

import java.util.function.Predicate;

public abstract class StrategyBase {
    //This returns a UI component used for settings of this Strategy
    public abstract Node getSettings();
    public abstract Predicate<TreeItem<StatItem>> getPredicate();
    public abstract String getName();

    protected final SimpleBooleanProperty configValid = new SimpleBooleanProperty(true);

    public boolean isConfigValid() { return configValid.get(); }
    public ReadOnlyBooleanProperty isConfigValidProperty() { return configValid; }

    @Override
    public String toString() {
        return getName();
    }
}
