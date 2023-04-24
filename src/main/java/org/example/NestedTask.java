package org.example;

import javafx.concurrent.Task;

import java.util.List;

public class NestedTask<T extends Task<?>> extends Task<Void> {

    private final TaskManager tskmgr;
    private final List<T> dependants;

    public NestedTask(TaskManager mgr, List<T> dependants) {
        this.tskmgr = mgr;
        this.dependants = dependants;
    }

    @Override
    protected Void call() throws Exception {

        dependants.forEach(tskmgr::execute);

        while(!dependants.stream().allMatch(t -> t.isDone())) {
            //Thread.sleep(100);
            Thread.yield();
        }

        return null;
    }

    public List<T> getDependants() { return dependants; }
}
