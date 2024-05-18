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

package net.adamcin.jardelta.core.manifest;

import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Manifests implements Element<Optional<Manifest>> {
    public static final Name NAME_MANIFEST = Name.of(JarFile.MANIFEST_NAME);

    private final Both<Optional<Manifest>> values;

    public Manifests(@NotNull Both<Optional<Manifest>> values) {
        this.values = values;
    }

    @Override
    public @NotNull Name name() {
        return NAME_MANIFEST;
    }

    @Override
    public @NotNull Both<Optional<Manifest>> values() {
        return values;
    }
}
