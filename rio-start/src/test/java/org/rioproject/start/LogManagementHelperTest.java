package org.rioproject.start;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.junit.Test;
import org.rioproject.config.Constants;
import org.rioproject.config.RioProperties;
import org.rioproject.config.RioPropertiesTest;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author Dennis Reedy
 */
public class LogManagementHelperTest {
    @Test
    public void checkConfigurationReset() throws Exception {
        try {
            MemoryAppender memoryAppender1 = new MemoryAppender();
            getLogger(memoryAppender1).info("=============== Nothing ===============");
            assertTrue(memoryAppender1.getLogMessages().size()==1);
            System.setProperty(Constants.ENV_PROPERTY_NAME,
                               System.getProperty("user.dir") + "/src/test/resources/config/rio-with-logback.env");

            RioProperties.load();
            LogManagementHelper.checkConfigurationReset();
            MemoryAppender memoryAppender2 = new MemoryAppender();
            getLogger(memoryAppender2).info("=============== Something ===============");
            assertTrue(memoryAppender2.getLogMessages().size()==1);
        } finally {
            System.clearProperty(Constants.ENV_PROPERTY_NAME);
        }
    }

    private Logger getLogger(MemoryAppender memoryAppender) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();

        encoder.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        encoder.setContext(loggerContext);
        encoder.start();

        memoryAppender.setEncoder(encoder);
        memoryAppender.setContext(loggerContext);
        memoryAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(RioPropertiesTest.class);
        logger.addAppender(memoryAppender);
        return logger;
    }

}