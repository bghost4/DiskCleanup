package org.example;

import com.sun.source.tree.Tree;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.*;
import org.w3c.dom.css.Rect;

import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    private final SimpleObjectProperty<EventHandler<MouseEvent>> mouseHandler = new SimpleObjectProperty<>();

    private EventHandler<MouseEvent> clickHandler  = (mouseEvent) -> {
        if(mouseHandler.get() != null) { mouseHandler.get().handle(mouseEvent); }
    };

    private final ProgressIndicator spinnymajig = new ProgressIndicator();

    private final Service<Stream<Rectangle>> treeMapGenerator = new Service<>() {
        @Override
        protected Task<Stream<Rectangle>> createTask() {
            return new Task<Stream<Rectangle>>() {
                @Override
                protected Stream<Rectangle> call() throws Exception {
                    Bound parent = new Bound(0, 0,pUsage.getWidth(),pUsage.getHeight());
                    return recurse(parent,context.get())
                            .map(
                            (pair) -> {
                                TreeItem<StatItem> ti = pair.a();
                                Bound b = pair.b();

                                Rectangle r = new Rectangle(b.x(), b.y(), b.width(), b.height());

                                r.setFill(typePainter.get().apply(TreeItemUtils.getType(ti)));
                                Tooltip tt = new Tooltip(TreeItemUtils.relativize(context.get(),ti) + " (" + TreeItemUtils.getFriendlySize(ti) + ")");
                                Tooltip.install(r, tt);
                                r.setStrokeWidth(1);
                                r.setStroke(Color.BLACK);
                                r.setStrokeType(StrokeType.INSIDE);

                                pathToRect.put(ti, r);
                                rectToPath.put(r, ti);

                                return r;
                            }
                    );
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
    private Path shader;


    private void generateTreeMap(TreeItem<StatItem> root) {
        pUsage.getChildren().clear();
        rectToPath.clear();
        pathToRect.clear();

        if(root != null) {
            if(treeMapGenerator.isRunning()) {
                treeMapGenerator.cancel();
            } else {
                getChildren().add(spinnymajig);
            }
            treeMapGenerator.restart();
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
            refresh();
        });

        heightProperty().addListener( (ob,ov,nv) -> {
            System.out.println("Height Changed: "+nv);
                pUsage.setPrefHeight(nv.doubleValue()-5);
            refresh();
        });

        treeMapGenerator.setOnSucceeded(eh -> {
            System.out.println("Tree Map is Generated");
            treeMapGenerator.getValue().forEach(pair -> {
                pUsage.getChildren().add(pair);
            });
            getChildren().remove(spinnymajig);
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
        //Create an overlay that goes in the stackpane that somehow obscures any item not selected
        if(this.shader != null) {
            getChildren().remove(this.shader);
        }
        this.shader = createShader(pathToRect.get(nv));
        getChildren().add(shader);
    }

    public void setSelection(Predicate<TreeItem<StatItem>> predicate) {

        if(this.shader != null) {
            getChildren().remove(this.shader);
        }

        Path p = new Path();

        p.setFill(Color.BLACK);
        p.setStroke(null);
        p.setOpacity(0.75);
        p.setFillRule(FillRule.EVEN_ODD);

        p.getElements().addAll(createRect(0,0,pUsage.getWidth(),pUsage.getHeight(),false));



        this.shader = p;
        getChildren().add(shader);

    }

    public void setSelection(Collection<TreeItem<StatItem>> itemsToSelect) {
        Path p = new Path();

        p.setFill(Color.BLACK);
        p.setStroke(null);
        p.setOpacity(0.75);
        p.setFillRule(FillRule.EVEN_ODD);

        p.getElements().addAll(createRect(0,0,pUsage.getWidth(),pUsage.getHeight(),false));


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

    Path createShader(Rectangle selected) {
        Path p = new Path();

        p.setFill(Color.BLACK);
        p.setStroke(null);
        p.setOpacity(0.75);
        p.setFillRule(FillRule.EVEN_ODD);

        p.getElements().addAll(createRect(0,0,pUsage.getWidth(),pUsage.getHeight(),false));
        p.getElements().addAll(createRect(selected.getX(),selected.getY(),selected.getWidth(),selected.getHeight(),true));

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

    public EventHandler<MouseEvent> getMouseHandler() {
        return mouseHandler.get();
    }

    public SimpleObjectProperty<EventHandler<MouseEvent>> mouseHandlerProperty() {
        return mouseHandler;
    }

    public void setMouseHandler(EventHandler<MouseEvent> mouseHandler) {
        this.mouseHandler.set(mouseHandler);
    }

    public EventHandler<MouseEvent> getClickHandler() {
        return clickHandler;
    }

    public void setClickHandler(EventHandler<MouseEvent> clickHandler) {
        this.clickHandler = clickHandler;
    }
}
