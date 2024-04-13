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

import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.manifest.AttributeHandler;
import net.adamcin.jardiff.core.manifest.MFAttribute;
import net.adamcin.jardiff.core.manifest.MFAttributeDiff;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.jar.Attributes.Name;
import java.util.stream.Stream;

public class FallbackAttributeHandler implements AttributeHandler {
    public static final String DIFF_KIND = "manifest";
    public static final Differ<MFAttribute> ANY_ATTRIBUTE = diffed ->
            Stream.of(MFAttributeDiff.ofRawValue(DIFF_KIND, diffed));

    @Override
    public @Nullable Differ<MFAttribute> getDiffer(@NotNull Name attributeName) {
        return ANY_ATTRIBUTE;
    }
}
