package org.example.searchStrategy;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class StrategyChoiceDialog extends Dialog<StrategyBase> {

    private final ComboBox<StrategyBase> cboStrategy;

    public StrategyChoiceDialog(DataSupplier s) {
        super();

        cboStrategy = new ComboBox<>();
        cboStrategy.setItems(s.getStrategies());

        HBox hb = new HBox();
        hb.getChildren().addAll(new Label("Select Strategy"),cboStrategy);
        getDialogPane().setContent(hb);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        setResultConverter(eh -> {
            if(eh == ButtonType.OK) {
                return cboStrategy.getValue();
            } else {
                return null;
            }
        });
    }




}
