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

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericDiffersTest {
    final Emitter baseEmitter = Diff.emitterOf(Kind.of("test"))
            .forName(Name.of("testRoot"));

    @Test
    void ofObjectEquality() {
        assertEquals(0, GenericDiffers.ofObjectEquality(
                        baseEmitter,
                        Both.of(1, 1))
                .count());

        assertEquals(Optional.of(baseEmitter.changed()), GenericDiffers.ofObjectEquality(
                        baseEmitter, Both.of(1, 2))
                .findFirst());

        assertEquals(Optional.of(baseEmitter.changed()), GenericDiffers.ofObjectEquality(
                        baseEmitter, Both.of(1, 1), (left, right) -> false)
                .findFirst());

        assertEquals(0, GenericDiffers.ofObjectEquality(
                        baseEmitter,
                        Both.of(1, 2), (left, right) -> true)
                .count());
    }

    @Test
    void ofOptionals() {
        assertEquals(Optional.empty(),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(1, 1))
                        .findFirst());

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(1, 2),
                                values -> Stream.of(sentinel))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.added()),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(null, 1))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.removed()),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(1, null))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.changed()),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(1, 2))
                        .findFirst());

        assertEquals(baseEmitter.added("1").getHints(),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(null, 1))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(baseEmitter.removed("1").getHints(),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(1, null))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(baseEmitter.changed(Both.of("1", "2")).getHints(),
                GenericDiffers.ofOptionals(baseEmitter, Both.ofNullables(1, 2))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

    }

    @Test
    void ofResults() {
        assertEquals(Optional.empty(),
                GenericDiffers.ofResults(baseEmitter, Both.of(Result.success(1), Result.success(1)))
                        .findFirst());

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofResults(baseEmitter, Both.of(Result.success(1), Result.success(1)),
                                values -> Stream.of(sentinel))
                        .findFirst());

        final Result<Integer> leftFail = Result.failure("leftFail");
        final Result<Integer> rightFail = Result.failure("rightFail");

        assertEquals(Optional.of(baseEmitter.errLeft(leftFail)),
                GenericDiffers.ofResults(baseEmitter, Both.of(leftFail, Result.success(1)))
                        .findFirst());

        assertEquals(baseEmitter.errLeft(leftFail).getHints(),
                GenericDiffers.ofResults(baseEmitter, Both.of(leftFail, Result.success(1)))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));


        assertEquals(Optional.of(baseEmitter.errRight(rightFail)),
                GenericDiffers.ofResults(baseEmitter, Both.of(Result.success(1), rightFail))
                        .findFirst());

        assertEquals(baseEmitter.errRight(rightFail).getHints(),
                GenericDiffers.ofResults(baseEmitter, Both.of(Result.success(1), rightFail))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(baseEmitter.errBoth(Both.of(leftFail, rightFail)).collect(Collectors.toList()),
                GenericDiffers.ofResults(baseEmitter, Both.of(leftFail, rightFail))
                        .collect(Collectors.toList()));

        assertEquals(baseEmitter.errBoth(Both.of(leftFail, rightFail)).map(Diff::getHints).collect(Collectors.toList()),
                GenericDiffers.ofResults(baseEmitter, Both.of(leftFail, rightFail))
                        .map(Diff::getHints).collect(Collectors.toList()));
    }

    @Test
    void ofAtMostOne() {
        assertEquals(Optional.empty(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), List.of(1)))
                        .findFirst());

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), List.of(1)),
                                values -> Stream.of(sentinel))
                        .findFirst());
        assertEquals(Optional.empty(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(Collections.emptyList(), Collections.emptyList()),
                                values -> Stream.of(sentinel))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.added()),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(Collections.emptyList(), List.of(1)))
                        .findFirst());

        assertEquals(baseEmitter.added("1").getHints(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(Collections.emptyList(), List.of(1)))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(Optional.of(baseEmitter.removed()),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), Collections.emptyList()))
                        .findFirst());

        assertEquals(baseEmitter.removed("1").getHints(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), Collections.emptyList()))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(Optional.of(baseEmitter.changed()),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), List.of(2)))
                        .findFirst());

        assertEquals(baseEmitter.changed(Both.of("1", "2")).getHints(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), List.of(2)))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        final Result<Integer> failure = Result.failure(GenericDiffers.ERROR_AT_MOST_ONE);

        assertEquals(Optional.of(baseEmitter.errLeft(failure)),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1, 2), List.of(2)))
                        .findFirst());

        assertEquals(baseEmitter.errLeft(failure).getHints(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1, 2), List.of(2)))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(Optional.of(baseEmitter.errRight(failure)),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), List.of(2, 1)))
                        .findFirst());

        assertEquals(baseEmitter.errRight(failure).getHints(),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1), List.of(2, 1)))
                        .findFirst().map(Diff::getHints).orElse(Both.empty()));

        assertEquals(baseEmitter.errBoth(Both.of(failure, failure)).collect(Collectors.toList()),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1, 2), List.of(1, 2)))
                        .collect(Collectors.toList()));

        assertEquals(baseEmitter.errBoth(Both.of(failure, failure)).map(Diff::getHints).collect(Collectors.toList()),
                GenericDiffers.ofAtMostOne(baseEmitter, Both.of(List.of(1, 2), List.of(1, 2)))
                        .map(Diff::getHints).collect(Collectors.toList()));
    }

    @Test
    void ofAllInEitherSet() {
        assertEquals(Optional.empty(),
                GenericDiffers.ofAllInEitherSet(baseEmitter, Both.of(List.of(1), List.of(1)))
                        .findFirst());

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofAllInEitherSet(baseEmitter, Both.of(List.of(1), List.of(1)),
                                (emitter, values) -> Stream.of(sentinel))
                        .findFirst());
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofAllInEitherSet(baseEmitter, Both.of(List.of(1), List.of(1)),
                                LinkedHashSet::new,
                                (emitter, values) -> Stream.of(sentinel))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.forChild("2").added()),
                GenericDiffers.ofAllInEitherSet(baseEmitter::forChild, Both.of(List.of("1"), List.of("1", "2")))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.forChild("2").removed()),
                GenericDiffers.ofAllInEitherSet(baseEmitter::forChild, Both.of(List.of("1", "2"), List.of("1")))
                        .findFirst());
    }

    @Test
    void ofAllInEitherMap() {
        assertEquals(Optional.empty(),
                GenericDiffers.ofAllInEitherMap(baseEmitter, Both.of(Map.of("foo", "foo1"), Map.of("foo", "foo1")))
                        .findFirst());

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofAllInEitherMap(baseEmitter, Both.of(Map.of("foo", "foo1"), Map.of("foo", "foo1")),
                                (emitter, values) -> Stream.of(sentinel))
                        .findFirst());
        assertEquals(Optional.of(sentinel),
                GenericDiffers.ofAllInEitherMap(baseEmitter, Both.of(Map.of("foo", "foo1"), Map.of("foo", "foo1")),
                                LinkedHashSet::new,
                                (emitter, values) -> Stream.of(sentinel))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.forChild("foo").added()),
                GenericDiffers.ofAllInEitherMap(baseEmitter::forChild, Both.of(Collections.emptyMap(), Map.of("foo", "foo1")))
                        .findFirst());

        assertEquals(Optional.of(baseEmitter.forChild("foo").removed()),
                GenericDiffers.ofAllInEitherMap(baseEmitter::forChild, Both.of(Map.of("foo", "foo1"), Collections.emptyMap()))
                        .findFirst());
    }
}
