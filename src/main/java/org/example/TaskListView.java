package org.example;

import javafx.concurrent.Task;
import javafx.scene.control.ListView;

public class TaskListView extends ListView<Task<?>> {

    private TaskManager tm;


    public TaskListView() {



    }

    public void setTaskManager(TaskManager tm) {
        this.tm = tm;
        this.setItems(tm.getTasks());
    }


}
