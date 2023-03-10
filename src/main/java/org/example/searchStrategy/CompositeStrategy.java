package org.example.searchStrategy;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.BiConsumer;

public abstract class CompositeStrategy extends StrategyBase {

    protected final SimpleObjectProperty<StrategyBase> a = new SimpleObjectProperty<>();
    protected final SimpleObjectProperty<StrategyBase> b = new SimpleObjectProperty<>();
    protected final Label lblSubName = new Label();
    protected final DataSupplier supplier;
    protected final StrategyChoiceDialog strategySelector;

    private final VBox container = new VBox();

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

    public CompositeStrategy(DataSupplier dataSupplier) {
        HBox.setHgrow(apA,Priority.ALWAYS);
        HBox.setHgrow(apB,Priority.ALWAYS);
        HBox hba = new HBox();
        hba.getChildren().addAll(tglInvertA,btnChangeA,apA);
        HBox hbb = new HBox();
        hbb.getChildren().addAll(tglInvertB,btnChangeB,apB);
        VBox.setMargin(hba,new Insets(0,0,0,10));
        VBox.setVgrow(hba, Priority.ALWAYS);
        VBox.setMargin(hbb,new Insets(0,0,0,10));
        VBox.setVgrow(hbb,Priority.ALWAYS);
        container.getChildren().addAll(lblSubName, hba, hbb);

        this.supplier = dataSupplier;
        this.strategySelector = new StrategyChoiceDialog(supplier);

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



        btnChangeA.setOnAction(eh -> strategySelector.showAndWait().ifPresent(n -> handler.accept(n,a)));
        btnChangeB.setOnAction(eh -> strategySelector.showAndWait().ifPresent(n -> handler.accept(n,b)));

    }


    public static final BiConsumer<StrategyBase,ObjectProperty<StrategyBase>> handler = (nv, prop) -> {
        if(nv instanceof CompositeStrategy cs) {
            StrategyBase old = prop.get();
            if(old != null) {
                if(old instanceof CompositeStrategy ocs) {
                    prop.set(cs);
                    cs.strategyAProperty().set(ocs.strategyAProperty().get());
                    cs.strategyBProperty().set(ocs.strategyBProperty().get());
                } else {
                    prop.set(cs);
                    cs.strategyAProperty().set(old);
                }
            } else {
                prop.set(nv);
            }
        } else {
            if(prop.get() != null && prop.get() instanceof CompositeStrategy ocs) {
                System.out.println("Old Property Was Composite");
                if(ocs.strategyAProperty().get() != null && ocs.strategyAProperty().get().getName().equals(nv.getName())) {
                    System.out.println("\tNew Strategy Name Matched Field A, Using Setting from Old A");
                    prop.set(ocs.strategyAProperty().get());
                } else if (ocs.strategyBProperty().get() != null && ocs.strategyBProperty().get().getName().equals(nv.getName())) {
                    System.out.println("\tNew Strategy Name Matched Field B, Using Setting from Old B");
                    prop.set(ocs.strategyBProperty().get());
                } else {
                    System.out.println("New Strategy("+nv.getName()+") did not match: A("+ocs.strategyAProperty().get().getName()+") or B("+ocs.strategyBProperty().get().getName()+")");
                    prop.set(nv);
                }
            } else {
                prop.set(nv);
            }
        }
    };


    public ObjectProperty<StrategyBase> strategyAProperty() { return a; }
    public ObjectProperty<StrategyBase> strategyBProperty() { return b; }


}
