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

package net.adamcin.jardiff.core.osgi.scr;

import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Differ;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class ScrComponentsDiffer implements Differ<ScrComponents> {
    @Override
    public @NotNull Stream<Diff> diff(@NotNull ScrComponents diffed) {
        final Diff.Builder diffBuilder = Diff.builder(ScrRefinementStrategy.KIND).named(diffed.getName());
//        final Stream<Diff> ocdElementDiffs = Stream.of(
//                Fun.toEntry("@activate", diffed.both().mapOptional(ComponentMetadata::getActivate)),
//                Fun.toEntry("@deactivate", diffed.both().mapOptional(ComponentMetadata::getDeactivate)),
//                Fun.toEntry("@configurationPid", diffed.both().mapOptional(ComponentMetadata::getConfigurationPid)),
//                Fun.toEntry("@configurationPolicy", diffed.both().mapOptional(ComponentMetadata::getConfigurationPolicy))
//        ).flatMap(Fun.mapEntry((key, values) -> diffElements()));

        return Stream.empty();
    }

    Stream<Diff> diffElements() {
        return Stream.empty();
    }
}
