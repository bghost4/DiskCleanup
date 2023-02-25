package org.example;

import java.nio.file.Path;

public record StatItem(Path p, boolean isProcesing, long length) {
    public static StatItem empty(Path p) {
        return new StatItem(p, true, 0);
    }

    public StatItem update(long length) {
        return new StatItem(p, false, length);
    }
}
