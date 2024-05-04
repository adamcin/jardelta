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

import net.adamcin.jardelta.core.entry.JarEntry;
import net.adamcin.jardelta.core.entry.JarEntryDiffer;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class JarDiffer implements Differ<Jars> {
    private final Settings settings;

    public JarDiffer(@NotNull Settings settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Jars diffed) {
        final Both<Map.Entry<JarPath, Set<Name>>> resources = diffed.both()
                .map(jar -> Fun.toEntry(jar, jar.getNames()));
        Set<Name> allNames = resources.map(Map.Entry::getValue).stream()
                .reduce(new TreeSet<>(), (acc, add) -> {
                    acc.addAll(add);
                    return acc;
                });

        final JarEntryDiffer differ = new JarEntryDiffer(settings);
        return allNames.stream()
                .map(resourceName -> new JarEntry(resourceName,
                        resources.map(entry -> entry.getKey().getResourceMetadata(resourceName))))
                .flatMap(differ::diff);
    }

}
