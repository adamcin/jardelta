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

import aQute.lib.collections.Enumerations;
import net.adamcin.jardelta.core.osgi.ocd.JarMetaTypeProvider;
import net.adamcin.streamsupport.Fun;
import org.apache.felix.metatype.MetaData;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import osgimock.org.apache.felix.framework.capabilityset.SimpleFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.streamsupport.Fun.result0;
import static net.adamcin.streamsupport.Fun.uncheck0;

/**
 * This implements {@link org.osgi.framework.Bundle} in ways that are specific to our purposes for artifact comparison.
 * Much of the logic in the Felix Bundle implementation varies based on the bundle's activation state, JRE local settings,
 * and on other related bundle artifacts, like applicable fragments, that may be installed in the framework. We must
 * obviously ignore those considerations, and focus solely on the implicit expression of resources present in this jar
 * through OSGi-specified interfaces.
 */
final class BundleFacade implements Bundle {
    private final OpenJar jar;
    private final Manifest manifest;
    private final Attributes mainAttributes;
    private final MockBundle mockBundle;
    private static final BundleContext mockBundleContext = MockOsgi.newBundleContext();
    private final Map<String, Properties> localeCache;
    public BundleFacade(@NotNull OpenJar jar) {
        this.jar = jar;
        this.manifest = uncheck0(jar::getManifest).get();
        this.mainAttributes = Optional.ofNullable(manifest)
                .map(Fun.compose1(Manifest::getMainAttributes, Fun.infer1(Attributes::new)))
                .orElseGet(Attributes::new);
        this.mockBundle = new MockBundle(mockBundleContext);
        final MetaData emptyMetaData = new MetaData();
        final JarMetaTypeProvider emptyMetaTypeProvider = new JarMetaTypeProvider(this, emptyMetaData);
        final Set<String> locales = Stream.ofNullable(emptyMetaTypeProvider.getLocales())
                .flatMap(Stream::of)
                .collect(Collectors.toSet());
        final String localePrefix = emptyMetaTypeProvider.getLocalePrefix();
        final Map<String, Properties> localeCache = new TreeMap<>();
        locales.forEach(locale -> {
            final String localePathName = localePrefix + "_" + locale + ".properties";
            if (this.jar.getNames().contains(bundlePathToName(localePathName))) {
                result0(() -> {
                    Properties props = new Properties();
                    try (InputStream inputStream = this.getResource(localePathName).openStream()) {
                        props.load(inputStream);
                        return props;
                    }
                }).get().toOptional().ifPresent(props -> localeCache.put(locale, props));
            }
        });
        this.localeCache = localeCache;
    }

    @Override
    public int getState() {
        return Bundle.RESOLVED;
    }

    @Override
    public void start(int options) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop(int options) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(InputStream input) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void uninstall() throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        // default to empty locale/uninstalled behavior instead of Locale.default() for our use case
        return new AttributesToStringDictionary(this.mainAttributes);
    }

    @Override
    public long getBundleId() {
        return this.mockBundle.getBundleId();
    }

    @Override
    public String getLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference<?>[] getRegisteredServices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference<?>[] getServicesInUse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasPermission(Object permission) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getResource(String name) {
        return jar.urlFor(name);
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        if (locale == null || locale.isEmpty()) {
            return getHeaders();
        }

        final Attributes localizedHeaders = jar.getLocalizedHeaders();
        if (localizedHeaders.isEmpty()) {
            return getHeaders();
        }

        final Attributes headers = new Attributes(manifest.getMainAttributes());
        final Optional<Properties> localeProps = Optional.ofNullable(localeCache.get(locale));
        for (Map.Entry<Object, Object> entry : headers.entrySet()) {
            final Attributes.Name name = (Attributes.Name) entry.getKey();
            final String value = entry.getValue().toString();
            if (value.startsWith("%")) {
                final String key = value.substring(1);
                localeProps.map(props -> props.getProperty(key, key));
                headers.put(name, value);
            }
        }

        return new AttributesToStringDictionary(headers);
    }

    @Override
    public String getSymbolicName() {
        return jar.getSymbolicName();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    Name bundlePathToName(@NotNull String path) {
        return Name.of(path.replaceFirst("^/+", ""));
    }

    Predicate<Name> getParentPathPredicate(@NotNull String path, boolean recurse) {
        final Name pathName = bundlePathToName(path);
        if (pathName.isRoot()) {
            return recurse ? name -> true : name -> name.getParent() == null;
        } else {
            return recurse
                    ? name -> name.getParent() != null && name.getParent().startsWith(pathName)
                    : name -> name.getParent() != null && name.getParent().equals(pathName);
        }
    }

    Stream<String> internalGetEntryPaths(@NotNull String path, boolean recurse) {
        final Predicate<Name> parentPredicate = getParentPathPredicate(path, recurse);
        return Stream.concat(jar.getNames().stream()
                .filter(parentPredicate)
                .map(Name::toString), jar.getDirNames().stream()
                .filter(parentPredicate)
                .map(Name::toString));
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        List<String> children = internalGetEntryPaths(path, false)
                .collect(Collectors.toList());
        return children.isEmpty() ? null : Enumerations.enumeration(children.spliterator());
    }

    @Override
    public URL getEntry(String path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLastModified() {
        return jar.lastModified();
    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
        // see EntryFilterEnumeration
        // Sanity check the parameters.
        if (path == null) {
            throw new IllegalArgumentException("The path for findEntries() cannot be null.");
        }

        // File pattern defaults to "*" if not specified.
        filePattern = (filePattern == null) ? "*" : filePattern;
        final List<String> fileFilter = SimpleFilter.parseSubstring(filePattern);
        // for tests see https://github.com/apache/felix-dev/blob/b6fff2adcc1afee039f0f60713032363144ad0fa/framework/src/test/java/org/apache/felix/framework/ResourceLoadingTest.java
        return Enumerations.enumeration(internalGetEntryPaths(path, recurse)
                .filter(name -> SimpleFilter.compareSubstring(fileFilter,
                        bundlePathToName(name).getSegment()))
                .map(jar::urlFor).spliterator());
    }

    @Override
    public BundleContext getBundleContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getVersion() {
        return new Version(jar.getVersion());
    }

    @Override
    public <A> A adapt(Class<A> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDataFile(String filename) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(@NotNull Bundle o) {
        throw new UnsupportedOperationException();
    }

    static class AttributesToStringDictionary extends Dictionary<String, String> {
        private final Attributes attributes;

        public AttributesToStringDictionary(Attributes attributes) {
            this.attributes = attributes;
        }

        @Override
        public int size() {
            return attributes.size();
        }

        @Override
        public boolean isEmpty() {
            return attributes.isEmpty();
        }

        @Override
        public Enumeration<String> keys() {
            return Collections.enumeration(attributes.keySet().stream().map(Objects::toString).collect(Collectors.toSet()));
        }

        @Override
        public Enumeration<String> elements() {
            return Collections.enumeration(attributes.values().stream().map(Objects::toString).collect(Collectors.toSet()));
        }

        @Override
        public String get(Object key) {
            return attributes.getValue(key.toString());
        }

        @Override
        public String put(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String remove(Object key) {
            throw new UnsupportedOperationException();
        }
    }
}
