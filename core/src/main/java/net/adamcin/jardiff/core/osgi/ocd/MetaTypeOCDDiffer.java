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

package net.adamcin.jardiff.core.osgi.ocd;

import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class MetaTypeOCDDiffer implements Differ<MetaTypeOCD> {
    public static final String DIFF_KIND = "osgi.ocd";

    @Override
    public @NotNull Stream<Diff> diff(@NotNull MetaTypeOCD diffed) {
        final Diff.Builder diffBuilder = new Diff.Builder(DIFF_KIND).named(diffed.getName());
        final Stream<Diff> ocdElementDiffs = Stream.of(
                Fun.toEntry("@id", diffed.both().mapOptional(ObjectClassDefinition::getID)),
                Fun.toEntry("@name", diffed.both().mapOptional(ObjectClassDefinition::getName)),
                Fun.toEntry("@description", diffed.both().mapOptional(ObjectClassDefinition::getDescription))
        ).flatMap(Fun.mapEntry((key, values) -> diffElements(diffBuilder, key, values)));

        return ocdElementDiffs;
    }

    @NotNull
    static Both<Optional<String>> requireChars(@NotNull Both<Optional<String>> values) {
        return values.map(value -> value.filter(Fun.inferTest1(String::isEmpty).negate()));
    }

    Stream<Diff> diffElements(@NotNull Diff.Builder parentBuilder,
                              @NotNull String key,
                              @NotNull Both<Optional<String>> values) {
        final Diff.Builder elementDiff = parentBuilder.child(key);
        final Both<Optional<String>> required = requireChars(values);
        if (required.left().isEmpty()) {
            if (required.right().isPresent()) {
                return Stream.of(elementDiff.added());
            }
        } else if (required.right().isEmpty()) {
            return Stream.of(elementDiff.removed());
        } else if (!required.testBoth(Objects::equals)) {
            return Stream.of(elementDiff.changed());
        }
        return Stream.empty();
    }
}
