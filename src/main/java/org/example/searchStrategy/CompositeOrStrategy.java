package org.example.searchStrategy;

import javafx.scene.control.TreeItem;
import org.example.StatItem;

import java.util.function.Predicate;

public class CompositeOrStrategy extends CompositeStrategy {

    public CompositeOrStrategy(DataSupplier s) {
        super(s);

        lblSubName.setText("Or");

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
        return pA.or(pB);
    }

    @Override
    public String getName() {
        return "Or";
    }
}
