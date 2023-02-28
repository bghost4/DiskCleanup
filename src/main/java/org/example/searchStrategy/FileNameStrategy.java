package org.example.searchStrategy;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.StatItem;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileNameStrategy extends StrategyBase {

    private final Node settingUI;
    private final TextField txtName;

    private final RadioButton btnCaseSensitive = new RadioButton("Case Sensitive");
    private final RadioButton btnIgnoreCase = new RadioButton("Ignore Case");
    private final RadioButton btnWildcard = new RadioButton("Wildcard");
    private final RadioButton btnRegex = new RadioButton("Regex");

    public enum NameMatchStrategy { CASE_SENSITIVE,IGNORE_CASE,WILDCARD,REGEX }

    private final SimpleObjectProperty<NameMatchStrategy> nameMatchStrategy = new SimpleObjectProperty<>(NameMatchStrategy.IGNORE_CASE);

    private Pattern regexPattern;

    final ToggleGroup tglGroup = new ToggleGroup();

    public FileNameStrategy() {
        VBox vb = new VBox();
        HBox hb = new HBox();

        tglGroup.getToggles().setAll(btnCaseSensitive,btnIgnoreCase,btnWildcard,btnRegex);

        HBox hbRadio = new HBox();
        hbRadio.getChildren().setAll(btnCaseSensitive,btnIgnoreCase,btnWildcard,btnRegex);

        txtName = new TextField();

        tglGroup.selectToggle(btnIgnoreCase);

        hb.getChildren().addAll(new Label("File Name"),txtName);

        vb.getChildren().addAll(hbRadio,hb);

        txtName.textProperty().addListener(il -> {
            if(tglGroup.getSelectedToggle().equals(btnRegex)) {
                try {
                    regexPattern = Pattern.compile(txtName.getText());
                    configValid.set(true);
                } catch(PatternSyntaxException e) {
                    configValid.set(false);
                    txtName.setStyle("-fx-background: lightcoral;");
                }
            } else {
                txtName.setStyle("-fx-background: white");
                configValid.set(true);
            }
        });

        nameMatchStrategy.addListener( il -> {
            switch(nameMatchStrategy.get()) {
                case REGEX -> tglGroup.selectToggle(btnRegex);
                case IGNORE_CASE -> tglGroup.selectToggle(btnIgnoreCase);
                case CASE_SENSITIVE -> tglGroup.selectToggle(btnCaseSensitive);
                case WILDCARD -> tglGroup.selectToggle(btnWildcard);
            }
        });

        settingUI = vb;
    }

    @Override
    public Node getSettings() {
        return settingUI;
    }

    @Override
    public Predicate<TreeItem<StatItem>> getPredicate() {
        if(tglGroup.getSelectedToggle().equals(btnCaseSensitive)) {
            return (ti) -> txtName.getText().equals(ti.getValue().p().getFileName().toString());
        } else if( tglGroup.getSelectedToggle().equals(btnIgnoreCase) ) {
            return (ti) -> txtName.getText().equalsIgnoreCase(ti.getValue().p().getFileName().toString());
        } else if(tglGroup.getSelectedToggle().equals(btnWildcard)) {
            String intermediary = txtName.getText().replace(".","\\.").replace("*",".*").replace("?",".");
            try {
                regexPattern = Pattern.compile(intermediary);
                return ti -> ti.getValue().p().getFileName().toString().matches(intermediary);
            } catch(PatternSyntaxException e) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setHeaderText("Error Constructing Wildcard");
                a.showAndWait();
                return ti -> false;
            }
        } else if(tglGroup.getSelectedToggle().equals(btnRegex)) {
            return ti -> ti.getValue().p().getFileName().toString().matches(txtName.getText());
        } else {
            //if you got here, you did something wrong
            System.err.println("File Name Strategy :: getPredicate - something went wrong");
            return ti -> false;
        }

    }

    public StringProperty fileNamePatternProperty() {
        return txtName.textProperty();
    }

    public ObjectProperty<NameMatchStrategy> nameMatchStrategyProperty() {
        return nameMatchStrategy;
    }

    public NameMatchStrategy getNameMatchStrategy() {
        return nameMatchStrategy.get();
    }

    public void setNameMatchStrategy(NameMatchStrategy nameMatchStrategy) {
        this.nameMatchStrategy.set(nameMatchStrategy);
    }

    public void setFileNamePattern(String s) {
        txtName.setText(s);
    }

    public String getFileNamePattern() { return txtName.getText(); }

    @Override
    public String getName() {
        return "File Name";
    }
}
