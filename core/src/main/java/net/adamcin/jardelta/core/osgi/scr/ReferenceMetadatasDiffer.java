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
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ReferenceMetadatasDiffer implements Differ<Element<ReferenceMetadata>> {
    private final CompositeDiffer<ReferenceMetadata> differs = CompositeDiffer.of(builder -> {
        builder.put("@interface", Differs.ofEquality(ReferenceMetadata::getInterface));
        builder.put("@target", Differs.ofNullables(ReferenceMetadata::getTarget));
        builder.put("@bind", Differs.ofNullables(ReferenceMetadata::getBind));
        builder.put("@unbind", Differs.ofNullables(ReferenceMetadata::getUnbind));
        builder.put("@cardinality", Differs.ofNullables(ReferenceMetadata::getCardinality));
        builder.put("@updated", Differs.ofNullables(ReferenceMetadata::getUpdated));
        builder.put("@field", Differs.ofNullables(ReferenceMetadata::getField));
        builder.put("@fieldOption", Differs.ofNullables(ReferenceMetadata::getFieldOption));
        builder.put("@collectionType", Differs.ofNullables(ReferenceMetadata::getCollectionType));
        builder.put("@policy", Differs.ofNullables(ReferenceMetadata::getPolicy));
        builder.put("@policyOption", Differs.ofNullables(ReferenceMetadata::getPolicyOption));
        builder.put("@scope", Differs.ofEquality(ReferenceMetadata::getScope));
        builder.put("@parameter", Differs.ofNullables(ReferenceMetadata::getParameterIndex));
    });

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<ReferenceMetadata> element) {
        final Emitter emitter = baseEmitter.forSubElement(element);
        return differs.diff(emitter, element);
    }
}
