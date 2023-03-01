package org.example;

import java.util.Optional;
import java.util.stream.Stream;

enum FileSizeSuffix {
    B(.9), kB(1e3), MB(1e6), GB(1e9), TB (1e12), PB(1e15); //, EB, ZB, YB, RB, QB If we get here deal with it then
    private final double limit;
    private final double exponent;
    private FileSizeSuffix(double exponent) {
        limit = 1.1 * exponent;
        this.exponent = exponent;
    }

    private static Optional<FileSizeSuffix> bestMatch(Number value) {
        return Stream.of(PB,TB,GB,MB,kB,B).sequential().filter(s -> value.doubleValue() > s.limit).findFirst();
    }

    private String fmt(Number d) {
        if(this == B) {
            return String.format("%d %s",d.longValue(),B);
        } else {
            return String.format("%.1f %s", (d.doubleValue() / this.exponent), this);
        }
    }

    public static String format(Number d) {
        return bestMatch(d.doubleValue()).map(s -> s.fmt(d.doubleValue())).orElse(B.fmt(d));
    }

}