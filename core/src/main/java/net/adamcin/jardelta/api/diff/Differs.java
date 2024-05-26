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

package net.adamcin.jardelta.api.diff;

import net.adamcin.jardelta.api.Kind;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import net.adamcin.streamsupport.throwing.ThrowingFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
public final class Differs {
    static final String ERROR_AT_MOST_ONE = "unexpected plurality";
    static final BiPredicate<Object, Object> DEFAULT_EQUALITY_TEST = Objects::deepEquals;
    static final Function<Object, Optional<String>> DEFAULT_HINTER = Fun.compose1(
            value -> value instanceof Object[] ? Arrays.deepToString((Object[]) value) : Objects.toString(value),
            Optional::of);

    /**
     * Concatenate multiple differs of the same type into a single differ of that type.
     *
     * @param differs an array of differs
     * @param <T>     the common differ element type
     * @return a single differ composed of the input differs
     */
    public static <T> Differ<T> concat(@NotNull Iterable<Differ<T>> differs) {
        return (baseEmitter, element) -> {
            Stream<Diff> diffStream = Stream.empty();
            for (Differ<T> differ : differs) {
                diffStream = Stream.concat(diffStream, differ.diff(baseEmitter, element));
            }
            return diffStream;
        };
    }

    /**
     * Concatenate multiple differs of the same type into a single differ of that type.
     *
     * @param differs an array of differs
     * @param <T>     the common differ element type
     * @return a single differ composed of the input differs
     */
    @SafeVarargs
    public static <T> Differ<T> concat(@NotNull Differ<T>... differs) {
        return concat(List.of(differs));
    }

    public static <T> Differ<T> emitChild(@NotNull String childName, @NotNull Differ<T> differ) {
        return (baseEmitter, element) -> differ.diff(baseEmitter.forChild(childName), element);
    }

    public static <T> Differ<T> emitKind(@NotNull Kind subKind, @NotNull Differ<T> differ) {
        return (baseEmitter, element) -> differ.diff(baseEmitter.ofSubKind(subKind), element);
    }

