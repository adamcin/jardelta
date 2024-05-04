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
import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.Differ;
import net.adamcin.jardelta.core.util.GenericDiff;
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
    public static final String DIFF_KIND = "osgi.ocd";

    @Override
    public @NotNull Stream<Diff> diff(@NotNull MetaTypeOCD diffed) {
        final Diff.Builder diffBuilder = new Diff.Builder(DIFF_KIND).named(diffed.getName());
        final Stream<Diff> ocdElementDiffs = Stream.of(
                Fun.toEntry("@name", diffed.both().mapOptional(ObjectClassDefinition::getName)),
                Fun.toEntry("@description", diffed.both().mapOptional(ObjectClassDefinition::getDescription))
        ).flatMap(Fun.mapEntry((key, values) -> diffElements(diffBuilder, key, values)));

        final Both<Map<String, List<AttributeDefinition>>> bothAttributeDefinitions = diffed.both()
                .map(ocd -> {
                    AttributeDefinition[] attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                    Stream<AttributeDefinition> attributeStream = attributes == null ? Stream.empty() : Stream.of(attributes);
                    return attributeStream.collect(Collectors.groupingBy(AttributeDefinition::getID));
                });

        return Stream.concat(ocdElementDiffs, GenericDiff.ofAllInEitherMap(diffBuilder::child, bothAttributeDefinitions,
                (attributeId, bothLists) -> {
                    final Diff.Builder childBuilder = diffBuilder.child(attributeId);
                    return GenericDiff.ofAtMostOne(childBuilder, bothLists.map(Optional::get),
                            bothAttributes -> Stream.concat(diffADProperties(childBuilder, bothAttributes),
                                    diffOptions(childBuilder, bothAttributes))
                    );
                }));
    }

    @NotNull
    static Both<Optional<String>> requireChars(@NotNull Both<Optional<String>> values) {
        return values.map(value -> value.filter(Fun.inferTest1(String::isEmpty).negate()));
    }

    Stream<Diff> diffADProperties(@NotNull Diff.Builder childBuilder, @NotNull Both<AttributeDefinition> bothAttributes) {
        return Stream.concat(Stream.of(
                        Fun.toEntry("@name", bothAttributes.mapOptional(AttributeDefinition::getName)),
                        Fun.toEntry("@description", bothAttributes.mapOptional(AttributeDefinition::getDescription)),
                        Fun.toEntry("@defaultValue", bothAttributes.mapOptional(AttributeDefinition::getDefaultValue)
                                .map(odv -> odv.map(Stream::of).map(dvs -> dvs
                                        .map(Json::createValue)
                                        .collect(JsonCollectors.toJsonArray()).toString())))
                ).flatMap(Fun.mapEntry((key, values) -> diffElements(childBuilder, key, values))),
                Stream.of(
                        Fun.toEntry("@type", bothAttributes.map(AttributeDefinition::getType)),
                        Fun.toEntry("@cardinality", bothAttributes.map(AttributeDefinition::getCardinality))
                ).flatMap(Fun.mapEntry((key, values) -> diffIntegers(childBuilder, key, values))));
    }

    Stream<Diff> diffOptions(@NotNull Diff.Builder parentBuilder,
                             @NotNull Both<AttributeDefinition> bothAttributes) {
        final Diff.Builder optionsDiff = parentBuilder.child("@options");
        final Both<Optional<Map<String, Optional<String>>>> bothOptionsMaps = bothAttributes
                .mapOptional(AttributeDefinition::getOptionValues)
                .zipWith(bothAttributes.mapOptional(AttributeDefinition::getOptionLabels),
                        (maybeValues, maybeLabels) -> maybeValues.flatMap(values ->
                                maybeLabels.map(labels -> IntStream.range(0, values.length).mapToObj(index ->
                                                Fun.toEntry(values[index], index < labels.length
                                                        ? Optional.ofNullable(labels[index])
                                                        : Optional.<String>empty()))
                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
        return GenericDiff.ofOptionals(optionsDiff, bothOptionsMaps, optionsMap ->
                GenericDiff.ofAllInEitherMap(optionsDiff::child, optionsMap, (option, optOptLabels) -> {
                    final Diff.Builder optionValueDiff = optionsDiff.child(option);
                    return GenericDiff.ofOptionals(optionValueDiff,
                            optOptLabels.map(optOptLabel -> optOptLabel.flatMap(label -> label)),
                            labels -> labels.testBoth(Objects::equals)
                                    ? Stream.empty()
                                    : Stream.of(optionValueDiff.changed()));
                }));
    }

    Stream<Diff> diffIntegers(@NotNull Diff.Builder parentBuilder,
                              @NotNull String key,
                              @NotNull Both<Integer> values) {
        final Diff.Builder elementDiff = parentBuilder.child(key);
        if (values.testBoth(Objects::equals)) {
            return Stream.empty();
        } else {
            return Stream.of(elementDiff.changed());
        }
    }

    Stream<Diff> diffElements(@NotNull Diff.Builder parentBuilder,
                              @NotNull String key,
                              @NotNull Both<Optional<String>> values) {
        final Diff.Builder elementDiff = parentBuilder.child(key);
        final Both<Optional<String>> required = requireChars(values);
        return GenericDiff.ofOptionals(elementDiff, required, diffed -> {
            if (diffed.testBoth(Objects::equals)) {
                return Stream.empty();
            } else {
                return Stream.of(elementDiff.changed());
            }
        });
    }
}
