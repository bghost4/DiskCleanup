package org.example;

import javafx.concurrent.Task;

import java.util.List;

public class NestedTask<T extends Task<?>> extends Task<Void> {

    private final TaskManager tskmgr;
    private final List<T> dependants;

    public NestedTask(TaskManager mgr, List<T> dependants) {
        updateTitle("Nested Task");
        this.tskmgr = mgr;
        this.dependants = dependants;
    }

    @Override
    protected Void call() throws Exception {
        //dependants.forEach(d -> d.run());
        for(T subtask : dependants) {
            updateTitle(subtask.getTitle());
            subtask.run();
        }
        return null;
    }

    public List<T> getDependants() { return dependants; }
}
