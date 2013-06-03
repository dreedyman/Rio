package org.rioproject.test.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * Creates an out of memory exception
 */
public class OutOfMemoryServiceImpl implements OutOfMemory {
    public static final int MB = 1048576;
    public final Object[] holder = new Object[MB];
    static Logger logger = LoggerFactory.getLogger(OutOfMemoryServiceImpl.class);
   
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
                            if(count % 100 == 0)
                                logger.info("Percent Heap Memory Used: {}", pctUsed);
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            logger.error("Oops", e);
                        }
                    }
                }
            }).start();
        } catch(Throwable t) {
            t.printStackTrace();
        }
        logger.info("Started memory creation thread.");
    }
}
