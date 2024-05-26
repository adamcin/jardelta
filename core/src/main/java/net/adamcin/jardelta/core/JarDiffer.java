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

package net.adamcin.jardelta.core;

import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.api.jar.OpenJar;
import net.adamcin.jardelta.core.entry.JarEntryDiffer;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class JarDiffer implements Differ<OpenJar> {
    private final Settings settings;

    public JarDiffer(@NotNull Settings settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<OpenJar> element) {
        final Both<Map.Entry<OpenJar, Set<Name>>> resources = element.values()
                .map(jar -> Fun.toEntry(jar, jar.getEntryNames()));
        Set<Name> allNames = resources.map(Map.Entry::getValue).stream()
                .reduce(new TreeSet<>(), (acc, add) -> {
                    acc.addAll(add);
                    return acc;
                });

        final JarEntryDiffer differ = new JarEntryDiffer(settings);
        return allNames.stream()
                .map(resourceName -> Element.of(resourceName,
                        resources.map(entry -> entry.getKey().getEntryMeta(resourceName))))
                .flatMap(entry -> differ.diff(baseEmitter, entry));
    }

}
