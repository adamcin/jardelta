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

package net.adamcin.jardiff.core.osgi.header;

import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.manifest.MFAttribute;
import net.adamcin.jardiff.core.manifest.MFAttributeDiff;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class LocalizedHeaderDiff implements Differ<MFAttribute> {
    public static final String DIFF_KIND = "osgi.header.locale";

    private final String locale;

    public LocalizedHeaderDiff(@NotNull String locale) {
        this.locale = locale;
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull MFAttribute diffed) {
        return diffed.isDiff() ? Stream.of(MFAttributeDiff.ofRawValue(DIFF_KIND, diffed)) : Stream.empty();
    }
}
