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

package net.adamcin.jardelta.testing.coreats;

import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

public enum ExampleJars {
    BUNDLE_SCR("example-bundle-scr.jar"),
    BUNDLE_DS("example-bundle-ds.jar"),
    BND_DS("example-bnd-ds.jar");

    private final Result<URL> jarUrl;

    ExampleJars(@NotNull String baseName) {
        jarUrl = Fun.result0(() -> {
            URL maybeUrl = ExampleJars.class.getClassLoader().getResource(baseName);
            if (maybeUrl == null) {
                throw new NullPointerException("Example jar not found: " + baseName);
            }
            return maybeUrl;
        }).get();
    }

    @NotNull
    public URL getJarUrl() {
        return jarUrl.getOrThrow();
    }
}
