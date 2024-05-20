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

import aQute.bnd.header.Attrs;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParameterListDiffer implements Differ<ParameterListElement> {
    private final boolean allowDuplicates;

    public ParameterListDiffer(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull ParameterListElement element) {
        if (allowDuplicates) {
            return diffWithDuplicates(baseEmitter.forSubElement(element), element.values());
        } else {
            return GenericDiffers.ofOptionals(baseEmitter.forSubElement(element),
                    element.values(), this::diffWithAtMostOne);
        }
    }

    @NotNull Stream<Diff> diffWithAtMostOne(@NotNull Emitter baseEmitter,
                                            @NotNull Both<ParameterList> values) {
        return GenericDiffers.ofAtMostOne(baseEmitter, values.map(ParameterList::getAttrList),
                (emitter, attrs) -> {
                    if (attrs.testBoth(Attrs::isEqual)) {
                        return Stream.empty();
                    } else {
                        return Stream.of(emitter.changed(values.zip(attrs).map(entry ->
                                entry.getKey().getKey() + entry.getValue().toString())));
                    }
                });
    }

    @NotNull Stream<Diff> diffWithDuplicates(@NotNull Emitter baseEmitter,
                                             @NotNull Both<Optional<ParameterList>> values) {
        final Set<String> allAttrNames = values.map(list -> list
                        .map(ParameterList::getAllAttrs)
                        .orElse(Collections.emptySet())).stream()
                .reduce(new TreeSet<>(), GenericDiffers::mergeSets, GenericDiffers::mergeSets);
        return GenericDiffers.ofAllInEitherSet(baseEmitter,
                values.map(value -> value.map(list -> list.attrsToStrings(allAttrNames)
                                .collect(Collectors.toSet()))
                        .orElse(Collections.emptySet())));
    }
}
