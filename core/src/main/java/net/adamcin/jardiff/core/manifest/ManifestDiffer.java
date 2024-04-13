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

package net.adamcin.jardiff.core.manifest;

import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManifestDiffer implements Differ<Manifests> {
    public static final String DIFF_KIND = "manifest";

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Manifests diffed) {
        final Diff.Builder diffBuilder = Diff.builder(DIFF_KIND).named(diffed.getName());
        return GenericDiffers.diffOptionals(diffBuilder, diffed.both(), values ->
                diffMainAttributes(values.map(Manifest::getMainAttributes)));
    }

    @NotNull
    Stream<Diff> diffMainAttributes(@NotNull Both<Attributes> bothAttributes) {
        final Set<Attributes.Name> allNames = bothAttributes
                .stream()
                .flatMap(Fun.compose1(Attributes::keySet, Set::stream))
                .filter(Attributes.Name.class::isInstance)
                .map(Attributes.Name.class::cast)
                .collect(Collectors.toSet());

        return allNames.stream()
                .map(name -> new MFAttribute(Manifests.NAME_MANIFEST.append(name.toString()),
                        FallbackAttributeHandler.ANY_ATTRIBUTE,
                        bothAttributes.mapOptional(attributes -> attributes.getValue(name))))
                .filter(MFAttribute::isDiff)
                .flatMap(diffable -> diffable.getDiffer().diff(diffable));
    }

}
