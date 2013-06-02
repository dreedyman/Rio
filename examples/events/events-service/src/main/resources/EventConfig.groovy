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


import net.jini.core.entry.Entry
import net.jini.lookup.ui.MainUI
import org.rioproject.config.Component
import org.rioproject.entry.UIDescriptorFactory
import org.rioproject.serviceui.UIComponentFactory
/**
 * Configuration loaded as a resource for the Events example
 */
@Component('org.rioproject.examples.events.service')
class EventConfig {

    Entry[] getServiceUIs(String codebase) {
        String uiClass = 'org.rioproject.examples.events.service.ui.HelloEventUI'
        URL url = new URL("artifact:org.rioproject.examples..events:events-ui:2.1")
        def entry = [UIDescriptorFactory.getUIDescriptor(MainUI.ROLE, new UIComponentFactory(url, uiClass))]
        return entry as Entry[]
    }

}
