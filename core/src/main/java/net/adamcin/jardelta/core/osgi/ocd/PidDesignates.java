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

package net.adamcin.jardelta.core.osgi.ocd;

import net.adamcin.jardelta.core.Diffed;
import net.adamcin.jardelta.core.Name;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class PidDesignates implements Diffed<List<MetaTypeDesignate>> {
    private final String pid;
    private final Both<List<MetaTypeDesignate>> values;

    public PidDesignates(@NotNull String pid, @NotNull Both<List<MetaTypeDesignate>> values) {
        this.pid = pid;
        this.values = values;
    }

    @NotNull
    public String getPid() {
        return pid;
    }

    @Override
    public @NotNull Name getName() {
        return MetaTypeRefinementStrategy.NAME_PREFIX.append(pid);
    }

    @Override
    public @NotNull Both<List<MetaTypeDesignate>> both() {
        return values;
    }

    @NotNull
    public Stream<MetaTypeOCD> ocds(final @NotNull Both<MetaTypeDesignate> designates, final @NotNull String locale) {
        final Both<Optional<ObjectClassDefinition>> ocds =
                designates.mapOptional(designate -> designate.getObjectClassDefinitions().get(locale));
        return ocds.left().flatMap(left -> ocds.right().map(right ->
                        new MetaTypeOCD(getName().append(localeName(locale)), Both.of(left, right))))
                .stream();
    }

    @NotNull
    public static String localeName(final @NotNull String locale) {
        return String.format("{locale:%s}", locale);
    }
}
