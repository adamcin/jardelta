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

import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.Differ;
import net.adamcin.jardelta.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class PidDesignatesDiffer implements Differ<PidDesignates> {
    public static final String DIFF_KIND = "osgi.metatype.designate";

    @Override
    public @NotNull Stream<Diff> diff(@NotNull PidDesignates diffed) {
        final Diff.Builder diffBuilder = Diff.builder(DIFF_KIND).named(diffed.name());
        return GenericDiffers.ofAtMostOne(diffBuilder, diffed.both(), firsts -> diffFirst(diffed, firsts));
    }

    @NotNull
    Stream<Diff> diffFirst(@NotNull PidDesignates diffed,
                           @NotNull Both<MetaTypeDesignate> bothValues) {
        final Diff.Builder diffBuilder = Diff.builder(DIFF_KIND).named(diffed.name());
        final List<Diff> diffs = new ArrayList<>();

        if (bothValues.left().isFactory() != bothValues.right().isFactory()) {
            diffs.add(diffBuilder.child("{isFactory}").changed());
        }
        final MetaTypeOCDDiffer ocdDiffer = new MetaTypeOCDDiffer();
        final Both<Set<String>> bothLocales = bothValues.map(MetaTypeDesignate::getLocales);
        GenericDiffers.ofAllInEitherSet(diffBuilder, bothLocales, locale -> diffed.ocds(bothValues, locale)
                .flatMap(ocdDiffer::diff))
                .forEachOrdered(diffs::add);
        return diffs.stream();
    }
}
