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

package net.adamcin.jardiff.core.osgi.header;

import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ParameterDiffer implements Differ<Parameter> {
    public static final String DIFF_KIND = "osgi.header.parameter";

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Parameter diffed) {
        final Diff.Builder diffBuilder = Diff.builder(DIFF_KIND).named(diffed.getName());
        return GenericDiffers.diffOptionals(diffBuilder, diffed.both(),
                values -> diffParameterLists(diffBuilder, diffed, values));
    }

    @NotNull Stream<Diff> diffParameterLists(@NotNull Diff.Builder diffBuilder,
                                             @NotNull Parameter diffed,
                                             @NotNull Both<ParameterList> bothParameterLists) {
        return Stream.of(diffBuilder.changed());
    }
}
