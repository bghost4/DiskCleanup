package org.example;

import javafx.concurrent.Task;
import java.util.function.Supplier;

public class TaskSupplier<T> {

    private final Supplier<Task<T>> s;
    private final TaskManager taskManager;

    public TaskSupplier(Supplier<Task<T>> s, TaskManager tmgr) {
        this.s = s;
        this.taskManager = tmgr;
    }



}
