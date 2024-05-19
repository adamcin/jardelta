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

import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Emitter;
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
 * Provides differs for building {@link net.adamcin.jardelta.api.diff.Action#ADDED},
 * {@link net.adamcin.jardelta.api.diff.Action#REMOVED}, {@link net.adamcin.jardelta.api.diff.Action#ERR_LEFT}, and
 * {@link net.adamcin.jardelta.api.diff.Action#ERR_RIGHT} diffs based on generic container types, with delegation to
 * provided lambdas for non-generic type handling when the containers are otherwise identical.
 */
public final class GenericDiffers {
    static final String ERROR_AT_MOST_ONE = "unexpected plurality";

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when {@code equalityTest} returns true,
     * and {@link net.adamcin.jardelta.api.diff.Action#CHANGED} when it returns false.
     *
     * @param emitter      a diff builder
     * @param values       both values
     * @param equalityTest delegate function for present-present case
     * @param <T>          the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofObjectEquality(
            @NotNull Emitter emitter,
            @NotNull Both<T> values,
            @NotNull BiPredicate<? super T, ? super T> equalityTest) {
        if (values.testBoth(equalityTest)) {
            return Stream.empty();
        } else {
            return Stream.of(emitter.changed(values.map(Objects::toString)));
        }
    }

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when
     * {@link Objects#deepEquals(Object, Object)} returns true, and {@link net.adamcin.jardelta.api.diff.Action#CHANGED}
     * when it returns false.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofObjectEquality(
            @NotNull Emitter emitter,
            @NotNull Both<T> values) {
        return ofObjectEquality(emitter, values, Objects::deepEquals);
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Action#REMOVED}
     * for present-empty, while delegating present-present to the provided {@link java.util.function.Function}.
     *
     * @param emitter       a diff builder
     * @param values        both values
     * @param ifBothPresent delegate function for present-present case
     * @param <T>           the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofOptionals(
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothPresent) {
        if (values.left().isEmpty()) {
            return values.right().stream().map(value -> emitter.added(Objects.toString(value)));
        } else if (values.right().isEmpty()) {
            return values.left().stream().map(value -> emitter.removed(Objects.toString(value)));
        } else {
            return ifBothPresent.apply(values.map(Optional::get));
        }
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Action#REMOVED}
     * for present-empty, while delegating present-present to
     * {@link #ofObjectEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofOptionals(
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values) {
        return ofOptionals(emitter, values, present -> ofObjectEquality(emitter, present));
    }

    /**
     * Compare both results, delegating present-present to the provided {@link java.util.function.Function} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Action#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Action#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param emitter       a diff builder
     * @param values        both values
     * @param ifBothSuccess delegate function for success-success case
     * @param <T>           the type parameter of the Result values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofResults(
            @NotNull Emitter emitter,
            @NotNull Both<Result<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothSuccess) {
        if (values.map(Result::isSuccess).testBoth((left, right) -> left && right)) {
            return ifBothSuccess.apply(values.map(Result::getOrThrow));
        } else {
            return emitter.errBoth(values);
        }
    }

    /**
     * Compare both results, delegating present-present to
     * {@link #ofObjectEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Action#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Action#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Result values
     * @return the diff stream
     * @see #ofResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both, java.util.function.Function)
     */
    @NotNull
    public static <T> Stream<Diff> ofResults(
            @NotNull Emitter emitter,
            @NotNull Both<Result<T>> values) {
        return ofResults(emitter, values, successes -> ofObjectEquality(emitter, successes));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.Function)} and {@link #ofOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.Function)}
     * and then to the provided {@code ifBothSingle} function.
     *
     * @param emitter      a diff builder
     * @param values       both values
     * @param ifBothSingle delegate function for single-single case
     * @param <T>          the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAtMostOne(
            @NotNull Emitter emitter,
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
                    return Result.failure(ERROR_AT_MOST_ONE);
                } else {
                    return Result.success(Optional.ofNullable(first));
                }
            }
        });
        return ofResults(emitter, iterated, results ->
                ofOptionals(emitter, results, ifBothSingle));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.Function)} and {@link #ofOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.Function)}
     * and then to {@link #ofObjectEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAtMostOne(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Iterable<T>> values) {
        return ofAtMostOne(emitter, values, present -> ofObjectEquality(emitter, present));
    }

    /**
     * An internal utility function that serves as both a reducer and combiner for a stream of collections reduced to a
     * single collection of a possibly different type.
     *
     * @param left  the accumulator or left-hand binary operand
     * @param right the next stream collection element or the right-hand binary operand
     * @param <T>   the element type of the collections
     * @param <C>   the type of the returned accumulator collection
     * @param <D>   the type of the streamed collections
     * @return a union collection
     */
    static <T, C extends Collection<T>, D extends Collection<T>> C combineCollections(C left, D right) {
        left.addAll(right);
        return left;
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !contains-contains,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for contains-!contains, and delegating to the provided
     * {@code ifIntersection} function for contains-contains.
     *
     * @param emitterFactory a diff builder factory function
     * @param bothSets       both values
     * @param setSupplier    provide a {@link java.util.Set} supplier appropriate for element type {@code T}
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Function<? super T, Emitter> emitterFactory,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Supplier<? extends Set<T>> setSupplier,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        final Set<T> allValues = bothSets.stream().reduce(setSupplier.get(),
                GenericDiffers::combineCollections, GenericDiffers::combineCollections);

        Stream<Diff> stream = Stream.empty();
        for (T value : allValues) {
            final Emitter childEmitter = emitterFactory.apply(value);
            if (!bothSets.left().contains(value)) {
                if (bothSets.right().contains(value)) {
                    stream = Stream.concat(stream, Stream.of(childEmitter.added()));
                }
                // empty-empty case, leave stream unmodified
            } else if (!bothSets.right().contains(value)) {
                stream = Stream.concat(stream, Stream.of(childEmitter.removed()));
            } else {
                stream = Stream.concat(stream, ifIntersection.apply(childEmitter, value));
            }
        }

        return stream;
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitterFactory a diff builder factory function
     * @param bothSets       both values
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both, java.util.function.Supplier,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Function<? super T, Emitter> emitterFactory,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(emitterFactory, bothSets, TreeSet::new, ifIntersection);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitterFactory a diff builder factory function
     * @param bothSets       both values
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both, java.util.function.Supplier,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Function<? super T, Emitter> emitterFactory,
            @NotNull Both<? extends Collection<T>> bothSets) {
        return ofAllInEitherSet(emitterFactory, bothSets, (emitter, elements) -> Stream.empty());
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     *
     * @param emitter        a diff emitter
     * @param bothSets       both values
     * @param setSupplier    provide a {@link java.util.Set} supplier appropriate for element type {@code T}
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both, java.util.function.Supplier,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Supplier<? extends Set<T>> setSupplier,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(element -> emitter, bothSets, setSupplier, ifIntersection);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitter        a diff emitter
     * @param bothSets       both values
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(element -> emitter, bothSets, ifIntersection);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitter  a diff emitter
     * @param bothSets both values
     * @param <T>      the type parameter of the Collection values
     * @return the diff stream
     * @see #ofAllInEitherSet(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     * @see #ofAllInEitherSet(java.util.function.Function, net.adamcin.streamsupport.Both)
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Collection<T>> bothSets) {
        return ofAllInEitherSet(element -> emitter, bothSets, (childEmitter, elements) -> Stream.empty());
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     *
     * @param emitterFactory a diff emitter factory function
     * @param bothMaps       both values
     * @param setSupplier    provide a {@link java.util.Set} supplier appropriate for element type {@code K}
     * @param ifIntersection delegate function for intersecting keys
     * @param <K>            the type parameter of the map's keys
     * @param <V>            the type parameter of the map's values
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super K, Emitter> emitterFactory,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull Supplier<? extends Set<K>> setSupplier,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherSet(emitterFactory, bothMaps.map(Map::keySet), setSupplier,
                (emitter, key) -> ifIntersection.apply(emitter, bothMaps.mapOptional(map -> map.get(key))));
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitterFactory a diff emitter factory function
     * @param bothMaps       both values
     * @param ifIntersection delegate function for intersecting keys
     * @param <K>            the key type parameter of the Map
     * @param <V>            the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super K, Emitter> emitterFactory,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(emitterFactory, bothMaps, TreeSet::new, ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitterFactory a diff emitter factory function
     * @param bothMaps       both values
     * @param <K>            the key type parameter of the Map
     * @param <V>            the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super K, Emitter> emitterFactory,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return ofAllInEitherMap(emitterFactory, bothMaps, GenericDiffers::ofOptionals);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     *
     * @param emitter        a diff emitter
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
            @NotNull Emitter emitter,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull Supplier<? extends Set<K>> setSupplier,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(key -> emitter, bothMaps, setSupplier, ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitter        a diff emitter
     * @param bothMaps       both values
     * @param ifIntersection delegate function for intersecting keys
     * @param <K>            the key type parameter of the Map
     * @param <V>            the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     * @see #ofAllInEitherMap(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(key -> emitter, bothMaps, ifIntersection);
    }


    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Action#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Action#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param emitter  a diff emitter
     * @param bothMaps both values
     * @param <K>      the key type parameter of the Map
     * @param <V>      the value type parameter of the Map
     * @return the diff stream
     * @see #ofAllInEitherMap(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.Supplier, java.util.function.BiFunction)
     * @see #ofAllInEitherMap(java.util.function.Function, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return ofAllInEitherMap(key -> emitter, bothMaps);
    }
}
