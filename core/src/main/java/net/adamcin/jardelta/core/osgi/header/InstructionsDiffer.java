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
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.manifest.ManifestAttribute;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

    static final Differ<Element<Parameters>> DIFFER_DEFAULT_PARAMETERS = Differs.ofMaps(Function.identity(),
            Differs.ofMapValues(Differs.ofMaps(Function.identity())));

    static final Differ<Element<Parameters>> DIFFER_DUPLICATE_PARAMETERS = Differs.ofMaps(
            parameters -> parameters.keySet().stream()
                    .filter(key -> !key.endsWith("~") || !parameters.containsKey(key.substring(0, key.length() - 1)))
                    .map(key -> Fun.toEntry(key, ParameterList.fromDuplicates(key, parameters)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            Differs.ofMapValues((key, value) -> Optional.of(value), new ParameterListDiffer(true)));

    static final Differ<Element<Parameters>> DIFFER_ALIAS_PARAMETERS = Differs.ofMaps(
            parameters -> parameters.keySet().stream()
                    .map(key -> key.contains("=") ? key.substring(0, key.indexOf('=')) : key)
                    .map(key -> Fun.toEntry(key, ParameterList.fromAliases(key, parameters)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            Differs.ofMapValues((key, value) -> Optional.of(value), new ParameterListDiffer(false)));

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Instructions element) {
        final Attributes.Name attributeName = element.getAttributeName();
        Differ<Element<Parameters>> chosenDiffer = DIFFER_DEFAULT_PARAMETERS;
        if (NAMES_WITH_DUPLICATE_PARAMETERS.containsKey(attributeName)) {
            chosenDiffer = DIFFER_DUPLICATE_PARAMETERS;
        } else if (NAMES_WITH_ALIAS_PARAMETERS.containsKey(attributeName)) {
            chosenDiffer = DIFFER_ALIAS_PARAMETERS;
        }
        return Differs.ofOptionals(Function.identity(), chosenDiffer).diff(baseEmitter.forSubElement(element), element);
    }

}
