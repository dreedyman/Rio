/*
 * Copyright to the original author or authors.
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

import java.lang.annotation.*;

/**
 * <p>
 * Defines the property to invoke to set the {@link TestManager}. The
 * {@link TestManager} will be injected by the {@link RioTestRunner} prior to
 * test case method invocation.
 * </p>
 *
 * <p>
 *  Note: {@link SetTestManager @SetTestManager} can be applied at either the
 * class or method level.
 * </p>
 */
@Documented
@Retention (RetentionPolicy.RUNTIME)
@Target (value={ElementType.METHOD, ElementType.FIELD})
@Inherited
public @interface SetTestManager {
}
