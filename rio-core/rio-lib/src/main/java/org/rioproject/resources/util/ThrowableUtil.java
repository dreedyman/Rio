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
package org.rioproject.resources.util;

import com.sun.jini.constants.ThrowableConstants;
import org.rioproject.deploy.ServiceBeanInstantiationException;

import java.lang.reflect.InvocationTargetException;

/**
 * Utility for getting things from a Throwable
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class ThrowableUtil {
    public static Throwable getRootCause(Throwable e) {
        Throwable cause = e;
        if(e instanceof InvocationTargetException) {
            cause = e.getCause()==null? ((InvocationTargetException)e).getTargetException(): e.getCause();
        } else if(e instanceof ServiceBeanInstantiationException) {
            if(((ServiceBeanInstantiationException)e).getCauseExceptionDescriptor()!=null) {
                ServiceBeanInstantiationException.ExceptionDescriptor exDesc =
                    ((ServiceBeanInstantiationException)e).getCauseExceptionDescriptor();

                if(exDesc.getCauses().size()>0) {
                    exDesc = exDesc.getCauses().get(0);
                }
                Throwable t = new Throwable(exDesc.getMessage());
                t.setStackTrace(exDesc.getStacktrace());
                return t;
            }
        } else {
            Throwable t = cause;
            while(t != null) {
                t = cause.getCause();
                if(t != null)
                    cause = t;
            }
        }
        return (cause);
    }

    public static boolean isRetryable(Throwable t) {
        boolean retryable = true;
        final int category = ThrowableConstants.retryable(t);
        Throwable cause = getRootCause(t);
        if (category == ThrowableConstants.BAD_INVOCATION ||
            category == ThrowableConstants.BAD_OBJECT ||
            cause instanceof java.net.ConnectException) {
            retryable = false;
        }
        return retryable;
    }
}
