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

package net.adamcin.jardelta.core;

import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.jar.EntryMeta;
import net.adamcin.jardelta.api.jar.OpenJar;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import net.adamcin.streamsupport.throwing.ThrowingFunction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public final class Jars {
    private final Both<String> names;
    private final Both<Path> values;
    private final Both<Map<Name, Result<EntryMeta>>> resourceCaches = Both.of(new TreeMap<>(), new TreeMap<>());

    public Jars(@NotNull Both<Path> values) {
        this(values.map(Path::toString), values);
    }

    public Jars(@NotNull Both<String> names, @NotNull Both<Path> values) {
        this.names = names;
        this.values = values;
    }

    public <T> Result<T> openThen(@NotNull ThrowingFunction<Element<OpenJar>, ? extends T> usingFn) {
        return Fun.result0(() -> {
            try (OpenJarImpl leftJar = OpenJarImpl.fromFile(names.left(), values.left(), resourceCaches.left());
                 OpenJarImpl rightJar = OpenJarImpl.fromFile(names.right(), values.right(), resourceCaches.right())) {
                final Both<OpenJar> openJars = Both.of(leftJar, rightJar);
                return (T) usingFn.tryApply(new Element<>() {
                    @Override
                    public @NotNull Name name() {
                        return Name.ROOT;
                    }

                    @Override
                    public @NotNull Both<OpenJar> values() {
                        return openJars;
                    }
                });
            }
        }).get();
    }

    public @NotNull Both<Path> both() {
        return values;
    }

    public static Jars from(@NotNull URL left, @NotNull URL right) {
        return new Jars(Both.of(left, right).map(Fun.uncheck1(URL::toURI)).map(Paths::get));
    }

    public static Jars from(@NotNull Path left, @NotNull Path right) {
        return new Jars(Both.of(left, right));
    }

    public static Jars from(@NotNull File left, @NotNull File right) {
        return new Jars(Both.of(left, right).map(File::toPath));
    }

}
