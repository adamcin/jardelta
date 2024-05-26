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

package net.adamcin.jardelta.testing.coreats;

import aQute.bnd.osgi.Constants;
import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Delta;
import net.adamcin.jardelta.core.Jars;
import net.adamcin.jardelta.core.Plan;
import net.adamcin.jardelta.core.manifest.Manifests;
import net.adamcin.jardelta.core.osgi.header.HeaderRefinementStrategy;
import net.adamcin.jardelta.testing.DiffTestUtil;
import net.adamcin.streamsupport.Both;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class PlanIT {

    @Test
    void bundleScrAndDsResources() {
        final Context context = Context.builder()
                .jars(Jars.from(ExampleJars.BUNDLE_SCR.getJarUrl(), ExampleJars.BUNDLE_DS.getJarUrl())).build();
        final Delta delta = new Plan().execute(context);
        final Diffs diffs = delta.getResults();
        Emitter headerEmitter = Diff.emitterOf(HeaderRefinementStrategy.DIFF_KIND).forName(Manifests.NAME_MANIFEST);
        DiffTestUtil.assertDiffs(diffs.stream(), headerEmitter
                .ofSubKind(Kind.of("locale"))
                .forChild(Constants.BUNDLE_NAME)
                .forChild("{locale:}")
                .changed(Both.of("jardelta - example-bundle-scr bundle", "jardelta - example-bundle-ds bundle")));
    }

    @Test
    void bundleScrAndBndDsResources() {
        final Context context = Context.builder()
                .jars(Jars.from(ExampleJars.BUNDLE_SCR.getJarUrl(), ExampleJars.BND_DS.getJarUrl())).build();
        Diffs diffs = new Plan().execute(context).getResults();
        Emitter headerEmitter = Diff.emitterOf(HeaderRefinementStrategy.DIFF_KIND).forName(Manifests.NAME_MANIFEST);
        DiffTestUtil.assertDiffs(diffs.stream(), headerEmitter
                .ofSubKind(Kind.of("locale"))
                .forChild(Constants.BUNDLE_DESCRIPTION)
                .forChild("{locale:}")
                .changed(Both.of("Example Bundle SCR", "Example BND DS")));
    }

    @Test
    void bundleDsAndBndDsResources() {
        final Context context = Context.builder()
                .jars(Jars.from(ExampleJars.BUNDLE_DS.getJarUrl(), ExampleJars.BND_DS.getJarUrl())).build();
        final Diffs diffs = new Plan().execute(context).getResults();
        assertFalse(diffs.isEmpty());
    }
}
