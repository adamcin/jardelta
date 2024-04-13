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

import aQute.bnd.header.Parameters;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InstructionsDiffer implements Differ<Instructions> {
    public static final String DIFF_KIND = "osgi.header";

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Instructions diffed) {
        final Diff.Builder diffBuilder = Diff.builder(DIFF_KIND).named(diffed.getName());
        return GenericDiffers.diffOptionals(diffBuilder, diffed.both(), values -> diffParameters(diffed, values));
    }

    @NotNull Stream<Diff> diffParameters(@NotNull Instructions diffed, @NotNull Both<Parameters> bothParameters) {
        final Set<String> keys = bothParameters.stream()
                .flatMap(Fun.compose1(Parameters::keySet, Set::stream))
                .collect(Collectors.toSet());

        final ParameterDiffer differ = new ParameterDiffer();
        return keys.stream()
                .map(key -> new Parameter(diffed.getName(), key, bothParameters.map(map ->
                        Optional.ofNullable(ParameterList.from(key, map)))))
                .filter(Parameter::isDiff)
                .flatMap(differ::diff);
    }
}
