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

import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import net.adamcin.jardelta.core.entry.JarEntryDiffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanTest {

    @Test
    void emptyAndDefaultManifest() {

    }

    @Test
    void compareScr() {
        final Context context = Context.builder().jars(Jars.from(
                        getResourceAbsolute("examples/simpleText1/"),
                        getResourceAbsolute("examples/simpleText2/"))).build();
        final Diffs diffs = new Plan().execute(context).getResults();
        assertEquals(
                Diffs.of(Diff.emitterOf(JarEntryDiffer.DIFF_KIND).forName(Name.of("helloworld.txt")).changed()), diffs);
    }

    URL getResourceAbsolute(@NotNull String name) {
        return getClass().getResource(name.replaceFirst("^(?!/)", "/"));
    }

}
