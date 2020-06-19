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
package org.rioproject.test.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A bean that will deadlock
 */
public class Deadlock {
    int visitorNumber = 1;
    static final Logger logger = LoggerFactory.getLogger("deadlock.service");

    public void setParameters(Map<String, Object> params) {
        System.err.println("***********");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }

    public void postStart() {
        StringBuilder buff = new StringBuilder();
        buff.append("***********\n");
        buff.append("Started, kick off threads for deadlock test\n");

        final Friend alphonse = new Friend("Alphonse");
        final Friend gaston = new Friend("Gaston");
        new Thread(new Runnable() {
            public void run() {
                alphonse.bow(gaston);
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                gaston.bow(alphonse);
            }
        }).start();
        buff.append("***********\n");
        logger.info(buff.toString());
    }

    class Friend {
        private final String name;

        public Friend(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public synchronized void bow(Friend bower) {
            System.err.format("%s: %s has bowed to me!%n",
                              this.name, bower.getName());
            bower.bowBack(this);
        }

        public synchronized void bowBack(Friend bower) {
            System.err.format("%s: %s has bowed back to me!%n",
                              this.name, bower.getName());
        }
    }
}
