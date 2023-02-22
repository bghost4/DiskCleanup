package org.example;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileScannerTask extends Task<List<TreeItem<StatItem>>> {
    private final TreeItem<StatItem> parent;

    public FileScannerTask(TreeItem<StatItem> start){
        this.parent = start;
    }

    @Override
    protected List<TreeItem<StatItem>> call(){
        try {
            return Files.walk(parent.getValue().p(), 1)
                    .flatMap(c -> c.equals(parent.getValue().p()) ? Stream.empty() : Stream.of(buildTree(c)))
                    .sorted(Comparator.comparingLong((TreeItem<StatItem> i) -> i.getValue().length()).reversed())
                    .toList();
        } catch(Exception e) {
            System.err.println("File Scan Task Failed for: "+getParent().getValue().p().toString());
            return Collections.emptyList();
        }
    }

    private TreeItem<StatItem> buildTree(Path childPath) {
        TreeItem<StatItem> childItem = new TreeItem<>(StatItem.empty(childPath));
        if(Files.isRegularFile(childPath)) {
            childItem.setValue(new StatItem(childPath,false,childPath.toFile().length()));
            return childItem;
        } else if(Files.isDirectory(childPath)) {
            try {
                List<TreeItem<StatItem>> children = Files.walk(childPath, 1)
                        .flatMap(c -> c.equals(childPath) ? Stream.empty() : Stream.of(buildTree(c)))
                        .sorted(Comparator.comparingLong((TreeItem<StatItem> i) -> i.getValue().length()).reversed())
                        .toList();
                childItem.getChildren().addAll(children);
                childItem.setValue(childItem.getValue().update(
                        childItem.getChildren().stream().mapToLong(c -> c.getValue().length()).sum()
                ));
            } catch (IOException e) {
                //throw new RuntimeException(e);
                //todo warn about not being able to walk directory
                //todo set a flag on this tree item that it failed
                e.printStackTrace();
                childItem.setValue(childItem.getValue().update(0));
            }
        }
        return childItem;
    }

    public TreeItem<StatItem> getParent() { return parent; }

//    void updateParent(TreeItem<StatItem> parent) {
//        if(parent == null) {
//            return;
//        }
//        if(parent.getChildren().stream().noneMatch(ti -> ti.getValue().isProcesing())) {
//            parent.setValue(parent.getValue().update(
//                    parent.getChildren().stream().mapToLong(
//                            ti -> ti.getValue().length()
//                    ).sum()
//            ));
//            parent.getChildren().sort(Comparator.comparingLong((TreeItem<StatItem> ti) ->ti.getValue().length()).reversed());
//        }
//    }

}
