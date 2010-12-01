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
package org.rioproject.examples.hospital;

/**
 * Thrown when there is a problem admitting a patient
 */
public class AdmissionException extends Exception {
    static final long serialVersionUID = 1L;

    /**
     * Constructs an <code>AdmissionException</code> with the specified
     * detail message and optional exception that was raised while admitting a
     * {@link Patient}
     * 
     * @param reason The reason for the exception
     * @param cause the exception that was raised while instantiating the JSB
     */
    public AdmissionException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * Constructs an AdmissionException with the specified detail
     * message.
     *
     * @param reason The reason for the exception
     */
    public AdmissionException(String reason) {
        super(reason);
    }

    /**
     * Constructs an AdmissionException with no detail message.
     */
    public AdmissionException() {
        super();
    }
}
