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

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.diff.Action;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.jar.OpenJar;
import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Refinement;
import net.adamcin.jardelta.core.RefinementStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static net.adamcin.jardelta.core.manifest.ManifestDiffer.DIFF_KIND;

public class ManifestRefinementStrategy implements RefinementStrategy {

    @Override
    public @NotNull Kind getKind() {
        return DIFF_KIND;
    }

    @Override
    public @NotNull Refinement refine(@NotNull Context context,
                                      @NotNull Diffs diffs,
                                      @NotNull Element<OpenJar> openJars) {
        List<Diff> superseded = diffs
                .withExactName(Manifests.NAME_MANIFEST)
                .withActions(Action.CHANGED)
                .stream().collect(Collectors.toList());
        if (superseded.isEmpty()) {
            return Refinement.EMPTY;
        } else {
            return new Refinement(superseded,
                    new ManifestDiffer().diff(Diff.emitterOf(DIFF_KIND),
                                    new Manifests(openJars.values().mapOptional(OpenJar::getManifest)))
                            .collect(Diffs.collector()));
        }
    }
}
