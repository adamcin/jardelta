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
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ReferenceMetadatasDiffer implements Differ<ReferenceMetadatas> {
    private final CompositeDiffer<ReferenceMetadata> differs = CompositeDiffer.of(nextDiff -> {
        nextDiff.accept("@interface", (builder, element) ->
                GenericDiffers.ofObjectEquality(builder, element.both().map(ReferenceMetadata::getInterface)));
        nextDiff.accept("@target", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getTarget)));
        nextDiff.accept("@bind", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getBind)));
        nextDiff.accept("@unbind", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getUnbind)));
        nextDiff.accept("@cardinality", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getCardinality)));
        nextDiff.accept("@updated", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getUpdated)));
        nextDiff.accept("@field", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getField)));
        nextDiff.accept("@fieldOption", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getFieldOption)));
        nextDiff.accept("@collectionType", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getCollectionType)));
        nextDiff.accept("@policy", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getPolicy)));
        nextDiff.accept("@policyOption", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getPolicyOption)));
        nextDiff.accept("@scope", (builder, element) ->
                GenericDiffers.ofObjectEquality(builder, element.both().map(ReferenceMetadata::getScope)));
        nextDiff.accept("@parameter", (builder, element) ->
                GenericDiffers.ofOptionals(builder, element.both().mapOptional(ReferenceMetadata::getParameterIndex)));
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull ReferenceMetadatas diffed) {
        final Diff.Builder diffBuilder = Diff.builder(ScrRefinementStrategy.KIND).named(diffed.name());
        return differs.diff(diffBuilder, diffed);
    }
}
