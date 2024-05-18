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

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.adamcin.jardelta.api.diff.Diffs;

import java.util.Collections;
import java.util.List;

@Builder
@Data
public final class Delta {
    @NonNull
    @Builder.Default
    private Diffs results = Diffs.EMPTY;
    @NonNull
    @Builder.Default
    private Diffs initial = Diffs.EMPTY;
    @NonNull
    @Builder.Default
    private List<Refinement> refinements = Collections.emptyList();
}
