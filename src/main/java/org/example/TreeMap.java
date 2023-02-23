package org.example;

import com.sun.source.tree.Tree;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import org.w3c.dom.css.Rect;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TreeMap extends StackPane {
    private final HashMap<TreeItem<StatItem>,Rectangle> pathToRect = new HashMap<>();
    private final HashMap<Rectangle,TreeItem<StatItem>> rectToPath = new HashMap<>();

    private final RectPacker<TreeItem<StatItem>> packer;

    private final ObservableList<TreeItem<StatItem>> selection = FXCollections.observableArrayList();

    private final SimpleObjectProperty<Function<String, Paint>> typePainter = new SimpleObjectProperty<>((s) -> Color.LIGHTGRAY);

    private final Pane pUsage = new Pane();

    private final SimpleObjectProperty<TreeItem<StatItem>> context = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<BiConsumer<MouseEvent,TreeItem<StatItem>>> mouseHandler = new SimpleObjectProperty<>();

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

    private final ProgressIndicator spinnymajig = new ProgressIndicator();



    private final Service<List<Pair<TreeItem<StatItem>,Bound>>> treeMapPacker = new Service<List<Pair<TreeItem<StatItem>,Bound>>>() {
        @Override
        protected Task<List<Pair<TreeItem<StatItem>,Bound>>> createTask() {
            return new Task<List<Pair<TreeItem<StatItem>,Bound>>>() {
                @Override
                protected List<Pair<TreeItem<StatItem>, Bound>> call() throws Exception {
                    System.out.println("TreeMapPacker Started");
                    Bound parent = new Bound(0, 0, pUsage.getWidth(), pUsage.getHeight());
                    return recurse(parent, context.get()).toList();
                }

                protected Stream<Pair<TreeItem<StatItem>,Bound>> recurse(Bound space, TreeItem<StatItem> item) {
                    if(item.isLeaf()) {
                        return packer.pack(space,Stream.of(item));
                    } else {
                        return packer.pack(space,item.getChildren().stream()).flatMap(pair -> recurse(pair.b(),pair.a()));
                    }
                }

            };
        }
    };

    private final Service<Void> rectangleCreator = new Service<>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    System.out.println("Retangle Creator Started");
                    List<TreeItem<StatItem>> items = TreeItemUtils.flatMapTreeItem(context.getValue()).filter(TreeItem::isLeaf).toList();

                    items.forEach( ti -> {
                        Rectangle r = new Rectangle();
                        r.setFill(typePainter.get().apply(TreeItemUtils.getType(ti)));
                        r.setStrokeType(StrokeType.INSIDE);
                        r.setStrokeWidth(1);
                        r.addEventFilter(MouseEvent.ANY,defaultMouseHandler);
                        pathToRect.put(ti,r);
                        rectToPath.put(r,ti);
                        }
                    );

                    System.out.println("Created "+items.size()+" Rectangles");

                    Platform.runLater(() ->  pUsage.getChildren().setAll(rectToPath.keySet()) );

                    return null;
                }

                };
        }
    };
    private Path shader;


    //Complete Regeneration of treeMap
    private void generateTreeMap(TreeItem<StatItem> root) {
        pUsage.getChildren().clear();
        rectToPath.clear();
        pathToRect.clear();
        if(shader != null) { getChildren().remove(shader); shader = null; }

        if(root != null) {
            if(rectangleCreator.isRunning()) {
                rectangleCreator.cancel();
            } else {
                getChildren().add(spinnymajig);
            }
            rectangleCreator.restart();
        }
    }

    public TreeMap() {
        super();

        getChildren().add(pUsage);

        packer = new RectPacker<>(ti -> ti.getValue().length());

        System.out.println("Initial Width: "+getWidth());
        System.out.println("Initial Height: "+getHeight());

        widthProperty().addListener( (ob,ov,nv) -> {
            System.out.println("Width Changed: "+nv);
                pUsage.setPrefWidth(nv.doubleValue()-5);
            treeMapPacker.restart();
        });

        heightProperty().addListener( (ob,ov,nv) -> {
            System.out.println("Height Changed: "+nv);
                pUsage.setPrefHeight(nv.doubleValue()-5);
            treeMapPacker.restart();
        });

        context.addListener((ob,ov,nv) -> {
            System.out.println("Context Changed: "+nv.getValue().p());
            if(ov == null) {
                generateTreeMap(nv);
            } else {
                if(TreeItemUtils.flatMapTreeItem(ov).anyMatch(ti -> ti == nv)) {
                    treeMapPacker.restart();
                } else {
                    generateTreeMap(nv);
                }
            }
        });

        rectangleUpdater.setLookupFunction(ti -> Optional.ofNullable(pathToRect.get(ti)));

        rectangleCreator.setOnSucceeded(eh -> {
            treeMapPacker.restart();
        });

        treeMapPacker.setOnSucceeded(eh -> {
            System.out.println("Tree Map is repacked");
            rectangleUpdater.setPackingOrder(treeMapPacker.getValue());
            //getChildren().remove(spinnymajig);
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
        generateTreeMap(context.get());
    }

    public void setSelection(TreeItem<StatItem> nv) {
        selection.clear();
        selection.addAll(
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

    List<PathElement> createRect(Rectangle r,boolean remove) {
        return createRect(r.getX(),r.getY(),r.getWidth(),r.getHeight(),remove);
    }

    List<PathElement> createRect(double x,double y, double w, double h,boolean remove) {
        if(remove) {
            return List.of(
                    new MoveTo(x+w, y+h),
                    new VLineTo(y),
                    new HLineTo(x),
                    new VLineTo(y+h),
                    new HLineTo(x+w) );
        } else {
            return List.of(
                    new MoveTo(x, y),
                    new HLineTo(w),
                    new VLineTo(h),
                    new HLineTo(x),
                    new VLineTo(y));
        }
    }

    private Path createShaderForSelection() {
        Path p = new Path();

        p.setFill(Color.WHITE);
        p.setStroke(null);
        p.setOpacity(0.75);
        p.setFillRule(FillRule.EVEN_ODD);

        p.getElements().addAll(createRect(0,0,pUsage.getWidth(),pUsage.getHeight(),false));
        selection.stream().flatMap(ti -> Optional.ofNullable(pathToRect.get(ti)).stream()).forEach(r -> {
            p.getElements().addAll(createRect(r,true));
        });

        //wish there was a way to make path transparent to mouse events
        p.setMouseTransparent(true);

        if(shader != null) {
            getChildren().remove(shader);
        }
        shader = p;
        getChildren().add(shader);
        return p;
    }

    public void clearSelection() {
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
