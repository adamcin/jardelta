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
import net.adamcin.jardelta.api.diff.Differs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public class PidDesignatesDiffer implements Differ<Element<List<MetaTypeDesignate>>> {

    public static @NotNull Stream<Element<ObjectClassDefinition>>
    objectClassDefinitions(final @NotNull Element<MetaTypeDesignate> element,
                           final @NotNull String locale) {
        final Both<Optional<ObjectClassDefinition>> ocds =
                element.values().mapOptional(designate -> designate.getObjectClassDefinitions().get(locale));
        return ocds.left().flatMap(left -> ocds.right().map(right ->
                        Element.of(element.name().appendSegment(localeName(locale)), Both.of(left, right))))
                .stream();
    }

    @NotNull
    public static String localeName(final @NotNull String locale) {
        return String.format("{locale:%s}", locale);
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<List<MetaTypeDesignate>> element) {
        return Differs.ofAtMostOne(Function.<List<MetaTypeDesignate>>identity(),
                        this::diffFirst)
                .diff(baseEmitter.forSubElement(element), element);
    }

    @NotNull
    Stream<Diff> diffFirst(@NotNull Emitter baseEmitter,
                           @NotNull Element<MetaTypeDesignate> element) {
        final List<Diff> diffs = new ArrayList<>();

        if (element.values().map(MetaTypeDesignate::isFactory).testBoth((left, right) -> left != right)) {
            diffs.add(baseEmitter.forChild("{isFactory}").changed());
        }
        final OCDDiffer ocdDiffer = new OCDDiffer();
        final Both<Set<String>> bothLocales = element.values().map(MetaTypeDesignate::getLocales);
        Differs.diffSets(baseEmitter, builder -> builder.emitterProjection(
                                (emitter, locale) -> emitter.forChild(localeName(locale))),
                        bothLocales, (localeEmitter, locale) -> objectClassDefinitions(element, locale)
                                .map(Fun.zipValuesWithKeyFunc(value -> localeEmitter))
                                .flatMap(Fun.mapEntry(ocdDiffer::diff)))
                .forEachOrdered(diffs::add);
        return diffs.stream();
    }
}
