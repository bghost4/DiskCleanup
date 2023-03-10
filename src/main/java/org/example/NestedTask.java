package org.example;

import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.Executor;

public class NestedTask<T extends Task<?>> extends Task<Void> {

    private final Executor executor;
    private final List<T> dependants;

    public NestedTask(Executor executor, List<T> dependants) {
        this.executor = executor;
        this.dependants = dependants;
    }

    @Override
    protected Void call() throws Exception {

        dependants.forEach(executor::execute);

        while(!dependants.stream().allMatch(t -> t.isDone())) {
            //Thread.sleep(100);
            Thread.yield();
        }

        return null;
    }

    public List<T> getDependants() { return dependants; }
}
