package io.github.student.agile.utils.collection;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@UtilityClass
public class CollectionsUtils {

    public static <T> boolean isNullOrEmpty(Collection<T> collection) {
        return isNull(collection) || collection.isEmpty();
    }

    /**
     * @return Pair of two sets. First contains data that fulfill given predicate, second not.
     */
    public static <T> Pair<Set<T>, Set<T>> splitSetIntoTwoSubSets(Collection<T> collection, Predicate<T> predicate) {
        Set<T> secondSubset = new HashSet<>(collection);
        Set<T> firstSubset = collection.stream()
                .filter(predicate)
                .collect(Collectors.toSet());
        secondSubset.removeAll(firstSubset);
        return  Pair.of(firstSubset, secondSubset);
    }
}
