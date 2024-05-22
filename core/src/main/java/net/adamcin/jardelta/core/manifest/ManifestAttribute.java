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
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManifestAttribute implements Element<Optional<String>> {
    private final @NotNull Name name;
    private final @NotNull Both<Optional<String>> values;

    public ManifestAttribute(@NotNull Name name,
                             @NotNull Both<Optional<String>> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public @NotNull Name name() {
        return name;
    }

    @Override
    @NotNull
    public Both<Optional<String>> values() {
        return values;
    }

    public boolean isDiff() {
        return !Objects.equals(values.left(), values.right());
    }

    public static Attributes.Name nameOf(@NotNull String name) {
        return new Attributes.Name(name);
    }

    public static Attributes attributeSet(@NotNull String... names) {
        return Stream.of(names).map(ManifestAttribute::nameOf).collect(
                Collectors.toMap(
                        Object.class::cast,
                        Fun.compose1(Fun.infer1(Attributes.Name::toString), Object.class::cast),
                        (left, right) -> left,
                        Attributes::new));
    }

    public static Predicate<String> inAttributeSet(@NotNull Attributes attributeSet) {
        return name -> attributeSet.containsKey(nameOf(name));
    }

    public static Map<Attributes.Name, String> attributesMap(@NotNull Attributes attributes) {
        return attributes.entrySet().stream()
                .filter(Fun.testKey(Attributes.Name.class::isInstance))
                .map(Fun.mapKey(Attributes.Name.class::cast))
                .map(Fun.mapValue(Object::toString))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, HashMap::new));
    }
}
