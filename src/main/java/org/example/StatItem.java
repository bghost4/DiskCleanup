package org.example;

import java.nio.file.Path;

public record StatItem(Path p, boolean isProcesing, long length,String type,String ext) {
    public static StatItem empty(Path p) {
        return new StatItem(p, true, 0,null,null);
    }

    public StatItem update(long length) { return new StatItem(p,false,length,type,ext); }

    public StatItem update(long length,String type,String ext) {
        return new StatItem(p, false, length,type,ext);
    }

}
