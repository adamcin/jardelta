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

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

import java.util.stream.Stream;

/**
 * Describes a {@link java.util.function.BiFunction} which accepts a {@link Emitter}
 * and an {@link Element} to produce a stream of {@link net.adamcin.jardelta.api.diff.Diff}s.
 *
 * @param <T> the element type
 */
@ConsumerType
@FunctionalInterface
public interface Differ<T extends Element<?>> {

    /**
     * Emit a stream of zero-to-many diffs computed for the given element. An implementing type may use the provided
     * emitter if appropriate, or it may use an internal emitter if required.
     *
     * @param baseEmitter a base emitter provided by the caller
     * @param element the element being diffed
     * @return a diff stream
     */
    @NotNull
    Stream<Diff> diff(@NotNull Emitter baseEmitter, @NotNull T element);
}
