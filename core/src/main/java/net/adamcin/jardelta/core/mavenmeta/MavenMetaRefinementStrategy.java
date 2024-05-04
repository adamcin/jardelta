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

package net.adamcin.jardelta.core.mavenmeta;

import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Name;
import net.adamcin.jardelta.core.entry.JarEntryDiffer;
import net.adamcin.jardelta.core.Diffs;
import net.adamcin.jardelta.core.RefinedDiff;
import net.adamcin.jardelta.core.RefinementStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * Remove entry diffs related to maven metadata.
 */
public class MavenMetaRefinementStrategy implements RefinementStrategy {
    public static final Name NAME_PREFIX = Name.of("META-INF/maven");

    @Override
    public @NotNull RefinedDiff refine(@NotNull Context context, @NotNull Diffs diffs) {
        return new RefinedDiff(diffs.stream().filter(diff -> JarEntryDiffer.DIFF_KIND.equals(diff.getKind())
                        && diff.getName().startsWith(NAME_PREFIX)
                        && (diff.getName().endsWith(Name.of("pom.xml"))
                        || diff.getName().endsWith(Name.of("pom.properties"))))
                .collect(Collectors.toList()), Diffs.EMPTY);
    }
}
