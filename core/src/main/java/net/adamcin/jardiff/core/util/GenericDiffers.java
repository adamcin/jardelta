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

package net.adamcin.jardiff.core.util;

import net.adamcin.jardiff.core.Action;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Provides differs for building {@link Action#ADDED}, {@link Action#REMOVED}, {@link Action#ERR_LEFT}, and
 * {@link Action#ERR_RIGHT} diffs based on generic container types, with delegation to provided lambdas for non-generic
 * type handling when the containers are otherwise identical.
 */
public final class GenericDiffers {

    @NotNull
    public static <T> Stream<Diff> diffOptionals(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<Optional<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothPresent) {
        if (values.left().isEmpty()) {
            if (values.right().isPresent()) {
                return Stream.of(diffBuilder.added());
            }
        } else if (values.right().isEmpty()) {
            return Stream.of(diffBuilder.removed());
        } else {
            return ifBothPresent.apply(values.map(Optional::get));
        }
        return Stream.empty();
    }

    @NotNull
    public static <T> Stream<Diff> diffResults(
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

    @NotNull
    public static <T> Stream<Diff> diffAtMostOne(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Iterable<T>> values,
            @NotNull Function<Both<T>, Stream<Diff>> ifBothSingle) {
        Both<Result<Optional<T>>> iterated = values.map(Iterable::iterator).map(iter -> {
            if (!iter.hasNext()) {
                return Result.success(Optional.empty());
            } else {
                T first = iter.next();
                if (iter.hasNext()) {
                    return Result.failure("more than one element is present");
                } else {
                    return Result.success(Optional.ofNullable(first));
                }
            }
        });
        return diffResults(diffBuilder, iterated, results ->
                diffOptionals(diffBuilder, results, ifBothSingle));
    }

    @NotNull
    public static <T> Stream<Diff> diffAllInEitherSet(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Function<T, Stream<Diff>> ifIsInBoth) {
        return diffAllInEitherSet(diffBuilder, bothSets, TreeSet::new, ifIsInBoth);
    }

    @NotNull
    public static <T> Stream<Diff> diffAllInEitherSet(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull Supplier<Set<T>> setSupplier,
            @NotNull Function<T, Stream<Diff>> ifIsInBoth) {
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
                    stream = Stream.concat(stream, Stream.of(diffBuilder.added()));
                }
            } else if (!bothSets.right().contains(value)) {
                stream = Stream.concat(stream, Stream.of(diffBuilder.removed()));
            } else {
                stream = Stream.concat(stream, ifIsInBoth.apply(value));
            }
        }

        return stream;
    }

    @NotNull
    public static <K, V> Stream<Diff> diffAllInEitherMap(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<K, Both<Optional<V>>, Stream<Diff>> ifIsInBoth) {
        return diffAllInEitherMap(diffBuilder, bothMaps, TreeSet::new, ifIsInBoth);
    }

    @NotNull
    public static <K, V> Stream<Diff> diffAllInEitherMap(
            @NotNull Diff.Builder diffBuilder,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull Supplier<Set<K>> setSupplier,
            @NotNull BiFunction<K, Both<Optional<V>>, Stream<Diff>> ifIsInBoth) {

        return diffAllInEitherSet(diffBuilder, bothMaps.map(Map::keySet), setSupplier,
                key -> ifIsInBoth.apply(key, bothMaps.mapOptional(map -> map.get(key))));
    }
}
