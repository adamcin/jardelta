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

import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import net.adamcin.jardelta.core.entry.JarEntryMetadata;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Bundle;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static net.adamcin.streamsupport.Fun.mapEntry;
import static net.adamcin.streamsupport.Fun.result0;
import static net.adamcin.streamsupport.Fun.uncheck0;

public class OpenJar implements Closeable {
    private final Jar jar;
    private final Set<Name> names;
    private final Set<Name> dirNames;
    private final Manifest manifest;
    private final Bundle bundleFacade;
    private final Map<Name, Result<JarEntryMetadata>> resourceCache;

    private OpenJar(@NotNull Jar jar, @NotNull Map<Name, Result<JarEntryMetadata>> resourceCache) {
        this.resourceCache = resourceCache;
        this.jar = jar;
        this.names = this.jar.getResources().keySet().stream()
                .map(Name::of)
                .collect(Collectors.toCollection(TreeSet::new));
        this.dirNames = this.jar.getDirectories().keySet().stream()
                .map(Name::of)
                .collect(Collectors.toCollection(TreeSet::new));
        this.manifest = result0(jar::getManifest).get().getOrDefault(null);
        this.bundleFacade = new BundleFacade(this);
    }

    public String getVersion() {
        return uncheck0(jar::getVersion).get();
    }

    public String getSymbolicName() {
        return uncheck0(jar::getBsn).get();
    }

    public long lastModified() {
        return jar.lastModified();
    }

    public Set<Name> getNames() {
        return Set.copyOf(names);
    }

    public Set<Name> getDirNames() {
        return Set.copyOf(dirNames);
    }

    public Optional<Result<JarEntryMetadata>> getResourceMetadata(@NotNull Name name) {
        return names.contains(name) ? Optional.of(resourceCache.computeIfAbsent(name, namePath -> {
            final String nameString = namePath.toString();
            return Optional.ofNullable(jar.getResource(nameString))
                    .map(Result::success)
                    .orElseGet(() -> Result.failure(new NullPointerException("no resource for name " + nameString)))
                    .flatMap(JarEntryMetadata::fromResource);
        })) : Optional.empty();
    }

    @Nullable
    public Manifest getManifest() {
        return manifest;
    }

    URL jarUrl() throws MalformedURLException {
        if (jar.getName().contains("!/")) {
            throw new MalformedURLException("Nested jar urls are not supported");
        } else {
            return Path.of(jar.getName()).toUri().toURL();
        }
    }

    public URL urlFor(@NotNull String resourceName) {
        return uncheck0(() -> new URL("jar:" + jarUrl().toString() + "!/" + (resourceName.startsWith("/")
                ? resourceName.substring(1) : resourceName))).get();
    }

    @Nullable
    public String getMainAttributeValue(final @NotNull Name attribute) {
        return Optional.ofNullable(getManifest())
                .flatMap(manny ->
                        result0(() -> manny.getMainAttributes()
                                .getValue(attribute.getSegment()))
                                .get().toOptional())
                .orElse(null);
    }

    @NotNull
    public Attributes getLocalizedHeaders() {
        final Attributes localizedHeaders = new Attributes();
        Optional.ofNullable(getManifest()).map(Manifest::getMainAttributes).ifPresent(mainAttrs -> {
            for (Map.Entry<Object, Object> entry : mainAttrs.entrySet()) {
                if (entry.getValue().toString().startsWith("%")) {
                    localizedHeaders.put(entry.getKey(), entry.getValue());
                }
            }
        });
        return localizedHeaders;
    }

    @NotNull
    public Bundle getBundle() {
        return bundleFacade;
    }

    public boolean isBundle() {
        return result0(jar::getBsn).get().map(Objects::nonNull).getOrDefault(false);
    }

    @Override
    public void close() throws IOException {
        this.jar.close();
    }

    @NotNull
    public static OpenJar fromFile(@Nullable String name,
                                   @NotNull Path path,
                                   @NotNull Map<Name, Result<JarEntryMetadata>> resourceCache) throws Exception {
        return new OpenJar(Jar.fromResource(name, new FileResource(path)), resourceCache);
    }

}
