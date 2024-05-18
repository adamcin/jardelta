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
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A hierarchical path name capable of representing JAR resource/entry names and virtual resource names.
 */
@EqualsAndHashCode
public final class Name implements Comparable<Name>, Serializable {
    public static final Name ROOT = new Name("");
    static final String ERROR_PREFIX_UNEXPECTED_RIGHT_BRACKET = "Name segment contains unexpected '}' bracket: ";
    static final String ERROR_PREFIX_UNTERMINATED_LEFT_BRACKET = "Name segment contains unterminated '{' bracket: ";

    /**
     * Return a {@link Name} for the provided path value.
     *
     * @param value a resource name string
     * @return a {@link Name} for the provided value
     * @throws java.lang.IllegalArgumentException if provided value is not acceptable as a name
     */
    @NotNull
    public static Name of(@NotNull String value) {
        return of(value, value, 0);
    }

    /**
     * Return a {@link Name} for the provided path segment. It will be wrapped with curly braces if it contains a
     * possibly-delimiting slash.
     *
     * @param segment a resource name segment
     * @return a {@link Name} for the provided segment
     * @throws java.lang.IllegalArgumentException if provided value is not acceptable as a name
     */
    @NotNull
    public static Name ofSegment(@NotNull String segment) {
        if (segment.isEmpty()) {
            return ROOT;
        }
        return findUnbracketedSlash(segment, segment, 0) >= 0
                ? Name.of("{" + segment + "}")
                : Name.of(segment);
    }

    /**
     * Return a {@link Name} for the provided path value.
     *
     * @param value a resource name string
     * @return a {@link Name} for the provided value
     * @throws java.lang.IllegalArgumentException if provided value is not acceptable as a name
     */
    static Name of(@NotNull String value,
                   final @NotNull String debugOriginal,
                   final int debugStart) {
        if (value.isEmpty()) {
            return ROOT;
        }
        if (value.startsWith("/")) {
            return of(value.substring(1), debugOriginal, debugStart + 1);
        }
        int slash = findUnbracketedSlash(value, debugOriginal, debugStart);
        if (slash >= 0) {
            return of(value.substring(0, slash), debugOriginal, debugStart)
                    .append(of(value.substring(slash + 1), debugOriginal, debugStart + slash + 1));
        }
        return new Name(value);
    }

    private final Name parent;

    @NonNull
    private final String segment;

    private Name(@NotNull String segment) {
        this(null, segment);
    }

    private Name(@Nullable Name parent, @NotNull String segment) {
        // empty segments are not allowed to have parents, and root is not allowed to be a parent
        assert parent == null || !segment.isEmpty() && !parent.isRoot();
        this.parent = parent;
        this.segment = segment;
    }

    @Nullable
    public Name getParent() {
        return this.parent;
    }

    @NotNull
    public String getSegment() {
        return segment;
    }

    @NotNull
    public Stream<String> segments() {
        return stream().map(Name::getSegment);
    }

    @NotNull
    public Stream<Name> stream() {
        return Stream.concat(Stream.ofNullable(parent).flatMap(Name::stream), Stream.of(this));
    }

    @NotNull
    public Name append(@NotNull Name child) {
        if (this.isRoot()) {
            return child;
        } else if (child.isRoot()) {
            return this;
        } else {
            return new Name(Optional.ofNullable(child.getParent())
                    .map(this::append)
                    .orElse(this), child.segment);
        }
    }

    /**
     * Append a child name with the given value. It will be wrapped with curly braces if it contains a possibly-delimiting
     * slash.
     *
     * @param childSegment the child name segment
     * @return a {@link Name} whose parent is this
     * @see #ofSegment(String)
     */
    @NotNull
    public Name appendSegment(@NotNull String childSegment) {
        return append(ofSegment(childSegment));
    }

    /**
     * Returns true if the name has no parent, and its filename is the empty string. Such a name is pretty much useless,
     * but it is allowed to be constructed because it maps to the root path, which is often treated as a unique
     * placeholder value for all immediate children of a jar.
     *
     * @return true if the name has no parent, and its filename is the empty string
     */
    public boolean isRoot() {
        return parent == null && segment.isEmpty();
    }

