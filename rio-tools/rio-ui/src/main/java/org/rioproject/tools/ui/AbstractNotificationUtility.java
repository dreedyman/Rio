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
package org.rioproject.tools.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets notification updates.
 *
 * @author Dennis Reedy
 */
public abstract class AbstractNotificationUtility extends JPanel implements NotificationUtility {
    final List<NotificationUtilityListener> listeners = new ArrayList<NotificationUtilityListener>();

    protected AbstractNotificationUtility(LayoutManager layout) {
        super(layout);
    }

    public void subscribe(NotificationUtilityListener listener) {
        synchronized(listeners) {
            if(!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void unsubscribe(NotificationUtilityListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    public void notifyListeners() {
        NotificationUtilityListener[] nls = listeners.toArray(new NotificationUtilityListener[listeners.size()]);
        for(NotificationUtilityListener nl : nls)
            nl.notify(this);
    }

    public abstract int getTotalItemCount();
}
