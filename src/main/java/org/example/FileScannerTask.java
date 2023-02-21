package org.example;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileScannerTask extends Task<TreeItem<MainWindow.TI>> {

    private TreeItem<MainWindow.TI> myItem;
    Executor executor;

    final Consumer<TreeItem<MainWindow.TI>> whenDoneFunc;

    public FileScannerTask(TreeItem<MainWindow.TI> start,Executor e,Consumer<TreeItem<MainWindow.TI>> doneFunc){
        this.myItem = start;
        this.executor = e;
        whenDoneFunc = doneFunc;
    }

    public FileScannerTask(TreeItem<MainWindow.TI> start,Executor e){
        this.myItem = start;
        this.executor = e;
        whenDoneFunc = (i) -> {};
    }



    @Override
    protected TreeItem<MainWindow.TI> call() throws Exception {
        System.out.println("Scanning: "+myItem.getValue().toString());
        if(Files.isDirectory(myItem.getValue().p())) {
            List<TreeItem<MainWindow.TI>> children = Files.walk(myItem.getValue().p(), 1)
                    .flatMap(c -> c.equals(myItem.getValue().p()) ? Stream.empty() : Stream.of(MainWindow.TI.empty(c)))
                    .map(TreeItem::new).collect(Collectors.toList());
            Platform.runLater(() -> {
                myItem.getChildren().addAll(children);
//                myItem.addEventHandler(TreeItem.childrenModificationEvent(),evt -> {
//                    long total =myItem.getChildren().size();
//                    long done = myItem.getChildren().stream().filter(i -> !i.getValue().isProcesing()).count();
//                    System.out.println("Event: "+myItem.getValue().p().toString()+" "+done+"/"+total);
//
//                    if(myItem.getChildren().stream().noneMatch(ti -> ti.getValue().isProcesing())) {
//                        myItem.getValue().update(myItem.getChildren().stream().mapToLong(c -> c.getValue().length()).sum());
//                        System.out.println("Fiished: "+myItem.getValue().p().toString());
//                    } else {
//                        myItem.getChildren().stream().filter(ti -> ti.getValue().isProcesing()).forEach(ti -> {
//                            System.out.println("\tWaiting on: "+ti.getValue().p());
//                        });
//                    }
//                });
            });

            NestedTask childrenTasks = new NestedTask(
                    children.stream().map(
                            ti -> new FileScannerTask(ti,executor)
                    ).collect(Collectors.toList()),executor
            );

            executor.execute(childrenTasks);
        } else {
            myItem.setValue(myItem.getValue().update(myItem.getValue().p().toFile().length()));
            //if All my siblings are processed, update the parent to show it
            //this isn't a great way to do it, but its the "easiest"
            if(myItem.getParent().getChildren().stream().noneMatch(ti -> ti.getValue().isProcesing())) {
                myItem.getParent().setValue(myItem.getParent().getValue().update(
                        myItem.getParent().getChildren().stream().mapToLong(
                                ti -> ti.getValue().length()
                        ).sum()
                ));
            }
        }
        return myItem;
    }

    private class NestedTask<Void> extends Task<Void> {

        private List<Task<FileScannerTask>> dependantTasks;
        private int runningTasks = 0;
        private Executor executor;

        public NestedTask(List<Task<FileScannerTask>> dependants, Executor executor) {
            dependantTasks = dependants;
            runningTasks = dependantTasks.size();
            this.executor = executor;
        }

        @Override
        protected Void call() throws Exception {
            EventHandler<WorkerStateEvent> finishedHandler = (e) -> {
                runningTasks--;
            };

            dependantTasks.stream().forEach( t -> {
                executor.execute(t);
            });

            return null;
        }
    }

}
