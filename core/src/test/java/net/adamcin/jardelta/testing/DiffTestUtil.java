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

package net.adamcin.jardelta.testing;

import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DiffTestUtil {

    public static void assertContainsDiffNarrowly(@NotNull Collection<Diff> removingFrom, @NotNull Diff expectedDiff) {
        if (!removingFrom.remove(expectedDiff)) {
            if (removingFrom.size() <= 1) {
                assertEquals(List.of(expectedDiff), List.copyOf(removingFrom));
            } else {
                final TreeSet<Diff> sorted = new TreeSet<>(removingFrom);
                sorted.add(expectedDiff);
                final List<Diff> expectedAsList = new ArrayList<>();
                final List<Diff> actualAsList = new ArrayList<>();
                Diff prevDiff = null;
                for (Iterator<Diff> diffIt = sorted.iterator(); diffIt.hasNext();) {
                    Diff eachDiff = diffIt.next();
                    if (eachDiff.equals(expectedDiff)) {
                        if (prevDiff != null) {
                            expectedAsList.add(prevDiff);
                            actualAsList.add(prevDiff);
                        }
                        expectedAsList.add(expectedDiff);
                        if (diffIt.hasNext()) {
                            Diff nextDiff = diffIt.next();
                            expectedAsList.add(nextDiff);
                            actualAsList.add(nextDiff);
                        }
                        break;
                    }
                    prevDiff = eachDiff;
                }

                assertEquals(expectedAsList, actualAsList);
            }
        }
    }

    public static Diffs assertDiffs(@NotNull Stream<Diff> actual, @NotNull Diff... expected) {
        HashSet<Diff> collected = actual.collect(Collectors.toCollection(HashSet::new));
        if (expected.length == 0) {
            assertTrue(collected.isEmpty(), "expected no diffs");
        } else {
            assertFalse(collected.isEmpty(), "expected some diffs");
            HashSet<Diff> remaining = new HashSet<>(collected);
            for (Diff expectedDiff : expected) {
                assertContainsDiffNarrowly(remaining, expectedDiff);
                Optional<Diff> actualDiff = collected.stream().filter(expectedDiff::equals).findAny();
                assertEquals(Optional.of(expectedDiff.getHints()), actualDiff.map(Diff::getHints),
                        "expect matching hints for diff " + expectedDiff);
            }
        }
        return collected.stream().collect(Diffs.collector());
    }

    public static Diffs assertAllDiffs(@NotNull Stream<Diff> actual, @NotNull Diff... expected) {
        return assertAllDiffs(actual, Stream.of(expected));
    }

    public static Diffs assertAllDiffs(@NotNull Stream<Diff> actual, @NotNull Stream<Diff> expected) {
        TreeSet<Diff> actualDiffs = actual.collect(Collectors.toCollection(TreeSet::new));
        TreeSet<Diff> expectedDiffs = expected.collect(Collectors.toCollection(TreeSet::new));
        if (expectedDiffs.isEmpty()) {
            assertTrue(actualDiffs.isEmpty(), "expected no diffs");
        } else {
            assertFalse(actualDiffs.isEmpty(), "expected some diffs");
            TreeSet<Diff> remaining = new TreeSet<>(actualDiffs);
            for (Diff expectedDiff : expectedDiffs) {
                assertContainsDiffNarrowly(remaining, expectedDiff);
                Optional<Diff> actualDiff = actualDiffs.stream().filter(expectedDiff::equals).findAny();
                assertEquals(Optional.of(expectedDiff.getHints()), actualDiff.map(Diff::getHints),
                        "expect matching hints for diff " + expectedDiff);
            }
            assertEquals(new TreeSet<Diff>(), remaining, "expect no remaining diffs");
        }
        return actualDiffs.stream().collect(Diffs.collector());
    }
}
