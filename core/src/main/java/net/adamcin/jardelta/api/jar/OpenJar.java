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

package net.adamcin.jardelta.api.jar;

import net.adamcin.jardelta.api.Name;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Having opened a jar file, this type provides access to the names and entries that it contains.
 */
public interface OpenJar {
    Set<Name> getEntryNames();
    Set<Name> getDirNames();
    Optional<Result<EntryMeta>> getEntryMeta(@NotNull Name name);
    @Nullable Manifest getManifest();
    @Nullable String getMainAttributeValue(@NotNull Name attribute);
    @Nullable Set<Name> getEntryAttributeNames(@NotNull Name name);
    @Nullable String getEntryAttributeValue(@NotNull Name entryName, @NotNull Name attribute);
    <T> @Nullable T adaptTo(@NotNull Class<T> adapter);
}
