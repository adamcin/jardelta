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
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.util.CompositeDiffer;
import net.adamcin.streamsupport.Fun;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScrComponentsDiffer implements Differ<Element<ComponentMetadata>> {

    private final ReferenceMetadatasDiffer referenceMetadatasDiffer = new ReferenceMetadatasDiffer();

    private final CompositeDiffer<ComponentMetadata> differs = CompositeDiffer.of(builder -> {
        builder.put("@activate", Differs.ofNullables(ComponentMetadata::getActivate));
        builder.put("@modified", Differs.ofNullables(ComponentMetadata::getModified));
        builder.put("@deactivate", Differs.ofNullables(ComponentMetadata::getDeactivate));
        builder.put("@factoryIdentifier", Differs.ofNullables(ComponentMetadata::getFactoryIdentifier));
        builder.put("@configurationPolicy", Differs.ofNullables(ComponentMetadata::getConfigurationPolicy));
        builder.put("@dsVersion", Differs.ofEquality(ComponentMetadata::getDSVersion));
        builder.put("@serviceMetadata", Differs.ofNullables(ComponentMetadata::getServiceMetadata,
                CompositeDiffer.of(smBuilder -> {
                    smBuilder.put("@scope", Differs.ofEquality(ServiceMetadata::getScope));
                    smBuilder.put("@provides", Differs.ofSets(Fun.compose1(ServiceMetadata::getProvides, List::of)));
                })));
        builder.put("@properties", Differs.ofMaps(ComponentMetadata::getProperties));
        builder.put("@factoryProperties", Differs.ofMaps(ComponentMetadata::getFactoryProperties));
        builder.put("@references", Differs.ofMaps(Fun.compose1(
                        ComponentMetadata::getDependencies,
                        references -> references.stream().collect(Collectors.groupingBy(ReferenceMetadata::getName))),
                Differs.ofAtMostOne(Map.Entry::getValue, referenceMetadatasDiffer))
        );
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<ComponentMetadata> element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return differs.diff(emitter, element);
    }

}
