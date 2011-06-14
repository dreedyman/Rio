/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.core;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;

/**
 * Thrown when a Service Bean cannot be instantiated
 *
 * @author Dennis Reedy
 */
public class JSBInstantiationException extends Exception {
    /**
     * serialVersionUID
     */
    static final long serialVersionUID = 1L;
    /**
     * This field indicates that the raised exception is in reference to a
     * ServiceBean that is not instantiable, due to such reasons as missing
     * classes or resources
     */
    private boolean unInstantiable = false;
    /**
     * An embedded formatted cause stackTrace
     */
    private String causeStackTrace;

    private ExceptionDescriptor exDesc;

    /**
     * Constructs a <code>JSBInstantiationException</code> with no detail
     * message.
     */
    public JSBInstantiationException() {
        super();
    }

    /**
     * Constructs a <code>JSBInstantiationException</code> with the specified
     * detail message
     *
     * @param s the detail message.
     */
    public JSBInstantiationException(String s) {
        super(s);
    }

    @Deprecated
    public JSBInstantiationException(String s,
                                     String causeStackTrace,
                                     boolean unInstantiable) {
        super(s);
        this.causeStackTrace = causeStackTrace;
        this.unInstantiable = unInstantiable;
    }

    public JSBInstantiationException(String s,
                                     ExceptionDescriptor exDesc,
                                     boolean unInstantiable) {
        super(s);
        this.exDesc = exDesc;
        this.unInstantiable = unInstantiable;
    }

    /**
     * Constructs a <code>JSBInstantiationException</code> with the specified
     * detail message and optional exception that was raised while instantiating
     * the JSB
     *
     * @param s the detail message
     * @param cause the exception that was raised while instantiating the JSB
     */
    public JSBInstantiationException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a <code>JSBInstantiationException</code> with the specified
     * detail message, optional exception that was raised while instantiating
     * the JSB and whether
     *
     * @param s The detail message
     * @param cause The exception that was raised while instantiating the JSB
     * @param unInstantiable - Whether the raised exception is in reference to a
     * ServiceBean that is not instantiable, due to such reasons as missing
     * classes or resources
     */
    public JSBInstantiationException(String s, 
                                     Throwable cause,
                                     boolean unInstantiable) {
        super(s, cause);
        this.unInstantiable = unInstantiable;
    }    

    /**
     * Returns whether the raised exception is in reference to a ServiceBean
     * that is not instantiable, due to such reasons as missing classes or
     * resources
     *
     * @return True if the service bean is uninstantiable
     */
    public boolean isUninstantiable() {
        return (unInstantiable);
    }

    /**
     * Get the {@link ExceptionDescriptor} for the cause
     *
     * @return The ExceptionDescriptor, or null if not recorded
     */
    public ExceptionDescriptor getCauseExceptionDescriptor() {
        return exDesc;
    }

    /**
     * Prints the stack backtrace to the specified print stream. If an exception
     * occurred during JSB instantiation it prints that exception's stack trace,
     * or else prints the stack backtrace of this exception.
     */
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if(exDesc!=null) {
            ps.print(exDesc.format());
        }
    }

    /**
     * Prints the stack backtrace to the specified print writer. If an exception
     * occurred during class loading it prints that exception's stack trace, or
     * else prints the stack backtrace of this exception.
     */
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if(exDesc!=null) {
            pw.print(exDesc.format());
        }
    }

    /**
     * The <code>ExceptionDescriptor</code> is used to capture details of an exception that has been raised by
     * underlying software that may include classes that are not available on the clients classpath or may
     * include non-serializable objects.
     */
    public static class ExceptionDescriptor implements Serializable {
        private String className;
        private String message;
        private StackTraceElement[] stackTrace;

        public ExceptionDescriptor(String className,
                                   String message,
                                   StackTraceElement[] stackTrace) {
            this.className = className;
            this.message = message;
            this.stackTrace = stackTrace;
        }

        public String getClassName() {
            return className;
        }

        public String getMessage() {
            return message;
        }

        public StackTraceElement[] getStacktrace() {
            return stackTrace;
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("Caused by: ")
                .append(className)
                .append(": ")
                .append(message).append("\n");
            for(StackTraceElement e : stackTrace) {
                sb.append("\t").append("at ").append(e).append("\n");
            }
            return sb.toString();
        }

        /**
         * Returns a short description of this ExceptionDescriptor.
         * The result is the concatenation of:
         * <ul>
         * <li> the {@linkplain Class#getName() name} of the class of this object
         * <li> ": " (a colon and a space)
         * <li> the result of invoking this object's {@link #getMessage}
         *      method
         * </ul>
         * If <tt>getMessage</tt> returns <tt>null</tt>, then just
         * the class name is returned.
         *
         * @return a string representation of this ExceptionDescriptor.
         */
        public String toString() {
            String s = getClass().getName();
            String message = getMessage();
            return (message != null) ? (s + ": " + message) : s;
        }
    }
}
