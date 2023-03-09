package org.example;

import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class FileScannerTask extends Task<List<TreeItem<StatItem>>> {
    private final TreeItem<StatItem> parent;
    private final Function<Path,String> typeExtractor;

    public FileScannerTask(TreeItem<StatItem> start,Function<Path,String> typeExtractor){
        this.typeExtractor = typeExtractor;
        this.parent = start;
    }

    @Override
    protected List<TreeItem<StatItem>> call(){
        try {
            Path path = TreeItemUtils.buildPath(parent);
            //System.out.println("File Scanner Call: "+path);

            return Files.walk(path, 1)
                    .flatMap(c -> c.getFileName().equals(parent.getValue().p().getFileName()) ? Stream.empty() : Stream.of(buildTree(path,c)))
                    .sorted(Comparator.comparingLong((TreeItem<StatItem> i) -> i.getValue().length()).reversed())
                    .toList();
        } catch(Exception e) {
            System.err.println("File Scan Task Failed for: "+getParent().getValue().p().toString());
            return Collections.emptyList();
        }
    }

    private TreeItem<StatItem> buildTree(Path parent,Path childPath) {
        //System.out.println("Build Tree Called on: "+childPath);
        TreeItem<StatItem> childItem = new TreeItem<>(StatItem.empty(parent.relativize(childPath)));
        if(Files.isRegularFile(childPath)) {
            try {
                PathType pt;
                if(Files.isSymbolicLink(childPath)) {
                    pt = PathType.LINK;
                } else {
                    pt = PathType.FILE;
                }
                BasicFileAttributes bfa = Files.readAttributes(childPath, BasicFileAttributes.class);
                FileOwnerAttributeView foa = Files.getFileAttributeView(childPath,FileOwnerAttributeView.class);
                childItem.setValue(new StatItem(parent.relativize(childPath),pt,false,pt == PathType.FILE ? childPath.toFile().length() : 0, typeExtractor.apply(childPath), FilenameUtils.getExtension(childPath),bfa.creationTime().toInstant(),bfa.lastModifiedTime().toInstant(),foa.getOwner()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return childItem;
        } else if(Files.isDirectory(childPath)) {
            try {
                List<TreeItem<StatItem>> children = Files.walk(childPath, 1)
                        .flatMap(c -> c.getFileName().equals(childPath.getFileName()) ? Stream.empty() : Stream.of(buildTree(childPath,c)))
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

}
