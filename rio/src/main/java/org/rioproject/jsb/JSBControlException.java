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
package org.rioproject.jsb;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Thrown when a Service Bean cannot be controlled
 *
 * @author Dennis Reedy
 */
public class JSBControlException extends Exception {
    /**
     * serialVersionUID
     */
    static final long serialVersionUID = 2L;

    /**
     * Constructs a <code>JSBControlException</code> with no detail message.
     */
    public JSBControlException() {
        super();
    }

    /**
     * Constructs a <code>JSBControlException</code> with the specified detail
     * message
     * 
     * @param s the detail message.
     */
    public JSBControlException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>JSBControlException</code> with the specified detail
     * message and optional exception that was raised while controlling the JSB
     * 
     * @param s The detail message
     * @param cause The exception that was raised while controlling the JSB
     */
    public JSBControlException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Prints the stack backtrace. If an exception occurred during JSB
     * instantiation it prints that exception's stack trace, or else prints the
     * stack backtrace of this exception.
     * 
     * @see java.lang.System#err
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Prints the stack backtrace to the specified print stream. If an exception
     * occurred during JSB instantiation it prints that exception's stack trace,
     * or else prints the stack backtrace of this exception.
     */
    public void printStackTrace(PrintStream ps) {
        synchronized(ps) {
            Throwable cause = getCause();
            if(cause != null) {
                ps.print("org.rioproject.jsb.JSBControlException: ");
                cause.printStackTrace(ps);
            } else {
                super.printStackTrace(ps);
            }
        }
    }

    /**
     * Prints the stack backtrace to the specified print writer. If an exception
     * occurred during class loading it prints that exception's stack trace, or
     * else prints the stack backtrace of this exception.
     */
    public void printStackTrace(PrintWriter pw) {
        synchronized(pw) {
            Throwable cause = getCause();
            if(cause != null) {
                pw.print("org.rioproject.jsb.JSBControlException: ");
                cause.printStackTrace(pw);
            } else {
                super.printStackTrace(pw);
            }
        }
    }
}
