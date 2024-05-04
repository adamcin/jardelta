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

import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.Map;
import java.util.Set;

public class MetaTypeDesignate {
    private final String pid;
    private final boolean isFactory;
    private final Set<String> locales;
    private final Map<String, ObjectClassDefinition> objectClassDefinitions;

    public MetaTypeDesignate(@NotNull String pid,
                             boolean isFactory,
                             @NotNull Set<String> locales,
                             @NotNull Map<String, ObjectClassDefinition> objectClassDefinitions) {
        this.pid = pid;
        this.isFactory = isFactory;
        this.locales = Set.copyOf(locales);
        this.objectClassDefinitions = objectClassDefinitions;
    }

    @NotNull
    public String getPid() {
        return pid;
    }

    public boolean isFactory() {
        return isFactory;
    }

    @NotNull
    public Set<String> getLocales() {
        return locales;
    }

    @NotNull
    public Map<String, ObjectClassDefinition> getObjectClassDefinitions() {
        return objectClassDefinitions;
    }

}
