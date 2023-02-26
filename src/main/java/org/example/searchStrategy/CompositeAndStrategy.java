package org.example.searchStrategy;

import javafx.scene.control.TreeItem;
import org.example.StatItem;

import java.util.function.Predicate;

public class CompositeAndStrategy extends CompositeStrategy {

    public CompositeAndStrategy() {
        super();

        lblSubName.setText("And");

    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        Predicate<TreeItem<StatItem>> pA = a.get().getPredicate();
        Predicate<TreeItem<StatItem>> pB = b.get().getPredicate();
        if(tglInvertA.isSelected()) {
            pA = pA.negate();
        }
        if(tglInvertB.isSelected()) {
            pB = pB.negate();
        }
        return pA.and(pB);
    }

    @Override
    public String getName() {
        return "And";
    }
}
