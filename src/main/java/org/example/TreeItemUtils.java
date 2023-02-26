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

    public static Tika typeDetector = new Tika();

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
        return getExtension(item.getValue().p());
    }

    public static String getExtension(Path p) {
        if(Files.isDirectory(p)) {
            return "<Directory>";
        } else {
            String type = FilenameUtils.getExtension(p.getFileName().toString());
            if(type.isBlank() || type.isEmpty()) {
                return "<Typeless>";
            } else {
                return type;
            }
        }
    }
//
//    public static String getType(Path p) {
//        try {
//            return typeDetector.detect(p);
//        } catch(IOException e) {
//            return "<ERROR>";
//        }
//    }
//
//    public static String getType(TreeItem<StatItem> item) {
//       return getType(item.getValue().p());
//    }

    public static boolean isRegularFile(TreeItem<StatItem> statItemTreeItem) {
        return Files.isRegularFile(statItemTreeItem.getValue().p());
    }

    public static boolean isDirectory(TreeItem<StatItem> statItemTreeItem) {
        return Files.isDirectory(statItemTreeItem.getValue().p());
    }

    public static void recursiveExpand(TreeItem<?> item) {
        if(item.getParent() == null) { return; }
        item.getParent().setExpanded(true);
        recursiveExpand(item.getParent());
    }

    public static boolean isChildOf(TreeItem<StatItem> haystack,TreeItem<StatItem> needle) {
        TreeItem<StatItem> last = needle;
        if(haystack == needle) { return true; }
        while(last.getParent() != null) {
            if(last.getParent() == haystack) {
                return true;
            }
            last = last.getParent();
        }
        return false;
    }
}
