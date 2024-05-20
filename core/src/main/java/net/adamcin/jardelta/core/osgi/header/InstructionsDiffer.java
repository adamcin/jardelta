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

package net.adamcin.jardelta.core.osgi.header;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.manifest.ManifestAttribute;
import net.adamcin.jardelta.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InstructionsDiffer implements Differ<Instructions> {
    public static final Kind DIFF_KIND = Kind.of("header");
    public static final Attributes NAMES_WITH_DUPLICATE_PARAMETERS = ManifestAttribute.attributeSet(
            Constants.REQUIRE_CAPABILITY,
            Constants.PROVIDE_CAPABILITY);
    public static final Attributes NAMES_WITH_ALIAS_PARAMETERS = ManifestAttribute.attributeSet(
            Constants.INCLUDE_RESOURCE);


    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Instructions element) {
        Attributes.Name attributeName = element.getAttributeName();
        return GenericDiffers.ofOptionals(baseEmitter.forSubElement(element), element.values(),
                (childEmitter, values) -> {
                    if (NAMES_WITH_DUPLICATE_PARAMETERS.containsKey(attributeName)) {
                        return diffParameterListsWithDuplicates(childEmitter, values);
                    } else if (NAMES_WITH_ALIAS_PARAMETERS.containsKey(attributeName)) {
                        return diffParameterListsWithAliases(childEmitter, values);
                    } else {
                        return diffParameters(childEmitter, values);
                    }
                });
    }

    @NotNull Stream<Diff> diffParameters(@NotNull Emitter baseEmitter,
                                         @NotNull Both<Parameters> bothParameters) {
        return GenericDiffers.ofAllInEitherMap(baseEmitter, bothParameters,
                (emitter, optAttrs) -> GenericDiffers.ofOptionals(emitter, optAttrs, GenericDiffers::ofAllInEitherMap));
    }

    @NotNull Stream<Diff> diffParameterListsWithAliases(@NotNull Emitter emitter,
                                                        @NotNull Both<Parameters> bothParameters) {
        final Set<String> keys = bothParameters.stream()
                .flatMap(Fun.compose1(Parameters::keySet, Set::stream))
                .map(key -> key.contains("=") ? key.substring(0, key.indexOf('=')) : key)
                .collect(Collectors.toSet());

        final ParameterListDiffer differ = new ParameterListDiffer(false);
        return keys.stream()
                .map(key -> new ParameterListElement(emitter.getName(), key, bothParameters.map(map ->
                        Optional.ofNullable(ParameterList.fromAliases(key, map)))))
                .filter(ParameterListElement::isDiff)
                .flatMap(parameterListElement -> differ.diff(emitter, parameterListElement));
    }

    @NotNull Stream<Diff> diffParameterListsWithDuplicates(@NotNull Emitter emitter,
                                                           @NotNull Both<Parameters> bothParameters) {
        final Set<String> keys = bothParameters.stream()
                .flatMap(Fun.compose1(Parameters::keySet, Set::stream))
                .collect(Collectors.toSet());

        final ParameterListDiffer differ = new ParameterListDiffer(true);
        return keys.stream()
                .filter(key -> !key.endsWith("~") || !keys.contains(key.substring(0, key.length() - 1)))
                .map(key -> new ParameterListElement(emitter.getName(), key, bothParameters.map(map ->
                        Optional.ofNullable(ParameterList.fromDuplicates(key, map)))))
                .filter(ParameterListElement::isDiff)
                .flatMap(parameterListElement -> differ.diff(emitter, parameterListElement));
    }

}
