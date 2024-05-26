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

package net.adamcin.jardelta.api.diff;

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.Name;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
@ProviderType
public interface FilteredDiffs {

    @NotNull Stream<Diff> stream();

    default boolean isEmpty() {
        return stream().findAny().isEmpty();
    }

    @NotNull FilteredDiffs filter(@NotNull Predicate<Diff> predicate);

    default @NotNull FilteredDiffs withKind(@NotNull Kind kind) {
        return filter(diff -> diff.getKind().isSubKindOf(kind));
    }

    default @NotNull FilteredDiffs withExactKind(@NotNull Kind kind) {
        return filter(diff -> diff.getKind().equals(kind) );
    }

    default @NotNull FilteredDiffs withName(@NotNull Name name) {
        return filter(diff -> diff.getName().startsWithName(name));
    }

    default @NotNull FilteredDiffs withExactName(@NotNull Name name) {
        return filter(diff -> diff.getName().equals(name));
    }

    default @NotNull FilteredDiffs withVerbs(@NotNull Verb... verbs) {
        if (verbs.length == 0) {
            return filter(diff -> false);
        } else if (verbs.length == 1) {
            Verb verb = verbs[0];
            return filter(diff -> diff.getVerb().equals(verb));
        } else {
            final Set<Verb> verbSet = new TreeSet<>(List.of(verbs));
            return filter(Fun.composeTest1(Diff::getVerb, Fun.inSet(verbSet)));
        }
    }

    default String stringify() {
        return "FilteredDiffs[" + stream().map(Diff::toString).collect(Collectors.joining(",")) + "]";
    }

}
