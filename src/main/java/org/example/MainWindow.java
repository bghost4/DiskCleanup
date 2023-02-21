package org.example;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
    private TreeTableView<TI> ttFileView;

    @FXML
    private TreeTableColumn<TI, String> ttcName;

    @FXML
    private TreeTableColumn<TI, Long> ttcSize;

    @FXML
    private TableColumn<FileTypeSizeCount,Long> tcCount,tcSize;
    @FXML
    private TableColumn<FileTypeSizeCount,String> tcType;

    private Thread genTreeMapThread;

    RectPacker<TreeItem<TI>,Pair<TreeItem<TI>,Rectangle>> packer;
    record Pair<A,B> (A a,B b) { }

    private final HashMap<TreeItem<TI>,Rectangle> pathToRect = new HashMap<>();
    private final HashMap<Rectangle,TreeItem<TI>> rectToPath = new HashMap<>();

    private final HashMap<String,Color> typeColor = new HashMap<>();

    record TI (Path p,boolean isProcesing,long length) {
        public static TI empty(Path p) {
            return new TI(p,true,0);
        }
        public TI update(long length) {
            return new TI(p,false,length);
        }
    }

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
        Color c = Color.rgb(r.nextInt(0,255),r.nextInt(0,255),r.nextInt(0,255));
        return c;
    }

    @FXML
    public void onScanFolder(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        File startDir = dc.showDialog(null);
        if(startDir.isDirectory()) {
            beginDirectoryScan(startDir.toPath());
        }
    }

    private void beginDirectoryScan(Path rootPath) {
            ttFileView.setRoot(buildTree(TI.empty(rootPath)));
            pathToRect.clear();
            rectToPath.clear();
    }

    private void generateTreeMap() {
        pUsageView.getChildren().clear();
        rectToPath.clear();
        pathToRect.clear();

        if(ttFileView.getRoot() != null) {
            Runnable  t = () -> {
                generateTreeMap(new Bound(0, 0,pUsageView.getWidth(),pUsageView.
                getHeight()),List.of(ttFileView.getRoot()));
            };
            if(genTreeMapThread != null && genTreeMapThread.isAlive()) {
                genTreeMapThread.interrupt();
            }
            genTreeMapThread = new Thread(t);
            genTreeMapThread.start();
        }

    }

    private void generateTreeMap(Bound b,List<TreeItem<TI>> items) {
            if(Thread.currentThread().isInterrupted()) { return; }
            packer.pack(b, items).forEach(
                    p -> {
                        if(Files.isDirectory(p.a.getValue().p) && p.a.getChildren().size() > 0) {
                            p.b.setFill(Color.TRANSPARENT); //force override Color
                            generateTreeMap(fromRect(p.b),p.a.getChildren());
                        }
                    }
            );
        }


    private static Bound fromRect(Rectangle r) {
        return new Bound(r.getX(),r.getY(),r.getWidth(),r.getHeight());
    }

    private TreeItem<TI> buildTree(TI p) {
        TreeItem<TI> me = new TreeItem(p);
        Executor exec = Executors.newFixedThreadPool(8);
        FileScannerTask fst = new FileScannerTask(me,exec,(item) -> {
        });
        exec.execute(fst);
        me.valueProperty().addListener( (ob,ov,nv) -> {
           if(!nv.isProcesing()) {
               generateFileTypeLegend();
               generateTreeMap();
           }
        });
        return me;
    }

    @FXML
    private void initialize() {
        ttFileView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        ttcName.setMaxWidth(1f * Integer.MAX_VALUE * 80);
        ttcSize.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        ttcName.setCellValueFactory( vf -> new ReadOnlyStringWrapper(vf.getValue().getValue().p().getFileName().toString()) );
        ttcSize.setCellValueFactory( vf -> new ReadOnlyObjectWrapper( vf.getValue().getValue().length) );
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
                    TI subItem = this.getTableRow().getItem();
                    if(subItem != null && subItem.isProcesing) {
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

        packer = new RectPacker<TreeItem<TI>,Pair<TreeItem<TI>,Rectangle>>(
                t -> t.getValue().length,
                (ti,b) -> {
                    Rectangle r = new Rectangle(b.x(),b.y(),b.width(),b.height());
                    if(Files.isDirectory(ti.getValue().p())) { r.setFill(Color.TRANSPARENT); } else {
                        r.setFill(typeColor.get(getType(ti.getValue().p())));
                    }
                    r.setStroke(Color.BLACK);
                    r.setStrokeWidth(1);
                    Tooltip tt = new Tooltip(ti.getValue().toString()+" ("+FileUtils.byteCountToDisplaySize(ti.getValue().length)+")");
                    Tooltip.install(r,tt);
                    r.setOnMouseClicked( eh -> {
                        setTreeSelection(ti);
                    });
                    pathToRect.put(ti,r);
                    rectToPath.put(r,ti);
                    Platform.runLater(() -> {
                        pUsageView.getChildren().add(r);
                    });

                    //System.out.println("Created Rectangle for: "+ti.getValue().toString());
                    return new Pair(ti,r);
                }
        );

        pUsageView.widthProperty().addListener(il -> generateTreeMap());
        pUsageView.heightProperty().addListener(il->generateTreeMap());

        ttFileView.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            if(ov != null) {
                pathToRect.get(ov).setStroke(Color.BLACK);
            }
            if(nv != null) {
                pathToRect.get(nv).toFront();
                pathToRect.get(nv).setStroke(Color.RED);
            }
        });

        tblStats.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            ttFileView.getSelectionModel().clearSelection(); //clear selection from Tree View
            resetRectStroke();
            flatMapTreeItem(ttFileView.getRoot()).filter(ti ->FileTypeSizeCount.fromPath(ti.getValue().p()).type().equals(nv.type())).forEach(
                    ti -> {
                        Rectangle r = pathToRect.get(ti);
                        if(r != null ) {
                            r.setStroke(Color.YELLOW);
                        } else {
                            System.out.println("No Rectangle for: "+ti.toString());
                        }
                    }
            );
        });
    }

    private void setTreeSelection(TreeItem<TI> ti) {
        expandTree(ti);
        ttFileView.scrollTo(ttFileView.getRow(ti));
        ttFileView.getSelectionModel().select(ti);
    }

    private void expandTree(TreeItem<TI> selectedItem) {
        if(selectedItem != null) {
            expandTree(selectedItem.getParent());
            if(!selectedItem.isLeaf()) {
                selectedItem.setExpanded(true);
            }
        }
    }

    String getType(Path p) {
        return FileTypeSizeCount.fromPath(p).type;
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
                    Collectors.groupingBy(FileTypeSizeCount::type,Collectors.collectingAndThen(
                            Collectors.reducing(
                                    (FileTypeSizeCount a,FileTypeSizeCount b) -> new FileTypeSizeCount(a.type,(a.size()+b.size),(a.count+b.count))
                            ), Optional::get
                    )
            )).values()
        );
        tblStats.getItems().sort(Comparator.comparingLong(FileTypeSizeCount::size).reversed());

        tblStats.getItems().forEach(i -> {
            typeColor.put(i.type(),randomColor(r));
        });

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
        System.out.println("Main Window Load Procedure Called");
    }
}