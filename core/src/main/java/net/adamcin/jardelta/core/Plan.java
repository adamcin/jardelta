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

import net.adamcin.jardelta.core.manifest.ManifestRefinementStrategy;
import net.adamcin.jardelta.core.mavenmeta.MavenMetaRefinementStrategy;
import net.adamcin.jardelta.core.osgi.header.HeaderRefinementStrategy;
import net.adamcin.jardelta.core.osgi.ocd.MetaTypeRefinementStrategy;
import net.adamcin.jardelta.core.osgi.scr.ScrRefinementStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Plan {

    private List<RefinementStrategy> refinementStrategies = List.of(
            new ManifestRefinementStrategy(),
            new HeaderRefinementStrategy(),
            new ScrRefinementStrategy(),
            new MetaTypeRefinementStrategy(),
            new MavenMetaRefinementStrategy());

    @NotNull
    public Delta execute(@NotNull Context context) {
        final JarDiffer differ = new JarDiffer(Optional.ofNullable(context.getSettings())
                .orElse(Settings.DEFAULT_SETTINGS));

        final Delta.DeltaBuilder deltaBuilder = new Delta.DeltaBuilder();

        Diffs diffs = context.getJars().using(openJars -> differ.diff(openJars).collect(Diffs.collect())).getOrThrow();
        deltaBuilder.initial(diffs);
        final List<Refinement> refinements = new ArrayList<>();
        for (RefinementStrategy strategy : refinementStrategies) {
            final Diffs toRefine = diffs;
            Refinement refinement = context.getJars().using(openJars -> strategy.refine(context, toRefine, openJars))
                    .getOrThrow();
            diffs = diffs.refinedBy(refinement);
            refinements.add(refinement);
        }
        deltaBuilder.refinements(List.copyOf(refinements));
        deltaBuilder.results(diffs);
        return deltaBuilder.build();
    }
}
