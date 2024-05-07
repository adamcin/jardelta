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

import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.Differ;
import net.adamcin.jardelta.core.Name;
import net.adamcin.jardelta.core.Settings;
import net.adamcin.jardelta.core.util.CompositeDiffer;
import net.adamcin.jardelta.core.util.GenericDiffers;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class JarEntryDiffer implements Differ<JarEntry> {
    public static final String DIFF_KIND = "entry";
    private final CompositeDiffer<JarEntryMetadata> differs;

    public JarEntryDiffer(final @NotNull Settings settings) {
        this.differs = CompositeDiffer.of(nextDiff -> {
            nextDiff.accept("", (builder, element) -> GenericDiffers.ofObjectEquality(builder, element.both().map(JarEntryMetadata::getSha256)));
            nextDiff.accept("{extra}", (builder, element) -> GenericDiffers.ofOptionals(builder, element.both().mapOptional(JarEntryMetadata::getExtra)));
            if (settings.isCompareLastModified()) {
                nextDiff.accept("{lastModified}", (builder, element) -> GenericDiffers.ofObjectEquality(builder, element.both().map(JarEntryMetadata::getSha256)));
            }
        });
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull JarEntry diffed) {
        final Diff.Builder builder = Diff.builder(DIFF_KIND).named(diffed.name());
        return GenericDiffers.ofOptionals(builder, diffed.both(), results ->
                GenericDiffers.ofResults(builder, results, values ->
                        differs.diff(builder, diffed.project(Name.ROOT, values))));
    }
}
