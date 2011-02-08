/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.config;

import java.lang.annotation.*;

/**
 * The Component annotation is used to indicate what
 * configuration component a class is to be used for.
 *
 * <p>The component identifies the object whose behavior will be configured
 * using the object returned. The value of component must be a
 * <i>QualifiedIdentifier</i>, as defined in the
 * <i>Java(TM) Language Specification (JLS)</i>, and is typically the class or
 * package name of the object being configured.
 *
 * @author Dennis Reedy
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target (value= ElementType.TYPE)
public @interface Component {    
    String value();
}
