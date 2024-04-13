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

package net.adamcin.jardiff.testing.example.impl;

import net.adamcin.jardiff.testing.example.ExampleFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@org.osgi.service.component.annotations.Component(property = {
        "exampleFactory.top.long1:Long=45"
})
@Designate(ocd = ExampleFactoryImpl.Config.class, factory = true)
@Component(metatype = true, configurationFactory = true, label = "Example Factory Component Name")
@Service
@Properties({
        @Property(name = "exampleFactory.top.long1", longValue = 45)
})
public class ExampleFactoryImpl implements ExampleFactory {

    public static final String DEFAULT_CONST = "const value";

    @Property(value = DEFAULT_CONST, label = "Const String", description =  "Const String - description")
    public static final String PROP_CONST = "exampleFactory.const.string";

    @ObjectClassDefinition(name = "Example Factory Component Name")
    public @interface Config {
        @AttributeDefinition(name = "Const String", description = "Const String - description")
        String exampleFactory_const_string() default DEFAULT_CONST;
    }

    @Activate
    @org.osgi.service.component.annotations.Activate
    protected void activate() {
        /* do nothing */
    }
}
