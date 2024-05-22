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
import net.adamcin.jardelta.api.diff.Verb;
import net.adamcin.jardelta.api.diff.Diff;
import net.adamcin.jardelta.api.diff.Differ;
import net.adamcin.jardelta.api.diff.Diffs;
import net.adamcin.jardelta.api.diff.Element;
import net.adamcin.jardelta.api.diff.Emitter;
import net.adamcin.jardelta.api.jar.OpenJar;
import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Refinement;
import net.adamcin.jardelta.core.RefinementStrategy;
import net.adamcin.jardelta.core.manifest.ManifestAttribute;
import net.adamcin.jardelta.core.manifest.ManifestRefinementStrategy;
import net.adamcin.jardelta.core.manifest.Manifests;
import net.adamcin.jardelta.core.osgi.OsgiUtil;
import net.adamcin.jardelta.core.osgi.ocd.PidDesignatesDiffer;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.apache.felix.metatype.DefaultMetaTypeProvider;
import org.apache.felix.metatype.MetaData;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeaderRefinementStrategy implements RefinementStrategy {
    private static final Attributes NAMES = ManifestAttribute.attributeSet(
            Constants.EXPORT_PACKAGE,
            Constants.IMPORT_PACKAGE,
            Constants.PRIVATE_PACKAGE,
            Constants.REQUIRE_BUNDLE,
            Constants.FRAGMENT_HOST,
            Constants.PROVIDE_CAPABILITY,
            Constants.REQUIRE_CAPABILITY,
            Constants.SERVICE_COMPONENT,
            Constants.BUNDLE_SYMBOLICNAME,
            Constants.INCLUDE_RESOURCE
    );

    private static final Predicate<Diff> REFINEMENT_TEST_COMMON = diff -> diff.getKind().isSubKindOf(ManifestRefinementStrategy.DIFF_KIND)
            && diff.getVerb() == Verb.CHANGED;
    private static final Predicate<Diff> REFINEMENT_TEST_PARAMETERIZED = Fun.composeTest1(
            Fun.compose1(Diff::getName, Name::getSegment),
            ManifestAttribute.inAttributeSet(NAMES));

    @Override
    public @NotNull Kind getKind() {
        return InstructionsDiffer.DIFF_KIND;
    }

    @Override
    public @NotNull Refinement refine(@NotNull Context context,
                                      @NotNull Diffs diffs,
                                      @NotNull Element<OpenJar> openJars) {
        Optional<Both<Bundle>> bundleAdapters = OsgiUtil.requireBothBundles(openJars.values());
        if (bundleAdapters.isEmpty()) {
            return Refinement.EMPTY;
        }

        final Both<Bundle> bothBundles = bundleAdapters.get();

        // always refine and diff any possibly localized headers
        final Both<Attributes> localizedAttrs = openJars.values().map(this::getLocalizedHeaders);
        final Predicate<Diff> localizedTest = diff -> localizedAttrs.testBoth((left, right) -> {
            final Attributes.Name attrName = new Attributes.Name(diff.getName().getSegment());
            return left.containsKey(attrName) || right.containsKey(attrName);
        });

        final List<Diff> refined = diffs.stream()
                .filter(REFINEMENT_TEST_COMMON.and(localizedTest.or(REFINEMENT_TEST_PARAMETERIZED)))
                .collect(Collectors.toList());

        if (refined.isEmpty() && localizedAttrs.testBoth((left, right) -> left.isEmpty() && right.isEmpty())) {
            return Refinement.EMPTY;
        }

        final MetaData emptyMetaData = new MetaData();
        final Both<Set<String>> bothLocales = bothBundles.map(bundle ->
                Stream.ofNullable(new DefaultMetaTypeProvider(bundle, emptyMetaData).getLocales())
                        .flatMap(Stream::of)
                        .collect(Collectors.toSet()));

        final Differ<ManifestAttribute> complexDiffer = (emitter, diffed) -> {
            assert diffed.isDiff();
            final Both<Optional<Parameters>> bothParams = diffed.values()
                    .map(value -> value.map(raw -> new Parameters(raw, null, true)));
            final Instructions details = new Instructions(ManifestAttribute.nameOf(diffed.name().getSegment()),
                    bothParams);
            return new InstructionsDiffer().diff(emitter, details);
        };

        final Emitter attrEmitter = Diff.emitterOf(InstructionsDiffer.DIFF_KIND);
        Stream<Diff> complexDiffs = refined.stream()
                .map(Diff::getName)
                .map(name -> new ManifestAttribute(name, openJars.values().map(jar ->
                        Optional.ofNullable(jar.getMainAttributeValue(name)))))
                .flatMap(mfAttr -> complexDiffer.diff(attrEmitter, mfAttr));

        // From 3.11.2 Manifest Localization: https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#i3189742
        // A localization entry contains key/value entries for localized information.
        // All headers in a bundle's manifest can be localized.
        // However, the Framework must always use the non-localized versions of headers that have Framework semantics.
        final Set<String> allLocales = bothLocales.stream().flatMap(Set::stream).collect(Collectors.toSet());
        Stream<Diff> localeDiffs = allLocales.stream()
                .flatMap(locale -> {
                    final Differ<ManifestAttribute> localeDiffer = new LocalizedHeaderDiff(locale);
                    final Both<Dictionary<String, String>> bothLocalizedHeaders = bothBundles.map(bundle -> bundle.getHeaders(locale));
                    return localizedAttrs.stream()
                            .map(Attributes::keySet)
                            .flatMap(Set::stream)
                            .map(Attributes.Name.class::cast)
                            .map(Attributes.Name::toString)
                            .map(Name::of)
                            .map(name -> new ManifestAttribute(Manifests.NAME_MANIFEST.append(name)
                                    .appendSegment(PidDesignatesDiffer.localeName(locale)), bothLocalizedHeaders.map(dict ->
                                    Optional.ofNullable(dict.get(name.toString())))))
                            .flatMap(mfAttr -> localeDiffer.diff(attrEmitter, mfAttr));
                });

        return new Refinement(refined, Stream.concat(complexDiffs, localeDiffs).collect(Diffs.collector()));
    }

    @NotNull Attributes getLocalizedHeaders(@NotNull OpenJar openJar) {
        final Attributes localizedHeaders = new Attributes();
        Optional.ofNullable(openJar.getManifest()).map(Manifest::getMainAttributes).ifPresent(mainAttrs -> {
            for (Map.Entry<Object, Object> entry : mainAttrs.entrySet()) {
                if (entry.getValue().toString().startsWith("%")) {
                    localizedHeaders.put(entry.getKey(), entry.getValue());
                }
            }
        });
        return localizedHeaders;
    }
}
