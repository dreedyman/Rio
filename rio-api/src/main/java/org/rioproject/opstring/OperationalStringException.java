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
package org.rioproject.opstring;

/**
 * This exception is thrown by an OperationalStringManager if there are errors
 * performing management on an OperationalString
 *
 * @author Dennis Reedy
 */
public class OperationalStringException extends Exception {
    private static final long serialVersionUID = 1L;
    private boolean managed = true;

    /**
     * Constructs a <code>OperationalStringException</code> with the specified
     * detail message and optional exception that was raised while instantiating
     * the JSB
     * 
     * @param s the detail message
     * @param cause the exception that was raised while instantiating the JSB
     */
    public OperationalStringException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs an OperationalStringException with the specified detail
     * message.
     *
     * @param reason The reason for the exception
     */
    public OperationalStringException(String reason) {
        super(reason);
    }

    /**
     * Constructs an OperationalStringException with the specified detail
     * message.
     *
     * @param reason The reason for the exception
     * @param managed Whether the OperationalString is managed
     */
    public OperationalStringException(String reason, boolean managed) {
        super(reason);
        this.managed = managed;
    }

    /**
     * Constructs an OperationalStringException with no detail message.
     */
    public OperationalStringException() {
        super();
    }

    /**
     * Get whether the OperationalString is managed
     *
     * @return True if managed, false if not
     */
    public boolean isManaged() {
        return managed;
    }
}
