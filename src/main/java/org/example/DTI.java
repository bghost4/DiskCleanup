package org.example;

import javafx.scene.control.TreeItem;

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
        if(hash == null) {
            //Tree Item
            return TreeItemUtils.buildPath(item).toString();
        } else {
            //Hexdump
            return hash;
        }
    }

    public TreeItem<StatItem> getItem() {
        return item;
    }
}
