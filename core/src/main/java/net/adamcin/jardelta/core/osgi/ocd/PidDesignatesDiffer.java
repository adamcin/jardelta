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

import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class PidDesignatesDiffer implements Differ<PidDesignates> {

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull PidDesignates element) {
        return GenericDiffers.ofAtMostOne(baseEmitter.forSubElement(element), element.values(),
                (emitter, firsts) -> diffFirst(emitter, element, firsts));
    }

    @NotNull
    Stream<Diff> diffFirst(@NotNull Emitter emitter,
                           @NotNull PidDesignates element,
                           @NotNull Both<MetaTypeDesignate> bothValues) {
        final List<Diff> diffs = new ArrayList<>();

        if (bothValues.left().isFactory() != bothValues.right().isFactory()) {
            diffs.add(emitter.forChild("{isFactory}").changed());
        }
        final MetaTypeOCDDiffer ocdDiffer = new MetaTypeOCDDiffer();
        final Both<Set<String>> bothLocales = bothValues.map(MetaTypeDesignate::getLocales);
        GenericDiffers.ofAllInEitherSet(Fun.compose1(PidDesignates::localeName, emitter::forChild),
                        bothLocales, (childEmitter, locale) -> element.ocds(bothValues, locale)
                                .map(Fun.zipValuesWithKeyFunc(value -> childEmitter))
                                .flatMap(Fun.mapEntry(ocdDiffer::diff)))
                .forEachOrdered(diffs::add);
        return diffs.stream();
    }
}
