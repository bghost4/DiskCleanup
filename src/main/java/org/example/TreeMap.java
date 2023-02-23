package org.example;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreeMap extends StackPane {
    private final HashMap<TreeItem<StatItem>,Rectangle> pathToRect = new HashMap<>();
    private final HashMap<Rectangle,TreeItem<StatItem>> rectToPath = new HashMap<>();

    private final RectPacker<TreeItem<StatItem>> packer;

    private final ObservableList<TreeItem<StatItem>> selection = FXCollections.observableArrayList();

    private final SimpleObjectProperty<Function<String, Paint>> typePainter = new SimpleObjectProperty<>((s) -> Color.LIGHTGRAY);

    private final SimpleObjectProperty<TreeItem<StatItem>> context = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<BiConsumer<MouseEvent,TreeItem<StatItem>>> mouseHandler = new SimpleObjectProperty<>();

    private final Pane pUsage = new Pane();
    private final ProgressIndicator spinnymajig = new ProgressIndicator();

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

    private final Service<List<Pair<TreeItem<StatItem>,Bound>>> treeMapPacker = new Service<>() {
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
                                r.setFill(typePainter.get().apply(TreeItemUtils.getType(ti)));
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

    //Complete Regeneration of treeMap
    private void generateTreeMap(TreeItem<StatItem> root) {
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
        spinnymajig.setMouseTransparent(true);

        getChildren().add(pUsage);
        getChildren().add(new Group());
        getChildren().add(spinnymajig);

        packer = new RectPacker<>(ti -> ti.getValue().length());

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
            if(ov == null) {
                generateTreeMap(nv);
            } else {
                if(TreeItemUtils.flatMapTreeItem(ov).anyMatch(ti -> ti == nv)) {
                    treeMapPacker.restart();
                    shader.get().setVisible(false);
                } else {
                    generateTreeMap(nv);
                }
            }
        });

        rectangleUpdater.setLookupFunction(ti -> Optional.ofNullable(pathToRect.get(ti)) );
        rectangleUpdater.setOnRunning(eh -> spinnymajig.setVisible(true));
        rectangleCreator.setOnSucceeded(eh -> treeMapPacker.restart());

        shader.addListener((ob,ov,nv) -> {
            getChildren().set(1, Objects.requireNonNullElseGet(nv, Group::new));
        });

        rectangleUpdater.setOnSucceeded(eh -> {
                spinnymajig.setVisible(false);
                if(selection.size() > 0) {
                    createShaderForSelection();
                }
            }
        );

        treeMapPacker.setOnSucceeded(eh -> {
            System.out.println("Tree Map is repacked");
            rectangleUpdater.setPackingOrder(treeMapPacker.getValue());
        });

        context.addListener((ob,ov,nv) -> {
            if(nv != null ) { refresh(); }
        });

        mouseHandler.addListener((ob,ov,nv) -> {
            if(nv != null ) { refresh(); }
        });

        typePainter.addListener((ob,ov,nv) -> {
            if(nv != null ) { refresh(); }
        });

    }

    public void setContext(TreeItem<StatItem> me) {
        context.set(me);
    }

    public void refresh() {
        System.out.println("Refresh Called");
        treeMapPacker.restart();
    }

    public void setSelection(TreeItem<StatItem> nv) {
        selection.setAll(
            TreeItemUtils.flatMapTreeItem(nv).toList()
        );
        createShaderForSelection();
    }

    public void setSelection(Predicate<TreeItem<StatItem>> predicate) {
       selection.clear();
       selection.addAll(
        pathToRect.keySet().stream().filter(predicate).toList()
       );
       createShaderForSelection();
    }

    Stream<PathElement> createRemoveRect(Rectangle r) {
        return createRect(r.getX(),r.getY(),r.getWidth(),r.getHeight(),true);
    }

    Stream<PathElement> createRect(double x, double y, double w, double h, boolean remove) {
        if(remove) {
            return Stream.of(
                    new MoveTo(x+w, y+h),
                    new VLineTo(y),
                    new HLineTo(x),
                    new VLineTo(y+h),
                    new HLineTo(x+w) );
        } else {
            return Stream.of(
                    new MoveTo(x, y),
                    new HLineTo(w),
                    new VLineTo(h),
                    new HLineTo(x),
                    new VLineTo(y));
        }
    }

    private void createShaderForSelection() {
        Path p = new Path();
        //Shader Setup
        p.setFill(Color.WHITE);
        p.setStroke(null);
        p.setOpacity(0.75);
        p.setFillRule(FillRule.EVEN_ODD);
        p.setMouseTransparent(true);

        p.getElements().addAll(createRect(0,0,pUsage.getWidth(),pUsage.getHeight(),false).toList());

        p.getElements().addAll(
                selection.stream().flatMap(ti -> Optional.ofNullable(pathToRect.get(ti)).stream()).flatMap(this::createRemoveRect).toList()
        );

        shader.set(p);
    }

    public void clearSelection() {
        selection.clear();
        shader.set(null);
    }

    public Function<String, Paint> getTypePainter() {
        return typePainter.get();
    }

    public SimpleObjectProperty<Function<String, Paint>> typePainterProperty() {
        return typePainter;
    }

    public void setTypePainter(Function<String, Paint> typePainter) {
        this.typePainter.set(typePainter);
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
}
