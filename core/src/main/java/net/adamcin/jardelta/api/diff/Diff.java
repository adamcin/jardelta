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

package net.adamcin.jardelta.api.diff;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.Name;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Optional;

/**
 * Represents a difference between two jar resources.
 */
@EqualsAndHashCode(exclude = "hints")
@ToString
public final class Diff implements Comparable<Diff>, Serializable {
    public static final Both<Optional<String>> NO_HINTS = Both.empty();

    @NonNull
    private final Name name;
    @NonNull
    private final Kind kind;
    @NonNull
    private final Action action;
    @NonNull
    private final Both<Optional<String>> hints;

    private Diff(@NotNull Name name,
                 @NotNull Kind kind,
                 @NotNull Action action,
                 @NotNull Both<Optional<String>> hints) {
        this.name = name;
        this.kind = kind;
        this.action = action;
        this.hints = hints;
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
    public @NotNull Kind getKind() {
        return kind;
    }

    /**
     * Return both of the hints of divergent values, which may be formatted for presentation in reports, but which will
     * not be considered for refinement.
     *
     * @return hints of divergent values
     */
    public @NotNull Both<Optional<String>> getHints() {
        return hints;
    }

    @Override
    public int compareTo(@NotNull Diff other) {
        int byName = this.name.compareTo(other.name);
        int byKind = this.kind.compareTo(other.kind);
        return byName != 0 ? byName : (byKind != 0 ? byKind : this.action.compareTo(other.action));
    }

    static final class Builder implements Emitter {

        private final Kind kind;
        private Name name = Name.ROOT;

        public Builder(@NotNull Kind kind) {
            this.kind = kind;
        }

        public Builder named(@NotNull Name name) {
            this.name = name;
            return this;
        }

        @Override
        public @NotNull Kind getKind() {
            return this.kind;
        }

        @Override
        @NotNull
        public Name getName() {
            return this.name;
        }

        @NotNull
        Diff build(@NotNull Action action) {
            return build(action, NO_HINTS);
        }

        @NotNull
        Diff build(@NotNull Action action, @NotNull Both<Optional<String>> hints) {
            return new Diff(name, kind, action, hints);
        }

        @Override
        @NotNull
        public Diff added() {
            return build(Action.ADDED);
        }

        @Override
        public @NotNull Diff added(@NotNull String hint) {
            return build(Action.ADDED, Both.ofNullables(null, hint));
        }

        @Override
        @NotNull
        public Diff removed() {
            return build(Action.REMOVED);
        }

        @Override
        public @NotNull Diff removed(@NotNull String hint) {
            return build(Action.REMOVED, Both.ofNullables(hint, null));
        }

        @Override
        @NotNull
        public Diff changed() {
            return build(Action.CHANGED);
        }

        @Override
        public @NotNull Diff changed(@NotNull Both<String> hints) {
            return build(Action.CHANGED, hints.map(Optional::of));
        }

        @Override
        @NotNull
        public Diff errLeft(@NotNull Result<?> failure) {
            return build(Action.ERR_LEFT, Both.ofNullables(errHint(failure), null));
        }

        @Override
        @NotNull
        public Diff errRight(@NotNull Result<?> failure) {
            return build(Action.ERR_RIGHT, Both.ofNullables(null, errHint(failure)));
        }

        @Nullable
        String errHint(@NotNull Result<?> result) {
            Optional<RuntimeException> err = result.getError();
            return err.map(topErr -> {
                if (topErr.getMessage() != null && !topErr.getMessage().isEmpty()) {
                    return topErr.getMessage();
                } else if (topErr.getCause() != null) {
                    Throwable cause = topErr.getCause();
                    if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                        return cause.getMessage();
                    }
                    return cause.getClass().getSimpleName();
                }
                return topErr.getClass().getSimpleName();
            }).orElse(null);
        }

        @Override
        public @NotNull Emitter ofSubKind(@NotNull Kind kind) {
            if (kind.isSubKindOf(this.kind)) {
                return new Builder(kind).named(this.name);
            } else {
                return new Builder(this.kind.subKind(kind)).named(this.name);
            }
        }

        @Override
        public @NotNull Emitter forName(@NotNull Name name) {
            return new Builder(this.kind).named(name);
        }
    }

    @NotNull
    public static Emitter emitterOf(@NotNull Kind kind) {
        return new Builder(kind);
    }

}
