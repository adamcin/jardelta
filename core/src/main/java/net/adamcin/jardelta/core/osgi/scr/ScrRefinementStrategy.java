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

package net.adamcin.jardelta.core.osgi.scr;

import aQute.bnd.osgi.Constants;
import net.adamcin.jardelta.core.Context;
import net.adamcin.jardelta.core.Diff;
import net.adamcin.jardelta.core.JarPath;
import net.adamcin.jardelta.core.Name;
import net.adamcin.jardelta.core.entry.JarEntryDiffer;
import net.adamcin.jardelta.core.Action;
import net.adamcin.jardelta.core.Diffs;
import net.adamcin.jardelta.core.RefinedDiff;
import net.adamcin.jardelta.core.RefinementStrategy;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.apache.felix.scr.impl.logger.NoOpLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScrRefinementStrategy implements RefinementStrategy {
    public static final Name NAME_PREFIX = Name.of("{osgi.scr}");
    public static final String KIND = "osgi.scr";

    @Override
    public @NotNull RefinedDiff refine(@NotNull Context context, @NotNull Diffs diffs) {
        // no point in deep comparison of scr unless both jars are bundles
        if (context.getJars().mixedPackaging()) {
            return RefinedDiff.EMPTY;
        }

        Both<Map<Name, Result<List<ComponentMetadata>>>> allDescriptors = context.getJars().both()
                .map(JarPath::getBundle)
                .map(this::getScrResources);

        final Set<Name> descriptorNames = allDescriptors.stream()
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        final List<Diff> refined = diffs.stream()
                .filter(diff -> JarEntryDiffer.DIFF_KIND.equals(diff.getKind())
                        && descriptorNames.contains(diff.getName()))
                .collect(Collectors.toList());

        if (refined.isEmpty()) {
            return RefinedDiff.EMPTY;
        }

        final Set<Name> refinedNames = refined.stream()
                .map(Diff::getName)
                .collect(Collectors.toCollection(TreeSet::new));

        descriptorNames.retainAll(refinedNames);

        // if any refined diff descriptor fails to parse, add an INVALID diff for it and exclude empty supersedes.
        final List<Diff> invalidDiffs = descriptorNames.stream()
                .flatMap(descriptorName -> {
                    final Both<Optional<Result<List<ComponentMetadata>>>> bothDescriptors = allDescriptors.map(map ->
                            Optional.ofNullable(map.get(descriptorName)));
                    if (bothDescriptors
                            .map(value -> value.map(Result::isFailure).orElse(false))
                            .testBoth((left, right) -> left || right)) {
                        return Stream.of(Diff.builder(KIND).named(descriptorName).build(Action.ERR_RIGHT));
                    } else {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        invalidDiffs.stream().map(Diff::getName).forEach(descriptorNames::remove);

        //Both.ofResults()
        final Diff.Builder diff = Diff.builder(KIND).named(NAME_PREFIX);
        Both<Map<String, List<ComponentMetadata>>> bothGrouped = allDescriptors.map(map -> map.entrySet().stream()
                .filter(Fun.testKey(Fun.inSet(descriptorNames)))
                .map(Fun.mapEntry((key, value) -> value.getOrDefault(Collections.emptyList())))
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(ComponentMetadata::getName)));

        Set<String> allComponentNames = bothGrouped.stream()
                .map(Map::keySet)
                .flatMap(Set::stream).collect(Collectors.toCollection(TreeSet::new));

        // check for special case
        if (allComponentNames.contains("icd")) {
            invalidDiffs.add(Diff.builder(KIND).named(NAME_PREFIX.append("icd.duplicates.detected")).build(Action.ERR_RIGHT));
            allComponentNames.remove("icd");
        }

        final ScrDescriptorsDiffer differ = new ScrDescriptorsDiffer();
        Diffs allDiffs = Stream.concat(invalidDiffs.stream(), allComponentNames.stream()
                .map(componentName ->
                        new ScrDescriptors(componentName,
                                bothGrouped.map(map -> map.getOrDefault(componentName, Collections.emptyList()))))
                .flatMap(differ::diff))
                .collect(Diffs.collect());
        return new RefinedDiff(refined, allDiffs);
    }

    Map<Name, Result<List<ComponentMetadata>>> getScrResources(final @NotNull Bundle bundle) {
        final String descriptorLocations = bundle.getHeaders("")
                .get(Constants.SERVICE_COMPONENT);

        // 112.4.1: The value of the the header is a comma separated list of XML entries within the Bundle
        StringTokenizer st = new StringTokenizer(descriptorLocations, ", ");
        Stream<Map.Entry<Name, Result<List<ComponentMetadata>>>> metadatas = Stream.empty();
        while (st.hasMoreTokens()) {
            String descriptorLocation = st.nextToken();

            // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
            // fragments, SCR must log an error message with the Log Service, if present, and continue.
            metadatas = Stream.concat(metadatas, Stream.of(findDescriptors(bundle, descriptorLocation))
                    .map(url -> loadDescriptor(bundle, url)));
        }
        // it is possible that the header lists duplicate descriptorLocations, which means that the same discovered URL
        // may be read more than once, with a possibility of nondeterministic failure affecting each read. Otherwise,
        // we don't want care about merging the lists within the results since we can assume that the contents will be
        // The merge operation here returns the first result if it is successful
        return metadatas.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (first, second) -> first.isSuccess() ? first : second, TreeMap::new));
    }

    /**
     * Finds component descriptors based on descriptor location.
     *
     * @param bundle             bundle to search for descriptor files
     * @param descriptorLocation descriptor location
     * @return array of descriptors or empty array if none found
     */
    static URL[] findDescriptors(final Bundle bundle, final String descriptorLocation) {
        if (bundle == null || descriptorLocation == null || descriptorLocation.trim().isEmpty()) {
            return new URL[0];
        }

        // split pattern and path
        final int lios = descriptorLocation.lastIndexOf("/");
        final String path;
        final String filePattern;
        if (lios > 0) {
            path = descriptorLocation.substring(0, lios);
            filePattern = descriptorLocation.substring(lios + 1);
        } else {
            path = "/";
            filePattern = descriptorLocation;
        }

        // find the entries
        final Enumeration<URL> entries = bundle.findEntries(path, filePattern, false);
        if (entries == null || !entries.hasMoreElements()) {
            return new URL[0];
        }

        // create the result list
        List<URL> urls = new ArrayList<>();
        while (entries.hasMoreElements()) {
            urls.add(entries.nextElement());
        }
        return urls.toArray(new URL[urls.size()]);
    }

    private Map.Entry<Name, Result<List<ComponentMetadata>>> loadDescriptor(final Bundle bundle, final URL descriptorURL) {
        // simple path for log messages
        final String descriptorLocation = descriptorURL.getPath();

        return Fun.toEntry(Name.of(descriptorLocation.replaceFirst("^/*", "")
                .replaceFirst(".*!/", "")), Fun.result0(() -> {
            try (InputStream stream = descriptorURL.openStream()) {

                XmlHandler handler = new XmlHandler(bundle, new NoOpLogger(), true,
                        false, null);
                final SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                final SAXParser parser = factory.newSAXParser();

                parser.parse(stream, handler);

                // 112.4.2 Component descriptors may contain a single, root component element
                // or one or more component elements embedded in a larger document
                List<ComponentMetadata> metadataList = handler.getComponentMetadataList();
                for (ComponentMetadata metadata : metadataList) {
                    metadata.validate();
                }
                return metadataList;
            }
        }).get());
    }
}
