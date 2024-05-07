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

package net.adamcin.jardelta.core.util;

import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.Element;
import net.adamcin.jardelta.core.Name;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public final class CompositeDiffer<T> {
    private final Map<String, BiFunction<Diff.Builder, Element<T>, Stream<Diff>>> differs;

    private CompositeDiffer(@NotNull Map<String, BiFunction<Diff.Builder, Element<T>, Stream<Diff>>> differs) {
        this.differs = differs;
    }

    @NotNull
    public Stream<Diff> diff(@NotNull Diff.Builder diffBuilder, @NotNull Element<T> values) {
        final Diff.Builder ourBuilder = diffBuilder.name().isRoot()
                ? diffBuilder.child("").named(values.name())
                : diffBuilder;
        return differs.entrySet().stream()
                .flatMap(Fun.mapEntry((key, func) -> func.apply(ourBuilder.child(key), values)));
    }

    @NotNull
    public static <T> CompositeDiffer<T> of(@NotNull Consumer<BiConsumer<String, BiFunction<Diff.Builder, Element<T>, Stream<Diff>>>> nextDiff) {
        Map<String, BiFunction<Diff.Builder, Element<T>, Stream<Diff>>> differs = new TreeMap<>();
        nextDiff.accept(differs::put);
        return new CompositeDiffer<>(differs);
    }

    @NotNull
    public static <T, U> Map.Entry<Diff.Builder, Element<U>> projectChild(@NotNull Diff.Builder parentBuilder,
                                                                          @NotNull Element<T> element,
                                                                          @NotNull String childName,
                                                                          @NotNull Both<U> newValues) {
        final Name segment = Name.ofSegment(childName);
        return Fun.toEntry(parentBuilder.child(childName), element.project(segment, newValues));
    }

    @NotNull
    public static <T, U> Map.Entry<Diff.Builder, Element<U>> projectChild(@NotNull Diff.Builder parentBuilder,
                                                                          @NotNull Element<T> element,
                                                                          @NotNull String childName,
                                                                          @NotNull Function<? super T, ? extends U> mapperFn) {
        final Both<U> newValues = element.both().map(mapperFn::apply);
        return projectChild(parentBuilder, element, childName, newValues);
    }
}
