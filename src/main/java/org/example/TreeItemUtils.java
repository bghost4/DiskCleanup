package org.example;

import javafx.scene.control.TreeItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class TreeItemUtils {

    public static <A> Stream<A> flatMapTreeItemUnwrap(TreeItem<A> item) {
        return Stream.concat(Stream.of(item.getValue()),item.getChildren().stream().flatMap(TreeItemUtils::flatMapTreeItemUnwrap));
    }

    public static <A> Stream<TreeItem<A>> flatMapTreeItem(TreeItem<A> item) {
        return Stream.concat(Stream.of(item),item.getChildren().stream().flatMap(TreeItemUtils::flatMapTreeItem));
    }

    public static String getFriendlySize(TreeItem<StatItem> item) {
        return FileUtils.byteCountToDisplaySize(item.getValue().length());
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

    public static Stream<TreeItem<?>> pathToRoot(TreeItem<?> item,Stream<TreeItem<?>> visited) {
        Stream<TreeItem<?>> updatedVisited = Stream.concat(Stream.of(item),visited);
        if(item.getParent() == null) {
            return updatedVisited;
        } else {
            return pathToRoot(item.getParent(),updatedVisited);
        }
    }

    public static Stream<TreeItem<?>> pathToRoot(TreeItem<?> item) {
        return pathToRoot(item,Stream.empty());
    }

    public static void recursiveExpand(TreeItem<?> item) {
        pathToRoot(item).forEach(ti -> ti.setExpanded(true));
    }

    public static boolean isChildOf(TreeItem<StatItem> haystack,TreeItem<StatItem> needle) {
        return pathToRoot(needle).anyMatch(ti -> ti == haystack);
    }
}
