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
 * This exception is thrown during processing of an OAR.
 */
public class OARException extends Exception {
    static final long serialVersionUID = 1L;

    /**
     * Constructs an <code>OARException</code> with the specified
     * detail message and optional exception that was raised while instantiating
     * the JSB
     * 
     * @param s the detail message
     * @param cause the exception that was raised while instantiating the JSB
     */
    public OARException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs an OARException with the specified detail
     * message.
     *
     * @param reason The reason for the exception
     */
    public OARException(String reason) {
        super(reason);
    }

    /**
     * Constructs an OARException with no detail message.
     */
    public OARException() {
        super();
    }
}
