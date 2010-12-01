package org.rioproject.test.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Creates an out of memory exception
 */
public class OutOfMemoryServiceImpl implements OutOfMemory {
    public static final int MB = 1048576;
    public final Object[] holder = new Object[MB];
   
    public void createOOME() {
        try {
            new Thread(new Runnable() {
                public void run() {
                    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                    int count = 0;
                    for (; count < holder.length; count++) {
                        holder[count] = new byte[MB];
                        try {
                            float used = memoryBean.getHeapMemoryUsage().getUsed();
                            float max = memoryBean.getHeapMemoryUsage().getMax();
                            float pctUsed = (used / max)*100;
                            System.err.print("Percent Heap Memory Used: " + pctUsed+"\r");
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch(Throwable t) {
            t.printStackTrace();
        }
        System.err.println("Started memory creation thread.");
    }
}
