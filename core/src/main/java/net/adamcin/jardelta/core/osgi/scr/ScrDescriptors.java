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

import net.adamcin.jardelta.core.Diffed;
import net.adamcin.jardelta.core.Name;
import net.adamcin.streamsupport.Both;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ScrDescriptors implements Diffed<List<ComponentMetadata>> {
    private final String componentName;
    private final Both<List<ComponentMetadata>> values;

    public ScrDescriptors(@NotNull String componentName, @NotNull Both<List<ComponentMetadata>> values) {
        this.componentName = componentName;
        this.values = values;
    }

    @NotNull
    public String getComponentName() {
        return componentName;
    }

    @Override
    public @NotNull Name getName() {
        return ScrRefinementStrategy.NAME_PREFIX.append(componentName);
    }

    @Override
    public @NotNull Both<List<ComponentMetadata>> both() {
        return values;
    }
}
