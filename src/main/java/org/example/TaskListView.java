package org.example;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

public class TaskListView extends ListView<Task<?>> {

    private TaskManager tm;


    public TaskListView() {
        setCellFactory(cdf -> new TaskListCell());
    }

    public void setTaskManager(TaskManager tm) {
        this.tm = tm;
        this.setItems(tm.getTasks());
    }

    public class TaskListCell extends ListCell<Task<?>> {
        final ChangeListener<String> titleListener = (observable, oldValue, newValue) -> setText(newValue);
        public TaskListCell() {
            this.itemProperty().addListener((observable, oldValue, newValue) -> {
                if(oldValue != null) {
                    oldValue.titleProperty().removeListener(titleListener);
                }
                if( newValue == null) {
                    setText(null);
                } else {
                    setText(newValue.titleProperty().get());
                    newValue.titleProperty().addListener(titleListener);
                }
            });
        }
    }

}
