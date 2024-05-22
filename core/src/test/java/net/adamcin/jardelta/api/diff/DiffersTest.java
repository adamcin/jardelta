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
import net.adamcin.jardelta.api.Name;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static net.adamcin.jardelta.test.DiffTestUtil.assertDiffs;

class DiffersTest {
    final Emitter baseEmitter = Diff.emitterOf(Kind.of("test"))
            .forName(Name.of("testRoot"));

    @Test
    void diffObjectEquality() {
        assertDiffs(Differs.diffEquality(baseEmitter, Both.of(1, 1)));

        assertDiffs(Differs.diffEquality(baseEmitter, Both.of(1, 2)),
                baseEmitter.changed(Both.of("1", "2")));

        assertDiffs(Differs.diffEquality(baseEmitter, Both.of(1, 1), (left, right) -> false),
                baseEmitter.changed(Both.of("1", "1")));

        assertDiffs(Differs.diffEquality(baseEmitter, Both.of(1, 2), (left, right) -> true));
    }

    @Test
    void diffOptionals() {
        assertDiffs(Differs.diffOptionals(baseEmitter, Both.ofNullables(1, 1)));

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertDiffs(Differs.diffOptionals(baseEmitter, Both.ofNullables(1, 2),
                (emitter, values) -> Stream.of(sentinel)), sentinel);

        assertDiffs(Differs.diffOptionals(baseEmitter, Both.ofNullables(null, 1)), baseEmitter.added("1"));
        assertDiffs(Differs.diffOptionals(baseEmitter, Both.ofNullables(1, null)), baseEmitter.removed("1"));
        assertDiffs(Differs.diffOptionals(baseEmitter, Both.ofNullables(1, 2)), baseEmitter.changed(Both.of("1", "2")));
        assertDiffs(Differs.diffOptionals(Fun.compose1(num -> String.format("num %d", num), Optional::of),
                        baseEmitter, Both.ofNullables(null, 1)),
                baseEmitter.added("num 1"));
        assertDiffs(Differs.diffOptionals(Fun.compose1(num -> String.format("num %d", num), Optional::of),
                        baseEmitter, Both.ofNullables(1, null)),
                baseEmitter.removed("num 1"));
    }

    @Test
    void diffResults() {
        assertDiffs(Differs.diffResults(baseEmitter, Both.of(Result.success(1), Result.success(1))));

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertDiffs(Differs.diffResults(baseEmitter, Both.of(Result.success(1), Result.success(1)),
                (emitter, values) -> Stream.of(sentinel)), sentinel);

        final Result<Integer> leftFail = Result.failure("leftFail");
        final Result<Integer> rightFail = Result.failure("rightFail");

        assertDiffs(Differs.diffResults(baseEmitter, Both.of(leftFail, Result.success(1))), baseEmitter.errLeft(leftFail));
        assertDiffs(Differs.diffResults(baseEmitter, Both.of(Result.success(1), rightFail)), baseEmitter.errRight(rightFail));
        assertDiffs(Differs.diffResults(baseEmitter, Both.of(leftFail, rightFail)), baseEmitter.errBoth(Both.of(leftFail, rightFail)));
    }

    @Test
    void diffAtMostOne() {
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1), List.of(1))));

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1), List.of(1)),
                (emitter, values) -> Stream.of(sentinel)), sentinel);
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(Collections.emptyList(), Collections.emptyList()),
                (emitter, values) -> Stream.of(sentinel)));
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(Collections.emptyList(), List.of(1))),
                baseEmitter.added("1"));
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1), Collections.emptyList())),
                baseEmitter.removed("1"));
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1), List.of(2))),
                baseEmitter.changed(Both.of("1", "2")));

        final Result<Integer> failure = Result.failure(Differs.ERROR_AT_MOST_ONE);

        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1, 2), List.of(2))),
                baseEmitter.errLeft(failure));
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1), List.of(2, 1))),
                baseEmitter.errRight(failure));
        assertDiffs(Differs.diffAtMostOne(baseEmitter, Both.of(List.of(1, 2), List.of(1, 2))),
                baseEmitter.errBoth(Both.of(failure, failure)));
    }

    @Test
    void diffAllInEitherSet() {
        assertDiffs(Differs.diffSets(baseEmitter, Both.of(List.of("1"), List.of("1"))));

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertDiffs(Differs.diffSets(baseEmitter, Both.of(List.of("1"), List.of("1")),
                (emitter, values) -> Stream.of(sentinel)), sentinel);
        assertDiffs(Differs.diffSets(baseEmitter, builder -> builder.setSupplier(LinkedHashSet::new),
                Both.of(List.of("1"), List.of("1")),
                (emitter, values) -> Stream.of(sentinel)), sentinel);
        assertDiffs(Differs.diffSets(baseEmitter, Both.of(List.of("1"), List.of("1", "2"))),
                baseEmitter.forChild("2").added());
        assertDiffs(Differs.diffSets(baseEmitter, Both.of(List.of("1", "2"), List.of("1"))),
                baseEmitter.forChild("2").removed());
    }

    @Test
    void diffAllInEitherMap() {
        assertDiffs(Differs.diffMaps(baseEmitter, Both.of(Map.of("foo", "foo1"), Map.of("foo", "foo1"))));

        final Diff sentinel = Diff.emitterOf(Kind.of("sentinel")).added();
        assertDiffs(Differs.diffMaps(baseEmitter, Both.of(Map.of("foo", "foo1"), Map.of("foo", "foo1")),
                (emitter, values) -> Stream.of(sentinel)), sentinel);
        assertDiffs(Differs.diffMaps(baseEmitter, builder -> builder.setSupplier(LinkedHashSet::new),
                Both.of(Map.of("foo", "foo1"), Map.of("foo", "foo1")),
                (emitter, values) -> Stream.of(sentinel)), sentinel);
        assertDiffs(Differs.diffMaps(baseEmitter, Both.of(Collections.emptyMap(), Map.of("foo", "foo1"))),
                baseEmitter.forChild("foo").added("foo1"));
        assertDiffs(Differs.diffMaps(baseEmitter, Both.of(Map.of("foo", "foo1"), Collections.emptyMap())),
                baseEmitter.forChild("foo").removed("foo1"));

    }
}
