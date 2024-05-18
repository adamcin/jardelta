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

package net.adamcin.jardelta.api;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * A hierarchical name capable of representing kinds of diffs á la namespaces.
 */
@EqualsAndHashCode
public final class Kind implements Comparable<Kind>, Serializable {
    public static final Pattern VALID_KIND = Pattern.compile("^([A-Za-z_$][A-Za-z_$0-9]*)(\\.[A-Za-z_$][A-Za-z_$0-9]*)*$");

    /**
     * Return a {@link net.adamcin.jardelta.api.Kind} for the provided value. The value must consist of characters
     * allowed for Java language identifiers, with multiple segments being separated by dots.
     *
     * @param value a kind string
     * @return a {@link net.adamcin.jardelta.api.Kind} for the provided value
     * @throws IllegalArgumentException if provided value is not acceptable as a kind
     */
    @NotNull
    public static Kind of(@NotNull String value) {
        return new Kind(value);
    }

    /**
     * Persist the value with a terminating dot so that {@link #compareTo(Kind)} is more performant and so
     * {@link #hashCode()} and {@link #equals(Object)} use the same underlying string.
     */
    @NonNull
    private final String valueDot;

    private Kind(@NotNull String value) {
        if (!VALID_KIND.matcher(value).matches()) {
            throw new IllegalArgumentException("Kind strings must match pattern '" + VALID_KIND.pattern() + "' : " + value);
        }
        this.valueDot = value + ".";
    }

    /**
     * Get the kind value as a string.
     *
     * @return the underlying value
     */
    @NotNull
    public String getValue() {
        return valueDot.substring(0, valueDot.length() - 1);
    }

    /**
     * Create a sub-kind of this kind by appending a dot followed by the provided suffix.
     *
     * @param suffix the suffix kind to append
     * @return a {@link net.adamcin.jardelta.api.Kind} whose parent is this
     */
    @NotNull
    public Kind subKind(@NotNull Kind suffix) {
        return Kind.of(valueDot + suffix.getValue());
    }

    /**
     * Tests if this kind is the same or a sub-kind of the given kind.
     *
     * @param that the candidate super-kind
     * @return true if this kind is a sub-kind of that kind
     */
    public boolean isSubKindOf(@NotNull Kind that) {
        return this.equals(that) || this.valueDot.startsWith(that.valueDot);
    }

    @Override
    public int compareTo(@NotNull Kind that) {
        return this.valueDot.compareTo(that.valueDot);
    }

    @Override
    public String toString() {
        return getValue();
    }
}
