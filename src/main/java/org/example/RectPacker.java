package org.example;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RectPacker<T,R> {
    final Function<T,Long> areaFunction;
    final BiFunction<T,Bound,R> mappingFunction;

    public RectPacker(Function<T, Long> areaFunction, BiFunction<T, Bound, R> mappingFunction) {
        this.areaFunction = areaFunction;
        this.mappingFunction = mappingFunction;
    }

    public Stream<R> pack(Bound space, List<T> toPack) {
        if(toPack.size() == 1) {
            return Stream.of(mappingFunction.apply(toPack.get(0),space));
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
                    pack(groups.get(0),bigGroup),
                    pack(groups.get(1),smallGroup));

        }
    }


}
