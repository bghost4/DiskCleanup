package org.example.searchStrategy;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import org.example.InstantStringConverter;
import org.example.StatItem;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import java.util.function.Predicate;

public class FileTimeStrategy extends StrategyBase {

    public enum TimeType { CREATION_TIME,LAST_MODIFIED_TIME }
    public enum SearchType { BEFORE,AFTER,BETWEEN }

    ComboBox<TimeType> cboTimeType = new ComboBox<>();
    ComboBox<SearchType> cboSearchType = new ComboBox<>();
    TextField txtFirst = new TextField(),txtLast = new TextField();

    private final Node settingsNode;

    public FileTimeStrategy() {
        super();

        InstantStringConverter isc = new InstantStringConverter();

        cboTimeType.getItems().addAll(TimeType.values());
        cboSearchType.getItems().addAll(SearchType.values());

        Instant now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();
        txtFirst.setText(isc.toString(now));
        txtLast.setText(isc.toString(now));

        cboSearchType.valueProperty().addListener(il -> {
            if(cboSearchType.getValue().equals(SearchType.BETWEEN)) {
                txtLast.setDisable(false);
            } else {
                txtLast.setDisable(true);
            }
        });

        HBox hb = new HBox();
        hb.getChildren().addAll(cboTimeType,cboSearchType,txtFirst,txtLast);
        settingsNode = hb;
    }

    @Override
    public Node getSettings() {
        return settingsNode;
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        SearchType st = cboSearchType.getValue();
        Function<TreeItem<StatItem>, Instant> extractor;

        InstantStringConverter isc = new InstantStringConverter();

        Instant iFirst,iLast;
        try {
            iFirst = isc.fromString(txtFirst.getText());
            iLast = isc.fromString(txtLast.getText());


            extractor = switch(cboTimeType.getValue()) {
                case CREATION_TIME -> (ti) -> ti.getValue().createTime();
                case LAST_MODIFIED_TIME -> (ti) -> ti.getValue().modTime();
            };

            return switch (st) {
                case BEFORE -> ti -> extractor.apply(ti).isBefore(iFirst);
                case AFTER -> ti -> extractor.apply(ti).isAfter(iFirst);
                case BETWEEN -> ti -> extractor.apply(ti).isAfter(iFirst) && extractor.apply(ti).isBefore(iLast);
            };

        } catch( DateTimeParseException dtpe ) {
            return ti -> false;
        }
    }

    @Override
    public String getName() {
        return "File Time";
    }
}
