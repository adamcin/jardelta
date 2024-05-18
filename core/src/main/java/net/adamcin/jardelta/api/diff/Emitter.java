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

import net.adamcin.jardelta.api.Kind;
import net.adamcin.jardelta.api.Name;
import net.adamcin.streamsupport.Both;
import net.adamcin.streamsupport.Result;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Interface for a type that emits diffs of a particular {@link net.adamcin.jardelta.api.Kind}.
 */
@ProviderType
public interface Emitter {

    /**
     * Get the kind of diff being emitted, which will be the value of
     * {@link net.adamcin.jardelta.api.diff.Diff#getKind()} for any diffs produced by this emitter.
     *
     * @return the kind of the diff being emitted
     */
    @NotNull
    Kind getKind();

    /**
     * Return a new emitter of the given sub-kind of this emitter.
     *
     * @param kind the sub-kind to emit for
     * @return an emitter of a sub-kind of diffs
     */
    @NotNull
    Emitter ofSubKind(@NotNull Kind kind);

    /**
     * Get the name of the element being diffed, which will be the value of
     * {@link net.adamcin.jardelta.api.diff.Diff#getName()} for any diffs produced by this emitter.
     *
     * @return the name of the element being diffed
     */
    @NotNull
    Name getName();

    /**
     * Return a new emitter for the given name.
     *
     * @param name the element name to emit for
     * @return an emitter for a child element
     */
    @NotNull
    Emitter forName(@NotNull Name name);

    /**
     * Emit a diff for an {@link net.adamcin.jardelta.api.diff.Action#ADDED} action.
     *
     * @return an ADDED diff
     */
    @NotNull
    Diff added();

    /**
     * Emit a diff for an {@link net.adamcin.jardelta.api.diff.Action#ADDED} action.
     *
     * @param hint a hint of the added value
     * @return an ADDED diff
     */
    @NotNull
    default Diff added(@NotNull String hint) {
        return added();
    }

    /**
     * Emit a diff for a {@link net.adamcin.jardelta.api.diff.Action#REMOVED} action.
     *
     * @return a REMOVED diff
     */
    @NotNull
    Diff removed();

    /**
     * Emit a diff for a {@link net.adamcin.jardelta.api.diff.Action#REMOVED} action.
     *
     * @param hint a hint of the removed value
     * @return a REMOVED diff
     */
    @NotNull
    default Diff removed(@NotNull String hint) {
        return removed();
    }

    /**
     * Emit a diff for a {@link net.adamcin.jardelta.api.diff.Action#CHANGED} action.
     *
     * @return a CHANGED diff
     */
    @NotNull
    Diff changed();

    /**
     * Emit a diff for a {@link net.adamcin.jardelta.api.diff.Action#CHANGED} action.
     *
     * @param hints both hints of the divergent values
     * @return a CHANGED diff
     */
    @NotNull
    default Diff changed(@NotNull Both<String> hints) {
        return changed();
    }

    /**
     * Emit a diff for an {@link net.adamcin.jardelta.api.diff.Action#ERR_LEFT} action.
     *
     * @param failure the failed result for logging
     * @return an ERR_LEFT diff
     */
    @NotNull
    Diff errLeft(@NotNull Result<?> failure);

    /**
     * Emit a diff for an {@link net.adamcin.jardelta.api.diff.Action#ERR_RIGHT} action.
     *
     * @param failure the failed result for logging
     * @return an ERR_RIGHT diff
     */
    @NotNull
    Diff errRight(@NotNull Result<?> failure);

    /**
     * Convenience method to transform both results into a stream of diffs emitted by respective calls to
     * {@link #errLeft(net.adamcin.streamsupport.Result)} and {@link #errRight(net.adamcin.streamsupport.Result)}
     * if either are failures. If both results are successful a both of empty values is returned.
     *
     * @param results possible failure results
     * @return a diff stream
     */
    @NotNull
    default <T> Stream<Diff> errBoth(@NotNull Both<Result<T>> results) {
        return errBothOptionals(results.map(Optional::of));
    }

    /**
     * Convenience method to transform both optional results into a stream of diffs emitted by respective calls to
     * {@link #errLeft(net.adamcin.streamsupport.Result)} and {@link #errRight(net.adamcin.streamsupport.Result)}
     * if either are failures.
     *
     * @param results possible failure results
     * @return a diff stream
     */
    @NotNull
    default <T> Stream<Diff> errBothOptionals(@NotNull Both<Optional<Result<T>>> results) {
        return results
                .flatMap((left, right) -> Both.of(left.filter(Result::isFailure).map(this::errLeft),
                        right.filter(Result::isFailure).map(this::errRight)))
                .stream().flatMap(Optional::stream);
    }

    /**
     * Return an appropriate emitter for a child element. This method may return this same emitter when appropriate.
     *
     * @param childName the name segment of the child
     * @return an emitter for a child element
     */
    @NotNull
    default Emitter forChild(@NotNull String childName) {
        return forName(getName().appendSegment(childName));
    }

    /**
     * Return an appropriate emitter for a provided sub-element.
     *
     * @param subElement the sub-element to emit for
     * @return an emitter for the provided sub-element
     */
    @NotNull
    default Emitter forSubElement(@NotNull Element<?> subElement) {
        if (getName().isRoot() || subElement.name().startsWithName(getName())) {
            return forName(subElement.name());
        } else {
            return forChild(subElement.name().getSegment());
        }
    }

}
