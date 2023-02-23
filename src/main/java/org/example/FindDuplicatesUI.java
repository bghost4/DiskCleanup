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

public class FindDuplicatesUI extends VBox {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private AnchorPane apStrategyArea;

    @FXML
    private ComboBox<StrategyBase> cboStrategy;

    @FXML
    private ListView<TreeItem<StatItem>> lstFoundFiles;

    @FXML
    private Label lblResultsize;

    private TreeMap treeMap;

    @FXML
    void onFindDuplicates(ActionEvent event) {
        if(searchContext.get() != null) {
            TreeItem<StatItem> root = searchContext.get().getRoot();
            List<TreeItem<StatItem>> result = TreeItemUtils.flatMapTreeItem(root).filter( ti ->
                    cboStrategy.getValue().getPredicate().test((TreeItem<org.example.StatItem>)ti)
            ).toList();
            if(treeMap != null) {
                treeMap.setSelection(result);
            }
            lstFoundFiles.getItems().setAll(result);
        }
    }

    private SimpleObjectProperty<TreeTableView<StatItem>> searchContext = new SimpleObjectProperty<>();

    public TreeTableView<StatItem> getSearchContext() {
        return searchContext.get();
    }

    public SimpleObjectProperty<TreeTableView<StatItem>> searchContextProperty() {
        return searchContext;
    }

    public void setSearchContext(TreeTableView<StatItem> searchContext) {
        this.searchContext.set(searchContext);
    }

    public FindDuplicatesUI() {
        super();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/findDuplicatesUI.fxml"));
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
            treeMap.setSelection(lstFoundFiles.getItems());
        }
    }

    @FXML
    void initialize() {
        assert apStrategyArea != null : "fx:id=\"apStrategyArea\" was not injected: check your FXML file 'findDuplicatesUI.fxml'.";
        assert cboStrategy != null : "fx:id=\"cboStrategy\" was not injected: check your FXML file 'findDuplicatesUI.fxml'.";
        assert lstFoundFiles != null : "fx:id=\"lstFoundFiles\" was not injected: check your FXML file 'findDuplicatesUI.fxml'.";

        cboStrategy.getItems().add(new FileNameStrategy());

        cboStrategy.getSelectionModel().selectedItemProperty().addListener( ( ob,ov,nv) -> {
            if(nv != null) {
                System.out.println("Selected: "+nv.getName());
                Node settings = nv.getSettings();
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
        });

        lblResultsize.textProperty().bind(Bindings.format("%d Files Found",Bindings.size(lstFoundFiles.getItems())));

        cboStrategy.getSelectionModel().select(cboStrategy.getItems().get(0));

    }

    public void setTreeMap(TreeMap treeMap) {
        this.treeMap = treeMap;
    }
}