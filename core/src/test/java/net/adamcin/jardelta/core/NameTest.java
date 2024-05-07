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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NameTest {

    @Test
    void compareTo() {
        assertEquals(0, Name.of("").compareTo(Name.of("")));
        assertEquals(0, Name.of("a").compareTo(Name.of("a")));
        assertEquals(-1, Name.of("a/b/x").compareTo(Name.of("a/b/x/1")));
        assertEquals(1, Name.of("a/b/x/1").compareTo(Name.of("a/b/x")));
        assertEquals("a".compareTo("b"), Name.of("a").compareTo(Name.of("b")));
        assertEquals("b".compareTo("a"), Name.of("b").compareTo(Name.of("a")));
        assertEquals("x".compareTo("y"), Name.of("a/x").compareTo(Name.of("a/y")));
        assertEquals("y".compareTo("x"), Name.of("a/y").compareTo(Name.of("a/x")));
        assertEquals("a".compareTo("b"), Name.of("a/y").compareTo(Name.of("b/x")));
        assertEquals("b".compareTo("a"), Name.of("b/y").compareTo(Name.of("a/x")));
        assertEquals("y".compareTo("b"), Name.of("a/y").compareTo(Name.of("a/b/x")));
        assertEquals("b".compareTo("y"), Name.of("a/b/x").compareTo(Name.of("a/y")));
    }

    @Test
    void constructor() throws Exception {
        Constructor<Name> constructor = Name.class.getDeclaredConstructor(Name.class, String.class);
        constructor.setAccessible(true);
        // assert that the ROOT instance can be created
        final Name root = constructor.newInstance(null, "");
        final Name validName = constructor.newInstance(null, "a");
        // assert that an empty segment cannot be created as a child of a non-empty segment
        Throwable cause = assertThrows(InvocationTargetException.class,
                () -> constructor.newInstance(validName, "")).getCause();
        assertInstanceOf(AssertionError.class, cause);
        // assert that a non-empty segment cannot be created as a child of the root name.
        cause = assertThrows(InvocationTargetException.class,
                () -> constructor.newInstance(root, validName.getSegment())).getCause();
        assertInstanceOf(AssertionError.class, cause);
    }

    @Test
    void append_Name() {
        final Name empty = Name.of("");
        final Name abc = Name.of("a/b/c");
        final Name xyz = Name.of("x/y/z");
        assertEquals(Name.of("a/b/c/x/y/z"), abc.append(xyz));
        assertEquals(Name.of("a/b/x/y/z"), abc.getParent().append(xyz));
        assertEquals(Name.of("a/b/c/x/y"), abc.append(xyz.getParent()));
        assertEquals(Name.of("a/b/x/y"), abc.getParent().append(xyz.getParent()));
        assertEquals(Name.of("x/y/z/a/b/c"), xyz.append(abc));
        assertEquals(abc, abc.append(empty));
        assertEquals(abc, empty.append(abc));
        assertEquals(abc, abc.appendSegment(empty.getSegment()));
    }

    @Test
    void of_parseErrors() {
        Map<String, Integer> unterminated = Map.of(
                "{", 0,
                "{{}", 0,
                "{}{", 2,
                "{{{{{}}}}", 0,
                "/{", 1,
                "{/}/{", 4,
                "{/{}/", 0
        );
        Map<String, Integer> unexpected = Map.of(
                "}", 0,
                "}{", 0,
                "{}}", 2,
                "{{{{}}}}}", 8,
                "/}", 1,
                "{/}/}", 4
        );
        Map<String, Map<String, Integer>> errorCases = Map.of(
                Name.ERROR_PREFIX_UNTERMINATED_LEFT_BRACKET, unterminated,
                Name.ERROR_PREFIX_UNEXPECTED_RIGHT_BRACKET, unexpected);

        for (Map.Entry<String, Map<String, Integer>> errorCase : errorCases.entrySet()) {
            for (Map.Entry<String, Integer> inputEntry : errorCase.getValue().entrySet()) {
                IllegalArgumentException error = assertThrowsExactly(IllegalArgumentException.class,
                        () -> Name.of(inputEntry.getKey()),
                        () -> errorCase.getKey() + "'" + inputEntry.getKey() + "'");
                Throwable cause = error.getCause();
                assertInstanceOf(ParseException.class, cause);
                ParseException parseError = (ParseException) cause;
                assertEquals(error.getMessage(), parseError.getMessage());
                assertEquals(inputEntry.getValue(), parseError.getErrorOffset(),
                        error.getMessage() + " (expected offset)");
            }
        }
    }

    @Test
    void of_debugOriginalAssertion() {
        Name.of("bar", "foobar", "foo".length());
        assertThrows(AssertionError.class, () -> Name.of("bar", "foobar", 0));
        assertThrows(AssertionError.class, () -> Name.of("bar", "foobar", 12));
        assertThrows(AssertionError.class, () -> Name.of("bar", "foo bar", "foo".length()));
        assertThrows(AssertionError.class, () -> Name.of("bar", "fobar", "foo".length()));
    }
}
