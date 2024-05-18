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
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenericDiffersTest {
    final Emitter diffBuilder = Diff.emitterOf(Kind.of("test")).forName(Name.of("testRoot"));

    @Test
    void ofObjectEquality() {
        assertEquals(0, GenericDiffers.ofObjectEquality(
                        diffBuilder,
                        Both.of(1, 1))
                .count());

        assertEquals(Optional.of(diffBuilder.changed()), GenericDiffers.ofObjectEquality(
                        diffBuilder, Both.of(1, 2))
                .findFirst());

        assertEquals(Optional.of(diffBuilder.changed()), GenericDiffers.ofObjectEquality(
                        diffBuilder, Both.of(1, 1), (left, right) -> false)
                .findFirst());

        assertEquals(0, GenericDiffers.ofObjectEquality(
                        diffBuilder,
                        Both.of(1, 2), (left, right) -> true)
                .count());
    }
}
