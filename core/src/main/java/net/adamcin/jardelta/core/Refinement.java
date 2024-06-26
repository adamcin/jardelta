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

package net.adamcin.jardelta.core;


import lombok.Getter;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

@Getter
public class Refinement {
    public static final Refinement EMPTY = new Refinement(Collections.emptyList(), Diffs.EMPTY);
    private final Collection<Diff> superseded;
    private final Diffs diffs;

    public Refinement(@NotNull Collection<Diff> superseded, @NotNull Diffs diffs) {
        this.superseded = superseded;
        this.diffs = diffs;
    }

    public boolean isEmpty() {
        return superseded.isEmpty() && diffs.isEmpty();
    }
}
