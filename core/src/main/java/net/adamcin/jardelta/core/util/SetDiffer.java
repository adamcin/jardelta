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
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A parameterized abstraction of a diff algorithm for two {@link net.adamcin.jardelta.api.diff.Element}s consisting of
 * collections of Set elements (apologies for the overloaded use of the word "element" in this situation). This class
 * can also be used to implement diffs for Maps based on their keys.
 *
 * @param <E> the Set element type
 */
public final class SetDiffer<E> implements Differ<Element<? extends Collection<E>>> {

    private final BiFunction<Emitter, ? super E, Emitter> emitterProjection;

    private final Supplier<? extends Set<E>> setSupplier;

    private final Function<? super E, Both<Optional<String>>> hinter;

    private final BiFunction<Emitter, ? super E, Stream<Diff>> intersectDiffer;

    private SetDiffer(@NotNull BiFunction<Emitter, ? super E, Emitter> emitterProjection,
                      @NotNull Supplier<? extends Set<E>> setSupplier,
                      @NotNull Function<? super E, Both<Optional<String>>> hinter,
                      @NotNull BiFunction<Emitter, ? super E, Stream<Diff>> intersectDiffer) {
        this.emitterProjection = emitterProjection;
        this.setSupplier = setSupplier;
        this.hinter = hinter;
        this.intersectDiffer = intersectDiffer;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter,
                                      @NotNull Element<? extends Collection<E>> element) {
        return diffSets(baseEmitter, element.values());
    }

    public @NotNull Stream<Diff> diffSets(@NotNull Emitter baseEmitter,
                                          @NotNull Both<? extends Collection<E>> bothSets) {
        final Set<E> allValues = bothSets.stream().reduce(setSupplier.get(),
                GenericDiffers::mergeSets, GenericDiffers::mergeSets);

        Stream<Diff> stream = Stream.empty();
        for (E value : allValues) {
            final Both<Optional<String>> hints = hinter.apply(value);
            final Emitter childEmitter = emitterProjection.apply(baseEmitter, value);
            if (!bothSets.left().contains(value)) {
                if (bothSets.right().contains(value)) {
                    stream = Stream.concat(stream, Stream.of(hints.right()
                            .map(childEmitter::added)
                            .orElseGet(childEmitter::added)));
                }
                // empty-empty case, leave stream unmodified
            } else if (!bothSets.right().contains(value)) {
                stream = Stream.concat(stream, Stream.of(hints.left()
                        .map(childEmitter::removed)
                        .orElseGet(childEmitter::removed)));
            } else {
                stream = Stream.concat(stream, intersectDiffer.apply(childEmitter, value));
            }
        }

        return stream;
    }

    public static class SetDifferBuilder<E> {
        private BiFunction<Emitter, ? super E, Emitter> emitterProjection =
                (baseEmitter, value) -> baseEmitter.forChild(value.toString());

        /**
         * A {@link java.util.Set} supplier appropriate for element type {@code T}. Defaults to {@link java.util.TreeSet#TreeSet()}.
         */
        private Supplier<? extends Set<E>> setSupplier = TreeSet::new;

        private Function<? super E, Both<Optional<String>>> hinter = value -> Both.empty();

        private BiFunction<Emitter, ? super E, Stream<Diff>> intersectDiffer =
                (emitter, elements) -> Stream.empty();

        private SetDifferBuilder() {
        }

        /**
         * A function to project a given {@link net.adamcin.jardelta.api.diff.Emitter} into one that is appropriate for
         * a given Set element. By default, the {@link net.adamcin.jardelta.api.diff.Emitter#forChild(String)} method
         * is used, given the {@code toString} result of the Set element.
         *
         * @param emitterProjection the Emitter projection function
         * @return this
         */
        public SetDifferBuilder<E> emitterProjection(@NotNull BiFunction<Emitter, ? super E, Emitter> emitterProjection) {
            this.emitterProjection = emitterProjection;
            return this;
        }

        /**
         * A {@link java.util.Set} supplier appropriate for element type {@code E}.
         * Supplies a {@link java.util.TreeSet} by default.
         *
         * @param setSupplier the Set supplier
         * @return this
         */
        public SetDifferBuilder<E> setSupplier(@NotNull Supplier<? extends Set<E>> setSupplier) {
            this.setSupplier = setSupplier;
            return this;
        }

        /**
         * A function for producing both hints for a given Set element. Produces no hints by default.
         *
         * @param hinter the hinter function
         * @return this
         */
        public SetDifferBuilder<E> hinter(@NotNull Function<? super E, Both<Optional<String>>> hinter) {
            this.hinter = hinter;
            return this;
        }

        /**
         * A delegate function for further diffing of a given element after confirming that it is present in both of
         * the original collections. Performs no further diffs by default.
         *
         * @param intersectDiffer delegate function for diffing the intersection
         * @return this
         */
        public SetDifferBuilder<E> intersectDiffer(@NotNull BiFunction<Emitter, ? super E, Stream<Diff>> intersectDiffer) {
            this.intersectDiffer = intersectDiffer;
            return this;
        }

        public SetDiffer<E> build() {
            return new SetDiffer<>(emitterProjection, setSupplier, hinter, intersectDiffer);
        }
    }

    public static <E> SetDifferBuilder<E> builder() {
        return new SetDifferBuilder<>();
    }
}
