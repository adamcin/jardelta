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
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManifestDiffer implements Differ<Manifests> {
    public static final Kind DIFF_KIND = Kind.of("manifest");

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Manifests element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return GenericDiffers.ofOptionals(emitter, element.values(), values -> Stream.concat(
                diffAttributes(emitter, values.map(Manifest::getMainAttributes)),
                GenericDiffers.ofAllInEitherMap(entryName -> emitter
                                .ofSubKind(Kind.of("entry"))
                                .forChild(String.format("{entry:%s}", entryName)), values.map(Manifest::getEntries),
                        (childEmitter, optAttrs) ->
                                GenericDiffers.ofOptionals(childEmitter, optAttrs,
                                        bothAttrs -> diffAttributes(childEmitter, bothAttrs)))));
    }

    @NotNull
    Stream<Diff> diffAttributes(@NotNull Emitter baseEmitter, @NotNull Both<Attributes> bothAttributes) {
        final Set<Attributes.Name> allNames = bothAttributes
                .stream()
                .flatMap(Fun.compose1(Attributes::keySet, Set::stream))
                .filter(Attributes.Name.class::isInstance)
                .map(Attributes.Name.class::cast)
                .collect(Collectors.toSet());

        return allNames.stream()
                .flatMap(name -> {
                    ManifestAttribute diffed = new ManifestAttribute(
                            Manifests.NAME_MANIFEST.appendSegment(name.toString()),
                            bothAttributes.mapOptional(attributes -> attributes.getValue(name)));
                    return GenericDiffers.ofOptionals(baseEmitter.forSubElement(diffed), diffed.values());
                });
    }

}