    public static <T, U> Differ<T> projecting(@NotNull Function<? super Element<T>, Element<U>> projector,
                                              @NotNull Differ<U> differ) {
        return (emitter, baseElement) -> differ.diff(emitter, projector.apply(baseElement));
    }

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
    public static <T> Stream<Diff> diffEquality(
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
     * Return a mapped element differ to compare objects on their equality, returning
     * {@link java.util.stream.Stream#empty()} when {@link Objects#deepEquals(Object, Object)} returns true, and
     * {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
     *
     * @param mapper       element mapping function
     * @param hinter       function to generate hints from mapped value
     * @param equalityTest equality test function
     * @param <T>          element value type
     * @param <U>          mapped value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofEquality(@NotNull Function<? super T, ? extends U> mapper,
                                                       @NotNull Function<? super U, Optional<String>> hinter,
                                                       @NotNull BiPredicate<? super U, ? super U> equalityTest) {
        return (emitter, element) -> Differs.diffEquality(hinter, emitter, element.values().map(mapper), equalityTest);
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
    public static <T> Stream<Diff> diffEquality(
            @NotNull Emitter emitter,
            @NotNull Both<T> values,
            @NotNull BiPredicate<? super T, ? super T> equalityTest) {
        return diffEquality(DEFAULT_HINTER, emitter, values, equalityTest);
    }

    /**
     * Return a mapped element differ to compare objects on their equality, returning
     * {@link java.util.stream.Stream#empty()} when {@link Objects#deepEquals(Object, Object)} returns true, and
     * {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
     *
     * @param mapper       element mapping function
     * @param equalityTest equality test function
     * @param <T>          element value type
     * @param <U>          mapped value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofEquality(@NotNull Function<? super T, ? extends U> mapper,
                                                       @NotNull BiPredicate<? super U, ? super U> equalityTest) {
        return (emitter, element) -> Differs.diffEquality(emitter, element.values().map(mapper), equalityTest);
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
    public static <T> @NotNull Stream<Diff> diffEquality(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<T> values) {
        return diffEquality(hinter, emitter, values, DEFAULT_EQUALITY_TEST);
    }

    /**
     * Return a mapped element differ to compare objects on their equality, returning
     * {@link java.util.stream.Stream#empty()} when {@link Objects#deepEquals(Object, Object)} returns true, and
     * {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
     *
     * @param mapper element mapping function
     * @param hinter function to generate hints from mapped value
     * @param <T>    element value type
     * @param <U>    mapped value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofEquality(@NotNull Function<? super T, ? extends U> mapper,
                                                       @NotNull Function<? super U, Optional<String>> hinter) {
        return (emitter, element) -> Differs.diffEquality(hinter, emitter, element.values().map(mapper));
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
    public static <T> Stream<Diff> diffEquality(
            @NotNull Emitter emitter,
            @NotNull Both<T> values) {
        return diffEquality(DEFAULT_HINTER, emitter, values, DEFAULT_EQUALITY_TEST);
    }

    /**
     * Return a mapped element differ to compare objects on their equality, returning
     * {@link java.util.stream.Stream#empty()} when {@link Objects#deepEquals(Object, Object)} returns true, and
     * {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @return a new differ
     */
    public static <T> @NotNull Differ<T> ofEquality(@NotNull Function<? super T, ?> mapper) {
        return (emitter, element) -> Differs.diffEquality(emitter, element.values().map(mapper));
    }

    /**
     * Return a mapped element differ to compare objects on their equality, returning
     * {@link java.util.stream.Stream#empty()} when {@link Objects#deepEquals(Object, Object)} returns true, and
     * {@link net.adamcin.jardelta.api.diff.Verb#CHANGED} when it returns false.
     *
     * @param <T> element value type
     * @return a new differ
     */
    public static <T> @NotNull Differ<T> ofEquality() {
        return (emitter, element) -> Differs.diffEquality(emitter, element.values());
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
     * for present-empty, while delegating present-present to the provided {@link java.util.function.Function}.
     *
     * @param hinter        function to stringify hints for added/removed diffs
     * @param emitter       a diff builder
     * @param values        both values
     * @param ifBothPresent delegate function for present-present case
     * @param <T>           the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> diffOptionals(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values,
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
     * Return a mapped element differ to compare optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper        element mapping function
     * @param hinter        function to stringify hints for added/removed diffs
     * @param ifBothPresent delegate differ for present-present case
     * @param <T>           element value type
     * @param <U>           mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofOptionals(@NotNull Function<? super T, Optional<U>> mapper,
                                                        @NotNull Differ<U> ifBothPresent,
                                                        @NotNull Function<? super U, Optional<String>> hinter) {
        return (emitter, element) -> Differs.diffOptionals(hinter, emitter, element.values().map(mapper),
                (emit, mapped) -> ifBothPresent.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Return a mapped element differ to compare nullable values as optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper        element mapping function
     * @param hinter        function to stringify hints for added/removed diffs
     * @param ifBothPresent delegate differ for present-present case
     * @param <T>           element value type
     * @param <U>           mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofNullables(@NotNull Function<? super T, U> mapper,
                                                        @NotNull Function<? super U, Optional<String>> hinter,
                                                        @NotNull Differ<U> ifBothPresent) {
        return (emitter, element) -> Differs.diffOptionals(hinter, emitter, element.values().mapOptional(mapper),
                (emit, mapped) -> ifBothPresent.diff(emit, Element.of(emit.getName(), mapped)));
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
    public static <T> Stream<Diff> diffOptionals(
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values,
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothPresent) {
        return diffOptionals(DEFAULT_HINTER, emitter, values, ifBothPresent);
    }

    /**
     * Return a mapped element differ to compare optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper        element mapping function
     * @param ifBothPresent delegate differ for present-present case
     * @param <T>           element value type
     * @param <U>           mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofOptionals(@NotNull Function<? super T, Optional<U>> mapper,
                                                        @NotNull Differ<U> ifBothPresent) {
        return (emitter, element) -> Differs.diffOptionals(emitter, element.values().map(mapper),
                (emit, mapped) -> ifBothPresent.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Return a mapped element differ to compare nullable values as optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper        element mapping function
     * @param ifBothPresent delegate differ for present-present case
     * @param <T>           element value type
     * @param <U>           mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofNullables(@NotNull Function<? super T, U> mapper,
                                                        @NotNull Differ<U> ifBothPresent) {
        return (emitter, element) -> Differs.diffOptionals(emitter, element.values().mapOptional(mapper),
                (emit, mapped) -> ifBothPresent.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
     * for present-empty, while delegating present-present to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param hinter  function to stringify hints for added/removed diffs
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> diffOptionals(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values) {
        return diffOptionals(hinter, emitter, values, Differs::diffEquality);
    }

    /**
     * Return a mapped element differ to compare optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper element mapping function
     * @param hinter function to stringify hints for added/removed diffs
     * @param <T>    element value type
     * @param <U>    mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofOptionals(@NotNull Function<? super T, Optional<U>> mapper,
                                                        @NotNull Function<? super U, Optional<String>> hinter) {
        return (emitter, element) -> Differs.diffOptionals(hinter, emitter, element.values().map(mapper));
    }

    /**
     * Return a mapped element differ to compare nullable values as optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper element mapping function
     * @param hinter function to stringify hints for added/removed diffs
     * @param <T>    element value type
     * @param <U>    mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofNullables(@NotNull Function<? super T, ? extends U> mapper,
                                                        @NotNull Function<? super U, Optional<String>> hinter) {
        return (emitter, element) -> Differs.diffOptionals(hinter, emitter, element.values().mapOptional(mapper));
    }

    /**
     * Compare both optionals, returning {@link java.util.stream.Stream#empty()} for empty-empty,
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED}
     * for present-empty, while delegating present-present to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Optional values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> diffOptionals(
            @NotNull Emitter emitter,
            @NotNull Both<Optional<T>> values) {
        return diffOptionals(emitter, values, Differs::diffEquality);
    }

    /**
     * Return a mapped element differ to compare optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @param <U>    mapped optional value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofOptionals(@NotNull Function<? super T, Optional<U>> mapper) {
        return (emitter, element) -> Differs.diffOptionals(emitter, element.values().map(mapper));
    }

    /**
     * Return a mapped element differ to compare optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param <T> element value type
     * @return a new differ
     */
    public static <T> @NotNull Differ<Optional<T>> ofOptionals() {
        return (emitter, element) -> Differs.diffOptionals(emitter, element.values());
    }

    /**
     * Return a mapped element differ to compare nullable values as optionals, returning
     * {@link java.util.stream.Stream#empty()} for empty-empty, {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for
     * empty-present, and {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for present-empty, while delegating
     * present-present to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @return a new differ
     */
    public static <T> @NotNull Differ<T> ofNullables(@NotNull Function<? super T, ?> mapper) {
        return (emitter, element) -> Differs.diffOptionals(emitter, element.values().mapOptional(mapper));
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
    public static <T> Stream<Diff> diffResults(
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
     * Return a mapped element differ to compare results, delegating to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param mapper        element mapping function
     * @param ifBothSuccess delegate differ for success-success case
     * @param <T>           element value type
     * @param <U>           mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofResults(@NotNull Function<? super T, Result<U>> mapper,
                                                      @NotNull Differ<U> ifBothSuccess) {
        return (emitter, element) -> Differs.diffResults(emitter, element.values().map(mapper),
                (emit, mapped) -> ifBothSuccess.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Return a mapped element differ to compare results of a throwing function, delegating to
     * {@code ifBothSuccess} for success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param mapper        element mapping function
     * @param ifBothSuccess delegate differ for success-success case
     * @param <T>           element value type
     * @param <U>           mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofThrowing(@NotNull ThrowingFunction<? super T, U> mapper,
                                                       @NotNull Differ<U> ifBothSuccess) {
        return (emitter, element) -> Differs.diffResults(emitter, element.values().map(Fun.result1(mapper)),
                (emit, mapped) -> ifBothSuccess.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Compare both results, delegating to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for success-success,
     * while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Result values
     * @return the diff stream
     * @see #diffResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both, java.util.function.BiFunction)
     */
    @NotNull
    public static <T> Stream<Diff> diffResults(
            @NotNull Emitter emitter,
            @NotNull Both<Result<T>> values) {
        return diffResults(emitter, values, Differs::diffEquality);
    }

    /**
     * Return a mapped element differ to compare results, delegating to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @param <U>    mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofResults(@NotNull Function<? super T, Result<U>> mapper) {
        return (emitter, element) -> Differs.diffResults(emitter, element.values().map(mapper));
    }

    /**
     * Return a mapped element differ to compare results, delegating to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param <T> element value type
     * @return a new differ
     */
    public static <T> @NotNull Differ<Result<T>> ofResults() {
        return (emitter, element) -> Differs.diffResults(emitter, element.values());
    }

    /**
     * Return a mapped element differ to compare results of a throwing function, delegating to
     * {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)} for
     * success-success, while returning {@link net.adamcin.jardelta.api.diff.Verb#ERR_LEFT},
     * {@link net.adamcin.jardelta.api.diff.Verb#ERR_RIGHT}, or both, for failure-success, success-failure, and
     * failure-failure, respectively.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @param <U>    mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofThrowing(@NotNull ThrowingFunction<? super T, ? extends U> mapper) {
        return (emitter, element) -> Differs.diffResults(emitter, element.values().map(Fun.result1(mapper)));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
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
    public static <T> Stream<Diff> diffAtMostOne(
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
        return diffResults(emitter, iterated, (childEmitter, results) ->
                diffOptionals(hinter, childEmitter, results, ifBothSingle));
    }

    /**
     * Return a mapped element differ to compare iterables based on cardinality, where only one comparable value is
     * expected. Cases of zero-one, one-zero, and one-one are mapped to
     * {@link net.adamcin.streamsupport.Result#success(Object)}, while a many cardinality on either side is mapped to a
     * respective {@link net.adamcin.streamsupport.Result#failure(String)}. This pair is then delegated to
     * {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper       element mapping function
     * @param hinter       function to stringify hints for added/removed diffs
     * @param ifBothSingle delegate differ for single-single case
     * @param <T>          element value type
     * @param <U>          mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofAtMostOne(@NotNull Function<? super T, ? extends Iterable<U>> mapper,
                                                        @NotNull Differ<U> ifBothSingle,
                                                        @NotNull Function<? super U, Optional<String>> hinter) {
        return (emitter, element) -> diffAtMostOne(hinter, emitter, element.values().map(mapper),
                (emit, mapped) -> ifBothSingle.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param emitter      a diff builder
     * @param values       both values
     * @param ifBothSingle delegate function for single-single case
     * @param <T>          the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> diffAtMostOne(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Iterable<T>> values,
            @NotNull BiFunction<Emitter, Both<T>, Stream<Diff>> ifBothSingle) {
        return diffAtMostOne(DEFAULT_HINTER, emitter, values, ifBothSingle);
    }

    /**
     * Return a mapped element differ to compare iterables based on cardinality, where only one comparable value is
     * expected. Cases of zero-one, one-zero, and one-one are mapped to
     * {@link net.adamcin.streamsupport.Result#success(Object)}, while a many cardinality on either side is mapped to a
     * respective {@link net.adamcin.streamsupport.Result#failure(String)}. This pair is then delegated to
     * {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper       element mapping function
     * @param ifBothSingle delegate differ for single-single case
     * @param <T>          element value type
     * @param <U>          mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofAtMostOne(@NotNull Function<? super T, ? extends Iterable<U>> mapper,
                                                        @NotNull Differ<U> ifBothSingle) {
        return (emitter, element) -> diffAtMostOne(emitter, element.values().map(mapper),
                (emit, mapped) -> ifBothSingle.diff(emit, Element.of(emit.getName(), mapped)));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param hinter  function to stringify hints for added/removed diffs
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> diffAtMostOne(
            @NotNull Function<? super T, Optional<String>> hinter,
            @NotNull Emitter emitter,
            @NotNull Both<? extends Iterable<T>> values) {
        return diffAtMostOne(hinter, emitter, values,
                (emit, vals) -> Differs.diffEquality(hinter, emit, vals));
    }

    /**
     * Return a mapped element differ to compare iterables based on cardinality, where only one comparable value is
     * expected. Cases of zero-one, one-zero, and one-one are mapped to
     * {@link net.adamcin.streamsupport.Result#success(Object)}, while a many cardinality on either side is mapped to a
     * respective {@link net.adamcin.streamsupport.Result#failure(String)}. This pair is then delegated to
     * {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper element mapping function
     * @param hinter function to stringify hints for added/removed diffs
     * @param <T>    element value type
     * @param <U>    mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofAtMostOne(@NotNull Function<? super T, ? extends Iterable<U>> mapper,
                                                        @NotNull Function<? super U, Optional<String>> hinter) {
        return (emitter, element) -> diffAtMostOne(hinter, emitter, element.values().map(mapper));
    }

    /**
     * Compare both iterables based on cardinality, where only one comparable value is expected. Cases of zero-one,
     * one-zero, and one-one are mapped to {@link net.adamcin.streamsupport.Result#success(Object)}, while a many
     * cardinality on either side is mapped to a respective {@link net.adamcin.streamsupport.Result#failure(String)}.
     * This pair is then delegated to {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param emitter a diff builder
     * @param values  both values
     * @param <T>     the type parameter of the Iterable values
     * @return the diff stream
     */
    @NotNull
    public static <T> Stream<Diff> diffAtMostOne(
            @NotNull Emitter emitter,
            @NotNull Both<? extends Iterable<T>> values) {
        return diffAtMostOne(emitter, values, Differs::diffEquality);
    }

    /**
     * Return a mapped element differ to compare iterables based on cardinality, where only one comparable value is
     * expected. Cases of zero-one, one-zero, and one-one are mapped to
     * {@link net.adamcin.streamsupport.Result#success(Object)}, while a many cardinality on either side is mapped to a
     * respective {@link net.adamcin.streamsupport.Result#failure(String)}. This pair is then delegated to
     * {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @param <U>    mapped result value type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofAtMostOne(@NotNull Function<? super T, ? extends Iterable<U>> mapper) {
        return (emitter, element) -> diffAtMostOne(emitter, element.values().map(mapper));
    }

    /**
     * Return a mapped element differ to compare iterables based on cardinality, where only one comparable value is
     * expected. Cases of zero-one, one-zero, and one-one are mapped to
     * {@link net.adamcin.streamsupport.Result#success(Object)}, while a many cardinality on either side is mapped to a
     * respective {@link net.adamcin.streamsupport.Result#failure(String)}. This pair is then delegated to
     * {@link #diffResults(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both,
     * java.util.function.BiFunction)} and {@link #diffOptionals(net.adamcin.jardelta.api.diff.Emitter,
     * net.adamcin.streamsupport.Both, java.util.function.BiFunction)}
     * and then to {@link #diffEquality(net.adamcin.jardelta.api.diff.Emitter, net.adamcin.streamsupport.Both)}.
     *
     * @param <T> element value type
     * @return a new differ
     */
    public static <T> @NotNull Differ<? extends Iterable<T>> ofAtMostOne() {
        return (emitter, element) -> diffAtMostOne(emitter, element.values());
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
    public static <T> Stream<Diff> diffSets(
            @NotNull Emitter baseEmitter,
            @NotNull Function<SetDiffer.SetDifferBuilder<T>, SetDiffer.SetDifferBuilder<T>> builderCustomizer,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        final SetDiffer.SetDifferBuilder<T> builder = SetDiffer.<T>builder()
                .intersectDiffer(ifIntersection);
        return builderCustomizer.apply(builder).build().diffSets(baseEmitter, bothSets);
    }

    /**
     * Return a mapped element differ to compare collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     *
     * @param mapper         element mapping function
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            element value type
     * @param <U>            mapped set element type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofSets(@NotNull Function<? super T, ? extends Collection<U>> mapper,
                                                   @NotNull Function<SetDiffer.SetDifferBuilder<U>, SetDiffer.SetDifferBuilder<U>> builderCustomizer,
                                                   @NotNull BiFunction<Emitter, ? super U, Stream<Diff>> ifIntersection) {
        return (emitter, element) -> diffSets(emitter, builderCustomizer, element.values().map(mapper), ifIntersection);
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
    public static <T> Stream<Diff> diffSets(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Collection<T>> bothSets,
            @NotNull BiFunction<Emitter, ? super T, Stream<Diff>> ifIntersection) {
        return SetDiffer.<T>builder().intersectDiffer(ifIntersection).build().diffSets(baseEmitter, bothSets);
    }

    /**
     * Return a mapped element differ to compare collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     *
     * @param mapper         element mapping function
     * @param ifIntersection delegate function for intersecting elements
     * @param <T>            element value type
     * @param <U>            mapped set element type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofSets(@NotNull Function<? super T, ? extends Collection<U>> mapper,
                                                   @NotNull BiFunction<Emitter, ? super U, Stream<Diff>> ifIntersection) {
        return (emitter, element) -> diffSets(emitter, element.values().map(mapper), ifIntersection);
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
    public static <T> Stream<Diff> diffSets(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Collection<T>> bothSets) {
        return SetDiffer.<T>builder().build().diffSets(baseEmitter, bothSets);
    }

    /**
     * Return a mapped element differ to compare collections by iterating over a set union of their elements, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for notContained-contained,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for contained-notContained, and returning
     * {@link java.util.stream.Stream#empty()} for contained-contained.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @param <U>    mapped set element type
     * @return a new differ
     */
    public static <T, U> @NotNull Differ<T> ofSets(@NotNull Function<? super T, ? extends Collection<U>> mapper) {
        return (emitter, element) -> diffSets(emitter, element.values().map(mapper));
    }

    public static <K, V, E> @NotNull Differ<Map.Entry<K, V>> ofMapValues(@NotNull BiFunction<? super K, ? super V, ? extends E> mapper,
                                                                         @NotNull Differ<E> valueDiffer) {
        return (emitter, element) -> valueDiffer.diff(emitter, Element.of(element.name(), element.values().map(Fun.mapEntry(mapper))));
    }

    public static <K, V> @NotNull Differ<Map.Entry<K, V>> ofMapValues(@NotNull Differ<V> valueDiffer) {
        return ofMapValues((key, value) -> value, valueDiffer);
    }

    public static <K, V> @NotNull Differ<Map.Entry<K, V>> ofMapValues() {
        return ofMapValues((key, value) -> value, ofEquality(Function.identity()));
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     *
     * @param valueHinter       a function to produce a hint for a given map value
     * @param baseEmitter       the base diff emitter
     * @param builderCustomizer function to customize the {@link SetDiffer.SetDifferBuilder}
     * @param bothMaps          both values
     * @param ifIntersection    delegate function for intersecting keys
     * @param <K>               the type of the map's keys
     * @param <V>               the type of the map's values
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> diffMaps(
            @NotNull Function<? super V, Optional<String>> valueHinter,
            @NotNull Emitter baseEmitter,
            @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Map.Entry<K, V>>, Stream<Diff>> ifIntersection) {
        final SetDiffer.SetDifferBuilder<K> builder = SetDiffer.<K>builder()
                .hinter(key -> bothMaps.map(map -> Optional.ofNullable(map.get(key)).flatMap(valueHinter)))
                .intersectDiffer((emitter, key) ->
                        ifIntersection.apply(emitter, bothMaps.map(map -> Fun.toEntry(key, map.get(key)))));
        return builderCustomizer.apply(builder).build().diffSets(baseEmitter, bothMaps.map(Map::keySet));
    }

    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param mapper            element mapping function
     * @param hinter            a function to produce a hint for a given map value
     * @param builderCustomizer function to customize the {@link SetDiffer.SetDifferBuilder}
     * @param ifIntersection    delegate function for intersecting keys
     * @param <T>               element value type
     * @param <K>               mapped key type
     * @param <V>               mapped value type
     * @return a new differ
     */
    public static <T, K, V> @NotNull Differ<T> ofMapsCustomized(@NotNull Function<? super T, ? extends Map<K, V>> mapper,
                                                                @NotNull Function<? super V, Optional<String>> hinter,
                                                                @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer,
                                                                @NotNull BiFunction<Emitter, Both<Map.Entry<K, V>>, Stream<Diff>> ifIntersection) {
        return (emitter, element) -> diffMaps(hinter, emitter, builderCustomizer, element.values().map(mapper), ifIntersection);
    }

    /**
     * Compare both maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and delegating to the provided
     * {@code ifIntersection} function for containsKey-containsKey.
     * NOTE: uses {@link java.util.TreeSet#TreeSet()} for aggregate set operations.
     *
     * @param baseEmitter       base emitter
     * @param builderCustomizer function to customize the {@link SetDiffer.SetDifferBuilder}
     * @param bothMaps          both values
     * @param ifIntersection    delegate function for intersecting keys
     * @param <K>               the key type parameter of the Map
     * @param <V>               the value type parameter of the Map
     * @return the diff stream
     */
    @NotNull
    public static <K, V> Stream<Diff> diffMaps(
            @NotNull Emitter baseEmitter,
            @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Map.Entry<K, V>>, Stream<Diff>> ifIntersection) {
        return diffMaps(DEFAULT_HINTER, baseEmitter, builderCustomizer, bothMaps, ifIntersection);
    }

    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param mapper            element mapping function
     * @param builderCustomizer function to customize the {@link SetDiffer.SetDifferBuilder}
     * @param ifIntersection    delegate differ for intersecting keys
     * @param <T>               element value type
     * @param <K>               mapped key type
     * @param <V>               mapped value type
     * @return a new differ
     */
    public static <T, K, V> @NotNull Differ<T> ofMapsCustomized(@NotNull Function<? super T, ? extends Map<K, V>> mapper,
                                                                @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer,
                                                                @NotNull Differ<Map.Entry<K, V>> ifIntersection) {
        return (emitter, element) -> diffMaps(emitter, builderCustomizer, element.values().map(mapper),
                (emit, mapped) -> ifIntersection.diff(emit, Element.of(emit.getName(), mapped)));
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
    public static <K, V> Stream<Diff> diffMaps(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Map<K, V>> bothMaps,
            @NotNull BiFunction<Emitter, Both<Map.Entry<K, V>>, Stream<Diff>> ifIntersection) {
        return diffMaps(DEFAULT_HINTER, baseEmitter, Function.identity(), bothMaps, ifIntersection);
    }

    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param mapper         element mapping function
     * @param ifIntersection delegate differ for intersecting keys
     * @param <T>            element value type
     * @param <K>            mapped key type
     * @param <V>            mapped value type
     * @return a new differ
     */
    public static <T, K, V> @NotNull Differ<T> ofMaps(@NotNull Function<? super T, ? extends Map<K, V>> mapper,
                                                      @NotNull Differ<Map.Entry<K, V>> ifIntersection) {
        return (emitter, element) -> diffMaps(emitter, element.values().map(mapper),
                (emit, mapped) -> ifIntersection.diff(emit, Element.of(emit.getName(), mapped)));
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
    public static <K, V> Stream<Diff> diffMaps(
            @NotNull Function<? super V, Optional<String>> valueHinter,
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return diffMaps(valueHinter, baseEmitter, Function.identity(), bothMaps,
                (emitter, values) -> Differs.diffEquality(valueHinter, emitter, values.map(Map.Entry::getValue)));
    }

    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param mapper            element mapping function
     * @param builderCustomizer function to customize the {@link SetDiffer.SetDifferBuilder}
     * @param <T>               element value type
     * @param <K>               mapped key type
     * @param <V>               mapped value type
     * @return a new differ
     */
    public static <T, K, V> @NotNull Differ<T> ofMapsCustomized(@NotNull Function<? super T, ? extends Map<K, V>> mapper,
                                                                @NotNull Function<SetDiffer.SetDifferBuilder<K>, SetDiffer.SetDifferBuilder<K>> builderCustomizer) {
        return (emitter, element) -> diffMaps(DEFAULT_HINTER, emitter, builderCustomizer, element.values().map(mapper),
                (emit, values) -> Differs.diffEquality(emit, values.map(Map.Entry::getValue)));
    }


    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param mapper element mapping function
     * @param hinter a function to produce a hint for a given map value
     * @param <T>    element value type
     * @param <K>    mapped key type
     * @param <V>    mapped value type
     * @return a new differ
     */
    public static <T, K, V> @NotNull Differ<T> ofMaps(@NotNull Function<? super T, ? extends Map<K, V>> mapper,
                                                      @NotNull Function<? super V, Optional<String>> hinter) {
        return (emitter, element) -> diffMaps(hinter, emitter, element.values().map(mapper));
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
    public static <K, V> Stream<Diff> diffMaps(
            @NotNull Emitter baseEmitter,
            @NotNull Both<? extends Map<K, V>> bothMaps) {
        return diffMaps(DEFAULT_HINTER, baseEmitter, Function.identity(), bothMaps,
                (emitter, values) -> Differs.diffEquality(emitter, values.map(Map.Entry::getValue)));
    }

    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param mapper element mapping function
     * @param <T>    element value type
     * @param <K>    mapped key type
     * @param <V>    mapped value type
     * @return a new differ
     */
    public static <T, K, V> @NotNull Differ<T> ofMaps(@NotNull Function<? super T, ? extends Map<K, V>> mapper) {
        return (emitter, element) -> diffMaps(emitter, element.values().map(mapper));
    }

    /**
     * Return a mapped element differ to compare maps by iterating over a set union of their keys, returning
     * {@link net.adamcin.jardelta.api.diff.Verb#ADDED} for !containsKey-containsKey,
     * {@link net.adamcin.jardelta.api.diff.Verb#REMOVED} for containsKey-!containsKey, and returning
     * {@link java.util.stream.Stream#empty()} for containsKey-containsKey.
     *
     * @param <K> mapped key type
     * @param <V> mapped value type
     * @return a new differ
     */
    public static <K, V> @NotNull Differ<Map<K, V>> ofMaps() {
        return (emitter, element) -> diffMaps(emitter, element.values());
    }

}
