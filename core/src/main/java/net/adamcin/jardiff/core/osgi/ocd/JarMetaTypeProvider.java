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

package net.adamcin.jardiff.core.osgi.ocd;

import net.adamcin.streamsupport.Fun;
import org.apache.felix.metatype.DefaultMetaTypeProvider;
import org.apache.felix.metatype.Designate;
import org.apache.felix.metatype.MetaData;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JarMetaTypeProvider extends DefaultMetaTypeProvider {
    public static final String DEFAULT_LOCALE = "";
    private URL source;
    private String localePrefix;

    public JarMetaTypeProvider(@NotNull Bundle bundle, @NotNull MetaData metadata) {
        super(bundle, metadata);
        this.source = metadata.getSource();
        this.localePrefix = Optional.ofNullable(metadata.getLocalePrefix()).orElseGet(() ->
                Optional.ofNullable(bundle.getHeaders().get(Constants.BUNDLE_LOCALIZATION))
                        .orElse(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME));
    }

    @NotNull
    public String getLocalePrefix() {
        return localePrefix;
    }

    public Set<String> getDesignatePids() {
        return ((List<Designate>) getDesignates()).stream()
                // exclude designates where pid is set and equal to its own factoryPid
                // this reproduces the implicit mapping in DefaultMetaTypeProvider.getDesignate(pid)
                // where the pid is checked against both the pid and factoryPid of each designate element, while not
                // ignoring if an OCD xml provides multiple designates for the same pid
                .filter(designate -> Objects.nonNull(designate.getPid())
                        && !designate.getPid().equals(designate.getFactoryPid()))
                .flatMap(designate -> Stream.ofNullable(designate.getPid()))
                .collect(Collectors.toSet());
    }

    public Set<String> getDesignateFactoryPids() {
        return ((List<Designate>) getDesignates()).stream()
                .flatMap(designate -> Stream.ofNullable(designate.getFactoryPid()))
                .collect(Collectors.toSet());
    }

    public Stream<MetaTypeDesignate> streamDesignates() {
        final Set<String> locales = Stream.concat(
                        Stream.of(DEFAULT_LOCALE),
                        Stream.ofNullable(getLocales())
                                .flatMap(Stream::of))
                .collect(Collectors.toSet());
        return Stream.concat(getDesignateFactoryPids().stream().map(pid -> getDesignatesForPid(locales, pid, true)),
                getDesignatePids().stream().map(pid -> getDesignatesForPid(locales, pid, false)));
    }

    private MetaTypeDesignate getDesignatesForPid(final @NotNull Set<String> locales, final @NotNull String pid, final boolean isFactory) {
        return new MetaTypeDesignate(pid, isFactory, locales, locales.stream()
                .flatMap(locale -> Stream.ofNullable(getObjectClassDefinition(pid, DEFAULT_LOCALE.equals(locale) ? null : locale))
                        .map(ocd -> Fun.toEntry(locale, ocd))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public URL getSource() {
        return source;
    }
}
