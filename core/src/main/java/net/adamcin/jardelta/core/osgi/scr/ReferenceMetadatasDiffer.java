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
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ReferenceMetadatasDiffer implements Differ<ReferenceMetadatas> {
    private final CompositeDiffer<ReferenceMetadata> differs = CompositeDiffer.of(builder -> {
        builder.put("@interface", (emitter, element) ->
                GenericDiffers.ofObjectEquality(emitter, element.values().map(ReferenceMetadata::getInterface)));
        builder.put("@target", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getTarget)));
        builder.put("@bind", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getBind)));
        builder.put("@unbind", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getUnbind)));
        builder.put("@cardinality", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getCardinality)));
        builder.put("@updated", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getUpdated)));
        builder.put("@field", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getField)));
        builder.put("@fieldOption", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getFieldOption)));
        builder.put("@collectionType", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getCollectionType)));
        builder.put("@policy", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getPolicy)));
        builder.put("@policyOption", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getPolicyOption)));
        builder.put("@scope", (emitter, element) ->
                GenericDiffers.ofObjectEquality(emitter, element.values().map(ReferenceMetadata::getScope)));
        builder.put("@parameter", (emitter, element) ->
                GenericDiffers.ofOptionals(emitter, element.values().mapOptional(ReferenceMetadata::getParameterIndex)));
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull ReferenceMetadatas element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return differs.diff(emitter, element);
    }
}
