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

package net.adamcin.jardiff.core.manifest;

import net.adamcin.jardiff.core.Action;
import net.adamcin.jardiff.core.Context;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Diffs;
import net.adamcin.jardiff.core.RefinedDiff;
import net.adamcin.jardiff.core.RefinementStrategy;
import net.adamcin.jardiff.core.URLJar;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ManifestRefinementStrategy implements RefinementStrategy {

    @Override
    public @NotNull RefinedDiff refine(@NotNull Context context, @NotNull Diffs diffs) {
        List<Diff> superseded = diffs.stream().filter(diff -> diff.getName().equals(Manifests.NAME_MANIFEST)
                && diff.getAction() == Action.CHANGED).collect(Collectors.toList());
        if (superseded.isEmpty()) {
            return RefinedDiff.EMPTY;
        } else {
            return new RefinedDiff(superseded,
                    new ManifestDiffer().diff(new Manifests(context.getJars().both().mapOptional(URLJar::getManifest)))
                            .collect(Diffs.collect()));
        }
    }
}
