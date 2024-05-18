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

package net.adamcin.jardelta.core.osgi.ocd;

import jakarta.json.Json;
import jakarta.json.stream.JsonCollectors;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MetaTypeOCDDiffer implements Differ<MetaTypeOCD> {

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull MetaTypeOCD element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        final Stream<Diff> ocdElementDiffs = Stream.of(
                Fun.toEntry("@name", element.values().mapOptional(ObjectClassDefinition::getName)),
                Fun.toEntry("@description", element.values().mapOptional(ObjectClassDefinition::getDescription))
        ).flatMap(Fun.mapEntry((key, values) -> GenericDiffers.ofObjectEquality(emitter.forChild(key), requireChars(values))));

        final Both<Map<String, List<AttributeDefinition>>> bothAttributeDefinitions = element.values()
                .map(ocd -> {
                    AttributeDefinition[] attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                    Stream<AttributeDefinition> attributeStream = attributes == null ? Stream.empty() : Stream.of(attributes);
                    return attributeStream.collect(Collectors.groupingBy(AttributeDefinition::getID));
                });

        return Stream.concat(ocdElementDiffs, GenericDiffers.ofAllInEitherMap(emitter::forChild, bothAttributeDefinitions,
                (childEmitter, bothLists) ->
                        GenericDiffers.ofAtMostOne(childEmitter, bothLists.map(Optional::get),
                                bothAttributes -> Stream.concat(diffADProperties(childEmitter, bothAttributes),
                                        diffOptions(childEmitter, bothAttributes))
                        )));
    }

    @NotNull
    static Both<Optional<String>> requireChars(@NotNull Both<Optional<String>> values) {
        return values.map(value -> value.filter(Fun.inferTest1(String::isEmpty).negate()));
    }

    Stream<Diff> diffADProperties(@NotNull Emitter baseEmitter, @NotNull Both<AttributeDefinition> bothAttributes) {
        return Stream.concat(Stream.of(
                        Fun.toEntry("@name", bothAttributes.mapOptional(AttributeDefinition::getName)),
                        Fun.toEntry("@description", bothAttributes.mapOptional(AttributeDefinition::getDescription)),
                        Fun.toEntry("@defaultValue", bothAttributes.mapOptional(AttributeDefinition::getDefaultValue)
                                .map(odv -> odv.map(Stream::of).map(dvs -> dvs
                                        .map(Json::createValue)
                                        .collect(JsonCollectors.toJsonArray()).toString())))
                ).flatMap(Fun.mapEntry((key, values) -> GenericDiffers.ofObjectEquality(baseEmitter.forChild(key), values))),
                Stream.of(
                        Fun.toEntry("@type", bothAttributes.map(AttributeDefinition::getType)),
                        Fun.toEntry("@cardinality", bothAttributes.map(AttributeDefinition::getCardinality))
                ).flatMap(Fun.mapEntry((key, values) -> GenericDiffers.ofObjectEquality(baseEmitter.forChild(key), values))));
    }

    Stream<Diff> diffOptions(@NotNull Emitter baseEmitter,
                             @NotNull Both<AttributeDefinition> bothAttributes) {
        final Emitter emitter = baseEmitter.forChild("@options");
        final Both<Optional<Map<String, Optional<String>>>> bothOptionsMaps = bothAttributes
                .mapOptional(AttributeDefinition::getOptionValues)
                .zipWith(bothAttributes.mapOptional(AttributeDefinition::getOptionLabels),
                        (maybeValues, maybeLabels) -> maybeValues.flatMap(values ->
                                maybeLabels.map(labels -> IntStream.range(0, values.length).mapToObj(index ->
                                                Fun.toEntry(values[index], index < labels.length
                                                        ? Optional.ofNullable(labels[index])
                                                        : Optional.<String>empty()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
        return GenericDiffers.ofOptionals(emitter, bothOptionsMaps, optionsMap ->
                GenericDiffers.ofAllInEitherMap(emitter::forChild, optionsMap, (optionValueDiff, optOptLabels) ->
                        GenericDiffers.ofOptionals(optionValueDiff,
                                optOptLabels.map(optOptLabel -> optOptLabel.flatMap(label -> label)),
                                labels -> labels.testBoth(Objects::equals)
                                        ? Stream.empty()
                                        : Stream.of(optionValueDiff.changed()))
                ));
    }
}
