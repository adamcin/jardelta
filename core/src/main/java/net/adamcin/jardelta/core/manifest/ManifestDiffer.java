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

package net.adamcin.jardelta.core.manifest;

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class ManifestDiffer implements Differ<Optional<Manifest>> {

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter,
                                      @NotNull Element<Optional<Manifest>> element) {
        return Differs.ofOptionals(Function.identity(),
                        Differs.concat(
                                Differs.ofMaps(Fun.compose1(Manifest::getMainAttributes, ManifestAttribute::attributesMap)),
                                Differs.ofMapsCustomized(Manifest::getEntries, builder -> builder
                                                .emitterProjection((emit, entryName) -> emit
                                                        .ofSubKind(Kind.of("entry"))
                                                        .forChild(String.format("{entry:%s}", entryName))),
                                        Differs.ofMapValues(Differs.ofMaps(Function.identity())))
                        ))
                .diff(baseEmitter.forSubElement(element), element);
    }

}
