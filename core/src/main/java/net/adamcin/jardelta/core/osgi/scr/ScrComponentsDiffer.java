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

import net.adamcin.jardelta.api.diff.CompositeDiffer;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Fun;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.jardelta.api.diff.Differs.ofAtMostOne;
import static net.adamcin.jardelta.api.diff.Differs.ofEquality;
import static net.adamcin.jardelta.api.diff.Differs.ofMaps;
import static net.adamcin.jardelta.api.diff.Differs.ofNullables;
import static net.adamcin.jardelta.api.diff.Differs.ofSets;

public class ScrComponentsDiffer implements Differ<ComponentMetadata> {

    private final CompositeDiffer<ComponentMetadata> differs = CompositeDiffer.of(builder -> {
        builder.put("@activate", ofNullables(ComponentMetadata::getActivate));
        builder.put("@modified", ofNullables(ComponentMetadata::getModified));
        builder.put("@deactivate", ofNullables(ComponentMetadata::getDeactivate));
        builder.put("@factoryIdentifier", ofNullables(ComponentMetadata::getFactoryIdentifier));
        builder.put("@configurationPolicy", ofNullables(ComponentMetadata::getConfigurationPolicy));
        builder.put("@dsVersion", ofEquality(ComponentMetadata::getDSVersion));
        builder.put("@serviceMetadata", ofNullables(ComponentMetadata::getServiceMetadata,
                CompositeDiffer.of(smBuilder -> {
                    smBuilder.put("@scope", ofEquality(ServiceMetadata::getScope));
                    smBuilder.put("@provides", ofSets(Fun.compose1(ServiceMetadata::getProvides, List::of)));
                })));
        builder.put("@properties", ofMaps(ComponentMetadata::getProperties));
        builder.put("@factoryProperties", ofMaps(ComponentMetadata::getFactoryProperties));
        builder.put("@references", ofMaps(Fun.compose1(ComponentMetadata::getDependencies,
                references -> references.stream()
                        .collect(Collectors.groupingBy(ReferenceMetadata::getName))), ofAtMostOne(Map.Entry::getValue,
                CompositeDiffer.of(rmBuilder -> {
                    rmBuilder.put("@interface", ofEquality(ReferenceMetadata::getInterface));
                    rmBuilder.put("@target", ofNullables(ReferenceMetadata::getTarget));
                    rmBuilder.put("@bind", ofNullables(ReferenceMetadata::getBind));
                    rmBuilder.put("@unbind", ofNullables(ReferenceMetadata::getUnbind));
                    rmBuilder.put("@cardinality", ofNullables(ReferenceMetadata::getCardinality));
                    rmBuilder.put("@updated", ofNullables(ReferenceMetadata::getUpdated));
                    rmBuilder.put("@field", ofNullables(ReferenceMetadata::getField));
                    rmBuilder.put("@fieldOption", ofNullables(ReferenceMetadata::getFieldOption));
                    rmBuilder.put("@collectionType", ofNullables(ReferenceMetadata::getCollectionType));
                    rmBuilder.put("@policy", ofNullables(ReferenceMetadata::getPolicy));
                    rmBuilder.put("@policyOption", ofNullables(ReferenceMetadata::getPolicyOption));
                    rmBuilder.put("@scope", ofEquality(ReferenceMetadata::getScope));
                    rmBuilder.put("@parameter", ofNullables(ReferenceMetadata::getParameterIndex));
                }))));
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<ComponentMetadata> element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return differs.diff(emitter, element);
    }

}
