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

import net.adamcin.jardelta.core.Name;
import net.adamcin.jardelta.core.Diffed;
import net.adamcin.jardelta.core.Differ;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;

public class MFAttribute implements Diffed<Optional<String>> {
    private final @NotNull Name name;
    private final @NotNull Differ<MFAttribute> differ;
    private final @NotNull Both<Optional<String>> values;

    public MFAttribute(@NotNull Name name,
                       @NotNull Differ<MFAttribute> differ,
                       @NotNull Both<Optional<String>> values) {
        this.name = name;
        this.differ = differ;
        this.values = values;
    }

    @Override
    public @NotNull Name getName() {
        return name;
    }

    @NotNull
    public Differ<MFAttribute> getDiffer() {
        return differ;
    }

    @Override
    @NotNull
    public Both<Optional<String>> both() {
        return values;
    }

    @NotNull
    public Both<Optional<String>> formatted() {
        return values;
    }

    public boolean isDiff() {
        return !Objects.equals(values.left(), values.right());
    }

    public static Attributes.Name nameOf(@NotNull String name) {
        return new Attributes.Name(name);
    }
}
