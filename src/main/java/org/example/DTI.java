package org.example;

import javafx.scene.control.TreeItem;

import java.util.Objects;

public class DTI {
    private final String hash;
    private final boolean iitem;
    private final TreeItem<StatItem> item;

    public DTI(String hash) {
        item = null;
        this.hash = hash;
        iitem = false;
    }

    public DTI(TreeItem<StatItem> item) {
        this.item = item;
        iitem = true;
        this.hash = null;
    }

    public boolean isItem() {
        return iitem;
    }

    public String toString() {
        //Tree Item
        //Hexdump
        return Objects.requireNonNullElseGet(hash, () -> TreeItemUtils.buildPath(item).toString());
    }

    public TreeItem<StatItem> getItem() {
        return item;
    }
}
