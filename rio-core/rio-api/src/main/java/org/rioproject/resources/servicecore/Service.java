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

import net.jini.admin.Administrable;
import org.rioproject.admin.MonitorableService;
import org.rioproject.event.EventProducer;
import org.rioproject.watch.Watchable;

import java.rmi.Remote;

/**
 * The Service interface provides an aggregating mechanism, putting requisite
 * interfaces together to represent the remote semantics of a core Service in
 * the architecture
 *
 * @author Dennis Reedy
 */
public interface Service extends Remote, Administrable, MonitorableService, EventProducer, Watchable {
}
