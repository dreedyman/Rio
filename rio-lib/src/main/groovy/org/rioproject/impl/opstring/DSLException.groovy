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
package org.rioproject.impl.opstring

/**
 * Thrown when a DSL parsing error occurs
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
class DSLException extends RuntimeException {
    static final long serialVersionUID = 1L;

    /**
     * Constructs an DSLException with no detail message.
     */
    public DSLException() {
    }

    /**
     * Constructs an DSLException with the specified
     * detail message.
     *
     * @param message the detail message
     */
    public DSLException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * <p>Note that the detail message associated with <code>cause</code> is
     * <i>not</i> automatically incorporated in this exception's detail
     * message.
     *
     * @param message the detail message (which is saved for later retrieval
     *         by the  {@link Throwable#getMessage()}  method).
     * @param cause the cause (which is saved for later retrieval by the
     * {@link Throwable#getCause()}  method).  (A <tt>null</tt> value
     *         is permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public DSLException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example,  {@link
     * java.security.PrivilegedActionException} ).
     *
     * @param cause the cause (which is saved for later retrieval by the
     * {@link Throwable#getCause()}  method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     */
    public DSLException(Throwable cause) {
        super(cause);
    }
}