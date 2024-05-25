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

package net.adamcin.jardelta.core.osgi.header;

import aQute.bnd.header.Parameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParameterListTest {

    @Test
    void from_duplicates() {
        final String requireCapabilities = "osgi.extender;filter:=\"(&(osgi.extender=osgi.component)(version>=1.3.0)(!(version>=2.0.0)))\",osgi.service;filter:=\"(objectClass=com.example.ServiceA)\";effective:=active,osgi.service;filter:=\"(objectClass=com.example.ServiceB)\";effective:=active,osgi.service;filter:=\"(objectClass=com.example.ServiceD)\";effective:=active;resolution:=optional,osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"";
        Parameters parameters = new Parameters(requireCapabilities, null, true);
        ParameterList parameterList = ParameterList.fromDuplicates("osgi.service", parameters);
        assertNotNull(parameterList);
        assertEquals(3, parameterList.getAttrsList().size());
    }
}
