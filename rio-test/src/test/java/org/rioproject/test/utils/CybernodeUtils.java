/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test.utils;

import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.deploy.ServiceRecord;
import org.rioproject.cybernode.Cybernode;

import java.rmi.RemoteException;
import java.util.List;

/**
 * The class provides static utility methods for manipulating Cybernodes.
 */
public class CybernodeUtils {

    /**
     * Searches the specified array of Cybernodes for the first occurence
     * of a Cybernode running one or more service elements.
     *
     * @param cybernodes    the array to be searched
     * @return              the found Cybernode, if successful.
     *                      Otherwise <code>null</null>.
     *
     * @throws  RemoteException if there was a communication failure
     *          while attempting to access one of the Cybernodes from
     *          the specified array.
     */
    public static Cybernode findBusy(Cybernode[] cybernodes)
            throws RemoteException {
        for (Cybernode cybernode : cybernodes) {
            if (cybernode.getServiceStatements().length > 0) {
                return cybernode;
            }
        }
        return null;
    }

    /**
     * Searches the specified list of Cybernodes for the first occurence
     * of a Cybernode running one or more service elements.
     *
     * @param cybernodes    the list to be searched
     * @return              the found Cybernode, if successful.
     *                      Otherwise <code>null</null>.
     */
    public static Cybernode findBusy(List<Cybernode> cybernodes) {
        for (Cybernode cybernode : cybernodes) {
            try {
                if (cybernode.getServiceStatements().length > 0) {
                    return cybernode;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Calculates the number of services of the specified type running on
     * each of the specified Cybernodes.
     *
     * @param cybernodes the Cybernodes to consider
     * @param type       the service type to look for
     * @return the resulting array. Every element of this array
     *         stores the number of services of the specified type
     *         running on a specific Cybernode. The order of elements
     *         corresponds to the one of services in the
     *         <code>cybernodes</code> parameter.
     * 
     * @throws  RemoteException if there was a communication failure
     *          while attempting to access one of the Cybernodes from
     *          the specified list.
     */
    public static int[] calcServices(Cybernode[] cybernodes, Class type) throws RemoteException {

        int[] res = new int[cybernodes.length];
        for (int i = 0; i < cybernodes.length; i++) {
            Cybernode cybernode = cybernodes[i];
            ServiceRecord[] records = cybernode.getServiceRecords(ServiceRecord.ACTIVE_SERVICE_RECORD);
            for (ServiceRecord record : records) {
                ServiceElement element = record.getServiceElement();
                ClassBundle[] exportBundles = element.getExportBundles();
                for (ClassBundle bundle : exportBundles) {
                    if (bundle.getClassName().equals(type.getName())) {
                        res[i]++;
                        break;
                    }
                }
            }
        }
        return res;
    }

    public static int[] calcServices(List<Cybernode> cybernodes, Class type) throws RemoteException {
        Cybernode[] cnodes = cybernodes.toArray(new Cybernode[cybernodes.size()]);
        return calcServices(cnodes, type);

    }
}
