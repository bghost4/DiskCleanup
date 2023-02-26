package org.example.searchStrategy;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public abstract class CompositeStrategy extends StrategyBase {

    protected final SimpleObjectProperty<StrategyBase> a = new SimpleObjectProperty<StrategyBase>();
    protected final SimpleObjectProperty<StrategyBase> b = new SimpleObjectProperty<StrategyBase>();
    protected final Label lblSubName = new Label();

    private final VBox container = new VBox();
    private final HBox hba = new HBox();
    private final HBox hbb = new HBox();

    protected final Button btnChangeA = new Button("#");
    protected final Button btnChangeB = new Button("#");
    protected final ToggleButton tglInvertA = new ToggleButton("!");
    protected final ToggleButton tglInvertB = new ToggleButton("!");

    private final AnchorPane apA = new AnchorPane();
    private final AnchorPane apB = new AnchorPane();


    @Override
    public Node getSettings() {
        return container;
    }

    public CompositeStrategy() {
        HBox.setHgrow(apA,Priority.ALWAYS);
        HBox.setHgrow(apB,Priority.ALWAYS);
        hba.getChildren().addAll(tglInvertA,btnChangeA,apA);
        hbb.getChildren().addAll(tglInvertB,btnChangeB,apB);
        VBox.setMargin(hba,new Insets(0,0,0,10));
        VBox.setVgrow(hba, Priority.ALWAYS);
        VBox.setMargin(hbb,new Insets(0,0,0,10));
        VBox.setVgrow(hbb,Priority.ALWAYS);
        container.getChildren().addAll(lblSubName,hba,hbb);

        apA.getChildren().add(new Group());
        apB.getChildren().add(new Group());

        a.addListener((ob,ov,nv) -> {
            if(nv != null) {
                Node child = nv.getSettings();
                AnchorPane.setTopAnchor(child,0.0);
                AnchorPane.setBottomAnchor(child,0.0);
                AnchorPane.setLeftAnchor(child,0.0);
                AnchorPane.setRightAnchor(child,0.0);
                apA.getChildren().set(0,child);
            }
        });

        b.addListener((ob,ov,nv) -> {
            if(nv != null) {
                Node child = nv.getSettings();
                AnchorPane.setTopAnchor(child,0.0);
                AnchorPane.setBottomAnchor(child,0.0);
                AnchorPane.setLeftAnchor(child,0.0);
                AnchorPane.setRightAnchor(child,0.0);
                apB.getChildren().set(0,child);
            }
        });

        btnChangeA.setOnAction(eh -> {
            ChoiceDialog<StrategyBase> cd = new ChoiceDialog<>(
                    new FileNameStrategy(),new FileExtStrategy(),new CompositeAndStrategy()
            );
            cd.showAndWait().ifPresent(sb -> a.set(sb));
        });

        btnChangeB.setOnAction(eh -> {
            ChoiceDialog<StrategyBase> cd = new ChoiceDialog<>(
                    new FileNameStrategy(),new FileExtStrategy(),new CompositeAndStrategy()
            );
            cd.showAndWait().ifPresent(sb -> b.set(sb));
        });

    }

}
