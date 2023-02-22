package org.example;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainWindow extends VBox {

    @FXML
    private Label lStatus;

    @FXML
    private Pane pUsageView;

    @FXML
    private TableView<FileTypeSizeCount> tblStats;

    @FXML
    private TreeTableView<StatItem> ttFileView;

    @FXML
    private TreeTableColumn<StatItem, String> ttcName;

    @FXML
    private TreeTableColumn<StatItem, Long> ttcSize;

    @FXML
    private TableColumn<FileTypeSizeCount,Long> tcCount,tcSize;
    @FXML
    private TableColumn<FileTypeSizeCount,String> tcType;

    @FXML
    private ProgressBar pbMemUsage;

    private Thread genTreeMapThread;

    private Double notSelectedOpacity = 0.35;

    //TODO make this a prefrence
    private final Executor exec = Executors.newFixedThreadPool(2);


    private final ScheduledService<Void> svc = new ScheduledService<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    double percent = 1 - ((double)Runtime.getRuntime().freeMemory() / (double)Runtime.getRuntime().totalMemory());
                    String text = String.format(
                            "%s Free of %s",
                            FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()),
                            FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory())
                    );
                    Platform.runLater(() -> {
                        pbMemUsage.setProgress(percent);
                        lStatus.setText(text);
                    });
                    return null;
                }
            };
        }
    };


    RectPacker<TreeItem<StatItem>,Pair<TreeItem<StatItem>,Rectangle>> packer;

    record Pair<A,B> (A a,B b) { }

    private final HashMap<TreeItem<StatItem>,Rectangle> pathToRect = new HashMap<>();
    private final HashMap<Rectangle,TreeItem<StatItem>> rectToPath = new HashMap<>();

    private final HashMap<String,Color> typeColor = new HashMap<>();

    private record FileTypeSizeCount (String type,long size,long count) {
        public static FileTypeSizeCount fromPath(Path p) {
            if(Files.isDirectory(p)) {
                return new FileTypeSizeCount("<Directory>",p.toFile().length(),1);
            } else {
                String type = FilenameUtils.getExtension(p.toString());
                if(type.isBlank() || type.isEmpty()) {
                    type = "<Typeless>";
                }
                return new FileTypeSizeCount(type, p.toFile().length(), 1);
            }
        }
    }

    private static Color randomColor(Random r) {
        return Color.rgb(r.nextInt(0,255),r.nextInt(0,255),r.nextInt(0,255));
    }

    @FXML
    public void onScanFolder(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        File startDir = dc.showDialog(null);

        pUsageView.getChildren().clear();
        rectToPath.clear();
        pathToRect.clear();
        //tblStats.getItems().clear();
        //ttFileView.setRoot(null);

        if(startDir.isDirectory()) {
            beginDirectoryScan(startDir.toPath());
        }
    }

    @FXML
    private void onFindDuplicates(ActionEvent e) {

    }

    @FXML
    private void onGraphTypeUsage(ActionEvent e) {

    }

    private void delete(TreeItem<StatItem> item) {
        TreeItem<StatItem> parent = item.getParent();
        if(parent != null) {
            if(ttFileView.getSelectionModel().getSelectedItem() == item) {
                ttFileView.getSelectionModel().select(item.getParent());
            }
            parent.getChildren().remove(item);

            //update parent size
            recalcChildrenRecursive(parent);

            generateTreeMap();
        } else {
            //TODO display message about removing a root node
        }
    }

    private void recalcChildrenRecursive(TreeItem<StatItem> start) {
        long childrenSize = start.getChildren().stream().mapToLong(c -> c.getValue().length()).sum();
        start.setValue(start.getValue().update(childrenSize));
        if(start.getParent() != null) {
            recalcChildrenRecursive(start.getParent());
        }
    }

    private void beginDirectoryScan(Path rootPath) {
            ttFileView.setRoot(buildTree(StatItem.empty(rootPath)));
    }

    private void generateTreeMap() {
        pUsageView.getChildren().clear();
        rectToPath.clear();
        pathToRect.clear();

        if(ttFileView.getRoot() != null) {
            Runnable  t = () -> generateTreeMap(new Bound(0, 0,pUsageView.getWidth(),pUsageView.
                getHeight()),List.of(ttFileView.getRoot()));
            if(genTreeMapThread != null && genTreeMapThread.isAlive()) {
                genTreeMapThread.interrupt();
            }
            genTreeMapThread = new Thread(t);
            genTreeMapThread.start();
        }

    }

    private void generateTreeMap(Bound b,List<TreeItem<StatItem>> items) {
            if(Thread.currentThread().isInterrupted()) { return; }
            packer.pack(b, items).forEach(
                    p -> {
                        if(Files.isDirectory(p.a.getValue().p()) && p.a.getChildren().size() > 0) {
                            p.b.setFill(Color.TRANSPARENT); //force override Color
                            generateTreeMap(fromRect(p.b),p.a.getChildren());
                        }
                    }
            );
        }


    private static Bound fromRect(Rectangle r) {
        return new Bound(r.getX(),r.getY(),r.getWidth(),r.getHeight());
    }

    private TreeItem<StatItem> buildTree(StatItem si) {
        TreeItem<StatItem> me = new TreeItem<>(si);

        try {
            me.getChildren().addAll(Files.walk(si.p(), 1)
                    .filter(p -> !p.equals(si.p()))
                    .map(p -> Files.isDirectory(p) ? new TreeItem<>(StatItem.empty(p)) : new TreeItem<>(new StatItem(p, false, p.toFile().length()))).toList());

        } catch(IOException e) {
            //todo say something useful
            e.printStackTrace();
        }
        me.setExpanded(true);



        NestedTask<FileScannerTask> nt = new NestedTask<>(exec,
                me.getChildren().stream().filter(ti -> Files.isDirectory(ti.getValue().p()))
                        .map(FileScannerTask::new).collect(Collectors.toList()));

        nt.getDependants().forEach(fst -> fst.setOnSucceeded(eh -> {
            TreeItem<StatItem> childItem = fst.getParent();
            try {
                List<TreeItem<StatItem>> subChildren = fst.get();
                long total = subChildren.stream().mapToLong(i -> i.getValue().length() ).sum();
                childItem.setValue(childItem.getValue().update(total));
                childItem.getChildren().addAll(subChildren);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }));

        nt.setOnSucceeded(eh -> {
            me.getChildren().sort(Comparator.comparingLong((TreeItem<StatItem> ti) -> ti.getValue().length()).reversed());

            me.setValue(me.getValue().update(
                    me.getChildren().stream().mapToLong((TreeItem<StatItem> i) -> i.getValue().length()).sum()
            ));

            generateFileTypeLegend();
            generateTreeMap();

        });
        exec.execute(nt);

        return me;
    }

    @FXML
    private void initialize() {
        ttFileView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        createTreeContextMenu();
        ttcName.setMaxWidth(1f * Integer.MAX_VALUE * 80);
        ttcSize.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        ttcName.setCellValueFactory( vf -> new ReadOnlyStringWrapper(vf.getValue().getValue().p().getFileName().toString()) );
        ttcSize.setCellValueFactory( vf -> new ReadOnlyObjectWrapper<>( vf.getValue().getValue().length()) );
        ttcSize.setCellFactory(vf -> new TreeTableCell<>(){
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if(item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(FileUtils.byteCountToDisplaySize(item));
                }
            }
        });
        ttcName.setCellFactory(vf -> new TreeTableCell<>(){
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(!empty && item != null) {
                    StatItem subItem = this.getTableRow().getItem();
                    if(subItem != null && subItem.isProcesing()) {
                        ProgressIndicator pi = new ProgressIndicator();
                        pi.setMaxWidth(16);
                        pi.setMaxHeight(16);
                        setGraphic(pi);
                    } else {
                        setGraphic(null);
                    }
                    setText(item);
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        });

        tcSize.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().size) );
        tcSize.setCellFactory(cdf -> new TableCell<>(){
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if(item == null || empty) {
                    setText(null);
                } else {
                    setText(FileUtils.byteCountToDisplaySize(item));
                }
            }
        });
        tcCount.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().count) );
        tcType.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().type) );
        tcType.setCellFactory(cdf -> new TableCell<>(){
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(!empty && item != null) {
                    setText(item);
                    Rectangle r = new Rectangle(16, 16);
                    r.setFill(typeColor.get(item));
                    setGraphic(r);
                } else {
                    setText(null); setGraphic(null);
                }
            }
        });

        packer = new RectPacker<>(
                t -> t.getValue().length(),
                (ti, b) -> {
                    Rectangle r = new Rectangle(b.x(), b.y(), b.width(), b.height());

                    if (Files.isDirectory(ti.getValue().p())) {
                        r.setFill(Color.TRANSPARENT);
                        r.setPickOnBounds(false); //hopefully keeps directories from catching mouse events
                    } else {
                        r.setFill(typeColor.get(getType(ti.getValue().p())));
                        Tooltip tt = new Tooltip(ttFileView.getRoot().getValue().p().relativize(ti.getValue().p()) + " (" + FileUtils.byteCountToDisplaySize(ti.getValue().length()) + ")");
                        Tooltip.install(r, tt);
                        r.setStrokeWidth(1);
                        r.setStroke(Color.BLACK);
                        r.setStrokeType(StrokeType.INSIDE);
                    }

                    pathToRect.put(ti, r);
                    rectToPath.put(r, ti);
                    Platform.runLater(() -> pUsageView.getChildren().add(r));
                    r.setOnMouseClicked(eh -> {

                            recursiveExpand(ti);
                            ttFileView.getSelectionModel().select(ti);
                            ttFileView.scrollTo(ttFileView.getRow(ti));

                        if( eh.getButton() == MouseButton.SECONDARY ) {
                            ContextMenu mnu = ttFileView.getContextMenu();
                            mnu.show(r,eh.getScreenX(),eh.getScreenY());
                        }
                    });
                    //System.out.println("Created Rectangle for: "+ti.getValue().toString());
                    return new Pair<>(ti, r);
                }
        );

        pUsageView.widthProperty().addListener(il -> generateTreeMap());
        pUsageView.heightProperty().addListener(il->generateTreeMap());

        //ttFileView.setContextMenu();

        ttFileView.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            if(ov != null) {
                Optional.ofNullable(pathToRect.get(ov)).ifPresent(r -> {
                    if(Files.isDirectory(ov.getValue().p())) {
                        r.toBack();
                    }
                    r.setStrokeWidth(2);
                    r.setStroke(Color.BLACK);
                });
            }
            if(nv != null) {
//                pathToRect.get(nv).toFront();
//                pathToRect.get(nv).setStroke(Color.RED);


                updateRects((ti -> isChildOf(nv,ti)), (t, r) -> r.setOpacity(1), (t, r) -> r.setOpacity(notSelectedOpacity));

                if(nv.getParent() != null) {
                    nv.getParent().setExpanded(true);
                }
                //The following makes this unusable!
                //it would be better if we could check the visibility of the TreeItem
                //and if it was not visible, scroll it to the middle so you can keep some context
                //ttFileView.scrollTo(ttFileView.getRow(nv));
            }
        });

        tblStats.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            ttFileView.getSelectionModel().clearSelection(); //clear selection from Tree View
            if(nv != null ) {
                //forceRectFill(ti -> getType(ti).equals(nv.type()), Color.LIGHTGRAY);
                updateRects(t -> getType(t).equals(nv.type()),(i,r) -> r.setOpacity(1),(i,r) -> r.setOpacity(notSelectedOpacity));
            } else {
                updateRects( t-> true,(i,r) -> r.setOpacity(1),(i,r) -> r.setOpacity(1));
            }
        });

        svc.setPeriod(Duration.seconds(5));
        svc.start();

    }

    boolean isChildOf(TreeItem<StatItem> haystack,TreeItem<StatItem> needle) {
        TreeItem<StatItem> last = needle;
        if(haystack == needle) { return true; }
        while(last.getParent() != null) {
            if(last.getParent() == haystack) {
                return true;
            }
            last = last.getParent();
        }
        return false;
    }

    void recursiveExpand(TreeItem<?> item) {
        if(item.getParent() == null) { return; }
        item.getParent().setExpanded(true);
        recursiveExpand(item.getParent());
    }

    private void createTreeContextMenu() {
        ContextMenu ctx = new ContextMenu();
        MenuItem miSystemOpen = new MenuItem("Open With System Viewer");
            miSystemOpen.setOnAction(eh -> openPath(ttFileView.getSelectionModel().getSelectedItem()));
        MenuItem miDeleteNode = new MenuItem("Delete (Not Really)");
            miDeleteNode.setOnAction(eh -> delete(ttFileView.getSelectionModel().getSelectedItem()));
        MenuItem miZoomInto = new MenuItem("Zoom Into");
            miZoomInto.setOnAction(eh -> zoomIn(ttFileView.getSelectionModel().getSelectedItem()));
        MenuItem miZoomOut = new MenuItem("Zoom Out");
            miZoomOut.setOnAction(eh -> zoomOut());
        MenuItem miZoomRoot = new MenuItem("Reset Zoom to TopLevel");
            miZoomRoot.setOnAction(eh -> zoomRoot());
        MenuItem miFindDuplicates = new MenuItem("Find Duplicates");
            miFindDuplicates.setOnAction(eh -> findDuplicates(ttFileView.getSelectionModel().getSelectedItem()));
        MenuItem miRebuildTree = new MenuItem("Rebuild Tree");
            miRebuildTree.setOnAction(eh -> generateTreeMap() );
        ctx.getItems().addAll(miSystemOpen,miDeleteNode,miRebuildTree,miZoomInto,miZoomOut,miZoomRoot,miFindDuplicates);
        ttFileView.setContextMenu(ctx);
    }

    private void updateRects(Predicate<TreeItem<StatItem>> matcher, BiConsumer<TreeItem<StatItem>,Rectangle> match, BiConsumer<TreeItem<StatItem>,Rectangle> nomatch) {
        flatMapTreeItem(ttFileView.getRoot())
                .filter(ti -> !Files.isDirectory(ti.getValue().p())) //Leave directories out of this they are special
                .forEach(ti -> {
                    Rectangle r = pathToRect.get(ti);
                    if(r != null ) {
                        if (matcher.test(ti)) {
                            match.accept(ti, r);
                        } else {
                            nomatch.accept(ti,r);
                        }
                    }
                });
    }

    private void findDuplicates(TreeItem<StatItem> selectedItem) {
    }

    private void zoomIn(TreeItem<StatItem> item) {
        ttFileView.setRoot(item);
        generateFileTypeLegend();
        generateTreeMap();
    }
    private void zoomOut() {
        TreeItem<StatItem> parent = ttFileView.getRoot().getParent();
        if(parent != null) {
            ttFileView.setRoot(parent);
            generateFileTypeLegend();
            generateTreeMap();
        }
    }

    private void zoomRoot() {
        TreeItem<StatItem> last = ttFileView.getRoot();
        while(true) {
            if(last.getParent() == null) {
                ttFileView.setRoot(last);
                break;
            } else {
                last = last.getParent();
            }
        }
        generateFileTypeLegend();
        generateTreeMap();
    }

    private void openPath(TreeItem<StatItem> item) {
        if(item == null) { return; }
        Path p = item.getValue().p();

            Runnable r = () -> {
                try {
                    Desktop.getDesktop().open(p.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            Thread openThread = new Thread(r);
            openThread.start();

    }

    String getType(Path p) {
        return FileTypeSizeCount.fromPath(p).type();
    }

    String getType(TreeItem<StatItem> item) {
        return getType(item.getValue().p());
    }
    String getType(StatItem item) {
        return getType(item.p());
    }

    void resetRectStroke() {
        rectToPath.keySet().forEach(r -> r.setStroke(Color.BLACK));
    }

    void generateFileTypeLegend() {
        tblStats.getItems().clear();
        tblStats.getSortOrder().clear();
        tcSize.setSortType(TableColumn.SortType.DESCENDING);
        tblStats.getSortOrder().add(tcSize);
        final Random r = new Random();
        tblStats.getItems().addAll(
            flatMapTreeItemUnwrap(ttFileView.getRoot()).filter(t -> Files.isRegularFile(t.p())).map(t -> FileTypeSizeCount.fromPath(t.p())).collect(
                    Collectors.toMap(FileTypeSizeCount::type, Function.identity(), (FileTypeSizeCount a, FileTypeSizeCount b) -> new FileTypeSizeCount(a.type, (a.size() + b.size), (a.count + b.count)))).values()
        );
        tblStats.getItems().sort(Comparator.comparingLong(FileTypeSizeCount::size).reversed());

        tblStats.getItems().forEach(i -> typeColor.put(i.type(),randomColor(r)));

    }

    <A> Stream<A> flatMapTreeItemUnwrap(TreeItem<A> item) {
        return Stream.concat(Stream.of(item.getValue()),item.getChildren().stream().flatMap(this::flatMapTreeItemUnwrap));
    }

    <A> Stream<TreeItem<A>> flatMapTreeItem(TreeItem<A> item) {
        return Stream.concat(Stream.of(item),item.getChildren().stream().flatMap(this::flatMapTreeItem));
    }

    public MainWindow() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mainwindow.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}