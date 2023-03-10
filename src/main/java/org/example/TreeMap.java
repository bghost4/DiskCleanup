package org.example;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class TreeMap extends StackPane {
    private final HashMap<TreeItem<StatItem>,Rectangle> pathToRect = new HashMap<>();
    private final HashMap<Rectangle,TreeItem<StatItem>> rectToPath = new HashMap<>();

    private final ObjectProperty<Supplier<Stream<TreeItem<StatItem>>>> selection = new SimpleObjectProperty<>();

    private final SimpleObjectProperty<Function<TreeItem<StatItem>, Paint>> typePainter = new SimpleObjectProperty<>((s) -> Color.LIGHTGRAY);

    private final SimpleObjectProperty<TreeItem<StatItem>> context = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<BiConsumer<MouseEvent,TreeItem<StatItem>>> mouseHandler = new SimpleObjectProperty<>();

    public final SimpleBooleanProperty busy = new SimpleBooleanProperty();


    private final Pane pUsage = new Pane();

    private final RectangleUpdateService rectangleUpdater = new RectangleUpdateService();

    private final EventHandler<MouseEvent> defaultMouseHandler = (mouseEvent) -> {
        if(mouseEvent.getSource() instanceof Rectangle rect) {
            TreeItem<StatItem> item = rectToPath.get(rect);
            if(item != null) {
                if (mouseHandler.get() != null) {
                    mouseHandler.get().accept(mouseEvent, item);
                    mouseEvent.consume();
                }
            }
        }
    };

    private final Service<Path> shadeMaker = new Service<>() {
        @Override
        protected Task<Path> createTask() {
            return new ShadeGenerator(pathToRect::get, selection.get().get(), pUsage.getWidth(), pUsage.getHeight());
        }
    };


    private final Service<List<Pair<TreeItem<StatItem>,Bound>>> treeMapPacker = new Service<>() {
        final RectPacker<TreeItem<StatItem>> packer = new RectPacker<>(ti -> ti.getValue().length());

        @Override
        protected Task<List<Pair<TreeItem<StatItem>, Bound>>> createTask() {

            return new Task<List<Pair<TreeItem<StatItem>, Bound>>>() {
                @Override
                protected List<Pair<TreeItem<StatItem>, Bound>> call() {
                    System.out.println("TreeMapPacker Started");
                    Bound parent = new Bound(0, 0, pUsage.getWidth(), pUsage.getHeight());
                    return recurse(parent, context.get()).toList();
                }

                protected Stream<Pair<TreeItem<StatItem>, Bound>> recurse(Bound space, TreeItem<StatItem> item) {
                    if (item.isLeaf()) {
                        return packer.pack(space, Stream.of(item));
                    } else {
                        return packer.pack(space, item.getChildren().stream()).flatMap(pair -> recurse(pair.b(), pair.a()));
                    }
                }

            };
        }
    };

    private final Service<Void> rectangleCreator = new Service<>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    System.out.println("Rectangle Creator Started");
                    List<Rectangle> items = TreeItemUtils.flatMapTreeItem(context.getValue())
                            .filter(TreeItemUtils::isRegularFile) //Files only
                            .map(ti -> {
                                if (pathToRect.get(ti) != null) {
                                    return pathToRect.get(ti);
                                }
                                Rectangle r = new Rectangle();
                                r.setFill(typePainter.get().apply(ti));
                                r.setStrokeType(StrokeType.INSIDE);
                                r.setStroke(Color.BLACK);
                                r.setStrokeWidth(1);
                                r.addEventFilter(MouseEvent.ANY, defaultMouseHandler);
                                pathToRect.put(ti, r);
                                rectToPath.put(r, ti);
                                return r;
                            }).toList();

                    Platform.runLater(() -> pUsage.getChildren().setAll(items));

                    return null;
                }

            };
        }
    };
    private final ObjectProperty<Path> shader = new SimpleObjectProperty<>();
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);

    //Complete Regeneration of treeMap
    private void generateTreeMap(TreeItem<StatItem> root) {
        if(!enabled.get()) { return; }
        pUsage.getChildren().clear();
        rectToPath.clear();
        pathToRect.clear();

        if(root != null) {
            if(rectangleUpdater.isRunning()) { rectangleUpdater.cancel(); }
            if(treeMapPacker.isRunning()) { treeMapPacker.cancel(); }
            if(rectangleCreator.isRunning()) { rectangleCreator.cancel(); }
            rectangleCreator.restart();
            }
    }

    public TreeMap() {
        super();

        //Keep progress indicator from getting mouse events
        ProgressIndicator spinnymajig = new ProgressIndicator();
        spinnymajig.setMouseTransparent(true);

        getChildren().add(pUsage);
        getChildren().add(new Group());
        getChildren().add(spinnymajig);

        //These events really need to kick off when the user is done dragging the window to size
            widthProperty().addListener( (ob,ov,nv) -> {
                pUsage.setPrefWidth(nv.doubleValue()-5);
                treeMapPacker.restart();
            });

            heightProperty().addListener( (ob,ov,nv) -> {
                pUsage.setPrefHeight(nv.doubleValue()-5);
                treeMapPacker.restart();
            });

        context.addListener((ob,ov,nv) -> {
            if(!enabled.get()) { return; }
            if(ov == null) {
                generateTreeMap(nv);
            } else {
                if(TreeItemUtils.flatMapTreeItem(ov).anyMatch(ti -> ti == nv)) {
                    treeMapPacker.restart();
                    if(shader.get() != null) {
                        shader.get().setVisible(false);
                    }
                } else {
                    generateTreeMap(nv);
                }
            }
        });

        rectangleUpdater.setLookupFunction(ti -> Optional.ofNullable(pathToRect.get(ti)) );
        rectangleCreator.setOnSucceeded(eh -> treeMapPacker.restart());

        shader.addListener((ob,ov,nv) -> getChildren().set(1, Objects.requireNonNullElseGet(nv, Group::new)));

        rectangleUpdater.colorPickerProperty().bind(typePainter);

        rectangleUpdater.setOnSucceeded(eh -> {
                if(selection.get() != null && selection.get().get().findAny().isPresent()) {
                    shadeMaker.restart();
                }
            }
        );

        treeMapPacker.setOnSucceeded(eh -> rectangleUpdater.setPackingOrder(treeMapPacker.getValue()));

        context.addListener((ob,ov,nv) -> {
            if(nv != null ) { refresh(); }
        });

        mouseHandler.addListener((ob,ov,nv) -> {
            if(nv != null ) { refresh(); }
        });

        typePainter.addListener((ob,ov,nv) -> {
            if(nv != null ) { refresh(); }
        });

        shadeMaker.setOnSucceeded(eh -> getChildren().set(1,shadeMaker.getValue()));

        selection.addListener( (ob,ov,nv) ->
        {
            if(!enabled.get()) { return; }
            if(nv != null) {
               shadeMaker.restart();
            } else {
                pUsage.getChildren().remove(shader.get());
            }
        });

        List<ReadOnlyBooleanProperty> items =
                List.of(rectangleCreator.runningProperty(),
                        rectangleUpdater.runningProperty(),
                        treeMapPacker.runningProperty(),
                        shadeMaker.runningProperty()
                );

        spinnymajig.visibleProperty().bind(
                Bindings.createBooleanBinding(() -> items.stream().anyMatch(BooleanExpression::getValue),
                        items.get(0),
                        items.get(1),
                        items.get(2),
                        items.get(3)
                        )
        );

        List<ObservableValue<Boolean>> lstStatus = List.of(
                    treeMapPacker.runningProperty(),
                    rectangleCreator.runningProperty(),
                    rectangleUpdater.runningProperty(),
                    shadeMaker.runningProperty()
            );

        busy.bind(Bindings.createBooleanBinding(() -> lstStatus.stream().anyMatch(ObservableValue::getValue),lstStatus.toArray(new Observable[]{})));

        enabled.addListener((ob,ov,nv) -> {
            if(nv) {
                generateTreeMap(context.get());
            } else {
                pUsage.getChildren().clear();
                System.gc();
            }
        });

    }

    public ReadOnlyBooleanProperty busyProperty() { return busy; }

    public void setSelection(Supplier<Stream<TreeItem<StatItem>>> s) {
        selection.set(s);
    }

    public void setContext(TreeItem<StatItem> me) {
        context.set(me);
    }

    public void refresh() {
        if(!enabled.get()) { return; }
        treeMapPacker.restart();
    }

    public void rebuild() {
        if(!enabled.get()) { return; }
        generateTreeMap(context.get());
    }

    public void clearSelection() {
        if(!enabled.get()) { return; }
        selection.set(null);
        shader.set(null);
    }

    public Function<TreeItem<StatItem>, Paint> getTypePainter() {
        return typePainter.get();
    }

    public SimpleObjectProperty<Function<TreeItem<StatItem>, Paint>> typePainterProperty() {
        return typePainter;
    }

    public void setTypePainter(Function<TreeItem<StatItem>, Paint> typePainter) {
        this.typePainter.set(typePainter);
        rectangleUpdater.restart();
    }

    public TreeItem<StatItem> getContext() {
        return context.get();
    }

    public SimpleObjectProperty<TreeItem<StatItem>> contextProperty() {
        return context;
    }

    public BiConsumer<MouseEvent,TreeItem<StatItem>> getMouseHandler() {
        return mouseHandler.get();
    }

    public SimpleObjectProperty<BiConsumer<MouseEvent,TreeItem<StatItem>>> mouseHandlerProperty() {
        return mouseHandler;
    }

    public void setMouseHandler(BiConsumer<MouseEvent,TreeItem<StatItem>> mouseHandler) {
        this.mouseHandler.set(mouseHandler);
    }


    public BooleanProperty enabledProperty() {
        return enabled;
    }
}
