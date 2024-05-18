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
import aQute.libg.cryptography.SHA256;
import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.jar.EntryMeta;
import net.adamcin.jardelta.api.jar.OpenJar;
import net.adamcin.jardelta.core.entry.EntryMetaImpl;
import net.adamcin.streamsupport.Fun;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Bundle;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.adamcin.streamsupport.Fun.result0;
import static net.adamcin.streamsupport.Fun.uncheck0;

public class OpenJarImpl implements OpenJar, Closeable {
    private final Jar jar;
    private final Set<Name> names;
    private final Set<Name> dirNames;
    private final Map<Name, Set<Name>> entryAttributeNames;
    private final Manifest manifest;
    private final Bundle bundleFacade;
    private final Map<Name, Result<EntryMeta>> resourceCache;

    private OpenJarImpl(@NotNull Jar jar, @NotNull Map<Name, Result<EntryMeta>> resourceCache) {
        this.resourceCache = resourceCache;
        this.jar = jar;
        this.names = this.jar.getResources().keySet().stream()
                .map(Name::of)
                .collect(Collectors.toCollection(TreeSet::new));
        this.dirNames = this.jar.getDirectories().keySet().stream()
                .map(Name::of)
                .collect(Collectors.toCollection(TreeSet::new));
        this.manifest = result0(jar::getManifest).get().getOrDefault(null);
        this.entryAttributeNames = Stream.ofNullable(manifest)
                .flatMap(manny -> manny.getEntries().entrySet().stream())
                .map(Fun.mapKey(Name::of))
                .map(Fun.mapValue(attrs -> attrs.keySet().stream()
                        .map(Objects::toString)
                        .map(Name::of)
                        .collect(Collectors.toCollection(TreeSet::new))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.bundleFacade = new BundleFacade(this);
    }

    public static Result<EntryMeta> fromResource(@NotNull Resource resource,
                                                 @Nullable Set<Name> attributeNames) {
        return result0(() -> {
            try (InputStream inputStream = resource.openInputStream()) {
                return (EntryMeta) new EntryMetaImpl(resource.lastModified(), resource.size(), resource.getExtra(),
                        SHA256.digest(inputStream).asHex(), attributeNames);
            }
        }).get();
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

    @Override
    public Set<Name> getEntryNames() {
        return Set.copyOf(names);
    }

    @Override
    public Set<Name> getDirNames() {
        return Set.copyOf(dirNames);
    }

    @Override
    public Set<Name> getEntryAttributeNames(@NotNull Name name) {
        return Optional.ofNullable(entryAttributeNames.get(name)).map(Set::copyOf).orElse(null);
    }

    @Override
    public Optional<Result<EntryMeta>> getEntryMeta(@NotNull Name name) {
        return names.contains(name) ? Optional.of(resourceCache.computeIfAbsent(name, namePath -> {
            final String nameString = namePath.toString();
            return Optional.ofNullable(jar.getResource(nameString))
                    .map(Result::success)
                    .orElseGet(() -> Result.failure(new NullPointerException("no resource for name " + nameString)))
                    .flatMap(resource -> OpenJarImpl.fromResource(resource, getEntryAttributeNames(name)));
        })) : Optional.empty();
    }

    @Override
    @Nullable
    public Manifest getManifest() {
        return Optional.ofNullable(manifest).map(Manifest::new).orElse(null);
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

    @Override
    @Nullable
    public String getMainAttributeValue(final @NotNull Name attribute) {
        return Optional.ofNullable(getManifest())
                .flatMap(manny ->
                        result0(() -> manny.getMainAttributes()
                                .getValue(attribute.getSegment()))
                                .get().toOptional())
                .orElse(null);
    }

    @Override
    public @Nullable String getEntryAttributeValue(@NotNull Name entryName, @NotNull Name attribute) {
        return Optional.ofNullable(getManifest())
                .flatMap(manny ->
                        result0(() -> Optional.ofNullable(manny.getAttributes(entryName.toString()))
                                .orElse(manny.getMainAttributes())
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
    @SuppressWarnings("unchecked")
    public <T> @Nullable T adaptTo(@NotNull Class<T> adapter) {
        if (Bundle.class.equals(adapter) && isBundle()) {
            return (T) getBundle();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        this.jar.close();
    }

    @NotNull
    public static OpenJarImpl fromFile(@Nullable String name,
                                       @NotNull Path path,
                                       @NotNull Map<Name, Result<EntryMeta>> resourceCache) throws Exception {
        return new OpenJarImpl(Jar.fromResource(name, new FileResource(path)), resourceCache);
    }

}
