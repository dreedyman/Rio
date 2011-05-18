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
package org.rioproject.gnostic.test;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * Provides ability to set the load for the TestService
 */
public interface TestService {
    enum Status {
        ALLOWED, DISALLOWED
    }
    double getLoad() throws RemoteException;
    void setLoad(double load) throws RemoteException;
    Status getStatus() throws IOException;
    void setStatus(Status status) throws IOException;
    void sendNotify() throws IOException;
    void executedRHS() throws IOException;
    int getNotificationCount() throws IOException;
    int getRHSExecutedCount() throws IOException;
}
