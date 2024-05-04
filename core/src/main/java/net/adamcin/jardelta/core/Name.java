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

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A JAR resource/entry name represented by a marker type wrapping a string. A name representing a directory entry ends
 * with a "/".
 */
@EqualsAndHashCode
public final class Name implements Serializable, Comparable<Name> {

    /**
     * A normalized path contains no double-slashes and no ./ or ../ path segments.
     */
    private static final Pattern ILLEGAL_SEGMENTS = Pattern.compile("((^|/)\\.\\.?(/|$)|(//))");

    @NonNull
    private final String value;

    private Name(@NotNull String value) {
        this.value = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    @Nullable
    public Name getParent() {
        final int lastSlash = value.lastIndexOf("/");
        if (lastSlash >= 0) {
            return new Name(value.substring(0, lastSlash));
        }
        // do not return an empty name
        return null;
    }

    @NotNull
    public Name getFileName() {
        final int lastSlash = value.lastIndexOf("/");
        if (lastSlash >= 0) {
            return new Name(value.substring(lastSlash + 1));
        }
        // do not return an empty name
        return this;
    }

    @NotNull
    public Name append(@NotNull Name child) {
        return this.isEmpty() ? child : Name.of(value + "/" + child.value);
    }

    @NotNull
    public Name append(@NotNull String childValue) {
        return Name.of(this.isEmpty() ? childValue : value + "/" + childValue);
    }

    /**
     * Returns true if the name has no parent, and its filename is the empty string. Such a name is pretty much useless,
     * but it is allowed to be constructed because it maps to the root path, which is often treated as a unique
     * placeholder value for all immediate children of a jar.
     *
     * @return true if the name has no parent, and its filename is the empty string
     */
    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * Tests if this name starts with the given name. This is performed by testing {@code otherName} for equality with
     * this OR (recursively) if the name returned by this name's {@link #getParent()} starts with {@code otherName}.
     *
     * @param otherName the candidate ancestor path name
     * @return true if this name is a descendant of otherName
     */
    public boolean startsWith(@NotNull Name otherName) {
        return this.equals(otherName)
                || Optional.ofNullable(getParent())
                .map(parent -> parent.startsWith(otherName))
                .orElse(false);
    }

    /**
     * Tests if this name starts with the given name using a pure string comparison.
     *
     * @param otherName the candidate ancestor path name
     * @return true if {@code otherName} is a string prefix for this name as a string
     */
    public boolean startsWith(@NotNull String otherName) {
        return this.value.startsWith(otherName);
    }

    /**
     * Tests if this name ends with the given name. This is performed by testing {@code otherName} for equality with
     * this OR {@code otherName} is a directory, and (recursively) if the name returned by this name's
     * {@link #getParent()} starts with {@code otherName}.
     *
     * @param otherName the candidate suffix path name
     * @return true if {@code otherName} is a relative path for this name against one of its parent names
     */
    public boolean endsWith(@NotNull Name otherName) {
        if (this.equals(otherName)) {
            return true;
        } else {
            Name parent = this.getParent();
            while (parent != null) {
                if (this.equals(parent.append(otherName))) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        }
    }

    /**
     * Tests if this name ends with the given name using a pure string comparison.
     *
     * @param otherName the candidate suffix path name
     * @return true if {@code otherName} is a string suffix for this name as a string
     */
    public boolean endsWith(@NotNull String otherName) {
        return this.value.endsWith(otherName);
    }

    @Override
    public int compareTo(@NotNull Name other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Return a {@link Name} for the provided normalized, relative path value. A value is
     * normalized when it has no empty path segments (double-slash) and no ./ or ../ path segments. A name may not
     * contain the "!/" string representing a "jar:" url entry name delimiter.
     *
     * @param value a resource name string
     * @return a {@link Name} for the provided value
     * @throws java.lang.IllegalArgumentException if provided value is not relative or normalized
     */
    @NotNull
    public static Name of(@NotNull String value) {
        if ("/".equals(value)) {
            // special case
            return new Name("");
        } else if (ILLEGAL_SEGMENTS.matcher(value).find()) {
            throw new IllegalArgumentException("Name must be in normalized form '" + value + "'");
        } else if (value.startsWith("/") || value.contains("!/")) {
            throw new IllegalArgumentException("Name must be relative '" + value + "'");
        } else {
            return new Name(value);
        }
    }
}
