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

package net.adamcin.jardelta.core.osgi.header;

import net.adamcin.jardelta.core.Element;
import net.adamcin.jardelta.core.Name;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Parameter implements Element<Optional<ParameterList>> {

    private final Name parentName;
    private final String key;
    private final Both<Optional<ParameterList>> values;

    public Parameter(@NotNull Name parentName,
                     @NotNull String key,
                     @NotNull Both<Optional<ParameterList>> values) {
        this.parentName = parentName;
        this.key = key;
        this.values = values;
    }

    @Override
    public @NotNull Name name() {
        return parentName.appendSegment(key);
    }

    @Override
    public @NotNull Both<Optional<ParameterList>> both() {
        return values;
    }

    public boolean isDiff() {
        return values.left()
                .map(l -> !values.right()
                        .map(l::isEqual).orElse(false))
                .orElseGet(() -> values.right().isPresent());
    }
}
