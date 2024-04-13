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

package net.adamcin.jardiff.core;

import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

public class Jars implements Diffed<URLJar> {
    private final Name name;
    private final Both<URLJar> values;

    public Jars(@NotNull Both<URLJar> values) {
        this(Name.of(""), values);
    }

    public Jars(@NotNull Name name, @NotNull Both<URLJar> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public @NotNull Name getName() {
        return name;
    }

    @Override
    public @NotNull Both<URLJar> both() {
        return values;
    }

    public boolean isBothBundles() {
        return both().map(URLJar::isBundle).testBoth((left, right) -> left && right);
    }

    public static Result<Jars> of(@NotNull URL left, @NotNull URL right) {
        return Both.ofResults(Both.of(left, right).map(URLJar::fromURL)).map(Jars::new);
    }
}
