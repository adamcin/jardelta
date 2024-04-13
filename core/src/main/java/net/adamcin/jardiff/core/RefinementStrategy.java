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

package net.adamcin.jardiff.core;

import org.jetbrains.annotations.NotNull;

/**
 * A strategy representing a subsequent phase of differentiation between the two jars, usually based on more specific
 * application assumptions, like Manifest attribute semantics or parsing of OSGI MetaType resources.
 */
public interface RefinementStrategy {

    /**
     * Execute a new diff starting with the provided context, based on the resulting diffs from prior diff phases.
     *
     * @param context the diff context provided to {@link Plan#execute(Context)}
     * @param diffs   the cumulative result of prior diff phases
     * @return a {@link net.adamcin.jardiff.core.RefinedDiff}
     */
    @NotNull
    RefinedDiff refine(@NotNull Context context, @NotNull Diffs diffs);
}
