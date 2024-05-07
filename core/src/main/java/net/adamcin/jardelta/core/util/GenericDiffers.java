/*
 * Copyright 2024 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.jardelta.core.util;

import net.adamcin.jardelta.core.Action;
import net.adamcin.jardelta.core.Diff;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Provides differs for building {@link net.adamcin.jardelta.core.Action#ADDED},
 * {@link net.adamcin.jardelta.core.Action#REMOVED}, {@link net.adamcin.jardelta.core.Action#ERR_LEFT}, and
 * {@link net.adamcin.jardelta.core.Action#ERR_RIGHT} diffs based on generic container types, with delegation to
 * provided lambdas for non-generic type handling when the containers are otherwise identical.
 */
public final class GenericDiffers {

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when {@code equalityTest} returns true,
     * and {@link net.adamcin.jardelta.core.Action#CHANGED} when it returns false.
     *
     * @param diffBuilder  a diff builder
     * @param values       both values
     * @param equalityTest delegate function for present-present case
     * @param <T>          the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofObjectEquality(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<T> values,
            @NotNull BiPredicate<? super T, ? super T> equalityTest) {
        if (values.testBoth(equalityTest)) {
            return Stream.empty();
        } else {
            return Stream.of(diffBuilder.changed());
        }
    }

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when
     * {@link Objects#deepEquals(Object, Object)} returns true, and {@link net.adamcin.jardelta.core.Action#CHANGED}
     * when it returns false.
     *
     * @param diffBuilder a diff builder
     * @param values      both values
     * @param <T>         the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofObjectEquality(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<T> values) {
        return ofObjectEquality(diffBuilder, values, Objects::deepEquals);
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.core.Action#ADDED} for empty-present, and {@link net.adamcin.jardelta.core.Action#REMOVED}
     * for present-empty, while delegating present-present to the provided {@link java.util.function.Function}.
     *
     * @param diffBuilder   a diff builder
     * @param values        both values
     * @param ifBothPresent delegate function for present-present case
     * @param <T>           the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofOptionals(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<Optional<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothPresent) {
        if (values.left().isEmpty()) {
            if (values.right().isPresent()) {
                return Stream.of(diffBuilder.added());
            }
            // empty-empty case, falls through to Stream.empty()
        } else if (values.right().isEmpty()) {
            return Stream.of(diffBuilder.removed());
        } else {
            return ifBothPresent.apply(values.map(Optional::get));
        }
        return Stream.empty();
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.core.Action#ADDED} for empty-present, and {@link net.adamcin.jardelta.core.Action#REMOVED}
     * for present-empty, while delegating present-present to
     * {@link #ofObjectEquality(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both)}.
     *
     * @param diffBuilder a diff builder
     * @param values      both values
     * @param <T>         the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofOptionals(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<Optional<T>> values) {
        return ofOptionals(diffBuilder, values, present -> ofObjectEquality(diffBuilder, present));
    }

    /**
     * Compare both results, delegating present-present to the provided {@link java.util.function.Function} for
     * success-success, while returning {@link net.adamcin.jardelta.core.Action#ERR_LEFT},
     * {@link net.adamcin.jardelta.core.Action#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param diffBuilder   a diff builder
     * @param values        both values
     * @param ifBothSuccess delegate function for success-success case
     * @param <T>           the type parameter of the Result values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofResults(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<Result<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothSuccess) {
        if (values.map(Result::isSuccess).testBoth((left, right) -> left && right)) {
            return ifBothSuccess.apply(values.map(Result::getOrThrow));
        } else {
            return values
                    .map(Result::getError)
                    .flatMap((left, right) -> Both.of(
                            left.map(error -> Action.ERR_LEFT),
                            right.map(error -> Action.ERR_RIGHT)))
                    .stream()
                    .flatMap(Optional::stream)
                    .map(diffBuilder::build);
        }
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.core.Diff.Builder,
     * net.adamcin.streamsupport.Both, java.util.function.Function)} and {@link #ofOptionals(net.adamcin.jardelta.core.Diff.Builder,
     * net.adamcin.streamsupport.Both, java.util.function.Function)}
     * and then to the provided {@code ifBothSingle} function.
     *
     * @param diffBuilder  a diff builder
     * @param values       both values
     * @param ifBothSingle delegate function for single-single case
     * @param <T>          the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAtMostOne(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Iterable<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothSingle) {
        Both<Result<Optional<T>>> iterated = values.map(Iterable::iterator).map(iter -> {
            if (!iter.hasNext()) {
                return Result.success(Optional.empty());
            } else {
                // take the first element
                T first = iter.next();
                // map to failure if there is a second
                if (iter.hasNext()) {
                    return Result.failure("more than one element is present");
                } else {
                    return Result.success(Optional.ofNullable(first));
                }
            }
        });
        return ofResults(diffBuilder, iterated, results ->
                ofOptionals(diffBuilder, results, ifBothSingle));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.core.Diff.Builder,
     * net.adamcin.streamsupport.Both, java.util.function.Function)} and {@link #ofOptionals(net.adamcin.jardelta.core.Diff.Builder,
     * net.adamcin.streamsupport.Both, java.util.function.Function)}
     * and then to {@link #ofObjectEquality(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both)}.
     *
     * @param diffBuilder a diff builder
     * @param values      both values
     * @param <T>         the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAtMostOne(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Iterable<T>> values) {
        return ofAtMostOne(diffBuilder, values, present -> ofObjectEquality(diffBuilder, present));
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !contains-contains,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for contains-!contains, and delegating to the provided
     * {@code ifIntersection} function for contains-contains.
     *
     * @param diffBuilderFactory a diff builder factory function
     * @param bothSets           both values
     * @param setSupplier        provide a {@link java.util.Set} supplier appropriate for element type {@code T}
     * @param ifIntersection     delegate function for intersecting elements
     * @param <T>                the type parameter of the Collection values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Function<? super T, Diff.Builder> diffBuilderFactory,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Supplier<Set<T>> setSupplier,
            @NotNull Function<T, Stream<Diff>> ifIntersection) {
        final Set<T> allValues = bothSets.stream().reduce(setSupplier.get(), (acc, next) -> {
            acc.addAll(next);
            return acc;
        }, (first, second) -> {
            first.addAll(second);
            return first;
        });

        Stream<Diff> stream = Stream.empty();
        for (T value : allValues) {
            if (!bothSets.left().contains(value)) {
                if (bothSets.right().contains(value)) {
                    stream = Stream.concat(stream, Stream.of(diffBuilderFactory.apply(value).added()));
                }
                // empty-empty case, falls through to Stream.empty()
            } else if (!bothSets.right().contains(value)) {
                stream = Stream.concat(stream, Stream.of(diffBuilderFactory.apply(value).removed()));
            } else {
                stream = Stream.concat(stream, ifIntersection.apply(value));
            }
        }

        return stream;
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilderFactory a diff builder factory function
     * @param bothSets           both values
     * @param ifIntersection     delegate function for intersecting elements
     * @param <T>                the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both, java.util.function.Supplier,
     * java.util.function.Function)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Function<? super T, Diff.Builder> diffBuilderFactory,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Function<T, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(diffBuilderFactory, bothSets, TreeSet::new, ifIntersection);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilderFactory a diff builder factory function
     * @param bothSets           both values
     * @param <T>                the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both, java.util.function.Supplier,
     * java.util.function.Function)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Function<? super T, Diff.Builder> diffBuilderFactory,
            @NotNull Both<? extends Collection<T>> bothSets) {
        return ofAllInEitherSet(diffBuilderFactory, bothSets, (elements) -> Stream.empty());
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     *
     * @param diffBuilder    a diff builder
     * @param bothSets       both values
     * @param setSupplier    provide a {@link java.util.Set} supplier appropriate for element type {@code T}
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both, java.util.function.Supplier,
     * java.util.function.Function)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Supplier<Set<T>> setSupplier,
            @NotNull Function<T, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(element -> diffBuilder, bothSets, setSupplier, ifIntersection);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilder    a diff builder
     * @param bothSets       both values
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.Function)
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.Function)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Function<T, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(element -> diffBuilder, bothSets, ifIntersection);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilder a diff builder
     * @param bothSets    both values
     * @param <T>         the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.Function)
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Collection<T>> bothSets) {
        return ofAllInEitherSet(element -> diffBuilder, bothSets, (elements) -> Stream.empty());
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     *
     * @param diffBuilderFactory a diff builder factory function
     * @param bothMaps           both values
     * @param setSupplier        provide a {@link java.util.Set} supplier appropriate for element type {@code K}
     * @param ifIntersection     delegate function for intersecting keys
     * @param <K>                the type parameter of the map's keys
     * @param <V>                the type parameter of the map's values
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super K, Diff.Builder> diffBuilderFactory,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull Supplier<Set<K>> setSupplier,
            @NotNull BiFunction<K, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(diffBuilderFactory, bothMaps.map(Map::keySet), setSupplier,
                key -> ifIntersection.apply(key, bothMaps.mapOptional(map -> map.get(key))));
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilderFactory a diff builder factory function
     * @param bothMaps           both values
     * @param ifIntersection     delegate function for intersecting keys
     * @param <K>                the key type parameter of the Map
     * @param <V>                the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super K, Diff.Builder> diffBuilderFactory,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<K, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(diffBuilderFactory, bothMaps, TreeSet::new, ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilderFactory a diff builder factory function
     * @param bothMaps           both values
     * @param <K>                the key type parameter of the Map
     * @param <V>                the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super K, Diff.Builder> diffBuilderFactory,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return ofAllInEitherMap(diffBuilderFactory, bothMaps,
                (key, bothValues) -> ofOptionals(diffBuilderFactory.apply(key), bothValues));
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     *
     * @param diffBuilder    a diff builder
     * @param bothMaps       both values
     * @param setSupplier    provide a {@link java.util.Set} supplier appropriate for element type {@code K}
     * @param ifIntersection delegate function for intersecting keys
     * @param <K>            the type parameter of the map's keys
     * @param <V>            the type parameter of the map's values
     * @return the diff stream
     * @see #ofAllInEitherMap(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull Supplier<Set<K>> setSupplier,
            @NotNull BiFunction<K, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(key -> diffBuilder, bothMaps, setSupplier, ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilder    a diff builder
     * @param bothMaps       both values
     * @param ifIntersection delegate function for intersecting keys
     * @param <K>            the key type parameter of the Map
     * @param <V>            the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     * @see #ofAllInEitherMap(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<K, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(key -> diffBuilder, bothMaps, ifIntersection);
    }


    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.core.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.core.Action#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param diffBuilder    a diff builder
     * @param bothMaps       both values
     * @param <K>            the key type parameter of the Map
     * @param <V>            the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.core.Diff.Builder, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     * @see #ofAllInEitherMap(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return ofAllInEitherMap(key -> diffBuilder, bothMaps);
    }
}
