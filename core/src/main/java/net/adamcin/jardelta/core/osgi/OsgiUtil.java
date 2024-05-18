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

package net.adamcin.jardelta.core.osgi;

import net.adamcin.jardelta.api.jar.OpenJar;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;

import java.util.Optional;

public class OsgiUtil {

    public static Optional<Both<Bundle>> requireBothBundles(@NotNull Both<OpenJar> openJars) {
        Both<Optional<Bundle>> bundleAdapters = openJars.mapOptional(jar -> jar.adaptTo(Bundle.class));
        if (bundleAdapters.testBoth((left, right) -> left.isEmpty() || right.isEmpty())) {
            return Optional.empty();
        }

        return Optional.of(bundleAdapters.map(Optional::get));
    }
}
