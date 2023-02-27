package org.example.searchStrategy;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.example.StatItem;

import java.util.function.Predicate;

public class FileTimeStrategy extends StrategyBase {

    public enum TimeType { CREATION_TIME,LAST_MODIFIED_TIME }

    ComboBox<TimeType> cboTimeType = new ComboBox<>();
    DatePicker dpTime = new DatePicker();
    ComboBox<Comparison> cboCompareType = new ComboBox<>();

    private final Node settingsNode;

    public FileTimeStrategy() {
        super();

        cboTimeType.getItems().addAll(TimeType.values());
        cboCompareType.getItems().addAll(Comparison.values());

        HBox hb = new HBox();
        hb.getChildren().addAll(cboTimeType,cboCompareType,dpTime);
        settingsNode = hb;

    }

    @Override
    public Node getSettings() {
        return settingsNode;
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
//        Comparison c = cboCompareType.getValue();
//        Function<TreeItem<StatItem>, FileTime> extractor;
//        FileTime ft = new FileTime();
//        if(cboTimeType.getValue().equals(TimeType.CREATION_TIME)) {
//            extractor = (ti) -> ti.getValue().createTime();
//        } else {
//            extractor = (ti) -> ti.getValue().modTime();
//        }
//        switch (c) {
//            case EQUAL -> ti -> extractor.apply(ti).equals(dpTime.)
//        }
        return ti -> false;
    }

    @Override
    public String getName() {
        return "File Time";
    }
}
