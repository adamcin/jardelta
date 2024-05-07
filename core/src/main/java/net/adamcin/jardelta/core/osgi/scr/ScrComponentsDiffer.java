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

package net.adamcin.jardelta.core.osgi.scr;

import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.Differ;
import net.adamcin.jardelta.core.util.CompositeDiffer;
import net.adamcin.jardelta.core.util.GenericDiffers;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScrComponentsDiffer implements Differ<ScrComponents> {

    private final ReferenceMetadatasDiffer referenceMetadatasDiffer = new ReferenceMetadatasDiffer();
    private final CompositeDiffer<ComponentMetadata> differs = CompositeDiffer.of(nextDiff -> {
        nextDiff.accept("@activate", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ComponentMetadata::getActivate)));
        nextDiff.accept("@modified", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ComponentMetadata::getModified)));
        nextDiff.accept("@deactivate", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ComponentMetadata::getDeactivate)));
        nextDiff.accept("@factoryIdentifier", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ComponentMetadata::getFactoryIdentifier)));
        nextDiff.accept("@configurationPolicy", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ComponentMetadata::getConfigurationPolicy)));
        nextDiff.accept("@dsVersion", (builder, element) ->
                GenericDiffers.ofObjectEquality(builder, element.both().map(ComponentMetadata::getDSVersion)));
        nextDiff.accept("@serviceMetadata", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ComponentMetadata::getServiceMetadata), metas ->
                        Stream.concat(
                                GenericDiffers.ofObjectEquality(builder.child("@scope"), metas.map(ServiceMetadata::getScope)),
                                GenericDiffers.ofAllInEitherSet(builder.child("@provides")::child,
                                        metas.map(ServiceMetadata::getProvides).map(List::of))
                        )));
        nextDiff.accept("@properties", (builder, element) ->
                GenericDiffers.ofAllInEitherMap(builder::child, element.both().map(ComponentMetadata::getProperties),
                        (key, props) -> GenericDiffers.ofOptionals(builder.child(key), props)));
        nextDiff.accept("@factoryProperties", (builder, element) ->
                GenericDiffers.ofAllInEitherMap(builder::child, element.both().map(ComponentMetadata::getFactoryProperties),
                        (key, props) -> GenericDiffers.ofOptionals(builder.child(key), props)));
        nextDiff.accept("@references", (builder, element) ->
                GenericDiffers.ofAllInEitherMap(builder::child, element.both()
                                .map(ComponentMetadata::getDependencies)
                                .map(references -> references.stream().collect(Collectors.groupingBy(ReferenceMetadata::getName))),
                        (key, optMetas) -> GenericDiffers.ofOptionals(builder.child(key), optMetas,
                                metas -> GenericDiffers.ofAtMostOne(builder.child(key), metas,
                                        singles -> referenceMetadatasDiffer.diff(new ReferenceMetadatas(Objects.requireNonNull(builder.child(key).name()), singles)))))
        );
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull ScrComponents diffed) {
        final Diff.Builder diffBuilder = Diff.builder(ScrRefinementStrategy.KIND).named(diffed.name());
        return differs.diff(diffBuilder, diffed);
    }
}
