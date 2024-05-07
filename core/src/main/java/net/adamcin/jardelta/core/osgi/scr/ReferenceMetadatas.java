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

import net.adamcin.jardelta.core.Element;
import net.adamcin.jardelta.core.Name;
import net.adamcin.streamsupport.Both;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.jetbrains.annotations.NotNull;

public class ReferenceMetadatas implements Element<ReferenceMetadata> {
    private final Name name;
    private final Both<ReferenceMetadata> values;

    public ReferenceMetadatas(@NotNull Name name, @NotNull Both<ReferenceMetadata> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public @NotNull Name name() {
        return name;
    }

    @Override
    public @NotNull Both<ReferenceMetadata> both() {
        return values;
    }
}
