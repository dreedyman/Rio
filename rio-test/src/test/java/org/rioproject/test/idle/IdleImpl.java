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
package org.rioproject.test.idle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author Dennis Reedy
 */
public class IdleImpl implements Idle {
    private final CountDownLatch countDown;
    Logger logger = LoggerFactory.getLogger(IdleImpl.class);

    public IdleImpl() {
        Random random = new Random();
        int value = 0;
        while(value==0) {
            value = random.nextInt(10);
        }
        logger.info("IdleImpl has count set to {}", value);
        countDown = new CountDownLatch(value);
    }

    public boolean isActive() throws IOException {
        countDown.countDown();
        logger.info("IdleImpl has count: {}", countDown.getCount());
        return countDown.getCount()>0;
    }
}
