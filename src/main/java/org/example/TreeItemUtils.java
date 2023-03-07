package org.example;

import javafx.scene.control.TreeItem;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class TreeItemUtils {

    public static <A> Stream<A> flatMapTreeItemUnwrap(TreeItem<A> item) {
        return Stream.concat(Stream.of(item.getValue()),item.getChildren().stream().flatMap(TreeItemUtils::flatMapTreeItemUnwrap));
    }

    public static <A> Stream<TreeItem<A>> flatMapTreeItem(TreeItem<A> item) {
        return Stream.concat(Stream.of(item),item.getChildren().stream().flatMap(TreeItemUtils::flatMapTreeItem));
    }

    public static String relativize(TreeItem<StatItem> context,TreeItem<StatItem> other) {
        return context.getValue().p().relativize(other.getValue().p()).toString();
    }

    public static String getExtension(TreeItem<StatItem> item) {
        return item.getValue().ext();
    }

    public static String getType(TreeItem<StatItem> item) {
       return item.getValue().type();
    }

    public static boolean isRegularFile(TreeItem<StatItem> statItemTreeItem) {
        return statItemTreeItem.getValue().pathType() == PathType.FILE;
    }

    public static boolean isDirectory(TreeItem<StatItem> statItemTreeItem) {
        return statItemTreeItem.getValue().pathType() == PathType.DIRECTORY;
    }

    public static <T> Stream<TreeItem<T>> pathToRoot(TreeItem<T> item,Stream<TreeItem<T>> previouslyVisited) {
        Stream<TreeItem<T>> visited = Stream.concat(Stream.of(item),previouslyVisited);
        if(item.getParent() == null) {
            return visited;
        } else {
            return pathToRoot(item.getParent(),visited);
        }
    }

    public static Path buildPath(TreeItem<StatItem> item) {
        List<TreeItem<StatItem>> fullpath = pathToRoot(item).toList();
        Path p = fullpath.get(0).getValue().p();
        for(int i=1; i < fullpath.size(); i++) {
            p = p.resolve(fullpath.get(i).getValue().p().toString());
        }
        return p;
    }

    public static <T> Stream<TreeItem<T>> pathToRoot(TreeItem<T> item) {
        return pathToRoot(item,Stream.empty());
    }

    public static void recursiveExpand(TreeItem<?> item) {
        pathToRoot(item).forEach(ti -> ti.setExpanded(true));
    }

    public static boolean isChildOf(TreeItem<StatItem> haystack,TreeItem<StatItem> needle) {
        return pathToRoot(needle).anyMatch(ti -> ti == haystack);
    }
}
