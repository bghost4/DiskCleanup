package org.example;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RectPacker<T> {
    final Function<T,Long> areaFunction;

    public RectPacker(Function<T, Long> areaFunction) {
        this.areaFunction = areaFunction;
    }

    public Stream<Pair<T,Bound>> pack(Bound space, Stream<T> toPackStream) {
        List<T> toPack = toPackStream.collect(Collectors.toList());
        if(toPack.size() == 1) {
            return Stream.of(new Pair<>(toPack.get(0),space));
        } else {
            final List<T> bigGroup;
            final List<T> smallGroup;

            double avg = toPack.stream().mapToDouble(areaFunction::apply).average().orElse(0);
            bigGroup = toPack.stream().filter(r -> areaFunction.apply(r) >= avg).sorted(Comparator.comparingDouble(areaFunction::apply).reversed()).collect(Collectors.toList());
            smallGroup = toPack.stream().filter(r -> !bigGroup.contains(r)).sorted(Comparator.comparingDouble(areaFunction::apply).reversed()).collect(Collectors.toList());

            if(toPack.size() == 0 ){
                throw new RuntimeException("Asked to Pack Nothing!");
            }

            if(smallGroup.size() == 0) {
                smallGroup.addAll(bigGroup);
                bigGroup.clear();
                bigGroup.add(smallGroup.remove(0));
            }

            double totalArea = toPack.stream().mapToDouble(areaFunction::apply).sum();
            double bigGroupPercent = bigGroup.stream().mapToDouble(areaFunction::apply).sum() / totalArea;

            List<Bound> groups = space.split(bigGroupPercent);
            return Stream.concat(
                    pack(groups.get(0),bigGroup.stream()),
                    pack(groups.get(1),smallGroup.stream()));

        }
    }


}
