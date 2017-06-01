package org.rioproject.test;

import java.lang.annotation.*;

/**
 * <p>
 * Defines the method to invoke to get additonal JVM system properties to pass to
 * created JVMs
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD, ElementType.FIELD})
@Inherited
public @interface AddSystemProperties {
}
