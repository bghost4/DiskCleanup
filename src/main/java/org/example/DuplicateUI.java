package org.example;

import com.sun.source.tree.Tree;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.DataInput;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

import javafx.fxml.FXML;
import org.example.searchStrategy.DataSupplier;

import java.io.IOException;
import java.util.stream.Collectors;

public class DuplicateUI extends VBox {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Button btnAction;

    @FXML
    private ProgressBar progress;

    @FXML
    private Label lblStatus;

    @FXML
    private Spinner<Long> spnMajorSize;
    private LongSpinnerValueFactory lsvf = new LongSpinnerValueFactory();

    @FXML
    private ComboBox<FileSizeSuffix> cboUnit;

    @FXML
    private TreeView<DTI> tvDuplicates;

    private long minFileSize = 1000;

    private final Service<Void> duplicateService = new Service<Void>() {

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    updateMessage("Finding Files with Common Sizes");
                    List<TreeItem<StatItem>> firstSort = TreeItemUtils.flatMapTreeItem(supplier.getTreeView().getRoot())
                            .filter(ti -> (ti.getValue().pathType() == PathType.FILE && ti.getValue().length() > minFileSize))
                            .collect(Collectors.groupingBy(ti -> ti.getValue().length()))
                            .entrySet().stream().filter(es -> es.getValue().size() > 1)
                            .flatMap(es -> es.getValue().stream())
                            .collect(Collectors.toList());
                    //size of list is work total, index is work done when this gets turned into a Task

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[4096]; // Maybe make this settable?
                    HashMap<String,List<TreeItem<StatItem>>> tree = new HashMap<>();

                    for(int i=0; i < firstSort.size(); i++) {
                        updateProgress(i,firstSort.size());
                        Path p = TreeItemUtils.buildPath(firstSort.get(i));
                        updateMessage("Hashing "+p);
                        digest.reset();

                        try(InputStream is = Files.newInputStream(p)) {
                            int b = 0;
                            while( (b = is.read(buffer)) != -1 ) {
                                digest.update(buffer,0,b);
                            }
                            String hash = bytesToHex(digest.digest());
                            tree.computeIfAbsent(hash,(h) -> new ArrayList<>()).add(firstSort.get(i));
                            //System.out.println("Adding Hash: "+hash+" Tree.size() "+tree.size());
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }

                    updateMessage("Sorting Out Duplicates");
                    TreeItem<DTI> root = new TreeItem<DTI>(new DTI("Results"));


                    List<String> keys = tree.keySet().stream().map(k -> new Pair<String,Long>(k,tree.get(k).stream().mapToLong(ti -> ti.getValue().length()).sum())).sorted(Comparator.comparingLong(p -> p.b())).map(p -> p.a()).collect(Collectors.toList());

                    //ArrayList<String> keys = new ArrayList<>(tree.keySet().stream().toList());
                    //keys.sort(Comparator.comparingLong(k -> tree.get(k).stream().mapToLong(ti->ti.getValue().length()).sum()).reversed());

                    for(int i=0; i < keys.size(); i++) {
                        String key = keys.get(i);
                        List<TreeItem<StatItem>> values = tree.get(key);
                        if( values.size() > 1) {
                            TreeItem<DTI> child = new TreeItem<DTI>(new DTI(key));
                            for (int j = 0; j < values.size(); j++) {
                                TreeItem<DTI> fileItem = new TreeItem<>(new DTI(values.get(j)));
                                child.getChildren().add(fileItem);
                            }
                            root.getChildren().add(child);
                        }
                    }

                    updateMessage(String.format("Complete (%d/%d)",keys.size(),firstSort.size()));
                    Platform.runLater(() -> tvDuplicates.setRoot(root));

                    return null;
                }
            };
        }
    };

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @FXML
    void initialize() {
        assert btnAction != null : "fx:id=\"btnAction\" was not injected: check your FXML file 'duplicate.fxml'.";
        assert progress != null : "fx:id=\"progress\" was not injected: check your FXML file 'duplicate.fxml'.";
        assert tvDuplicates != null : "fx:id=\"tvDuplicates\" was not injected: check your FXML file 'duplicate.fxml'.";

        progress.setProgress(0);

        btnAction.setOnAction(this::onFind);
        btnAction.textProperty().bind(Bindings.createStringBinding(() -> duplicateService.isRunning() ? "Cancel" : "Find Duplicates",duplicateService.runningProperty()));
        progress.progressProperty().bind(duplicateService.progressProperty());
        lblStatus.textProperty().bind(duplicateService.messageProperty());
        cboUnit.getItems().addAll(Arrays.asList(FileSizeSuffix.values()));
        spnMajorSize.setValueFactory(lsvf);
        spnMajorSize.setEditable(true);
        lsvf.setValue(1L);
        cboUnit.setValue(FileSizeSuffix.kB);

        tvDuplicates.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            if(nv != null) {
                if(nv.getValue().isItem()) {
                    supplier.getTreeView().getSelectionModel().select(nv.getValue().getItem());
                    supplier.getTreeView().scrollTo(supplier.getTreeView().getRow(nv.getValue().getItem()));
                } else {
                    supplier.getTreeMap().setSelection(() -> nv.getChildren().stream().map(ti -> ti.getValue().getItem()));
                }
            }
        });

        progress.disableProperty().bind(duplicateService.runningProperty().not());

    }

    private void onFind(ActionEvent e) {
        if(duplicateService.isRunning()) {
            duplicateService.cancel();
        } else {
            minFileSize = cboUnit.getValue().toBytes(spnMajorSize.getValue());
            System.out.println("Min File Size is: "+minFileSize+" Bytes");
            duplicateService.restart();
        }

    }

    private final DataSupplier supplier;

    public DuplicateUI(DataSupplier s) {
        super();

        this.supplier = s;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/duplicate.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
