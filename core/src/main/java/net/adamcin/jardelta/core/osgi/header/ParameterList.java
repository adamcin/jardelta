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
import aQute.bnd.header.Parameters;
import net.adamcin.jardelta.api.diff.SetDiffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ParameterList {
    private final String key;
    private final List<AttrsEntry> attrsList;
    private final Set<String> allAttrs;

    private ParameterList(@NotNull String key, @NotNull List<Attrs> attrsList) {
        this.key = key;
        this.attrsList = attrsList.stream().map(AttrsEntry::new).collect(Collectors.toList());
        this.allAttrs = Set.copyOf(this.attrsList.stream().map(AttrsEntry::attrNames)
                .reduce(new HashSet<>(), SetDiffer::mergeSets, SetDiffer::mergeSets));
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public List<AttrsEntry> getAttrsList() {
        return attrsList;
    }

    public final class AttrsEntry {
        private final Attrs attrs;
        private final String normalized;

        private AttrsEntry(@NotNull Attrs attrs) {
            this.attrs = attrs;
            Attrs normalAttrs = new Attrs();
            attrs.keySet().stream().sorted().forEach(name -> {
                Attrs.Type type = attrs.getType(name);
                normalAttrs.put(name, type, attrs.get(name));
            });
            this.normalized = normalAttrs.toString();
        }

        public @NotNull String getKey() {
            return ParameterList.this.key;
        }

        public Set<String> attrNames() {
            return attrs.keySet();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AttrsEntry that = (AttrsEntry) o;
            return getKey().equals(that.getKey()) && this.attrs.isEqual(that.attrs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(toString());
        }

        @Override
        public String toString() {
            return ParameterList.this.key + (attrs.isEmpty() ? "" : ";") + toAttrsString();
        }

        public String toAttrsString() {
            return normalized;
        }
    }

    @NotNull
    public Set<String> getAllAttrs() {
        return allAttrs;
    }

    @Override
    public String toString() {
        return attrsList.stream().map(AttrsEntry::toString).collect(Collectors.joining(","));
    }

    public boolean isEqual(final @Nullable ParameterList other) {
        if (this == other) {
            return true;
        }

        if (other == null || !this.key.equals(other.key) || attrsList.size() != other.attrsList.size()) {
            return false;
        }

        if (attrsList.isEmpty()) {
            return true;
        }

        for (int i = 0; i < attrsList.size(); i++) {
            if (!attrsList.get(i).equals(other.attrsList.get(i))) {
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
