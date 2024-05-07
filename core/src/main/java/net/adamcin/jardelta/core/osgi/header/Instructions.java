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

import aQute.bnd.header.Parameters;
import net.adamcin.jardelta.core.Element;
import net.adamcin.jardelta.core.Name;
import net.adamcin.jardelta.core.manifest.Manifests;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.jar.Attributes;

public class Instructions implements Element<Optional<Parameters>> {

    private final Attributes.Name attributeName;
    private final Both<Optional<Parameters>> values;

    public Instructions(@NotNull Attributes.Name attributeName,
                        @NotNull Both<Optional<Parameters>> values) {
        this.attributeName = attributeName;
        this.values = values;
    }

    @NotNull
    public Attributes.Name getAttributeName() {
        return attributeName;
    }

    @Override
    public @NotNull Name name() {
        return Manifests.NAME_MANIFEST.appendSegment(getAttributeName().toString());
    }

    @Override
    public @NotNull Both<Optional<Parameters>> both() {
        return values;
    }

    public boolean isDiff() {
        return values.left()
                .map(l -> !values.right()
                        .map(l::isEqual).orElse(false))
                .orElseGet(() -> values.right().isPresent());
    }

    @NotNull
    public Both<Optional<String>> formatted() {
        return values.map(value -> value.map(Parameters::toString));
    }

}
