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
import net.adamcin.jardelta.api.diff.CompositeDiffer;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.adamcin.jardelta.api.diff.Differs.ofMapValues;
import static net.adamcin.jardelta.api.diff.Differs.ofMaps;
import static net.adamcin.jardelta.api.diff.Differs.ofNullables;
import static net.adamcin.jardelta.api.diff.Differs.ofOptionals;

public class OCDDiffer implements Differ<ObjectClassDefinition> {

    private final CompositeDiffer<ObjectClassDefinition> differs = CompositeDiffer.of(builder -> {
        builder.put("@name", ofNullables(ObjectClassDefinition::getName));
        builder.put("@description", ofNullables(ObjectClassDefinition::getDescription));
        builder.put("", ofMaps(OCDDiffer::mapAttributeDefinitionLists,
                ofMapValues(Differs.ofAtMostOne(Function.identity(),
                        CompositeDiffer.of(adPropsBuilder -> {
                            // for every AttributeDefinition...
                            adPropsBuilder.put("@name", ofNullables(AttributeDefinition::getName));
                            adPropsBuilder.put("@description", ofNullables(AttributeDefinition::getDescription));
                            adPropsBuilder.put("@defaultValue", ofOptionals(Fun.compose1(AttributeDefinition::getDefaultValue,
                                    dv -> Optional.ofNullable(dv).map(Stream::of).map(dvs -> dvs
                                            .map(Json::createValue)
                                            .collect(JsonCollectors.toJsonArray()).toString()))));
                            adPropsBuilder.put("@type", Differs.ofEquality(AttributeDefinition::getType));
                            adPropsBuilder.put("@cardinality", Differs.ofEquality(AttributeDefinition::getCardinality));
                            adPropsBuilder.put("@options",
                                    Differs.projecting(OCDDiffer::projectOptionsMap,
                                            ofOptionals(Function.identity(),
                                                    ofMaps(Function.identity(),
                                                            ofMapValues(ofOptionals())))));
                        })))));
    });

    static Map<String, List<AttributeDefinition>> mapAttributeDefinitionLists(@NotNull ObjectClassDefinition ocd) {
        AttributeDefinition[] attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
        Stream<AttributeDefinition> attributeStream = attributes == null ? Stream.empty() : Stream.of(attributes);
        return attributeStream.collect(Collectors.groupingBy(AttributeDefinition::getID));
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<ObjectClassDefinition> element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return differs.diff(emitter, element);
    }

    @NotNull
    static Both<Optional<String>> requireChars(@NotNull Both<Optional<String>> values) {
        return values.map(value -> value.filter(Fun.inferTest1(String::isEmpty).negate()));
    }

    static Element<Optional<Map<String, Optional<String>>>> projectOptionsMap(@NotNull Element<AttributeDefinition> element) {
        return Element.of(element.name(), element.values()
                .mapOptional(AttributeDefinition::getOptionValues)
                .zipWith(element.values().mapOptional(AttributeDefinition::getOptionLabels),
                        (maybeValues, maybeLabels) -> maybeValues.flatMap(values ->
                                maybeLabels.map(labels -> IntStream.range(0, values.length).mapToObj(index ->
                                                Fun.toEntry(values[index], index < labels.length
                                                        ? Optional.ofNullable(labels[index])
                                                        : Optional.<String>empty()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))));
    }
}
