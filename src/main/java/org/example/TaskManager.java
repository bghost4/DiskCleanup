package org.example;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager implements Executor {
    private final ObservableList<Task<?>> tlist = FXCollections.observableArrayList(task -> new Observable[]{task.runningProperty()});

    private final ExecutorService exec;

    public Optional<String> getName(Task<?> t) {
        return Optional.ofNullable(t.getTitle());
    }

    public ObservableList<Task<?>> getTasks() { return tlist; }

    private final SimpleIntegerProperty runningTasks = new SimpleIntegerProperty();
    private final SimpleIntegerProperty numTasks = new SimpleIntegerProperty();


    public TaskManager(int threads) {

        exec = Executors.newFixedThreadPool(threads, r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });

        tlist.addListener( (ListChangeListener<Task<?>>)(change) -> {
             while(change.next()) {
                if(change.wasUpdated()) {
                    for(int i = change.getFrom(); i < change.getTo(); i++) {
                        Task<?> t = tlist.get(i);
                        if(t.isDone()) { tlist.remove(t); }
                    }
                }
                if(change.wasAdded()) {
                    for(Task<?> item : change.getAddedSubList()) {
                        exec.execute(item);
                    }
                }
             }
        });

        runningTasks.bind(Bindings.createIntegerBinding(() -> (int)tlist.stream().filter(t -> t.isRunning()).count(),tlist));
        numTasks.bind(Bindings.createIntegerBinding(() -> tlist.size(),tlist));

    }

    public ReadOnlyIntegerProperty runningTasksProperty() { return runningTasks; }
    public ReadOnlyIntegerProperty numTasksProperty() { return numTasks; }
    public int getNumTasks() { return numTasks.get(); }
    public int getRunningTasks() { return runningTasks.get(); }

    @Override
    public void execute(Runnable command) {
        if(command instanceof Task<?> T) {
            Platform.runLater(() -> tlist.add(T));
        } else if(command instanceof Service<?> service) {
            RunnableTask rt = new RunnableTask(service.getTitle(), command);
            Platform.runLater(() -> tlist.add(rt));
        } else {
            RunnableTask rt = new RunnableTask("Runnable "+command.getClass().getName(),command);
            Platform.runLater(() -> tlist.add(rt));
        }
    }
}
