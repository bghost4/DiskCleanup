package org.example;

import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;

public record StatItem(Path p, PathType pathType,boolean isProcesing, long length, String type, String ext, Instant createTime, Instant modTime,
                       UserPrincipal owner) {
    public static StatItem empty(Path p) {
        return new StatItem(p, PathType.DIRECTORY, true, 0, null, null, null, null, null);
    }

    public StatItem update(long length) { return new StatItem(p, pathType,false,length,type,ext,null,null,null); }

    public StatItem update(long length,String type,String ext,Instant cTime,Instant mTime,UserPrincipal owner,PathType pathtype) {
        return new StatItem(p, pathtype,false, length,type,ext,cTime,mTime,owner);
    }

    @Override
    public String toString() {
        return p().getFileName().toString();
    }
}
