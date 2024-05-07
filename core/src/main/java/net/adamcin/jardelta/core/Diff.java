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
import lombok.ToString;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a difference between two jar resources.
 */
@EqualsAndHashCode
@ToString
public class Diff implements Comparable<Diff> {
    @NonNull
    private final Name name;
    @NonNull
    private final String kind;
    @NonNull
    private final Action action;

    private Diff(@NotNull Name name,
                 @NotNull String kind,
                 @NotNull Action action) {
        this.name = name;
        this.kind = kind;
        this.action = action;
    }

    /**
     * Get the name of the resource within the jars being compared.
     *
     * @return the resource name
     */
    public @NotNull Name getName() {
        return name;
    }

    /**
     * Get the {@link Action} represented by this diff.
     *
     * @return the {@link Action} represented by this diff
     */
    public @NotNull Action getAction() {
        return action;
    }

    /**
     * Return the kind, or category, of difference. This is used for grouping and optional runtime exclusion.
     *
     * @return the kind of difference
     */
    public @NotNull String getKind() {
        return kind;
    }

    @Override
    public int compareTo(@NotNull Diff other) {
        int byName = this.name.compareTo(other.name);
        int byKind = this.kind.compareTo(other.kind);
        return byName != 0 ? byName : (byKind != 0 ? byKind : this.action.compareTo(other.action));
    }

    public static final class Builder {

        private final String kind;
        private Name name = Name.ROOT;

        public Builder(@NotNull String kind) {
            this.kind = kind;
        }

        public Builder named(@NotNull Name name) {
            this.name = name;
            return this;
        }

        @NotNull
        public Name name() {
            return this.name;
        }

        @NotNull
        public Diff build(@NotNull Action action) {
            return new Diff(name, kind, action);
        }

        @NotNull
        public Diff added() {
            return build(Action.ADDED);
        }

        @NotNull
        public Diff removed() {
            return build(Action.REMOVED);
        }

        @NotNull
        public Diff changed() {
            return build(Action.CHANGED);
        }

        @NotNull
        public Diff errLeft(Result<?> failure) {
            return build(Action.ERR_LEFT);
        }

        @NotNull
        public Diff errRight(Result<?> failure) {
            return build(Action.ERR_RIGHT);
        }

        @NotNull
        public Builder child(@NotNull String childName) {
            return new Builder(this.kind).named(name.appendSegment(childName));
        }
    }

    @NotNull
    public static Builder builder(@NotNull String kind) {
        return new Builder(kind);
    }
}
