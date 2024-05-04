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
import net.adamcin.jardelta.core.Settings;
import net.adamcin.jardelta.core.util.GenericDiff;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class JarEntryDiffer implements Differ<JarEntry> {
    public static final String DIFF_KIND = "entry";
    private final Settings settings;

    public JarEntryDiffer(@NotNull Settings settings) {
        this.settings = settings;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull JarEntry diffed) {
        final Diff.Builder builder = Diff.builder(DIFF_KIND).named(diffed.getName());
        return GenericDiff.ofOptionals(builder, diffed.both(), results ->
                GenericDiff.ofResults(builder, results, values ->
                        diffJarEntryMetaData(diffed, values)));
    }

    @NotNull
    Stream<Diff> diffJarEntryMetaData(@NotNull JarEntry diffed, @NotNull Both<JarEntryMetadata> bothMetas) {
        final List<Diff> diffs = new ArrayList<>();

        if (!bothMetas.map(JarEntryMetadata::getSha256).testBoth(String::equals)) {
            diffs.add(Diff.builder(DIFF_KIND).named(diffed.getName()).changed());
        }
        if (settings.isCompareLastModified() && !bothMetas.map(JarEntryMetadata::getLastModified).testBoth(Long::equals)) {
            diffs.add(Diff.builder(DIFF_KIND + ".lastModified").named(diffed.getName()).changed());
        }
        if (!bothMetas.mapOptional(JarEntryMetadata::getExtra).testBoth(Optional::equals)) {
            diffs.add(Diff.builder(DIFF_KIND + ".extra").named(diffed.getName()).changed());
        }
        return diffs.stream();
    }

}
