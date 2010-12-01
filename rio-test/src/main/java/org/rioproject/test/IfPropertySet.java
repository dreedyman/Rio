/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * Annotation to indicate that a test is to be run if a specific property is set.
 * </p>
 * <p>
 *  Note: {@link IfPropertySet @IfProfileValue} can be applied at either the
 * class or method level.
 * </p>
 * <p>
 * Examples: You can configure a test method to run only on Java VMs from
 * Sun Microsystems as follows:
 * </p>
 *
 * <pre class="code">
 * {@link IfPropertySet @IfPropertySet}(name=&quot;java.vendor&quot;, value=&quot;Sun Microsystems Inc.&quot;)
 * testSomething() {
 *     // ...
 * }
 * </pre>
 * <p>Or you can select to run if Windows is the operating system:
 * <pre class="code">
 * {@link IfPropertySet @IfPropertySet}(name=&quot;os.name&quot;, value=&quot;Windows*&quot;)
 * testSomething() {
 *     // ...
 * }
 * </pre>
 *
 * The '*' in the value acts as a wildcard (when placed as the last character of the value string)
 *
 * <p>Or you can select to run if Windows is <u>not</u> the operating system:
 * <pre class="code">
 * {@link IfPropertySet @IfPropertySet}(name=&quot;os.name&quot;, notvalue=&quot;Windows*&quot;)
 * testSomething() {
 *     // ...
 * }
 * </pre>
 */
@Retention (RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface IfPropertySet {
    /**
	 * The <code>name</code> of the system property against which to obtain.
     *
     * @return The <code>name</code> of the property.
     */
	String name();

	/**
	 * The <code>value</code> of the property for the given {@link #name() name}.
     *
     * @return The value of the property
	 */
	String value() default "";

    /**
	 * The <code>value</code> of the property for the given {@link #name() name}.
     *
     * @return The value of the property
	 */
	String notvalue() default "";
}
