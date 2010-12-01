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
package org.rioproject.resources.persistence;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Base class for exceptions thrown by PersistantStore
 */
public class StoreException extends Exception {
    private static final long serialVersionUID = 2L;

    /**
     * The exception (if any) that trigged the store exception.  This
     * may be <code>null</code>.
     *
     * @serial
     */
    final Throwable nestedException;

    /**
     * Create a store exception, forwarding a string to the superclass
     * constructor.
     * @param str a detailed message for the exception
     */
    public StoreException(String str) {
	super(str);
	nestedException = null;
    }

    /**
     * Create an exception, forwarding a string to the
     * superclass constructor.
     * @param str a detailed message for the exception
     * @param ex root cause for the exception, may be <code>null</code>
     */
    public StoreException(String str, Throwable ex) {
	super(str);
	nestedException = ex;
    }

    /**
     * Print the stack trace of this exception, plus that of the nested
     * exception, if any.
     */
    public void printStackTrace() {
	printStackTrace(System.err);
    }

    /**
     * Print the stack trace of this exception, plus that of the nested
     * exception, if any.
     */
    public void printStackTrace(PrintStream out) {
	super.printStackTrace(out);
	if (nestedException != null) {
	    out.println("nested exception:");
	    nestedException.printStackTrace(out);
	}
    }

    /**
     * Print the stack trace of this exception, plus that of the nested
     * exception, if any.
     */
    public void printStackTrace(PrintWriter out) {
	super.printStackTrace(out);
	if (nestedException != null) {
	    out.println("nested exception:");
	    nestedException.printStackTrace(out);
	}
    }
}

