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

package net.adamcin.jardiff.core.osgi.header;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import net.adamcin.jardiff.core.Action;
import net.adamcin.jardiff.core.Context;
import net.adamcin.jardiff.core.Diff;
import net.adamcin.jardiff.core.Differ;
import net.adamcin.jardiff.core.Diffs;
import net.adamcin.jardiff.core.Name;
import net.adamcin.jardiff.core.RefinedDiff;
import net.adamcin.jardiff.core.RefinementStrategy;
import net.adamcin.jardiff.core.URLJar;
import net.adamcin.jardiff.core.manifest.MFAttribute;
import net.adamcin.jardiff.core.manifest.ManifestDiffer;
import net.adamcin.jardiff.core.manifest.Manifests;
import net.adamcin.jardiff.core.osgi.ocd.PidDesignates;
import net.adamcin.jardiff.core.util.GenericDiffers;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import org.apache.felix.metatype.DefaultMetaTypeProvider;
import org.apache.felix.metatype.MetaData;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;

import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HeaderRefinementStrategy implements RefinementStrategy {
    private static final Attributes NAMES = Stream.of(
            Constants.EXPORT_PACKAGE,
            Constants.IMPORT_PACKAGE,
            Constants.REQUIRE_BUNDLE,
            Constants.FRAGMENT_HOST,
            Constants.PROVIDE_CAPABILITY,
            Constants.REQUIRE_CAPABILITY,
            Constants.SERVICE_COMPONENT,
            Constants.BUNDLE_SYMBOLICNAME,
            Constants.INCLUDE_RESOURCE
    ).map(MFAttribute::nameOf).collect(
            Collectors.toMap(
                    Object.class::cast,
                    Fun.compose1(Fun.infer1(Attributes.Name::toString), Object.class::cast),
                    (left, right) -> left,
                    Attributes::new));

    private static final Predicate<Diff> REFINEMENT_TEST_COMMON = diff -> ManifestDiffer.DIFF_KIND.equals(diff.getKind())
            && diff.getAction() == Action.CHANGED;
    private static final Predicate<Diff> REFINEMENT_TEST_PARAMETERIZED = diff ->
            NAMES.containsKey(MFAttribute.nameOf(diff.getName().getFileName().toString()));

    @Override
    public @NotNull RefinedDiff refine(@NotNull Context context, @NotNull Diffs diffs) {
        if (!context.getJars().isBothBundles()) {
            return RefinedDiff.EMPTY;
        }

        final Both<Bundle> bothBundles = context.getJars().both().map(URLJar::getBundle);

        // always refine and diff any possibly localized headers
        final Both<Attributes> localizedAttrs = context.getJars().both().map(URLJar::getLocalizedHeaders);
        final Predicate<Diff> localizedTest = diff -> localizedAttrs.testBoth((left, right) -> {
            final Attributes.Name attrName = new Attributes.Name(diff.getName().getFileName().toString());
            return left.containsKey(attrName) || right.containsKey(attrName);
        });

        final List<Diff> refined = diffs.stream()
                .filter(REFINEMENT_TEST_COMMON.and(localizedTest.or(REFINEMENT_TEST_PARAMETERIZED)))
                .collect(Collectors.toList());

        if (refined.isEmpty() && localizedAttrs.testBoth((left, right) -> left.isEmpty() && right.isEmpty())) {
            return RefinedDiff.EMPTY;
        }

        final MetaData emptyMetaData = new MetaData();
        final Both<Set<String>> bothLocales = bothBundles.map(bundle ->
                Stream.ofNullable(new DefaultMetaTypeProvider(bundle, emptyMetaData).getLocales())
                        .flatMap(Stream::of)
                        .collect(Collectors.toSet()));

        final Differ<MFAttribute> complexDiffer = diffed -> {
            assert diffed.isDiff();
            final Both<Optional<Parameters>> bothParams = diffed.both()
                    .map(value -> value.map(raw -> new Parameters(raw, null, true)));
            final Instructions details = new Instructions(MFAttribute.nameOf(diffed.getName().getFileName().toString()),
                    bothParams);
            return new InstructionsDiffer().diff(details);
        };

        Stream<Diff> complexDiffs = refined.stream()
                .map(Diff::getName)
                .map(name -> new MFAttribute(name, complexDiffer, context.getJars().both().map(jar ->
                        Optional.ofNullable(jar.getMainAttributeValue(name)))))
                .flatMap(mfAttr -> mfAttr.getDiffer().diff(mfAttr));

        // From 3.11.2 Manifest Localization: https://docs.osgi.org/specification/osgi.core/7.0.0/framework.module.html#i3189742
        // A localization entry contains key/value entries for localized information.
        // All headers in a bundle's manifest can be localized.
        // However, the Framework must always use the non-localized versions of headers that have Framework semantics.
        final Set<String> allLocales = bothLocales.stream().flatMap(Set::stream).collect(Collectors.toSet());
        Stream<Diff> localeDiffs = allLocales.stream()
                .flatMap(locale -> {
                    final Differ<MFAttribute> localeDiffer = new LocalizedHeaderDiff(locale);
                    final Both<Dictionary<String, String>> bothLocalizedHeaders = bothBundles.map(bundle -> bundle.getHeaders(locale));
                    return localizedAttrs.stream()
                            .map(Attributes::keySet)
                            .flatMap(Set::stream)
                            .map(Attributes.Name.class::cast)
                            .map(Attributes.Name::toString)
                            .map(Name::of)
                            .map(name ->
                                    new MFAttribute(Manifests.NAME_MANIFEST.append(name).append(PidDesignates.localeName(locale)),
                                            localeDiffer, bothLocalizedHeaders.map(dict ->
                                            Optional.ofNullable(dict.get(name.toString())))))
                            .flatMap(mfAttr -> mfAttr.getDiffer().diff(mfAttr));
                });

        return new RefinedDiff(refined, Stream.concat(complexDiffs, localeDiffs).collect(Diffs.collect()));
    }
}
