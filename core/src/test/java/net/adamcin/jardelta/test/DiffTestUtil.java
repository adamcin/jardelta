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

package net.adamcin.jardelta.test;

import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiffTestUtil {

    public static Diffs assertDiffs(@NotNull Stream<Diff> actual, @NotNull Diff... expected) {
        Diffs collected = actual.collect(Diffs.collector());
        if (expected.length == 0) {
            assertTrue(collected.isEmpty(), "expect no diffs");
        } else {
            assertEquals(Stream.of(expected).collect(Diffs.collector()), collected, "expect matching diffs");
            for (Diff expectedDiff : expected) {
                Optional<Diff> actualDiff = collected.filter(expectedDiff::equals).stream().findAny();
                assertEquals(Optional.of(expectedDiff.getHints()), actualDiff.map(Diff::getHints),
                        "expect matching hints for diff " + expectedDiff);
            }
        }
        return collected;
    }

    public static Diffs assertDiffs(@NotNull Stream<Diff> actual, @NotNull Stream<Diff> expected) {
        Diffs actualDiffs = actual.collect(Diffs.collector());
        Diffs expectedDiffs = expected.collect(Diffs.collector());
        if (expectedDiffs.isEmpty()) {
            assertTrue(actualDiffs.isEmpty(), "expect no diffs");
        } else {
            assertEquals(expectedDiffs, actualDiffs, "expect matching diffs");
            expectedDiffs.stream().forEachOrdered(expectedDiff -> {
                Optional<Diff> actualDiff = actualDiffs.filter(expectedDiff::equals).stream().findAny();
                assertEquals(Optional.of(expectedDiff.getHints()), actualDiff.map(Diff::getHints),
                        "expect matching hints for diff " + expectedDiff);
            });
        }
        return actualDiffs;
    }
}