    /**
     * Get the depth of this name segment. A segment with no parent has a depth of 0.
     *
     * @return depth of this name
     */
    public int getDepth() {
        if (parent == null) {
            return 0;
        } else {
            return parent.getDepth() + 1;
        }
    }

    /**
     * Tests if this name starts with the given name. This is performed by testing {@code otherName} for equality with
     * this OR (recursively) if the name returned by this name's {@link #getParent()} starts with {@code otherName}.
     *
     * @param otherName the candidate ancestor path name
     * @return true if this name is a descendant of otherName
     */
    public boolean startsWithName(@NotNull Name otherName) {
        return this.equals(otherName)
                || Optional.ofNullable(getParent())
                .map(parent -> parent.startsWithName(otherName))
                .orElse(false);
    }

    /**
     * Tests if this name segment starts with the given name using a pure string comparison.
     *
     * @param pattern the candidate ancestor path name
     * @return true if {@code otherName} is a string prefix for this name as a string
     */
    public boolean startsWith(@NotNull String pattern) {
        return this.segment.startsWith(pattern);
    }

    /**
     * Tests if this name ends with the given name. This is performed by testing {@code otherName} for equality with
     * this OR {@code otherName} is a directory, and (recursively) if the name returned by this name's
     * {@link #getParent()} starts with {@code otherName}.
     *
     * @param otherName the candidate suffix path name
     * @return true if {@code otherName} is a relative path for this name against one of its parent names
     */
    public boolean endsWithName(@NotNull Name otherName) {
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
     * @param pattern the candidate suffix path name
     * @return true if {@code otherName} is a string suffix for this name as a string
     */
    public boolean endsWith(@NotNull String pattern) {
        return this.segment.endsWith(pattern);
    }

    @Override
    public int compareTo(@NotNull Name that) {
        if (this.parent != null || that.parent != null) {
            if (this.getDepth() < that.getDepth()) {
                int parentResult = this.compareTo(Objects.requireNonNull(that.parent));
                return parentResult == 0 ? -1 : parentResult;
            } else if (this.getDepth() > that.getDepth()) {
                int parentResult = Objects.requireNonNull(this.parent).compareTo(that);
                return parentResult == 0 ? 1 : parentResult;
            } else {
                int parentResult = this.parent.compareTo(that.parent);
                if (parentResult != 0) {
                    return parentResult;
                }
            }
        }
        return this.segment.compareTo(that.segment);
    }

    @Override
    public String toString() {
        if (parent == null) {
            return segment;
        }
        return segments().collect(Collectors.joining("/"));
    }

    /**
     * Finds the first slash in the provided {@code value} that is not surrounded by balanced curly brackets.
     *
     * @param value         the haystack
     * @param debugOriginal the original input string for debugging, of which {@code value} is a substring
     * @param debugStart    the index in the input string where {@code value} begins errorOffset relative to the
     *                      original input string
     * @return the index of the first unbracketed slash, or -1 if such a slash is not found
     * @throws java.lang.IllegalArgumentException if a left-hand bracket is unterminated, or if a right-hand bracket is unexpected
     */
    static int findUnbracketedSlash(final @NotNull String value,
                                    final @NotNull String debugOriginal,
                                    final int debugStart) {
        assert debugOriginal.isEmpty()
                || debugOriginal.length() > debugStart && debugOriginal.substring(debugStart).startsWith(value);
        final Deque<Integer> stack = new ArrayDeque<>();
        final char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case '{':
                    stack.push(i);
                    break;
                case '}':
                    if (stack.isEmpty()) {
                        final String message = ERROR_PREFIX_UNEXPECTED_RIGHT_BRACKET + "'" + debugOriginal + "'";
                        throw new IllegalArgumentException(message, new ParseException(message, debugStart + i));
                    } else {
                        stack.pop();
                    }
                    break;
                case '/':
                    if (stack.isEmpty()) {
                        return i;
                    }
            }
        }
        if (stack.isEmpty()) {
            return -1;
        } else {
            final String message = ERROR_PREFIX_UNTERMINATED_LEFT_BRACKET + "'" + debugOriginal + "'";
            throw new IllegalArgumentException(message, new ParseException(message, debugStart + stack.peek()));
        }
    }
}
