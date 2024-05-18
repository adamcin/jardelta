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

import lombok.EqualsAndHashCode;
import net.adamcin.jardelta.core.Refinement;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A container of diffs.
 */
@EqualsAndHashCode
public final class Diffs implements FilteredDiffs {
    public static final Diffs EMPTY = new Diffs(Collections.emptySet());

    private final Set<Diff> diffs;

    private Diffs(@NotNull Set<Diff> diffs) {
        this.diffs = diffs;
    }

    @Override
    @NotNull
    public Stream<Diff> stream() {
        return diffs.stream();
    }

    @Override
    public boolean isEmpty() {
        return diffs.isEmpty();
    }

    @Override
    public @NotNull FilteredDiffs filter(@NotNull Predicate<Diff> predicate) {
        return new AppliedFilter(this, predicate);
    }

    @NotNull
    public Diffs refinedBy(@NotNull Refinement refinements) {
        if (refinements.isEmpty()) {
            return this;
        } else {
            Set<Diff> refined = new TreeSet<>(this.diffs);
            refined.removeAll(refinements.getSuperseded());
            return Stream.concat(refined.stream(), refinements.getDiffs().stream()).collect(collector());
        }
    }

    @Override
    public String toString() {
        return "Diffs[" + stream().map(Diff::toString).collect(Collectors.joining(",")) + "]";
    }

    @EqualsAndHashCode
    static class AppliedFilter implements FilteredDiffs {
        private final FilteredDiffs parent;
        private final Predicate<Diff> predicate;

        AppliedFilter(@NotNull FilteredDiffs parent, @NotNull Predicate<Diff> predicate) {
            this.parent = parent;
            this.predicate = predicate;
        }

        @Override
        public @NotNull Stream<Diff> stream() {
            return parent.stream().filter(predicate);
        }

        @Override
        public @NotNull FilteredDiffs filter(@NotNull Predicate<Diff> predicate) {
            return new AppliedFilter(this, predicate);
        }

        @Override
        public String toString() {
            return stringify();
        }
    }

    @NotNull
    public static Diffs of(Diff... values) {
        return Stream.of(values).filter(Objects::nonNull).collect(collector());
    }

    static final class Accumulator implements Consumer<Diff> {
        private static final Collector.Characteristics[] CHARACTERISTICS = Collectors.toSet().characteristics().stream()
                // remove IDENTITY_FINISH, but pass thru the other characteristics.
                .filter(Fun.inSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH)).negate())
                .toArray(Collector.Characteristics[]::new);
        private final Set<Diff> acc = new TreeSet<>();

        @Override
        public void accept(Diff diff) {
            acc.add(diff);
        }

        @NotNull
        public Accumulator combine(@NotNull Accumulator right) {
            if (this.acc.size() < right.acc.size()) {
                right.acc.addAll(this.acc);
                return right;
            } else {
                this.acc.addAll(right.acc);
                return this;
            }
        }

        @NotNull
        public Diffs finish() {
            return new Diffs(this.acc);
        }

        public static Collector.Characteristics[] characteristics() {
            return Arrays.copyOf(CHARACTERISTICS, CHARACTERISTICS.length);
        }
    }

    /**
     * Create a collector that accumulates a stream of Diff instances into a single Diffs.
     *
     * @return a Diffs collector
     */
    public static Collector<Diff, ?, Diffs> collector() {
        return Collector.of(
                Accumulator::new,
                Accumulator::accept,
                Accumulator::combine,
                Accumulator::finish,
                Accumulator.characteristics());
    }
}
