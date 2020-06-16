/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.*;

/**
 * The LoggerConfig class has been provided for dynamic services to specify 
 * {@link java.util.logging.Logger} attributes ({@link java.util.logging.Level}, 
 * {@link java.util.logging.Handler}, etc...) and have the attributes established 
 * without depending on machine resident <code>logger.properties</code> 
 * configuration attributes to be set.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class LoggerConfig implements Serializable {
    static final long serialVersionUID = 1L;
    /** The Logger name */
    private String loggerName;
    /**
     * The log level specifying which message levels will be logged by the
     * logger
     */
    private Level level;
    /**
     * Specify whether or not the logger should send its output to it's parent
     * Logger
     */
    private boolean useParentHandlers;
    /** Name of ResourceBundle to be used for localizing messages for the logger */
    private String resourceBundleName;
    /** Array of LogHandlerConfig instances */
    private LogHandlerConfig[] handlers;
    /** The logger for this utility */
    private static Logger myLogger = Logger.getLogger("org.rioproject.log");
    /** An instance of the Logger */
    private transient Logger logger;
    /* Instance of a HandlerFilter (if we created one) */
    private transient HandlerFilter handlerFilter;

    /**
     * Create a LoggerConfig, send logging output to its parent's handlers, with
     * no resource bundle
     * 
     * @param loggerName The Logger name
     * @param level Set the log level specifying which message levels will be
     * logged by the logger
     */
    public LoggerConfig(String loggerName, Level level) {
        this(loggerName, level, true, null, new LogHandlerConfig[0]);
    }

    /**
     * Create a LoggerConfig, send logging output to its parent's handlers, with
     * no resource bundle
     *
     * @param loggerName The Logger name
     * @param level Set the log level specifying which message levels will be
     * logged by the logger
     * @param handlerConfigs An Array of LogHandlerConfig instances
     */
    public LoggerConfig(String loggerName, Level level, LogHandlerConfig... handlerConfigs) {
        this(loggerName, level, true, null, handlerConfigs);
    }

    /**
     * Create a LoggerConfig with no resource bundle
     * 
     * @param loggerName The Logger name
     * @param level Set the log level specifying which message levels will be
     * logged by the logger
     * @param useParentHandlers Specify whether or not this logger should send
     * its output to it's parent Logger. This means that any LogRecords will
     * also be written to the parent's Handlers, and potentially to its parent,
     * recursively up the namespace
     * @param handlerConfigs An Array of LogHandlerConfig instances
     */
    public LoggerConfig(String loggerName, Level level, boolean useParentHandlers, LogHandlerConfig... handlerConfigs) {
        this(loggerName, level, useParentHandlers, null, handlerConfigs);
    }

    /**
     * Create a LoggerConfig
     * 
     * @param loggerName The Logger name
     * @param level Set the log level specifying which message levels will be
     * logged by the logger
     * @param useParentHandlers Specify whether or not this logger should send
     * its output to it's parent Logger. If true, then LogRecords will also be
     * written to the parent's Handlers, and potentially to its parent,
     * recursively up the namespace
     * @param resourceBundleName Name of ResourceBundle to be used for
     * localizing messages for the logger
     * @param handlerConfigs An Array of LogHandlerConfig instances
     */
    public LoggerConfig(String loggerName, 
                        Level level,
                        boolean useParentHandlers, 
                        String resourceBundleName,
                        LogHandlerConfig... handlerConfigs) {
        if(loggerName == null)
            throw new IllegalArgumentException("loggerName is null");
        if(level == null)
            throw new IllegalArgumentException("level is null");
        this.loggerName = loggerName;
        this.level = level;
        this.resourceBundleName = resourceBundleName;
        this.useParentHandlers = useParentHandlers;
        if(handlerConfigs != null) {
            handlers = new LogHandlerConfig[handlerConfigs.length];
            System.arraycopy(handlerConfigs, 0, handlers, 0, handlers.length);
        } else {
            if(!useParentHandlers) {
                throw new IllegalArgumentException("The logger must include a Handler " +
                                                   "if it will not delegate to parent handlers.");
            }
            handlers = new LogHandlerConfig[0];
        }
    }

    public void close() {
        if(logger!=null) {
            for (Handler h : logger.getHandlers()) {
                if(handlerFilter!=null && h.getFilter()!=null && h.getFilter().equals(handlerFilter)) {
                    h.setFilter(null);
                }
            }
        }
    }

    /**
     * Get the Logger, configured with attributes
     *
     * @return A configured Logger
     */
    public Logger getLogger() {
        if(logger != null)
            return (logger);
        logger = LogManager.getLogManager().getLogger(loggerName);
        if(logger==null) {
            if(resourceBundleName != null) {
                logger = Logger.getLogger(loggerName, resourceBundleName);
            } else {
                logger = Logger.getLogger(loggerName);
            }
        }

        /*
         * Set the level of the logger to the declared level. This will change
         * the level for all named logger instances
         */
        if(myLogger.isLoggable(Level.FINEST))
            myLogger.finest("Logger ["+loggerName+"] Level set to : "+level.getName());
        logger.setLevel(level);

        if(myLogger.isLoggable(Level.FINEST))
            myLogger.finest("Logger ["+loggerName+"] uses parent handlers : "+ useParentHandlers);
        logger.setUseParentHandlers(useParentHandlers);

        if(handlers.length>0) {
            for (LogHandlerConfig h : handlers) {
                String candidateHandler = h.getHandlerClassName();
                try {
                    boolean handlerClassAssociated = false;
                    Handler[] currentHandlers = logger.getHandlers();
                    for (Handler currentHandler : currentHandlers) {
                        if (currentHandler.getClass().getName().equals(candidateHandler)) {
                            handlerClassAssociated = true;
                            break;
                        }
                    }
                    if (!handlerClassAssociated) {
                        if (myLogger.isLoggable(Level.FINEST))
                            myLogger.finest("Logger ["+loggerName+"] adding Handler ["+h.getHandlerClassName()+"]");
                        Handler handler = h.getHandler();
                        if(h.getLevel()==null)  {
                            handler.setLevel(level);
                        } else {
                            handler.setLevel(h.getLevel());
                        }
                        if (logger.getUseParentHandlers()) {
                            handlerFilter = new HandlerFilter(logger, handler);
                            handler.setFilter(handlerFilter);
                        }
                        logger.addHandler(handler);
                    }
                } catch (Throwable t) {
                    myLogger.log(Level.WARNING,
                                 "Getting Handler [" + h.getHandlerClassName() + "] for Logger [" + loggerName + "]",
                                 (t.getCause() == null ? t : t.getCause()));
                }
            }
        } else {
            List<Handler> handlers = collectHandlers(logger);
            setHandlerLevel(handlers, level);
        }
        return (logger);
    }

    /**
     * @return Get the Logger name
     */
    public String getLoggerName() {
        return (loggerName);
    }

    /**
     * @return Get the Level for the Logger
     */
    public Level getLoggerLevel() {
        return (level);
    }
    
    /**
     * Override hashCode
     */
    public int hashCode() {
        int hc = 17;
        hc = 37*hc+loggerName.hashCode();
        hc = 37*hc+level.hashCode();
        for (LogHandlerConfig handler : handlers)
            hc = 37 * hc + handler.hashCode();
        return(hc);
    }
    
    /**
     * Override equals
     */
    public boolean equals(Object obj) {
        if(this == obj)
            return(true);
        if(!(obj instanceof LoggerConfig))
            return(false);        
        LoggerConfig that = (LoggerConfig)obj;               
        if(this.loggerName.equals(that.loggerName) &&
           this.level.equals(that.level)) {
            if(this.handlers.length == that.handlers.length) {
                for (LogHandlerConfig handler : this.handlers) {
                    boolean matched = false;
                    for (int j = 0; j < that.handlers.length; j++) {
                        if (handler.equals(that.handlers[j])) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched)
                        return (false);
                }
            }
            return(true);                
        }                
        return(false);
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Logger=").append(loggerName)
            .append(", Level=").append(level)
            .append(", UseParentHandlers=").append(useParentHandlers).append("\n");
        for (LogHandlerConfig handler : handlers) {
            buffer.append("Handler=").append(handler.getHandlerClassName());
            if (handler.formatterClassName != null) {
                buffer.append(", Formatter=").append(handler.formatterClassName).append("\n");
            } else {
                buffer.append("\n");
            }
        }
        return (buffer.toString());
    }
    
    /**
     * Determine if the current Logger is a new Logger
     * 
     * @param lConf The LoggerConfig to examine
     * @param loggerConfigs The current LoggerConfig
     * @return True if the Logger is new, false otherwise
     */
    public static boolean isNewLogger(LoggerConfig lConf, LoggerConfig[] loggerConfigs) {
        boolean matched = false;
        for (LoggerConfig loggerConfig : loggerConfigs) {
            if (loggerConfig.getLoggerName().equals(lConf.getLoggerName())) {
                matched = true;
            }
        }
        return !matched;
    }
    
    /**
     * Determine if the LoggerConfig provides a Level change
     * 
     * @param lConf The LoggerConfig to examine
     * @param loggerConfigs The current LoggerConfig
     * @return True if the Level requires changing, false otherwise
     */
    public static boolean levelChanged(LoggerConfig lConf, LoggerConfig[] loggerConfigs) {
        boolean levelChanged = false;
        for (LoggerConfig loggerConfig : loggerConfigs) {
            if (loggerConfig.getLoggerName().equals(lConf.getLoggerName())) {
                if (!loggerConfig.getLoggerLevel().
                    equals(lConf.getLoggerLevel())) {
                    levelChanged = true;
                    break;
                }
            }
        }
         return(levelChanged);   
    }

    /**
     * Filter for Handlers that handles parent Handler delegation. This avoids
     * multiple records being logged
     */
    private class HandlerFilter implements Filter {
        Logger logger;
        Handler handler;

        private HandlerFilter(Logger logger, Handler handler) {
            this.logger = logger;
            this.handler = handler;
        }

        public boolean isLoggable(LogRecord logRecord) {
            Level level = logRecord.getLevel();
            if(level.intValue()<handler.getLevel().intValue())
                return(false);
            Logger rootLogger = null;
            Logger temp = logger;
            while(rootLogger == null) {
                Logger l = temp.getParent();
                Handler[] handlers = l.getHandlers();
                for (Handler h : handlers) {
                    if(h.getClass().getName().equals(handler.getClass().getName()) &&
                       h.getLevel().intValue() >= handler.getLevel().intValue()) {
                        return (level.intValue() < h.getLevel().intValue());
                    }
                }
                if(l.getName().equals(""))
                    rootLogger = l;
                else
                    temp = l;
            }
            return(true);
        }
    }

    private List<Handler> collectHandlers(Logger l) {
        List<Handler> handlers = new ArrayList<Handler>();
        if(l.getHandlers().length>0) {
            Collections.addAll(handlers, l.getHandlers());
        }
        if(l.getUseParentHandlers() && l.getParent()!=null) {
            handlers.addAll(collectHandlers(l.getParent()));
        }
        return handlers;
    }

    private void setHandlerLevel(List<Handler> handlers, Level level) {
        for(Handler h : handlers) {
            Level was = h.getLevel();
            if(level.intValue()<h.getLevel().intValue()) {
                h.setLevel(level);
            }
        }
    }

    /**
     *  Provide a way to pass parameter lists around. 
     */
    public static class FormalArgument implements Serializable {
        static final long serialVersionUID = 1L;
        private String dataType;
        private String value;
        
        /**
         *  Constructs a model of a single argument in a parameter list.
         *
         *  @param dataType The argument's data type.
         *  @param value The argument's value.
         */
        public FormalArgument (String dataType, String value) {
            this.dataType = dataType;
            this.value = value;
        }
        
        /**
         * @return Returns the argument's data type.
         */
        public String getDataType() { 
            return(dataType); 
        }
        
        /**
         * @return Returns the argument's value.
         */
        public String getValue() { 
            return(value); 
        }
    }
    
    /**
     * LogerHandlerConfig provides attributes needed to create a 
     * {@link java.util.logging.Handler}
     */
    public static class LogHandlerConfig implements Serializable {
        static final long serialVersionUID = 1L;
        /** The log Handler */
        private transient Handler handler;
        /** The log Handler classname */
        private String handlerClassName;
        /** The Filter classname */
        private String formatterClassName;
        /**
         * The log level specifying which message levels will be logged by the
         * Handler
         */
        private Level level;
        /** Parameters for Handler construction */
        private List<FormalArgument> handlerArgList = new LinkedList<FormalArgument>();

        /**
         * Create a LogHandlerConfig
         * 
         * @param handlerClassName The class name (suitable for Class.forName use) 
         * of a log {@link java.util.logging.Handler} to receive logging messages
         */
        public LogHandlerConfig(String handlerClassName) {
            this(handlerClassName, null, null, null);
        }

        /**
         * Create a LogHandlerConfig
         *
         * @param handlerClassName The class name (suitable for Class.forName use)
         * of a log {@link java.util.logging.Handler} to receive logging messages
         * @param level Set the log level specifying which message levels will
         * be logged by the Handler. Message levels lower than this value will
         * be discarded
         */
        public LogHandlerConfig(String handlerClassName, Level level) {
            this(handlerClassName);
            this.level = level;
        }

        /**
         * Create a LogHandlerConfig
         * 
         * @param handlerClassName The class name (suitable for Class.forName
         * use) of a log Handler to receive logging messages
         * @param level Set the log level specifying which message levels will
         * be logged by the Handler. Message levels lower than this value will
         * be discarded
         * @param params Constructor parameters to create the Handler
         * @param formatterClassName The class name (suitable for
         * Class.forName use) of a java.util.logging.Formatter to use with the
         * Handler
         */
        public LogHandlerConfig(String handlerClassName,
                                Level level,
                                List<FormalArgument> params,                                
                                String formatterClassName) {
            if(handlerClassName == null)
                throw new IllegalArgumentException("handlerClassName is null");
            this.handlerClassName = handlerClassName;
            this.level = level;
            if(params != null)
                handlerArgList.addAll(params);
            this.formatterClassName = formatterClassName;
        }
        
        /**
         * Create a LogHandlerConfig. 
         * 
         * @param handler A log Handler to receive logging messages
         */
        public LogHandlerConfig(Handler handler) {
            if(handler == null)
                throw new IllegalArgumentException("handler is null");
            this.handler = handler;
            this.handlerClassName = handler.getClass().getName();
        }

        /**
         * Create a LogHandlerConfig.
         *
         * @param handler A log Handler to receive logging messages
         * @param level Set the log level specifying which message levels will
         * be logged by the Handler. Message levels lower than this value will
         * be discarded
         */
        public LogHandlerConfig(Handler handler, Level level) {
            if(handler == null)
                throw new IllegalArgumentException("handler is null");
            this.handler = handler;
            this.level = level;
            this.handlerClassName = handler.getClass().getName();
        }

        /**
         * Get the handler class name
         * 
         * @return String The Handler class name
         */
        String getHandlerClassName() {
            return (handlerClassName);
        }

        /**
         * Override hashCode
         */
        public int hashCode() {
            int hc = 17;
            hc = 37*hc+handlerClassName.hashCode();
            return(hc);
        }
        
        /**
         * Override equals
         */
        public boolean equals(Object obj) {
            if(this == obj)
                return(true);
            if(!(obj instanceof LogHandlerConfig))
                return(false);        
            LogHandlerConfig that = (LogHandlerConfig)obj;               
            if(this.handlerClassName.equals(that.handlerClassName))
                return(true);        
            return(false);
        }
        
        /**
         * Get the handler
         * 
         * @return Handler A suitable Handler
         *
         * @throws Exception if the handler cannot be loaded
         */
        public Handler getHandler() throws Exception {
            if(handler == null) {
                Class handlerClass = 
                    Thread.currentThread().getContextClassLoader().
                                           loadClass(handlerClassName);
                if(!handlerArgList.isEmpty()) {
                    Class[] parameterTypes = getParameterTypes();
                    Object[] initArgs = getInitArgs();
                    java.lang.reflect.Constructor constructor = 
                        handlerClass.getConstructor(parameterTypes);
                    handler = (Handler)constructor.newInstance(initArgs);
                } else {
                    handler = (Handler)handlerClass.newInstance();
                }
                //handler.setLevel(level);
                if(formatterClassName != null) {
                    Class formatClass = 
                        Class.forName(formatterClassName,
                                      true,
                                      Thread.currentThread().getContextClassLoader());
                    Formatter formatter = (Formatter)formatClass.newInstance();
                    handler.setFormatter(formatter);
                }
            }
            return (handler);
        }

        /**
         * Get the {@link java.util.logging.Level} to set for the handler
         *
         * @return The {@link java.util.logging.Level} for the handler, or null
         * to use the enclosing Logger's level
         */
        public Level getLevel() {
            return level;
        }

        /*
         * Get the parameter types for the Handler
         */
        private Class[] getParameterTypes() throws Exception {
            List<Class> classes = new LinkedList<Class> ();
            for (FormalArgument arg : handlerArgList) {
                String param = arg.getDataType();
                if (param.equals("boolean"))
                    classes.add(Boolean.TYPE);
                else if (param.equals("byte"))
                    classes.add(Byte.TYPE);
                else if (param.equals("short"))
                    classes.add(Short.TYPE);
                else if (param.equals("int"))
                    classes.add(Integer.TYPE);
                else if (param.equals("long"))
                    classes.add(Long.TYPE);
                else if (param.equals("float"))
                    classes.add(Float.TYPE);
                else if (param.equals("double"))
                    classes.add(Double.TYPE);
                else
                    classes.add(Class.forName(param,
                                              true,
                                              this.getClass().getClassLoader()));
            }
            return classes.toArray(new Class[classes.size()]);
        }

        /*
         * Get the initialization arguments based on parameter types for the
         * Handler
         */
        private Object[] getInitArgs() throws Exception {
            List<Object> values = new LinkedList<Object>();
            for (FormalArgument arg : handlerArgList) {
                String type = arg.getDataType();
                String value = arg.getValue();
                if (type.equals("boolean"))
                    values.add(Boolean.valueOf(value));
                else if (type.equals("byte"))
                    values.add(new Byte(value));
                else if (type.equals("short"))
                    values.add(new Short(value));
                else if (type.equals("int"))
                    values.add(new Integer(value));
                else if (type.equals("long"))
                    values.add(new Long(value));
                else if (type.equals("float"))
                    values.add(new Float(value));
                else if (type.equals("double"))
                    values.add(new Double(value));
                else {
                    Class clazz = Class.forName(type,
                                                true,
                                                this.getClass().getClassLoader());
                    if (clazz == String.class) {
                        value = transformString(value);
                        values.add(value);
                    } else if (clazz == Boolean.class)
                        values.add(Boolean.valueOf(value));
                    else if (clazz == Byte.class)
                        values.add(new Byte(value));
                    else if (clazz == Short.class)
                        values.add(new Short(value));
                    else if (clazz == Integer.class)
                        values.add(new Integer(value));
                    else if (clazz == Long.class)
                        values.add(new Long(value));
                    else if (clazz == Float.class)
                        values.add(new Float(value));
                    else if (clazz == Double.class)
                        values.add(new Double(value));
                }
            }
            return values.toArray(new Object[values.size()]);
        }

        /**
         * Transform a String that has control characters into a formatted
         * String with machine dependant File.separator and System property
         * values set
         *
         * @param input The input string
         *
         * @return A transformed string
         */
        String transformString(final String input) throws Exception {
            StringBuilder buffer = new StringBuilder();
            int index;
            String transformed = input;
            while ((index = input.indexOf("${")) != -1) {
                buffer.append(input.substring(0, index));
                int end = input.indexOf("}");
                if(end == -1)
                    continue;
                String value = input.substring(index + 2, end);
                if(value.equals("/")) {
                    buffer.append(File.separator);
                } else {
                    String property = System.getProperty(value);
                    if(property == null)
                        throw new Exception("Cannot resolve property ["+value+"]");
                    buffer.append(property);
                }
                transformed = buffer.toString() + input.substring(end + 1);
                buffer.delete(0, buffer.length());
            }
            return (transformed);
        }
    }
}
