package org.example;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.controlsfx.control.BreadCrumbBar;
import org.example.searchStrategy.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainWindow extends VBox implements DataSupplier {

    @FXML
    private Label lStatus;

    @FXML
    private AnchorPane pUsageView;

    @FXML
    private ComboBox<String> cboLegendSelect;

    @FXML
    private TableView<FileClassSizeCount> tblStats;

    @FXML
    private TreeTableView<StatItem> ttFileView;

    @FXML
    private TreeTableColumn<StatItem, String> ttcName;

    @FXML
    private TreeTableColumn<StatItem, Long> ttcSize;

    @FXML
    private TableColumn<FileClassSizeCount,Long> tcCount,tcSize;
    @FXML
    private TableColumn<FileClassSizeCount,String> tcExt;

    @FXML
    private BreadCrumbBar<StatItem> breadCrumbBar;

    private final Random random = new Random();

    private final ObservableList<String> fileTypes = FXCollections.observableArrayList();
    private final ObservableList<String> fileExts = FXCollections.observableArrayList();
    private final ObservableList<StrategyBase> strategies = FXCollections.observableArrayList();
    private final ObservableList<UserPrincipal> fileOwners = FXCollections.observableArrayList();


    @FXML
    private ProgressBar pbMemUsage;

    final TreeMap treeMap = new TreeMap();

    //TODO make this a prefrence
    private final Executor exec = Executors.newFixedThreadPool(2, r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    private final ScheduledService<Void> svcMemoryUsage = new ScheduledService<>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    double percent = 1 - ((double) Runtime.getRuntime().freeMemory() / (double) Runtime.getRuntime().totalMemory());
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

    private final HashMap<String,Color> colorPicker = new HashMap<>();

    private void changed(ObservableValue<? extends String> ob, String ov, String nv) {
        generateFileTypeLegend();
    }

    @Override
    public ObservableList<StrategyBase> getStrategies() {
        return strategies;
    }

    @Override
    public ObservableList<String> fileExtensions() {
        return fileExts;
    }

    @Override
    public ObservableList<String> fileTypes() {
        return fileTypes;
    }

    @Override
    public ObservableList<UserPrincipal> fileOwners() {
        return fileOwners;
    }

    @Override
    public TreeTableView<StatItem> getTreeView() {
        return ttFileView;
    }

    @Override
    public TreeMap getTreeMap() {
        return treeMap;
    }

    private record FileClassSizeCount(String type, long size, long count) {
        public static FileClassSizeCount fromPath(Path p) {
            if(Files.isDirectory(p)) {
                return new FileClassSizeCount("<Directory>",p.toFile().length(),1);
            } else {
                String type = TreeItemUtils.getExtension(p);
                if(type.isBlank() || type.isEmpty()) {
                    type = "<Typeless>";
                }
                return new FileClassSizeCount(type, p.toFile().length(), 1);
            }
        }
    }

    private Color randomColor() {
        return Color.rgb(random.nextInt(0,255),random.nextInt(0,255),random.nextInt(0,255));
    }

    @FXML
    public void onScanFolder(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        File startDir = dc.showDialog(null);

        if(startDir.isDirectory()) {
            beginDirectoryScan(startDir.toPath());
        }
    }

    @FXML
    private void onFindFiles(ActionEvent e) {
            Stage stage = new Stage();
            FileFinder ui = new FileFinder(this);
            ui.searchContextProperty().set(ttFileView);
            ui.setTreeMap(treeMap);
            Scene s = new Scene(ui);
            stage.setScene(s);
            stage.show();
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
            treeMap.refresh();
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

    private TreeItem<StatItem> buildTree(StatItem si) {
        TreeItem<StatItem> me = new TreeItem<>(si);

        try {
            me.getChildren().addAll(Files.walk(si.p(), 1)
                    .filter(p -> !p.equals(si.p()))
                    .map(p -> {
                        if(Files.isDirectory(p)) {
                            return new TreeItem<>(StatItem.empty(p));
                        } else {
                            try {
                                BasicFileAttributes bfa = Files.readAttributes(p, BasicFileAttributes.class);
                                FileOwnerAttributeView foa = Files.getFileAttributeView(p,FileOwnerAttributeView.class);
                                return new TreeItem<>(
                                        new StatItem(p, false, p.toFile().length(),TreeItemUtils.getType(p), FilenameUtils.getExtension(p.getFileName().toString()), bfa.creationTime().toInstant(), bfa.lastModifiedTime().toInstant(),foa.getOwner())
                                );
                            } catch(IOException e) {
                                return new TreeItem<>(StatItem.empty(p));
                            }
                        }
                    }).toList());
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
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }));

        nt.setOnSucceeded(eh -> {
            me.getChildren().sort(Comparator.comparingLong((TreeItem<StatItem> ti) -> ti.getValue().length()).reversed());

            me.setValue(me.getValue().update(
                    me.getChildren().stream().mapToLong((TreeItem<StatItem> i) -> i.getValue().length()).sum()
            ));

            generateFileTypeLegend();
            treeMap.setContext(me);

            fileExts.setAll(
                TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(ti -> Files.isRegularFile(ti.getValue().p())).map(item -> item.getValue().ext()).distinct().toList()
            );

            fileTypes.setAll(
                    TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(ti -> Files.isRegularFile(ti.getValue().p())).map(item -> item.getValue().type()).distinct().toList()
            );

            fileOwners.setAll(
                    TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(ti -> Files.isRegularFile(ti.getValue().p())).map(item -> item.getValue().owner()).distinct().toList()
            );


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

        breadCrumbBar.selectedCrumbProperty().addListener((ob,ov,nv) -> {
            if(ttFileView.getSelectionModel().getSelectedItem() != nv) {
                ttFileView.getSelectionModel().select(nv);
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

        strategies.setAll(
          new FileNameStrategy(),
          new FileExtStrategy(fileExts),
          new FileTypeStrategy(fileTypes),
          new FileTimeStrategy(),
          new FileOwnerStrategy(this),
          new FileSizeStrategy(),
          new ExactNameAndSize(this),
          new CompositeAndStrategy(this),
          new CompositeOrStrategy(this)
        );

        tcCount.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().count) );
        tcExt.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().type) );
        tcExt.setCellFactory(cdf -> new TableCell<>(){
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(!empty && item != null) {
                    setText(item);
                    Rectangle r = new Rectangle(16, 16);
                    r.setFill(colorPicker.computeIfAbsent(item,n -> randomColor()));
                    setGraphic(r);
                } else {
                    setText(null); setGraphic(null);
                }
            }
        });

        ttFileView.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            if(nv != null) {
                //updateRects((ti -> isChildOf(nv,ti)), (t, r) -> r.setOpacity(1), (t, r) -> r.setOpacity(notSelectedOpacity));
                TreeItemUtils.recursiveExpand(nv);
                breadCrumbBar.setSelectedCrumb(nv);
                if(nv.isLeaf()) {
                    treeMap.setSelection(() -> Stream.of(nv));
                } else {
                    treeMap.setSelection(() -> TreeItemUtils.flatMapTreeItem(nv));
                }
            } else {
                breadCrumbBar.setSelectedCrumb(ttFileView.getRoot());
                treeMap.clearSelection();
            }
        });

        tblStats.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            ttFileView.getSelectionModel().clearSelection(); //clear selection from Tree View
            if(nv != null ) {
                final String type = nv.type();
                if(cboLegendSelect.getValue().equals("File Type")) {
                    treeMap.setSelection(() -> TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(ti -> type.equals(ti.getValue().type())));
                } else {
                    treeMap.setSelection(() -> TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(ti -> type.equals(ti.getValue().ext())));
                }
            } else {
                treeMap.clearSelection();
            }
        });

        svcMemoryUsage.setPeriod(Duration.seconds(5));
        svcMemoryUsage.start();

        AnchorPane.setTopAnchor(treeMap,1.0);
        AnchorPane.setBottomAnchor(treeMap,1.0);
        AnchorPane.setLeftAnchor(treeMap,1.0);
        AnchorPane.setRightAnchor(treeMap,1.0);

        pUsageView.getChildren().add(treeMap);

        treeMap.setMouseHandler( (mouseEvent,ti) -> {
            if(mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                TreeItemUtils.recursiveExpand(ti);
                ttFileView.getSelectionModel().select(ti);
                ttFileView.scrollTo(ttFileView.getRow(ti));

                if( mouseEvent.getButton() == MouseButton.SECONDARY ) {
                    ContextMenu mnu = ttFileView.getContextMenu();
                    mnu.show(treeMap,mouseEvent.getScreenX(),mouseEvent.getScreenY());
                }
            }
        });

        createTableContextMenu();

        cboLegendSelect.getItems().addAll("Extension","File Type");
        cboLegendSelect.setValue("Extension");
        cboLegendSelect.valueProperty().addListener(il -> generateFileTypeLegend());

    }

    private void createTableContextMenu() {
        ContextMenu ctx = new ContextMenu();

        MenuItem miShowFiles = new MenuItem("Show Files");
            miShowFiles.setOnAction(eh -> {
                Stage stage = new Stage();
                StrategyBase strategy;

                if(cboLegendSelect.getValue().equals("File Type")) {
                    FileTypeStrategy fts  = new FileTypeStrategy(fileTypes);
                    fts.setTypes(fileTypes);
                    fts.setSelectedType(tblStats.getSelectionModel().getSelectedItem().type());
                    strategy = fts;
                } else {
                    FileExtStrategy fns = new FileExtStrategy(fileExts);
                    fns.setExtensions(fileExts);
                    fns.setSelectedExtension(tblStats.getSelectionModel().getSelectedItem().type());
                    strategy = fns;
                }

                FileFinder ui = new FileFinder(this);
                ui.setStrategy(strategy);
                Scene s = new Scene(ui);
                stage.setScene(s);
                stage.setTitle("Find File Type");
                stage.show();
            });

        ctx.getItems().add(miShowFiles);

        tblStats.setContextMenu(ctx);
    }

    private void createTreeContextMenu() {
        ContextMenu ctx = new ContextMenu();
        MenuItem miSystemOpen = new MenuItem("Open With System Viewer");
            miSystemOpen.setOnAction(eh -> openPath(ttFileView.getSelectionModel().getSelectedItem()));
        MenuItem miOpenFolder = new MenuItem("Open Folder");
            if(Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                miOpenFolder.setOnAction(eh -> openFolder(ttFileView.getSelectionModel().getSelectedItem()));
            } else {
                miOpenFolder.setOnAction( eh -> {
                    TreeItem<StatItem> p = ttFileView.getSelectionModel().getSelectedItem();
                    if(TreeItemUtils.isRegularFile(p)) {
                        p = p.getParent();
                    }
                    openPath(p);
                });
            }

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
            miRebuildTree.setOnAction(eh -> treeMap.refresh() );
        ctx.getItems().addAll(miSystemOpen,miOpenFolder,miDeleteNode,miRebuildTree,miZoomInto,miZoomOut,miZoomRoot,miFindDuplicates);

        ttFileView.setContextMenu(ctx);
    }



    private void findDuplicates(TreeItem<StatItem> selectedItem) {
        Stage stage = new Stage();
        FileFinder dupui = new FileFinder(this);
        ExactNameAndSize strategy = new ExactNameAndSize(this);
            strategy.fileNameProperty().set(selectedItem.getValue().p().getFileName().toString());
            strategy.fileSizeProperty().set(selectedItem.getValue().length());
        dupui.setStrategy(strategy);
        Scene s = new Scene(dupui);
        stage.setScene(s);
        stage.show();
    }

    private void zoomIn(TreeItem<StatItem> item) {
        ttFileView.setRoot(item);
        generateFileTypeLegend();
        treeMap.setContext(item);
    }
    private void zoomOut() {
        TreeItem<StatItem> parent = ttFileView.getRoot().getParent();
        if(parent != null) {
            ttFileView.setRoot(parent);
            generateFileTypeLegend();
            treeMap.setContext(ttFileView.getRoot());
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
        treeMap.setContext(last);
    }

    private void openFolder(TreeItem<StatItem> selectedItem) {
        if(selectedItem == null ) { return; }
        Path p = selectedItem.getValue().p();
        Runnable r = () -> Desktop.getDesktop().browseFileDirectory(p.toFile());
        exec.execute(r);
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
            exec.execute(r);
    }

    void generateFileTypeLegend() {
        tblStats.getItems().clear();
        tblStats.getSortOrder().clear();
        tcSize.setSortType(TableColumn.SortType.DESCENDING);
        tblStats.getSortOrder().add(tcSize);

        Function<StatItem,String> mappingFunction = StatItem::ext;

        if(cboLegendSelect.getValue().equals("Extension")) {
            mappingFunction = StatItem::ext;
            tcExt.setText("Ext");
        } else if(cboLegendSelect.getValue().equals("File Type")) {
            mappingFunction = StatItem::type;
            tcExt.setText("Type");
        }

        Map<String,List<StatItem>> result = TreeItemUtils.flatMapTreeItemUnwrap(ttFileView.getRoot()).filter(item -> Files.isRegularFile(item.p())).collect(Collectors.groupingBy(mappingFunction));
        List<FileClassSizeCount> items =  result.entrySet().stream().map(e -> new FileClassSizeCount(e.getKey(),e.getValue().stream().mapToLong(StatItem::length).sum(),e.getValue().size())).toList();
        tblStats.getItems().setAll(items);
        tblStats.getItems().sort(Comparator.comparingLong(FileClassSizeCount::size).reversed());
        treeMap.setTypePainter(this::getPaint);
    }

    public Color getPaint(TreeItem<StatItem> item) {
        if(cboLegendSelect.getValue().equals("File Type")) {
            return colorPicker.computeIfAbsent(item.getValue().type(),n -> randomColor());
        } else {
            return colorPicker.computeIfAbsent(item.getValue().ext(),n -> randomColor());
        }
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