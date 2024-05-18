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

package net.adamcin.jardelta.core.osgi.ocd;

import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.Name;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.ObjectClassDefinition;

public class MetaTypeOCD implements Element<ObjectClassDefinition> {
    private final Name parentName;
    private final Both<ObjectClassDefinition> values;

    public MetaTypeOCD(@NotNull Name parentName, @NotNull Both<ObjectClassDefinition> values) {
        this.parentName = parentName;
        this.values = values;
    }

    @Override
    public @NotNull Name name() {
        return parentName;
    }

    @Override
    public @NotNull Both<ObjectClassDefinition> values() {
        return values;
    }
}
