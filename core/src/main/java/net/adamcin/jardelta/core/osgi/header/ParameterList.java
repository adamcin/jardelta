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

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import net.adamcin.jardelta.api.diff.SetDiffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ParameterList {
    private final String key;
    private final List<Attrs> attrList;
    private final Set<String> allAttrs;

    private ParameterList(@NotNull String key, @NotNull List<Attrs> attrList) {
        this.key = key;
        this.attrList = List.copyOf(attrList);
        this.allAttrs = Set.copyOf(this.attrList.stream().map(Attrs::keySet)
                .reduce(new HashSet<>(), SetDiffer::mergeSets, SetDiffer::mergeSets));
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public List<Attrs> getAttrList() {
        return attrList;
    }

    @NotNull
    public Set<String> getAllAttrs() {
        return allAttrs;
    }

    @NotNull
    public Stream<String> attrsToStrings(@NotNull Set<String> names) {
        return attrList.stream().map(attrs -> {
            final StringBuilder sb = new StringBuilder(key);
            names.forEach(name -> {
                sb.append(';').append(name).append('=');
                OSGiHeader.quote(sb, attrs.get(name, ""));
            });
            return sb.toString();
        });
    }

    @Override
    public String toString() {
        return attrList.stream().map(attrs -> key + attrs.toString()).collect(Collectors.joining(","));
    }

    public boolean isEqual(final @Nullable ParameterList other) {
        if (this == other) {
            return true;
        }

        if (other == null || !this.key.equals(other.key) || attrList.size() != other.attrList.size()) {
            return false;
        }

        if (attrList.isEmpty()) {
            return true;
        }

        for (int i = 0; i < attrList.size(); i++) {
            if (!attrList.get(i).isEqual(other.attrList.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    public static ParameterList fromDuplicates(final @NotNull String key, final @NotNull Parameters parameters) {
        if (!parameters.containsKey(key)) {
            return null;
        }
        final List<Attrs> attrList = new ArrayList<>();
        String scanKey = key;
        while (parameters.containsKey(scanKey)) {
            attrList.add(parameters.get(scanKey));
            scanKey = scanKey + "~";
        }
        return new ParameterList(key, attrList);
    }

    @Nullable
    public static ParameterList fromAliases(final @NotNull String key, final @NotNull Parameters parameters) {
        final List<Map.Entry<String, Attrs>> entries = parameters.entrySet().stream()
                .filter(entry -> key.equals(entry.getKey()) || entry.getKey().startsWith(key + "="))
                .collect(Collectors.toList());
        if (entries.isEmpty()) {
            return null;
        }
        return new ParameterList(key, entries.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
    }

    @NotNull
    public static ParameterList just(final @NotNull String key) {
        return new ParameterList(key, List.of(new Attrs()));
    }
}
