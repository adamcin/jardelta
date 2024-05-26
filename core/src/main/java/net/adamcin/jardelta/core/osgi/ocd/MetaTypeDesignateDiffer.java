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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.adamcin.jardelta.api.diff.Differs.concat;
import static net.adamcin.jardelta.api.diff.Differs.emitChild;
import static net.adamcin.jardelta.api.diff.Differs.ofAtMostOne;
import static net.adamcin.jardelta.api.diff.Differs.ofMapsCustomized;

public class MetaTypeDesignateDiffer implements Differ<List<MetaTypeDesignate>> {

    private final Differ<List<MetaTypeDesignate>> firstDesignateDiffer = ofAtMostOne(Function.identity(),
            concat(
                    emitChild("{isFactory}", Differs.ofEquality(MetaTypeDesignate::isFactory)),
                    ofMapsCustomized(MetaTypeDesignate::getObjectClassDefinitions,
                            builder -> builder.emitterProjection(
                                    (baseEmitter, key) -> baseEmitter.forChild(localeName(key))),
                            Differs.ofMapValues(new OCDDiffer()))));

    @NotNull
    public static String localeName(final @NotNull String locale) {
        return String.format("{locale:%s}", locale);
    }

    @Override
    public @NotNull Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull Element<List<MetaTypeDesignate>> element) {
        return firstDesignateDiffer.diff(baseEmitter.forSubElement(element), element);
    }
}
