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
package org.rioproject.monitor.tasks;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * utility for dispatching tasks
 */
public class TaskTimer {
    private static TaskTimer instance;
    private final Timer taskTimer = new Timer(true);

    /**
     * Get an instance of the SigarHelper.
     *
     * @return An instance of the SigarHelper.
     */
    public static synchronized TaskTimer getInstance() {
        if(instance==null)
            instance = new TaskTimer();
        return instance;
    }

    public void schedule(TimerTask task, long delay) {
        taskTimer.schedule(task, delay);
    }

    public void schedule(TimerTask task, Date time) {
        taskTimer.schedule(task, time);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        taskTimer.scheduleAtFixedRate(task, firstTime, period);
    }

    public void cancel() {
        taskTimer.cancel();
    }
}
