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

import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Emitter;
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
    private final CompositeDiffer<ComponentMetadata> differs = CompositeDiffer.of(builder -> {
        builder.put("@activate", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ComponentMetadata::getActivate)));
        builder.put("@modified", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ComponentMetadata::getModified)));
        builder.put("@deactivate", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ComponentMetadata::getDeactivate)));
        builder.put("@factoryIdentifier", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ComponentMetadata::getFactoryIdentifier)));
        builder.put("@configurationPolicy", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ComponentMetadata::getConfigurationPolicy)));
        builder.put("@dsVersion", (emitter, element) ->
                GenericDiffers.ofObjectEquality(emitter, element.values().map(ComponentMetadata::getDSVersion)));
        builder.put("@serviceMetadata", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ComponentMetadata::getServiceMetadata), metas ->
                        Stream.concat(
                                GenericDiffers.ofObjectEquality(emitter.forChild("@scope"), metas.map(ServiceMetadata::getScope)),
                                GenericDiffers.ofAllInEitherSet(emitter.forChild("@provides")::forChild,
                                        metas.map(ServiceMetadata::getProvides).map(List::of))
                        )));
        builder.put("@properties", (emitter, element) ->
                GenericDiffers.ofAllInEitherMap(emitter::forChild, element.values().map(ComponentMetadata::getProperties),
                        GenericDiffers::ofOptionals));
        builder.put("@factoryProperties", (emitter, element) ->
                GenericDiffers.ofAllInEitherMap(emitter::forChild, element.values().map(ComponentMetadata::getFactoryProperties),
                        GenericDiffers::ofOptionals));
        builder.put("@references", (emitter, element) ->
                GenericDiffers.ofAllInEitherMap(emitter::forChild, element.values()
                                .map(ComponentMetadata::getDependencies)
                                .map(references -> references.stream().collect(Collectors.groupingBy(ReferenceMetadata::getName))),
                        (childEmitter, optMetas) -> GenericDiffers.ofOptionals(childEmitter, optMetas,
                                metas -> GenericDiffers.ofAtMostOne(childEmitter, metas,
                                        singles -> referenceMetadatasDiffer.diff(childEmitter,
                                                new ReferenceMetadatas(Objects.requireNonNull(childEmitter.getName()), singles)))))
        );
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull ScrComponents element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return differs.diff(emitter, element);
    }
}
