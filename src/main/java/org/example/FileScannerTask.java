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
import java.util.Comparator;
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
        //System.out.println("Scanning: "+myItem.getValue().toString());
        if(Files.isDirectory(myItem.getValue().p())) {
            List<TreeItem<MainWindow.TI>> children = Files.walk(myItem.getValue().p(), 1)
                    .flatMap(c -> c.equals(myItem.getValue().p()) ? Stream.empty() : Stream.of(MainWindow.TI.empty(c)))
                    .map(TreeItem::new).collect(Collectors.toList());
            if(children.size() != 0) {
                Platform.runLater(() -> {
                    myItem.getChildren().addAll(children);
                    myItem.addEventHandler(TreeItem.childrenModificationEvent(),evt -> {
                        updateParent(myItem.getParent());
                    });
                });

                children.stream().map(
                        ti -> new FileScannerTask(ti,executor)
                ).forEach(t -> executor.execute(t));

            } else {
                Platform.runLater(() -> {
                    myItem.setValue(myItem.getValue().update(0));
                    updateParent(myItem.getParent());
                });


            }
        } else {
            Platform.runLater(() ->{
                myItem.setValue(myItem.getValue().update(myItem.getValue().p().toFile().length()));
                updateParent(myItem.getParent());
            } );
        }
        return myItem;
    }

    void updateParent(TreeItem<MainWindow.TI> parent) {
        if(parent == null) {
            return;
        }
        if(parent.getChildren().stream().noneMatch(ti -> ti.getValue().isProcesing())) {
            parent.setValue(parent.getValue().update(
                    parent.getChildren().stream().mapToLong(
                            ti -> ti.getValue().length()
                    ).sum()
            ));
            parent.getChildren().sort(Comparator.comparingLong((TreeItem<MainWindow.TI> ti) ->ti.getValue().length()).reversed());
        }
    }

}
