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

import net.adamcin.jardelta.core.osgi.header.HeaderRefinementStrategy;
import net.adamcin.jardelta.core.osgi.ocd.MetaTypeRefinementStrategy;
import net.adamcin.jardelta.core.manifest.ManifestRefinementStrategy;
import net.adamcin.jardelta.core.mavenmeta.MavenMetaRefinementStrategy;
import net.adamcin.jardelta.core.osgi.scr.ScrRefinementStrategy;
import org.jetbrains.annotations.NotNull;

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
    public Diffs execute(@NotNull Context context) {
        final JarDiffer differ = new JarDiffer(Optional.ofNullable(context.getSettings())
                .orElse(Settings.DEFAULT_SETTINGS));

        Diffs diffs = differ.diff(context.getJars()).collect(Diffs.collect());
        for (RefinementStrategy strategy : refinementStrategies) {
            diffs = diffs.refinedBy(strategy.refine(context, diffs));
        }
        return diffs;
    }
}
