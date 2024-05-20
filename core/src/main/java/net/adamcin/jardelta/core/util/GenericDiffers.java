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
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Provides differs for building {@link net.adamcin.jardelta.api.diff.Verb#ADDED},
 * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}, {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT}, and
 * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT} diffs based on generic container types, with delegation to
 * provided lambdas for non-generic type handling when the containers are otherwise identical.
 */
public final class GenericDiffers {
    static final String ERROR_AT_MOST_ONE = "unexpected plurality";
    static final BiPredicate<Object, Object> DEFAULT_EQUALITY_TEST = Objects::deepEquals;
    static final Function<Object, Optional<String>> DEFAULT_HINTER = Fun.compose1(
            value -> value instanceof Object[] ? Arrays.deepToString((Object[]) value) : Objects.toString(value),
            Optional::of);

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when {@code equalityTest} returns true,
     * and {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
     *
     * @param hinter       a function to produce a hint for a given value
     * @param emitter      a diff builder
     * @param values       both values
     * @param equalityTest delegate function for present-present case
     * @param <T>          the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofObjectEquality(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<T> values,
            @NotNull BiPredicate<? super T, ? super T> equalityTest) {
        if (values.testBoth(equalityTest)) {
            return Stream.empty();
        } else {
            Both<Optional<String>> hints = values.map(hinter);
            if (hints.map(Optional::isPresent).testBoth((left, right) -> left && right)) {
                return Stream.of(emitter.changed(hints.map(Optional::get)));
            } else {
                return Stream.of(emitter.changed());
            }
        }
    }

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when {@code equalityTest} returns true,
     * and {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
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
        return ofObjectEquality(DEFAULT_HINTER, emitter, values, equalityTest);
    }

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when
     * {@link Objects#deepEquals(Object, Object)} returns true, and {@link net.adamcin.jardelta.api.diff.Verb#CHANGED}
     * when it returns false.
     *
     * @param hinter  a function to produce a hint for a given value
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofObjectEquality(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<T> values) {
        return ofObjectEquality(hinter, emitter, values, DEFAULT_EQUALITY_TEST);
    }

    /**
     * Compare both objects, returning {@link java.util.stream.Stream#empty()} when
     * {@link Objects#deepEquals(Object, Object)} returns true, and {@link net.adamcin.jardelta.api.diff.Verb#CHANGED}
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
        return ofObjectEquality(DEFAULT_HINTER, emitter, values, DEFAULT_EQUALITY_TEST);
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
     * for present-empty, while delegating present-present to the provided {@link java.util.function.Function}.
     *
     * @param emitter       a diff builder
     * @param values        both values
     * @param hinter        function to stringify hints for added/removed diffs
     * @param ifBothPresent delegate function for present-present case
     * @param <T>           the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofOptionals(
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values,
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothPresent) {
        if (values.left().isEmpty()) {
            return values.right().stream().map(value -> hinter.apply(value)
                    .map(emitter::added).orElseGet(emitter::added));
        } else if (values.right().isEmpty()) {
            return values.left().stream().map(value -> hinter.apply(value)
                    .map(emitter::removed).orElseGet(emitter::removed));
        } else {
            return ifBothPresent.apply(emitter, values.map(Optional::get));
        }
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
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
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothPresent) {
        return ofOptionals(emitter, values, DEFAULT_HINTER, ifBothPresent);
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
     * for present-empty, while delegating present-present to
     * {@link #ofObjectEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param hinter  function to stringify hints for added/removed diffs
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofOptionals(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values) {
        return ofOptionals(emitter, values, hinter, GenericDiffers::ofObjectEquality);
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
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
        return ofOptionals(emitter, values, GenericDiffers::ofObjectEquality);
    }

    /**
     * Compare both results, delegating present-present to the provided {@link java.util.function.Function} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
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
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothSuccess) {
        if (values.map(Result::isSuccess).testBoth((left, right) -> left && right)) {
            return ifBothSuccess.apply(emitter, values.map(Result::getOrThrow));
        } else {
            return emitter.errBoth(values);
        }
    }

    /**
     * Compare both results, delegating present-present to
     * {@link #ofObjectEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Result values
     * @return the diff stream
     * @see #ofResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both, java.util.function.BiFunction)
     */
    @NotNull
    public static <T> Stream<Diff> ofResults(
            @NotNull Emitter emitter,
            @NotNull Both<Result<T>> values) {
        return ofResults(emitter, values, GenericDiffers::ofObjectEquality);
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #ofOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to the provided {@code ifBothSingle} function.
     *
     * @param hinter       function to stringify hints for added/removed diffs
     * @param emitter      a diff emitter
     * @param values       both values
     * @param ifBothSingle delegate function for single-single case
     * @param <T>          the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAtMostOne(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<? extends Iterable<T>> values,
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothSingle) {
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
        return ofResults(emitter, iterated, (childEmitter, results) ->
                ofOptionals(childEmitter, results, hinter, ifBothSingle));
    }


    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #ofOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
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
            @NotNull Both<? extends Iterable<T>> values,
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothSingle) {
        return ofAtMostOne(DEFAULT_HINTER, emitter, values, ifBothSingle);
    }


    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #ofOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #ofObjectEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param hinter  function to stringify hints for added/removed diffs
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAtMostOne(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<? extends Iterable<T>> values) {
        return ofAtMostOne(hinter, emitter, values,
                (emit, vals) -> GenericDiffers.ofObjectEquality(hinter, emit, vals));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #ofResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #ofOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
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
        return ofAtMostOne(emitter, values, GenericDiffers::ofObjectEquality);
    }

    /**
     * An internal utility function for use with sets that serves as both a reducer and combiner for a stream of
     * collections reduced to a single collection of a possibly different type.
     *
     * @param left  the accumulator or left-hand binary operand
     * @param right the next stream collection element or the right-hand binary operand
     * @param <T>   the element type of the collections
     * @param <C>   the type of the returned accumulator collection
     * @param <D>   the type of the streamed collections
     * @return a union collection
     */
    public static <T, C extends Collection<T>, D extends Collection<T>> C mergeSets(C left, D right) {
        left.addAll(right);
        return left;
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !contains-contains,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for contains-!contains, and delegating to the provided
     * {@code ifIntersection} function for contains-contains.
     *
     * @param baseEmitter    base emitter
     * @param bothSets       both values
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Emitter baseEmitter,
            @NotNull Function<SetDiffer.SetDifferBuilder<T>, SetDiffer.SetDifferBuilder<T>> builderCustomizer,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        final SetDiffer.SetDifferBuilder<T> builder = SetDiffer.<T>builder()
                .intersectDiffer(ifIntersection);
        return builderCustomizer.apply(builder).build().diffSets(baseEmitter, bothSets);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for contained-notContained, and delegating to the provided
     * {@code ifIntersection} function for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param baseEmitter    base diff emitter
     * @param bothSets       both values
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            the type parameter of the Collection values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        return SetDiffer.<T>builder().intersectDiffer(ifIntersection).build().diffSets(baseEmitter, bothSets);
    }

    /**
     * Compare both collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param baseEmitter base diff emitter
     * @param bothSets    both values
     * @param <T>         the type parameter of the Collection values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> ofAllInEitherSet(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Collection<T>> bothSets) {
        return SetDiffer.<T>builder().build().diffSets(baseEmitter, bothSets);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     *
     * @param valueHinter       a function to produce a hint for a given map value
     * @param baseEmitter       the base diff emitter
     * @param builderCustomizer function to customize the {@link net.adamcin.jardelta.core.util.SetDiffer.SetDifferBuilder}
     * @param bothMaps          both values
     * @param ifIntersection    delegate function for intersecting keys
     * @param <K>               the type of the map's keys
     * @param <V>               the type of the map's values
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super V, Optional<String>> valueHinter,
            @NotNull Emitter baseEmitter,
            @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        final SetDiffer.SetDifferBuilder<K> builder = SetDiffer.<K>builder()
                .hinter(key -> bothMaps.map(map -> Optional.ofNullable(map.get(key)).flatMap(valueHinter)))
                .intersectDiffer((emitter, key) ->
                        ifIntersection.apply(emitter, bothMaps.mapOptional(map -> map.get(key))));
        return builderCustomizer.apply(builder).build().diffSets(baseEmitter, bothMaps.map(Map::keySet));
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param baseEmitter       base emitter
     * @param builderCustomizer function to customize the {@link net.adamcin.jardelta.core.util.SetDiffer.SetDifferBuilder}
     * @param bothMaps          both values
     * @param ifIntersection    delegate function for intersecting keys
     * @param <K>               the key type parameter of the Map
     * @param <V>               the value type parameter of the Map
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Emitter baseEmitter,
            @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(DEFAULT_HINTER, baseEmitter, builderCustomizer, bothMaps, ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param baseEmitter    base emitter
     * @param bothMaps       both values
     * @param ifIntersection delegate function for intersecting keys
     * @param <K>            the key type parameter of the Map
     * @param <V>            the value type parameter of the Map
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Optional<V>>, Stream<Diff>> ifIntersection) {
        return ofAllInEitherMap(DEFAULT_HINTER, baseEmitter, Function.identity(), bothMaps, ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param valueHinter a function to produce a hint for a given map value
     * @param baseEmitter base emitter
     * @param bothMaps    both values
     * @param <K>         the key type parameter of the Map
     * @param <V>         the value type parameter of the Map
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Function<? super V, Optional<String>> valueHinter,
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return ofAllInEitherMap(valueHinter, baseEmitter, Function.identity(), bothMaps,
                (emitter, values) -> GenericDiffers.ofOptionals(valueHinter, emitter, values));
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param baseEmitter base emitter
     * @param bothMaps    both values
     * @param <K>         the key type parameter of the Map
     * @param <V>         the value type parameter of the Map
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> ofAllInEitherMap(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return ofAllInEitherMap(DEFAULT_HINTER, baseEmitter, Function.identity(), bothMaps, GenericDiffers::ofOptionals);
    }

}
