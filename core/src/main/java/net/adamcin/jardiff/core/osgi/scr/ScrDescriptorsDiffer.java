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

import net.adamcin.jardiff.core.Action;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Differ;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public class ScrDescriptorsDiffer implements Differ<ScrDescriptors> {

    @Override
    public @NotNull Stream<Diff> diff(@NotNull ScrDescriptors diffed) {
        final Diff.Builder diff = Diff.builder(ScrRefinementStrategy.KIND).named(diffed.getName());
        Result<Both<Optional<ComponentMetadata>>> singledResult = Both.ofResults(diffed.both().map(list -> {
            if (list.isEmpty()) {
                return Result.success(Optional.empty());
            } else if (list.size() == 1) {
                return Result.success(Optional.ofNullable(list.get(0)));
            } else {
                return Result.failure("SCR Component has more than one mapping: " + diffed.getComponentName());
            }
        }));

        if (singledResult.isFailure()) {
            return Stream.of(diff.build(Action.ERR_RIGHT));
        } else {
            Both<Optional<ComponentMetadata>> singled = singledResult.getOrThrow();
            if (singled.left().isEmpty()) {
                if (singled.right().isPresent()) {
                    return Stream.of(diff.added());
                }
            } else if (singled.right().isEmpty()) {
                return Stream.of(diff.removed());
            } else {
                ScrComponents components = new ScrComponents(diffed.getComponentName(), singled.map(Optional::get));

            }

            return Stream.empty();
        }
    }
}
