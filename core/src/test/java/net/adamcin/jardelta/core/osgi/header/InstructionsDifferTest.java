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

package net.adamcin.jardelta.core.osgi.header;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import org.junit.jupiter.api.Test;

import java.util.jar.Attributes;

import static net.adamcin.jardelta.test.DiffTestUtil.assertDiffs;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionsDifferTest {
    final Emitter baseEmitter = Diff.emitterOf(Kind.of("test"))
            .forName(Name.of("testRoot"));

    @Test
    void diff_parameterAttrs() {
        final String leftValue = "net.adamcin.jardelta.testing.example;version=\"1.0.0\"";
        final String rightValue = "net.adamcin.jardelta.testing.example;version=\"2.0.0\"";

        final Emitter projectedEmitter = baseEmitter
                .forChild(Constants.EXPORT_PACKAGE)
                .forChild("net.adamcin.jardelta.testing.example")
                .forChild("version");

        final Instructions element = new Instructions(new Attributes.Name(Constants.EXPORT_PACKAGE),
                Both.ofNullables(new Parameters(leftValue), new Parameters(rightValue)));
        assertTrue(element.isDiff());

        InstructionsDiffer differ = new InstructionsDiffer();
        assertDiffs(differ.diff(baseEmitter, element), projectedEmitter.changed(Both.of("1.0.0", "2.0.0")));
    }

    @Test
    void diff_parameterAttrsDuplicates() {
        final String leftValue = "osgi.extender;filter:=\"(&(osgi.extender=osgi.component)(version>=1.3.0)(!(version>=2.0.0)))\",osgi.service;filter:=\"(objectClass=com.example.ServiceA)\";effective:=active,osgi.service;filter:=\"(objectClass=com.example.ServiceB)\";effective:=active,osgi.service;filter:=\"(objectClass=com.example.ServiceC)\";effective:=active;resolution:=optional,osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"";
        final String rightValue = "osgi.service;filter:=\"(objectClass=com.example.ServiceC)\";effective:=active,osgi.service;filter:=\"(objectClass=com.example.ServiceA)\";effective:=active,osgi.service;filter:=\"(objectClass=com.example.ServiceB)\";effective:=active,osgi.extender;filter:=\"(&(osgi.extender=osgi.component)(version>=1.4.0)(!(version>=2.0.0)))\",osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=11))\"";

        final Emitter capabilityEmitter = baseEmitter
                .forChild(Constants.REQUIRE_CAPABILITY);

        final Instructions element = new Instructions(new Attributes.Name(Constants.REQUIRE_CAPABILITY),
                Both.ofNullables(new Parameters(leftValue), new Parameters(rightValue)));
        assertTrue(element.isDiff());

        InstructionsDiffer differ = new InstructionsDiffer();
        final Diffs allDiffs = differ.diff(baseEmitter, element).collect(Diffs.collector());
        final Emitter osgiEeEmitter = capabilityEmitter.forChild("osgi.ee");
        assertDiffs(allDiffs.withName(osgiEeEmitter.getName()).stream(),
                osgiEeEmitter
                        .forChild("osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"")
                        .removed(),
                osgiEeEmitter
                        .forChild("osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=11))\"")
                        .added());

        final Emitter osgiExtenderEmitter = capabilityEmitter.forChild("osgi.extender");
        assertDiffs(allDiffs.withName(osgiExtenderEmitter.getName()).stream(),
                osgiExtenderEmitter
                        .forChild("osgi.extender;filter:=\"(&(osgi.extender=osgi.component)(version>=1.3.0)(!(version>=2.0.0)))\"")
                        .removed(),
                osgiExtenderEmitter
                        .forChild("osgi.extender;filter:=\"(&(osgi.extender=osgi.component)(version>=1.4.0)(!(version>=2.0.0)))\"")
                        .added());

        final Emitter osgiServiceEmitter = capabilityEmitter.forChild("osgi.service");
        assertDiffs(allDiffs.withName(osgiServiceEmitter.getName()).stream(),
                osgiServiceEmitter
                        .forChild("osgi.service;effective:=active;filter:=\"(objectClass=com.example.ServiceC)\";resolution:=optional")
                        .removed(),
                osgiServiceEmitter
                        .forChild("osgi.service;effective:=active;filter:=\"(objectClass=com.example.ServiceC)\"")
                        .added());
    }

    @Test
    void diff_parameterListAliases() {
        final String leftValue = "OSGI-INF/l10n=target/classes/OSGI-INF/l10n,META-INF/LICENSE=target/maven-shared-archive-resources/META-INF/LICENSE";
        final String rightValue = "META-INF/LICENSE=/opt/workspace/target/maven-shared-archive-resources/META-INF/LICENSE,xmpDamSchema/types/ISO16684-Types-Simple.rng=/opt/workspace/src/main/resources/xmpDamSchema/types/ISO16684-Types-Simple.rng";

        final Emitter projectedEmitter = baseEmitter
                .forChild(Constants.INCLUDE_RESOURCE);

        final Instructions element = new Instructions(new Attributes.Name(Constants.INCLUDE_RESOURCE),
                Both.ofNullables(new Parameters(leftValue), new Parameters(rightValue)));
        assertTrue(element.isDiff());

        InstructionsDiffer differ = new InstructionsDiffer();
        assertDiffs(differ.diff(baseEmitter, element),
                projectedEmitter.forChild("OSGI-INF/l10n")
                        .removed("OSGI-INF/l10n"),
                projectedEmitter.forChild("xmpDamSchema/types/ISO16684-Types-Simple.rng")
                        .added("xmpDamSchema/types/ISO16684-Types-Simple.rng"));
    }
}
