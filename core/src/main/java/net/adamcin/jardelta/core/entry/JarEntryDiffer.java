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
import net.adamcin.jardelta.api.diff.CompositeDiffer;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.api.jar.EntryMeta;
import net.adamcin.jardelta.core.Settings;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class JarEntryDiffer implements Differ<Element<Optional<Result<EntryMeta>>>> {
    public static final Kind DIFF_KIND = Kind.of("entry");
    private final Differ<Element<EntryMeta>> differs;

    public JarEntryDiffer(final @NotNull Settings settings) {
        this.differs = CompositeDiffer.of(builder -> {
            builder.put("", Differs.ofEquality(EntryMeta::getSha256));
            builder.put("{extra}", Differs.ofNullables(EntryMeta::getExtra));
            if (settings.isCompareLastModified()) {
                builder.put("{lastModified}", Differs.ofEquality(EntryMeta::getLastModified));
            }
        });
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter,
                                      @NotNull Element<Optional<Result<EntryMeta>>> element) {
        return Differs.ofOptionals(Function.identity(),
                        Differs.ofResults(Function.identity(), differs))
                .diff(baseEmitter.forSubElement(element), element);
    }
}
