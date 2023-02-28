package org.example;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import org.example.searchStrategy.*;

public class FileFinder extends VBox {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane apStrategyArea;

    @FXML
    private ComboBox<String> cboStrategy;

    private final SimpleObjectProperty<StrategyBase> strategy = new SimpleObjectProperty<>();

    private final DataSupplier dataSupplier;

    @FXML
    private ListView<TreeItem<StatItem>> lstFoundFiles;

    @FXML
    private Label lblResultsize;

    private TreeMap treeMap;

    @FXML
    void onFindFiles(ActionEvent event) {
        if(searchContext.get() != null) {
            TreeItem<StatItem> root = searchContext.get().getRoot();
            List<TreeItem<StatItem>> result = TreeItemUtils.flatMapTreeItem(root).filter(TreeItemUtils::isRegularFile).filter(ti ->
                    strategy.get().getPredicate().test(ti)
            ).toList();
            if(treeMap != null) {
                treeMap.setSelection(result::stream);
            }
            lstFoundFiles.getItems().setAll(result);
        }
    }

    private final SimpleObjectProperty<TreeTableView<StatItem>> searchContext = new SimpleObjectProperty<>();

    public TreeTableView<StatItem> getSearchContext() {
        return searchContext.get();
    }

    public SimpleObjectProperty<TreeTableView<StatItem>> searchContextProperty() {
        return searchContext;
    }

    public void setSearchContext(TreeTableView<StatItem> searchContext) {
        this.searchContext.set(searchContext);
    }


    public FileFinder(DataSupplier s) {
        super();

        dataSupplier = s;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fileFinder.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onShowSelectionInTreeMap(ActionEvent e) {
        if(treeMap != null) {
            System.out.println("Showing Selection in TreeMap");
            treeMap.setSelection( () -> lstFoundFiles.getItems().stream() );
        }
    }

    @FXML
    void initialize() {
        assert apStrategyArea != null : "fx:id=\"apStrategyArea\" was not injected: check your FXML file 'fileFinder.fxml'.";
        assert cboStrategy != null : "fx:id=\"cboStrategy\" was not injected: check your FXML file 'fileFinder.fxml'.";
        assert lstFoundFiles != null : "fx:id=\"lstFoundFiles\" was not injected: check your FXML file 'fileFinder.fxml'.";

        System.out.println("Initialize Called");

        cboStrategy.getSelectionModel().selectedItemProperty().addListener( ( ob,ov,nv) -> {
            if(nv != null) {
                dataSupplier.getStrategyByName(cboStrategy.getValue()).ifPresent(sb -> strategy.set(sb));
                System.out.println("Selected: "+strategy.get().getName());
                Node settings = strategy.get().getSettings();
                AnchorPane.setTopAnchor(settings, 10.0);
                AnchorPane.setLeftAnchor(settings, 10.0);
                AnchorPane.setBottomAnchor(settings, 10.0);
                AnchorPane.setRightAnchor(settings, 10.0);
                apStrategyArea.getChildren().setAll(settings);
            }
        });

        lstFoundFiles.setCellFactory( cf -> new ListCell<>() {
            @Override
            protected void updateItem(TreeItem<StatItem> item, boolean empty) {
                super.updateItem(item, empty);

                if(item != null && !empty) {
                    setText(item.getValue().p().toString());
                } else {
                    setText(null);
                }
                setGraphic(null);
            }
        });

        lstFoundFiles.getSelectionModel().selectedItemProperty().addListener((ob,ov,nv) -> {
            searchContext.get().getSelectionModel().select(nv);
            searchContext.get().scrollTo(searchContext.get().getRow(nv));
        });

        lblResultsize.textProperty().bind(Bindings.format("%d Files Found",Bindings.size(lstFoundFiles.getItems())));

        searchContext.set(dataSupplier.getTreeView());
        cboStrategy.getItems().setAll(dataSupplier.getStrategies());

        if(strategy.get() != null) {
           cboStrategy.setValue(strategy.get().getName());
        } else {
           cboStrategy.setValue(cboStrategy.getItems().get(0));
        }

        setTreeMap(dataSupplier.getTreeMap());
    }

    public void setStrategy(StrategyBase b) {
        cboStrategy.setValue(b.getName());
    }

    public void setTreeMap(TreeMap treeMap) {
        this.treeMap = treeMap;
    }
}