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

import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParameterListDiffer implements Differ<Element<Optional<ParameterList>>> {
    private final boolean allowDuplicates;

    private static final Differ<Element<Optional<ParameterList>>> DIFFER_AT_MOST_ONE =
            Differs.ofOptionals(Function.identity(),
                    Differs.ofAtMostOne(ParameterList::getAttrsList,
                            Differs.ofEquality(Function.identity())));

    // project an Optional<ParameterList> into a 0-many Set<String> because
    // "duplicate" keys are merely common prefixes of ;attr combos that together uniquely
    // identify entries. The presence of the "key" of the parsed ParameterList should not
    // be diffed as an element in isolation, because it is meaningless in that context.
    private static final Differ<Element<Optional<ParameterList>>> DIFFER_ALLOW_DUPLICATES =
            Differs.ofSets(oList -> oList.map(list -> list.getAttrsList().stream()
                            .map(ParameterList.AttrsEntry::toString)
                            .collect(Collectors.toList()))
                    .orElse(Collections.emptyList()));

    public ParameterListDiffer(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<Optional<ParameterList>> element) {
        final Differ<Element<Optional<ParameterList>>> differ = allowDuplicates
                ? DIFFER_ALLOW_DUPLICATES
                : DIFFER_AT_MOST_ONE;
        return differ.diff(baseEmitter.forSubElement(element), element);
    }
}
