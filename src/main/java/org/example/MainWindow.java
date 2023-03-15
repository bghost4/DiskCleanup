package org.example;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.tika.Tika;
import org.controlsfx.control.BreadCrumbBar;
import org.example.searchStrategy.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainWindow extends VBox implements DataSupplier {

    @FXML
    private Label lStatus;

    @FXML
    private Label lMemStatus;

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

    @FXML
    private CheckMenuItem miTreeMapEnabled;

    private final Random random = new Random();

    private final ObservableList<String> fileTypes = FXCollections.observableArrayList();
    private final ObservableList<String> fileExts = FXCollections.observableArrayList();
    private final ObservableList<String> strategies = FXCollections.observableArrayList();
    private final ObservableList<UserPrincipal> fileOwners = FXCollections.observableArrayList();

    private final Tika tika = new Tika();

    @FXML
    private ProgressBar pbMemUsage;

    final TreeMap treeMap = new TreeMap();

    //TODO make this a prefrence
    private final Executor exec = Executors.newFixedThreadPool(4, r -> {
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
                            FileSizeSuffix.format(Runtime.getRuntime().freeMemory()),
                            FileSizeSuffix.format(Runtime.getRuntime().totalMemory())
                    );
                    Platform.runLater(() -> {
                        pbMemUsage.setProgress(percent);
                        lMemStatus.setText(text);
                    });
                    return null;
                }
            };
        }
    };

    private final HashMap<String,Color> colorPicker = new HashMap<>();

    private final BooleanProperty buildTreeRunning = new SimpleBooleanProperty(false);

    @Override
    public ObservableList<String> getStrategies() {
        return strategies;
    }

    @Override
    public Optional<StrategyBase> getStrategyByName(String name) {
        //this is ugly
        return switch(name) {
            case "And" -> Optional.of(new CompositeAndStrategy(this));
            case "Or" -> Optional.of(new CompositeOrStrategy(this));
            case "File Name and Size" -> Optional.of(new ExactNameAndSize(this));
            case "File Extension" -> Optional.of(new FileExtStrategy(this));
            case "File Name" -> Optional.of(new FileNameStrategy());
            case "File Owner" -> Optional.of(new FileOwnerStrategy(this));
            case "File Size" -> Optional.of(new FileSizeStrategy());
            case "File Time" -> Optional.of(new FileTimeStrategy());
            case "File Type" -> Optional.of(new FileTypeStrategy(this));
            default -> Optional.empty();
        };
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

    private record FileClassSizeCount(String fileClass, long size, long count) { }

    private Color randomColor() {
        return Color.rgb(random.nextInt(0,255),random.nextInt(0,255),random.nextInt(0,255));
    }

    @FXML
    public void onScanFolder(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        File startDir = dc.showDialog(null);

        if(startDir != null && startDir.isDirectory()) {
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
    private void onFindDuplicates(ActionEvent e) {
        Stage stage = new Stage();
        DuplicateUI ui = new DuplicateUI(this);
        Scene s = new Scene(ui);
        stage.setScene(s);
        stage.show();
    }


    @FXML
    private void onGraphTypeUsage(ActionEvent e) {

    }

    private void hide(TreeItem<StatItem> item) {
        TreeItem<StatItem> parent = item.getParent();
        if(parent != null) {
            if(ttFileView.getSelectionModel().getSelectedItem() == item) {
                ttFileView.getSelectionModel().select(item.getParent());
            }
            parent.getChildren().remove(item);

            //update parent size
            recalcChildrenRecursive(parent);
            treeMap.deleteRectangles(item);
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
        buildTreeRunning.set(true);
        TreeItem<StatItem> me = new TreeItem<>(si);
        treeMap.setContext(me);

        //Try to use filename to limit file IO, if we get the generic Application/octet-stream try and dig in
        Function<Path,String> typeExtractor = (p) -> {
            String type = tika.detect(p.getFileName().toString());
//            if(type.equals("application/octet-stream")){
//                try {
//                    return tika.detect(p);
//                } catch (IOException e) {
//                    return type;
//                }
//            }
            return type;
        };

        try {
            me.getChildren().addAll(Files.walk(si.p(), 1)
                    .filter(p -> !p.equals(si.p()))
                    .map(p -> {
                        if(Files.isDirectory(p)) {
                            return new TreeItem<>(StatItem.empty(si.p().relativize(p)));
                        } else {
                            try {
                                PathType pt;
                                pt = Files.isSymbolicLink(p) ? PathType.LINK : PathType.FILE;
                                BasicFileAttributes bfa = Files.readAttributes(p, BasicFileAttributes.class);
                                FileOwnerAttributeView foa = Files.getFileAttributeView(p,FileOwnerAttributeView.class);
                                return new TreeItem<>(
                                        new StatItem(si.p().relativize(p), pt,false, pt == PathType.FILE ? p.toFile().length() : 0,typeExtractor.apply(p), FilenameUtils.getExtension(p), bfa.creationTime().toInstant(), bfa.lastModifiedTime().toInstant(),foa.getOwner())
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
                me.getChildren().stream().filter(ti -> Files.isDirectory(TreeItemUtils.buildPath(ti)))
                        .map(v -> new FileScannerTask(v,typeExtractor)).collect(Collectors.toList()));

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
                    treeMap.rebuild();

                Runnable r = () -> {
                    //The following need not happen on the Application Thread, and we should probably process them all at once instead of mulling through the tree 3 times
                    fileExts.setAll(
                            TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(TreeItemUtils::isRegularFile).map(item -> item.getValue().ext()).distinct().sorted().toList()
                    );

                    fileTypes.setAll(
                            TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(TreeItemUtils::isRegularFile).map(item -> item.getValue().type()).distinct().sorted().toList()
                    );

                    fileOwners.setAll(
                            TreeItemUtils.flatMapTreeItem(ttFileView.getRoot()).filter(TreeItemUtils::isRegularFile).map(item -> item.getValue().owner()).distinct().sorted(Comparator.comparing(Principal::getName)).toList()
                    );
                };
                exec.execute(r);
                buildTreeRunning.set(false);
        });
        exec.execute(nt);
        return me;
    }

    @FXML
    private void initialize() {
        ttFileView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
        createTreeContextMenu();
        javafx.scene.image.Image iFile,iFolder,iLink;

        iFile = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/file.png")));
        iFolder = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/folder.png")));
        iLink = new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/link.png")));

        ttcName.setMaxWidth(1f * Integer.MAX_VALUE * 80);
        ttcSize.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        ttcName.setCellValueFactory( vf -> new ReadOnlyStringWrapper(vf.getValue().getValue().toString() ));
        ttcSize.setCellValueFactory( vf -> new ReadOnlyObjectWrapper<>( vf.getValue().getValue().length()) );
        ttcSize.setCellFactory(vf -> new TreeTableCell<>(){
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if(item == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(FileSizeSuffix.format(item));
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
                        pi.setPrefHeight(16);
                        pi.setPrefWidth(16);
                        pi.setMaxWidth(16);
                        pi.setMaxHeight(16);
                        setGraphic(pi);
                    } else {
                        setGraphic(
                            switch(this.getTableRow().getItem().pathType()){
                                case FILE -> new ImageView(iFile);
                                case LINK -> new ImageView(iLink);
                                case DIRECTORY -> new ImageView(iFolder);
                            }
                        );
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
                ttFileView.scrollTo(ttFileView.getRow(nv));
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
                    setText(FileSizeSuffix.format(item));
                }
            }
        });


        strategies.setAll(
                "File Name",
                "File Size",
                "File Type",
                "File Extension",
                "File Time",
                "File Owner",
                "File Name and Size",
                "And",
                "Or"
        );

        tcCount.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().count) );
        tcExt.setCellValueFactory(vf -> new ReadOnlyObjectWrapper<>(vf.getValue().fileClass) );
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
                breadCrumbBar.setSelectedCrumb(nv);
                if(nv == ttFileView.getRoot()) {
                    System.out.println("Selection Cleared, Revealing Whole Treemap");
                    treeMap.clearSelection();
                } else if(nv.isLeaf()) {
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
                final String type = nv.fileClass();
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

        lStatus.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    if(buildTreeRunning.get()) {
                        return "Scanning Folders";
                    } else if(treeMap.busy.get()) {
                        return "Updating Tree Map";
                    } else {
                        return "Idle";
                    }
                },buildTreeRunning,treeMap.busy)
        );

        miTreeMapEnabled.setSelected(true); //Eventually make this a setting
        treeMap.enabledProperty().bind(miTreeMapEnabled.selectedProperty());

    }

    private void createTableContextMenu() {
        ContextMenu ctx = new ContextMenu();

        MenuItem miShowFiles = new MenuItem("Show Files");
            miShowFiles.setOnAction(eh -> {
                Stage stage = new Stage();
                StrategyBase strategy;

                if(cboLegendSelect.getValue().equals("File Type")) {
                    FileTypeStrategy fts  = new FileTypeStrategy(this);
                    fts.setSelectedType(tblStats.getSelectionModel().getSelectedItem().fileClass());
                    strategy = fts;
                } else {
                    FileExtStrategy fns = new FileExtStrategy(this);
                    fns.setSelectedExtension(tblStats.getSelectionModel().getSelectedItem().fileClass);
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
                    TreeItem<StatItem> item = ttFileView.getSelectionModel().getSelectedItem();
                    if(item.getValue().pathType()==PathType.FILE) {
                        openPath(item.getParent());
                    } else {
                        openPath(item);
                    }
                });
            }

        MenuItem miHideFile = new MenuItem("Hide");
            miHideFile.setOnAction(eh -> hide(ttFileView.getSelectionModel().getSelectedItem()));
        MenuItem miDelete = new MenuItem("Delete");
            miDelete.setOnAction(eh -> delete(ttFileView.getSelectionModel().getSelectedItem()) );

        MenuItem miCompress = new MenuItem("Compress");
            miCompress.setOnAction( eh -> compress(ttFileView.getSelectionModel().getSelectedItem()));

        MenuItem miRescan = new MenuItem("Rescan");
            miRescan.setOnAction( eh -> rescan(ttFileView.getSelectionModel().getSelectedItem()));

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
        ctx.getItems().addAll(miSystemOpen,miOpenFolder,miHideFile,miDelete,miCompress,miRebuildTree,miRescan,miZoomInto,miZoomOut,miZoomRoot,miFindDuplicates);

        ttFileView.setContextMenu(ctx);
    }

    private void delete(TreeItem<StatItem> item) {
        try {
            if (item != null) {
                //For testing purposes confirm delete
                Alert a = new Alert(Alert.AlertType.CONFIRMATION,"Really Delete "+item.getValue().p());
                a.initStyle(StageStyle.UTILITY);
                a.setHeaderText(null);
                a.getButtonTypes().setAll(ButtonType.YES,ButtonType.CANCEL);
                Optional<ButtonType> bchoice = a.showAndWait();
                if(bchoice.orElse(ButtonType.CANCEL).equals(ButtonType.YES)) {
                    if(item.getValue().pathType() == PathType.DIRECTORY) {
                        Files.walk(TreeItemUtils.buildPath(item))
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } else  {
                        Files.delete(TreeItemUtils.buildPath(item));
                    }
                    TreeItem<StatItem> parent = item.getParent();
                    parent.getChildren().remove(item);
                    treeMap.deleteRectangles(item);
                    treeMap.refresh();
                }
            }
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR,"Error During Delete");
            a.setContentText(e.toString());
            a.showAndWait();
        }
    }

    private Path getSelectionPath() {
        return TreeItemUtils.buildPath(ttFileView.getSelectionModel().getSelectedItem());
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

    private void compress(TreeItem<StatItem> selection) {
            Path archivePath;
            Path archiveParent = TreeItemUtils.buildPath(selection.getParent());
            if(selection.getValue().pathType() == PathType.FILE) {
                String ext = TreeItemUtils.getExtension(selection);
                String baseName = selection.getValue().p().getFileName().toString().replace(ext,"");
                archivePath = archiveParent.resolve(Paths.get(baseName+".zip"));
            } else if(selection.getValue().pathType() == PathType.DIRECTORY) {
                archivePath = archiveParent.resolve(Paths.get(selection.getValue().p().getFileName().toString()+".zip"));
            } else {
                return;
            }

            StatItem item = selection.getValue();
            StatItem replacement = new StatItem(item.p(),item.pathType(),true,item.length(),item.type(),item.ext(),item.createTime(),item.modTime(),item.owner());
            selection.setValue(replacement); //Makes the spinny thing again then it will disappear when done

            Task<Void> compressionTask = new Task<>() {
                @Override
                protected Void call() throws IOException {
                    ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archivePath));
                        List<TreeItem<StatItem>> stuff = TreeItemUtils.flatMapTreeItem(selection).filter(ti -> ti.getValue().pathType()==PathType.FILE).toList();
                        for(TreeItem<StatItem> ti : stuff) {
                            Path absPath = TreeItemUtils.buildPath(ti);
                            Path relPath = archiveParent.relativize(absPath);
                            ZipEntry entry = new ZipEntry(relPath.toString());
                            zos.putNextEntry(entry);
                            Files.newInputStream(absPath).transferTo(zos);
                            zos.closeEntry();
                        }
                    return null;
                }
            };

            compressionTask.setOnSucceeded(wse -> {
                delete(selection);
            });

            exec.execute(compressionTask);

    }

    private void rescan(TreeItem<StatItem> item) {
        item.getChildren().clear();
        FileScannerTask fileScannerTask = new FileScannerTask(item,p -> tika.detect(TreeItemUtils.buildPath(item).toString()));
        fileScannerTask.setOnSucceeded(eh -> {
            System.out.println("Rescan Done");
            ttFileView.fireEvent(new TreeItem.TreeModificationEvent<StatItem>(TreeItem.TreeModificationEvent.ANY,item));
            treeMap.rebuild();
        } );
        exec.execute(fileScannerTask);
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
        Path p = TreeItemUtils.buildPath(item);
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

        Map<String,List<StatItem>> result = TreeItemUtils.flatMapTreeItemUnwrap(ttFileView.getRoot()).filter(item -> item.pathType() == PathType.FILE).collect(Collectors.groupingBy(mappingFunction));
        List<FileClassSizeCount> items =  result.entrySet().stream().map(e -> new FileClassSizeCount(e.getKey(),e.getValue().stream().mapToLong(StatItem::length).sum(),e.getValue().size())).sorted(Comparator.comparingLong(FileClassSizeCount::size).reversed()).toList();
        tblStats.getItems().setAll(items);
        //tblStats.getItems().sort(Comparator.comparingLong(FileClassSizeCount::size).reversed());
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