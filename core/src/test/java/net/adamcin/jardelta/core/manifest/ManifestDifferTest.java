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

package net.adamcin.jardelta.core.manifest;

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Diffs;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.streamsupport.Both;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

class ManifestDifferTest {
    private final Emitter baseEmitter = Diff.emitterOf(ManifestRefinementStrategy.DIFF_KIND);
    private final Emitter manifestEmitter = baseEmitter.forName(Manifests.NAME_MANIFEST);

    @Test
    void diff_added() {
        final ManifestDiffer differ = new ManifestDiffer();
        assertEquals(Diffs.of(manifestEmitter.added()), differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(null, new Manifest())))
                .collect(Diffs.collector()));

        assertEquals(manifestEmitter.added(new Manifest().toString()).getHints().right(), differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(null, new Manifest())))
                .findFirst().map(Diff::getHints).flatMap(Both::right));
    }

    @Test
    void diff_removed() {
        final ManifestDiffer differ = new ManifestDiffer();
        assertEquals(Diffs.of(manifestEmitter.removed()), differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(new Manifest(), null)))
                .collect(Diffs.collector()));

        assertEquals(manifestEmitter.removed(new Manifest().toString()).getHints().left(), differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(new Manifest(), null)))
                .findFirst().map(Diff::getHints).flatMap(Both::left));
    }

    @Test
    void diff_none() {
        final ManifestDiffer differ = new ManifestDiffer();
        assertEquals(Diffs.EMPTY, differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(new Manifest(), new Manifest())))
                .collect(Diffs.collector()));
        Manifest single = new Manifest();
        assertEquals(Diffs.EMPTY, differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(single, single)))
                .collect(Diffs.collector()));
        assertEquals(Diffs.EMPTY, differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(new Manifest(single), new Manifest(single))))
                .collect(Diffs.collector()));
    }

    Manifest mapManifest(Map<String, String> mainAttributes) {
        Manifest manifest = new Manifest();
        mainAttributes.forEach(manifest.getMainAttributes()::putValue);
        return manifest;
    }

    void putEntry(Manifest manifest, String entry, Map<String, String> attributes) {
        attributes.forEach(manifest.getEntries().computeIfAbsent(entry, name -> new Attributes())::putValue);
    }

    @Test
    void diff_attrAdded() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of());
        Manifest right = mapManifest(Map.of("Class-Path", ".,foo"));
        Optional<Diff> expected = Optional.of(manifestEmitter
                .forChild("Class-Path")
                .added(".,foo"));
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
        assertEquals(Both.ofNullables(null, ".,foo"), actual.map(Diff::getHints)
                .orElse(Both.empty()));
    }

    @Test
    void diff_attrRemoved() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of("Class-Path", ".,foo"));
        Manifest right = mapManifest(Map.of());
        Optional<Diff> expected = Optional.of(manifestEmitter
                .forChild("Class-Path")
                .removed(".,foo"));
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
        assertEquals(Both.ofNullables(".,foo", null), actual.map(Diff::getHints)
                .orElse(Both.empty()));
    }

    @Test
    void diff_attrChanged() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of("Class-Path", "."));
        Manifest right = mapManifest(Map.of("Class-Path", ".,foo"));
        Optional<Diff> expected = Optional.of(manifestEmitter
                .forChild("Class-Path")
                .changed(Both.of(".", ".,foo")));
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
        assertEquals(Both.of(".", ".,foo"), actual.map(Diff::getHints)
                .map(both -> both.map(Optional::get))
                .orElse(Both.of("", "")));
    }

    @Test
    void diff_attrChanged_case() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of("class-path", "."));
        Manifest right = mapManifest(Map.of("Class-Path", ".,foo"));
        Optional<Diff> expected = Optional.of(manifestEmitter
                .forChild("class-path")
                .changed(Both.of(".", ".,foo")));
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
        assertEquals(Both.of(".", ".,foo"), actual.map(Diff::getHints)
                .map(both -> both.map(Optional::get))
                .orElse(Both.of("", "")));
    }

    @Test
    void diff_entryAttr_added() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of("Class-Path", "."));
        Manifest right = mapManifest(Map.of("Class-Path", "."));
        putEntry(right, "testEntry/", Map.of("Sealed", "true"));
        Optional<Diff> expected = Optional.of(manifestEmitter
                .ofSubKind(Kind.of("entry"))
                .forChild("{entry:testEntry/}")
                .added());
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
    }

    @Test
    void diff_entryAttr_removed() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of("Class-Path", "."));
        Manifest right = mapManifest(Map.of("Class-Path", "."));
        putEntry(left, "testEntry/", Map.of("Sealed", "true"));
        Optional<Diff> expected = Optional.of(manifestEmitter
                .ofSubKind(Kind.of("entry"))
                .forChild("{entry:testEntry/}")
                .removed());
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
    }

    @Test
    void diff_entryAttr_changed() {
        final ManifestDiffer differ = new ManifestDiffer();
        Manifest left = mapManifest(Map.of("Class-Path", "."));
        Manifest right = mapManifest(Map.of("Class-Path", "."));
        putEntry(left, "testEntry/", Map.of("Sealed", "true"));
        putEntry(right, "testEntry/", Map.of("Sealed", "false"));
        Optional<Diff> expected = Optional.of(manifestEmitter
                .ofSubKind(Kind.of("entry"))
                .forChild("{entry:testEntry/}")
                .forChild("Sealed")
                .changed(Both.of("true", "false")));
        Optional<Diff> actual = differ
                .diff(baseEmitter, new Manifests(Both.ofNullables(left, right)))
                .findFirst();
        assertEquals(expected, actual);
    }
}
