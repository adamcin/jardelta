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

package net.adamcin.jardiff.core;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Levels of severity for violations detected during package scans.
 */
public enum Significance {
    /**
     * SHOULD NOT have an effect on application functionality. Appropriate for reporting differences in
     * inert metadata or embedded documentation.
     */
    TRIVIAL(2),

    /**
     * May introduce a change in application behavior. Appropriate for reporting minor version bumps or additional
     * elements, or for more significant gaps in non-functional code like metatype descriptions. Should guide code
     * reviewers.
     */
    MINOR(1),

    /**
     * Likely to be the source of platform instability. Appropriate for reporting cross-package filter
     * overlap, destructive ACL handling modes, destruction of authorable content, or security violations.
     */
    MAJOR(0);

    private final int ignorability;

    Significance(int ignorability) {
        this.ignorability = ignorability;
    }

    /**
     * Runtime throwing function to lookup significance codes by name.
     *
     * @param name the significance level name
     * @return the associated significance level
     */
    public static Significance byName(final @NotNull String name) {
        for (Significance value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown significance level: " + name);
    }

    public boolean isLessSignificantThan(Significance other) {
        return this.ignorability > other.ignorability;
    }

    public Predicate<Significance> meetsMinimumSignificance() {
        return other -> !other.isLessSignificantThan(this);
    }

    public Significance maxSignificance(final @NotNull Significance other) {
        return this.isLessSignificantThan(other) ? other : this;
    }
}
