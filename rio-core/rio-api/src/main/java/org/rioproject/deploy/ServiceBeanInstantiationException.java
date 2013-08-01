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
package org.rioproject.deploy;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when a service cannot be instantiated
 *
 * @author Dennis Reedy
 */
public class ServiceBeanInstantiationException extends Exception {
    @SuppressWarnings("unused")
    static final long serialVersionUID = 1L;
    /**
     * This field indicates that the raised exception is in reference to a
     * ServiceBean that is not instantiable, due to such reasons as missing
     * classes or resources
     */
    private boolean unInstantiable = false;
    private ExceptionDescriptor exDesc;
    private transient Throwable cause;

    /**
     * Constructs a {@code ServiceBeanInstantiationException} with no detail
     * message.
     */
    public ServiceBeanInstantiationException() {
        super();
    }

    /**
     * Constructs a {@code ServiceBeanInstantiationException} with the specified
     * detail message
     *
     * @param s the detail message.
     */
    public ServiceBeanInstantiationException(final String s) {
        super(s);
    }

    /**
     * Constructs a {@code ServiceBeanInstantiationException} with the specified
     * detail message and optional exception that was raised while instantiating
     * the service
     *
     * @param s the detail message
     * @param cause the exception that was raised while instantiating the service
     */
    public ServiceBeanInstantiationException(final String s, final Throwable cause) {
        super(s);
        this.cause = cause;
        this.exDesc = new ExceptionDescriptor(cause);
    }

    /**
     * Constructs a {@code ServiceBeanInstantiationException} with the specified
     * detail message, optional exception that was raised while instantiating
     * the service and whether
     *
     * @param s The detail message
     * @param cause The exception that was raised while instantiating the service
     * @param unInstantiable - Whether the raised exception is in reference to a
     * ServiceBean that is not instantiable, due to such reasons as missing
     * classes or resources
     */
    public ServiceBeanInstantiationException(final String s, final Throwable cause, final boolean unInstantiable) {
        super(s);
        this.cause = cause;
        this.exDesc = new ExceptionDescriptor(cause);
        this.unInstantiable = unInstantiable;
    }    

    /**
     * Returns whether the raised exception is in reference to a service
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
     * occurred during service instantiation it prints that exception's stack trace,
     * or else prints the stack backtrace of this exception.
     */
    public void printStackTrace(final PrintStream ps) {
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
    public void printStackTrace(final PrintWriter pw) {
        super.printStackTrace(pw);
        if(exDesc!=null) {
            pw.print(exDesc.format());
        }
    }

    @Override
    public synchronized Throwable getCause() {
        if(exDesc==null)
            return super.getCause();
        if(cause!=null)
            return cause;
        Throwable cause = new Throwable(exDesc.getMessage());
        cause.setStackTrace(exDesc.getStacktrace());
        return cause;
    }

    /**
     * The {@code ExceptionDescriptor} is used to capture details of an exception that has been raised by
     * underlying software that may include classes that are not available on the clients classpath or may
     * include non-serializable objects.
     */
    public static class ExceptionDescriptor implements Serializable {
        private final String className;
        private final String message;
        private final StackTraceElement[] stackTrace;
        private final List<ExceptionDescriptor> causes = new ArrayList<ExceptionDescriptor>();

        public ExceptionDescriptor(final Throwable exception) {
            this.className = exception.getClass().getName();
            this.message = exception.getLocalizedMessage();
            this.stackTrace = exception.getStackTrace();
            Throwable cause = exception;
            while(cause.getCause()!=null) {
                this.causes.add(new ExceptionDescriptor(cause.getCause().getClass().getName(),
                                                        cause.getCause().getLocalizedMessage(),
                                                        cause.getCause().getStackTrace()));
                cause = cause.getCause();
            }
        }

        private ExceptionDescriptor(final String className, final String message, final StackTraceElement[] stackTrace) {
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

        public List<ExceptionDescriptor> getCauses() {
            return Collections.unmodifiableList(causes);
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("Caused by: ").append(className).append(": ").append(message).append("\n");
            for(StackTraceElement e : stackTrace) {
                sb.append("\t").append("at ").append(e).append("\n");
            }
            for(ExceptionDescriptor cause : causes) {
                sb.append(cause.format());
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
