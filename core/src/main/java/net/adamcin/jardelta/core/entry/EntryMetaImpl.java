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

package net.adamcin.jardelta.core.entry;

import net.adamcin.jardelta.api.Name;
import net.adamcin.jardelta.api.jar.EntryMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class EntryMetaImpl implements EntryMeta {
    private final long lastModified;
    private final long size;
    private final String extra;
    private final String sha256;
    private final Set<Name> attributeNames;

    public EntryMetaImpl(long lastModified,
                         long size,
                         @Nullable String extra,
                         @NotNull String sha256,
                         @Nullable Set<Name> attributeNames) {
        this.lastModified = lastModified;
        this.size = size;
        this.extra = extra;
        this.sha256 = sha256;
        this.attributeNames = attributeNames == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new TreeSet<>(attributeNames));
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    @Nullable
    public String getExtra() {
        return extra;
    }

    @Override
    @NotNull
    public String getSha256() {
        return sha256;
    }

    @Override
    public @NotNull Set<Name> getAttributeNames() {
        return this.attributeNames;
    }
}
