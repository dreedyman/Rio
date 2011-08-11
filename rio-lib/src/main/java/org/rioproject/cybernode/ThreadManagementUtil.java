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
package org.rioproject.cybernode;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Utilities for managing and working with threads.
 */
public class ThreadManagementUtil {
    static ThreadGroup rootThreadGroup = null;

    static Collection<Thread> getAllThreadsByName(final String name) {
        if (name == null)
            throw new NullPointerException("Null name");
        long t0 = System.currentTimeMillis();
        Collection<Thread> threads = new ArrayList<Thread>();
        for (Thread thread : getAllThreads())
            if (thread.getName().equals(name))
                threads.add(thread);
        long t1 = System.currentTimeMillis();
        System.out.println("===> Time to get threads by name: " + (t1 - t0) + " milliseconds");
        return threads;
    }

    public static Thread[] getAllThreads() {
        final ThreadGroup root = getRootThreadGroup();
        final ThreadMXBean tMxBean = ManagementFactory.getThreadMXBean();
        int nAlloc = tMxBean.getThreadCount();
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[nAlloc];
            n = root.enumerate(threads, true);
        } while (n == nAlloc);
        return java.util.Arrays.copyOf(threads, n);
    }

    static ThreadGroup getRootThreadGroup() {
        if (rootThreadGroup != null)
            return rootThreadGroup;
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;
        while ((ptg = tg.getParent()) != null)
            tg = ptg;
        return tg;
    }


}
