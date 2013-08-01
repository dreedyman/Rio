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
package org.rioproject.resources.servicecore;

import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.Administrable;
import org.rioproject.resources.util.ThrowableUtil;

import java.io.PrintStream;
import java.rmi.RemoteException;

/**
 * Utility for service termination.
 */
public class ServiceStopHandler {

    public static void destroyService(Object proxy, String name, PrintStream out) {
        if (proxy == null)
            return;
        Object admin;
        if (!(proxy instanceof Administrable)) {
            //System.err.println("Unable to acquire Administrable "+
            //                   "interface for ["+name+"], proxy does not " +
            //                   "implement Administrable");
            return;
        }
        try {
            admin = ((Administrable) proxy).getAdmin();
        } catch (RemoteException e) {
            //System.err.println("Unable to acquire Administrable " +
            //                   "interface for "+
            //                   "["+name+"], Exception : "+e.getMessage());
            return;
        }
        for (int i = 1; i < 3; i++) {
            try {
                ((DestroyAdmin) admin).destroy();
                out.println("Destroyed " + name);
                break;
            } catch (Exception e) {
                if (!ThrowableUtil.isRetryable(e)) {
                    break;
                }
                System.err.println("Unable to destroy " +
                                   "[" + name + "], Exception : " +
                                   e.getMessage() +
                                   "Retry [" + i + "]");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    /* ignore */
                }
            }
        }
    }

}
