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

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ParameterList {
    private final String key;
    private final List<Attrs> attrList;

    private ParameterList(@NotNull String key, @NotNull List<Attrs> attrList) {
        this.key = key;
        this.attrList = List.copyOf(attrList);
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public List<Attrs> getAttrList() {
        return attrList;
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
    public static ParameterList from(final @NotNull String key, final @NotNull Parameters parameters) {
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
}
