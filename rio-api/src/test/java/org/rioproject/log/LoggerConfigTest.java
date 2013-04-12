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
package org.rioproject.log;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rioproject.logging.jul.RioLogFormatter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.*;

/**
 * Test using the {@code LoggerConfig}
 */
public class LoggerConfigTest {
    StringifiedOutputStream out;

    @Before
    public void reset() {
        LogManager.getLogManager().reset();
        Logger rootLogger = Logger.getLogger("");
        RedirectableConsoleHandler redirectableConsoleHandler = new RedirectableConsoleHandler();
        out = new StringifiedOutputStream();
        redirectableConsoleHandler.setOutputStream(out);
        redirectableConsoleHandler.setFormatter(new RioLogFormatter());
        rootLogger.addHandler(redirectableConsoleHandler);
    }

    @Test
    public void testCreatingNewLoggerAtWarning() {
        String name = "test";
        Level level = Level.WARNING;
        Logger logger = createLoggerConfigAndGetLogger(name, level);
        Assert.assertNotNull(logger);

        doLog(logger);
        Assert.assertEquals(2, getLinesLogged(out.toString()));
    }

    @Test
    public void testCreatingNewLoggerAtConfig() {
        String name = "test";
        Level level = Level.CONFIG;
        Logger logger = createLoggerConfigAndGetLogger(name, level);
        Assert.assertNotNull(logger);

        doLog(logger);
        Assert.assertEquals(4, getLinesLogged(out.toString()));
    }

    @Test
    public void testCreatingNewLoggerAtFiner() {
        String name = "test";
        Level level = Level.FINER;
        Logger logger = createLoggerConfigAndGetLogger(name, level);
        Assert.assertNotNull(logger);

        doLog(logger);
        Assert.assertEquals(6, getLinesLogged(out.toString()));
    }

    @Test
    public void testCreatingNewLoggerAtFinest() {
        String name = "test";
        Level level = Level.FINEST;
        Logger logger = createLoggerConfigAndGetLogger(name, level);
        Assert.assertNotNull(logger);

        doLog(logger);
        Assert.assertEquals(7, getLinesLogged(out.toString()));
    }

    @Test
    public void testCreatingNewLoggerAtAll() {
        String name = "test";
        Level level = Level.ALL;
        Logger logger = createLoggerConfigAndGetLogger(name, level);
        Assert.assertNotNull(logger);

        doLog(logger);
        Assert.assertEquals(7, getLinesLogged(out.toString()));
    }

    @Test
    public void testCreatingNewLoggerAtOff() {
        String name = "test";
        Level level = Level.OFF;
        Logger logger = createLoggerConfigAndGetLogger(name, level);
        Assert.assertNotNull(logger);

        doLog(logger);
        Assert.assertEquals(0, getLinesLogged(out.toString()));
    }

    @Test
    public void testCreatingNewLoggerWithAHandler() {
        String name = "test";
        Level level = Level.CONFIG;
        LoggerConfig.LogHandlerConfig[] handlerConfigs =
            new LoggerConfig.LogHandlerConfig[]{new LoggerConfig.LogHandlerConfig(RedirectableConsoleHandler.class.getName(),
                                                                                  level,
                                                                                  null,
                                                                                  RioLogFormatter.class.getName())};
        Logger logger = createLoggerConfigAndGetLogger(name, level, true, handlerConfigs);
        StringifiedOutputStream out2 = new StringifiedOutputStream();
        for(Handler h : logger.getHandlers()) {
            if(h instanceof RedirectableConsoleHandler) {
                ((RedirectableConsoleHandler) h).setOutputStream(out2);
            }
        }

        Assert.assertNotNull(logger);
        doLog(logger);
        Assert.assertEquals(4, getLinesLogged(out.toString())+getLinesLogged(out2.toString()));
    }

    @Test
    public void testCreatingNewLoggerWithAHandlerNoParentDelegation() {
        String name = "test";
        Level level = Level.CONFIG;
        LoggerConfig.LogHandlerConfig[] handlerConfigs =
            new LoggerConfig.LogHandlerConfig[]{new LoggerConfig.LogHandlerConfig(RedirectableConsoleHandler.class.getName(),
                                                                                  level,
                                                                                  null,
                                                                                  RioLogFormatter.class.getName())};
        Logger logger = createLoggerConfigAndGetLogger(name, level, false, handlerConfigs);
        StringifiedOutputStream out2 = new StringifiedOutputStream();
        for(Handler h : logger.getHandlers()) {
            if(h instanceof RedirectableConsoleHandler) {
                ((RedirectableConsoleHandler) h).setOutputStream(out2);
            }
        }
        Assert.assertNotNull(logger);
        doLog(logger);
        Assert.assertEquals(4, getLinesLogged(out2.toString()));
    }

    private int getLinesLogged(String loggingOutput) {
        StringTokenizer st = new StringTokenizer(loggingOutput, "\n");
        List<String> lines = new ArrayList<String>();
        while(st.hasMoreTokens()) {
            lines.add(st.nextToken());
        }
        return lines.size();
    }
    
    private Logger createLoggerConfigAndGetLogger(String loggerName, Level level) {
        return createLoggerConfigAndGetLogger(loggerName, level, true, null);
    }

    private Logger createLoggerConfigAndGetLogger(String loggerName,
                                                  Level level,
                                                  boolean useParent,
                                                  LoggerConfig.LogHandlerConfig[] handlerConfigs) {
        String resourceBundleName = null;
        LoggerConfig loggerConfig = new LoggerConfig(loggerName,
                                                     level,
                                                     useParent,
                                                     resourceBundleName,
                                                     handlerConfigs);
        Assert.assertEquals(level, loggerConfig.getLoggerLevel());
        Assert.assertEquals(loggerName, loggerConfig.getLoggerName());
        return loggerConfig.getLogger();
    }
    
    private void doLog(Logger logger) {
        logger.severe("Severe message "+Level.SEVERE.intValue());
        logger.warning("Warning message "+Level.WARNING.intValue());
        logger.info("Info message "+Level.INFO.intValue());
        logger.config("Config message "+Level.CONFIG.intValue());
        logger.fine("Fine message "+Level.FINE.intValue());
        logger.finer("Finer message "+Level.FINER.intValue());
        logger.finest("Finest message "+Level.FINEST.intValue());
    }

    public static class RedirectableConsoleHandler extends ConsoleHandler {
        @Override 
        protected void setOutputStream(OutputStream outputStream) throws SecurityException {
            super.setOutputStream(outputStream); 
        }
    }

    class StringifiedOutputStream extends OutputStream {
        private StringBuilder stringBuilder = new StringBuilder();

        @Override
        public void write(int b) throws IOException {
            stringBuilder.append((char) b);
        }
        public String toString() {
            return stringBuilder.toString();
        }
    }
}
