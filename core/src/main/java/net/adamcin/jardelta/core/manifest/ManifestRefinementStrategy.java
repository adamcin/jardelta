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

import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.Element;
import net.adamcin.jardelta.core.OpenJar;
import net.adamcin.jardelta.core.Action;
import net.adamcin.jardelta.core.Diffs;
import net.adamcin.jardelta.core.Refinement;
import net.adamcin.jardelta.core.RefinementStrategy;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ManifestRefinementStrategy implements RefinementStrategy {

    @Override
    public @NotNull Refinement refine(@NotNull Context context,
                                      @NotNull Diffs diffs,
                                      @NotNull Element<OpenJar> openJars) throws Exception {
        List<Diff> superseded = diffs.stream().filter(diff -> diff.getName().equals(Manifests.NAME_MANIFEST)
                && diff.getAction() == Action.CHANGED).collect(Collectors.toList());
        if (superseded.isEmpty()) {
            return Refinement.EMPTY;
        } else {
            return new Refinement(superseded,
                    new ManifestDiffer().diff(new Manifests(openJars.both().mapOptional(OpenJar::getManifest)))
                            .collect(Diffs.collect()));
        }
    }
}
