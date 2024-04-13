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

package net.adamcin.jardiff.core.entry;

import net.adamcin.jardiff.core.Diffed;
import net.adamcin.jardiff.core.Name;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class JarEntry implements Diffed<Optional<Result<JarEntryMetadata>>> {

    private final Name name;
    private final Both<Optional<Result<JarEntryMetadata>>> values;

    public JarEntry(@NotNull Name name,
                    @NotNull Both<Optional<Result<JarEntryMetadata>>> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public @NotNull Name getName() {
        return name;
    }

    @Override
    public @NotNull Both<Optional<Result<JarEntryMetadata>>> both() {
        return values;
    }
}
