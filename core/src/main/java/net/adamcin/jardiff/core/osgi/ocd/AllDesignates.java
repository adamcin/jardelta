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

package net.adamcin.jardiff.core.osgi.ocd;

import net.adamcin.jardiff.core.Diffed;
import net.adamcin.jardiff.core.Name;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllDesignates implements Diffed<Map<String, List<MetaTypeDesignate>>> {
    private final Set<String> pids;
    private final Both<Map<String, List<MetaTypeDesignate>>> values;

    public AllDesignates(@NotNull Both<List<JarMetaTypeProvider>> values) {
        Both<Map<String, List<MetaTypeDesignate>>> bothDesignates = values
                .map(providers -> providers.stream()
                        .flatMap(JarMetaTypeProvider::streamDesignates)
                        .collect(Collectors.toMap(MetaTypeDesignate::getPid, List::of, (left, right) -> {
                            List<MetaTypeDesignate> acc = new ArrayList<>(left);
                            acc.addAll(right);
                            return acc;
                        })));
        this.pids = bothDesignates.map(Map::keySet).stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        this.values = bothDesignates;
    }

    @Override
    public @NotNull Name getName() {
        return MetaTypeRefinementStrategy.NAME_PREFIX;
    }

    public Set<String> getPids() {
        return pids;
    }

    @Override
    public @NotNull Both<Map<String, List<MetaTypeDesignate>>> both() {
        return values;
    }

    @NotNull
    public PidDesignates bothDesignatesForPid(@NotNull final String pid) {
        return new PidDesignates(pid, both().map(map -> map.getOrDefault(pid, Collections.emptyList())));
    }

    @NotNull
    public Stream<PidDesignates> stream() {
        return getPids().stream().map(this::bothDesignatesForPid);
    }
}
