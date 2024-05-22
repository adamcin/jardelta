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

package net.adamcin.jardelta.core.entry;

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.api.jar.EntryMeta;
import net.adamcin.jardelta.core.Settings;
import net.adamcin.jardelta.core.util.CompositeDiffer;
import net.adamcin.jardelta.api.diff.Differs;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class JarEntryDiffer implements Differ<JarEntry> {
    public static final Kind DIFF_KIND = Kind.of("entry");
    private final CompositeDiffer<EntryMeta> differs;

    public JarEntryDiffer(final @NotNull Settings settings) {
        this.differs = CompositeDiffer.of(builder -> {
            builder.put("", (emitter, element) -> Differs.diffEquality(emitter, element.values().map(EntryMeta::getSha256)));
            builder.put("{extra}", (emitter, element) -> Differs.diffOptionals(emitter, element.values().mapOptional(EntryMeta::getExtra)));
            if (settings.isCompareLastModified()) {
                builder.put("{lastModified}", (emitter, element) -> Differs.diffEquality(emitter, element.values().map(EntryMeta::getSha256)));
            }
        });
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull JarEntry element) {
        return Differs.diffOptionals(baseEmitter.forSubElement(element), element.values(),
                (emitter, results) -> Differs.diffResults(emitter, results,
                        (resultEmitter, values) -> differs.diff(resultEmitter, element.project(Name.ROOT, values))));
    }
}
