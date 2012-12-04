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
package org.rioproject.config;


import org.slf4j.LoggerFactory

import java.lang.reflect.Constructor

import net.jini.config.*

/**
 * Provides support for Groovy based configuration.
 *
 * @author Dennis Reedy
 */
class GroovyConfig implements Configuration {
    private Map<String, GroovyObject> groovyConfigs = new HashMap<String, GroovyObject>()
    private ConfigurationFile configFile
    private List <String> visited = new ArrayList<String>()
    private def logger = LoggerFactory.getLogger(GroovyConfig.class.getPackage().name)

    GroovyConfig(String gFile) {
        File f = new File(gFile)
        GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader())
        parseAndLoad(f.newInputStream(), gcl)
    }

    /**
     * Constructor required for Jini Configuration
     */
    public GroovyConfig(String[] args, ClassLoader loader) {
        if(args==null || args.length==0) {
            configFile = new ConfigurationFile(args, loader)
        } else {
            if(!args[0].endsWith(".groovy")) {
                configFile = new ConfigurationFile(args, loader)
            } else {
                /* Make sure we have all groovy files */
                checkInputs(args)
                traverseInputs(args, loader)
            }
        }
    }

    def checkInputs(String[] args) {
        args.each { arg ->
            if(arg.endsWith(".groovy")) {
                logger.trace(arg)
            } else {
                StringBuffer buffer = new StringBuffer()
                args.each { a ->
                    buffer.append(a).append(' ')
                }
                throw new ConfigurationException('When providing multiple configuration files, '+
                                                 'they must all be Groovy configurations ['+buffer.toString()+']');
            }
        }
    }

    def traverseInputs(String[] args, ClassLoader loader) {
        if(loader==null)
            loader = Thread.currentThread().getContextClassLoader()
        GroovyClassLoader gcl = new GroovyClassLoader(loader)
        args.each { arg ->
            String groovyFile = arg
            InputStream is = null
            long t0 = System.currentTimeMillis()
            try {
                if(groovyFile.startsWith("jar:")) {
                    String resource = groovyFile
                    int ndx = groovyFile.indexOf("!")
                    if(ndx!=-1)
                        resource = groovyFile.substring(ndx+2)
                    is = loader.getResourceAsStream(resource)
                } else {
                    if(groovyFile.startsWith("file:"))
                        is = new URL(groovyFile).openStream()
                    else
                        is = new FileInputStream(groovyFile);
                }
                parseAndLoad(is, gcl)
            } catch (FileNotFoundException e) {
                throw new ConfigurationNotFoundException("The configuration file [${groovyFile}] does not exist", e)
            } catch(Throwable t) {
                throw new ConfigurationException("The configuration file [${groovyFile}] could not be parsed", t)
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
                logger.trace "Time to parse ${groovyFile} : ${(System.currentTimeMillis()-t0)} milliseconds"
            }
        }
        gcl = null
    }

    def clear() {
        visited.clear()
        if(groovyConfigs!=null) {
            /*for(Map.Entry<String, GroovyObject> entry : groovyConfigs)  {
                InvokerHelper.removeClass(entry.value.class)
            }*/
            groovyConfigs.clear()
            groovyConfigs = null
        }
    }

    def parseAndLoad(InputStream is, GroovyClassLoader gcl) {
        gcl.parseClass(is)
        for(Class groovyClass : gcl.loadedClasses) {
            if(visited.contains(groovyClass.name))
                continue
            visited.add(groovyClass.name)
            for(Constructor c : groovyClass.getConstructors())  {
                if(c.parameterTypes.length==0) {
                    c.setAccessible(true)
                    GroovyObject gO = (GroovyObject)c.newInstance()
                    String component
                    if(groovyClass.isAnnotationPresent(Component.class)) {
                        Component comp = groovyClass.getAnnotation(Component.class)
                        component = comp.value()
                    } else {
                        component = getComponentName(gO.getMetaClass())
                    }
                    if (!validQualifiedIdentifier(component)) {
                        throw new IllegalArgumentException("component must be a valid qualified identifier");
                    }
                    groovyConfigs.put(component, gO)
                }
            }
        }
    }

    def String getComponentName(MetaClass mc) {
        String component = mc.getTheClass().name
        component = component.replace("_", ".")
        return component
    }

    def Object getEntry(String component, String name, Class type) {
        return getEntry(component, name, type, NO_DEFAULT);
    }

    def Object getEntry(String component, String name, Class type, Object defaultValue) {
        return getEntry(component, name, type, defaultValue, NO_DATA);
    }

    def Object getEntry(String component, String name, Class type, Object defaultValue, Object data) {
        if(configFile!=null)
            return configFile.getEntry(component, name, type, defaultValue, data)
        logger.trace("component=${component}, "+
                     "name=${name}, "+
                     "type=${type.getName()}, "+
                     "defautValue=${defaultValue}, "+
                     "data=${data}")
        if (component == null) {
            throw new NullPointerException("component cannot be null");
        } else if (name == null) {
            throw new NullPointerException("name cannot be null");
        } else if (type == null) {
            throw new NullPointerException("type cannot be null");
        } else if (defaultValue != NO_DEFAULT) {
            if (type.isPrimitive() ?
                (defaultValue == null ||
                 getPrimitiveType(defaultValue.getClass()) != type) :
                (defaultValue != null && !type.isAssignableFrom(defaultValue.getClass()))) {
                throw new IllegalArgumentException("defaultValue is of wrong type");
            }
        }

        GroovyObject groovyConfig = null;
        for(Map.Entry<String, GroovyObject> entry : groovyConfigs)  {
            if(entry.key.equals(component)) {
                groovyConfig = entry.value;
                break;
            }
        }
        if(groovyConfig==null) {
            if(defaultValue==NO_DEFAULT)
                throw new NoSuchEntryException("component name [${component}] not found in Groovy files, "+
                                               "and no default value was given.")
            else
                return defaultValue;
        }

        Object value
        if(data==NO_DATA) {
            try {
                value = groovyConfig.getProperty(name)
                logger.trace "Configuration entry [${component}.${name}] found in "+
                             "GroovyObject ${groovyConfig}, assign returned value: ${value}"
            } catch(MissingPropertyException e) {
                if(!e.getProperty().equals(name))
                    throw new ConfigurationException(e.getMessage(), e)

                logger.trace("${e.getClass().getName()}: looking for configuration entry "+
                             "[${component}.${name}] in GroovyObject "+
                             groovyConfig)
                if(defaultValue==NO_DEFAULT) {
                    throw new NoSuchEntryException("entry not found for component: $component, name: $name", e);
                } else {
                    logger.trace "Configuration entry [${component}.${name}] not found in "+
                                 "GroovyObject ${groovyConfig}, assign provided default: ${defaultValue}"
                    value = defaultValue;
                }
            }
        } else {
            try {
                MetaMethod mm = null
                String methodName = 'get'+name[0].toUpperCase()+name.substring(1, name.length())
                List<MetaMethod> methods = groovyConfig.metaClass.methods
                for(MetaMethod m : methods) {
                    if(m.name==methodName) {
                        logger.trace "Found matching method name [${methodName}], check for type match"
                        Class[] paramTypes = m.nativeParameterTypes
                        if(paramTypes.length==1 && paramTypes[0].isAssignableFrom(data.class)) {
                            mm = m;
                            break;
                        }
                    }
                }
                if(mm==null) {
                    if(defaultValue==NO_DEFAULT)
                        throw new NoSuchEntryException("entry not found for component: $component, "+
                                                       "name: $name, data argument: $data");

                    value = defaultValue;
                } else {
                    value = mm.invoke(groovyConfig, data)
                }
            } catch(MissingPropertyException e) {
                throw new NoSuchEntryException("entry not found for component: $component, name: $name, "+
                                               "data argument: $data", e);
            }
        }

        if(value!=null) {
            boolean mismatch = false
            if(type.isPrimitive()) {
                if(!type.isAssignableFrom(getPrimitiveType(value.getClass()))) {
                    mismatch = true
                }
            } else if(!type.isAssignableFrom(value.getClass())) {
                mismatch = true
            }
            if(mismatch) {
                throw new ConfigurationException("entry for component $component, name $name "+
                                                 "is of wrong type: ${value.getClass().name}, "+
                                                 "expected: ${type.name}");
            }
        }
        return value
    }

    /**
     * Returns the primitive type associated with a wrapper type or null if the
     * argument is not a wrapper type.
     *
     * @param type the wrapper type
     * @return the associated primitive type or null
     */
    def getPrimitiveType(Class type) {
        if (type == Boolean.class) {
            return Boolean.TYPE;
        } else if (type == Byte.class) {
            return Byte.TYPE;
        } else if (type == Character.class) {
            return Character.TYPE;
        } else if (type == Short.class) {
            return Short.TYPE;
        } else if (type == Integer.class) {
            return Integer.TYPE;
        } else if (type == Long.class) {
            return Long.TYPE;
        } else if (type == Float.class) {
            return Float.TYPE;
        } else if (type == Double.class) {
            return Double.TYPE;
        } else {
            return null;
        }
    }

    /**
     * Checks if the argument is a valid <i>Identifier</i>, as defined in the
     * <i>Java(TM) Language Specification</i>.
     *
     * @param name the name to check
     * @return <code>true</code> if <code>name</code> is a valid
     * 	       <i>Identifier</i>, else <code>false</code>
     */
    boolean validIdentifier(String name) {
        if (name == null || name.length() == 0 ||
            !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false
        }
        for (int i = name.length(); --i > 0;) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false
            }
        }
        /* Check across the names of all Java programming language keywords, plus
         * 'null', 'true', and 'false', which are not keywords, but are not
         * permitted as identifiers. */
        return !['abstract','continue','for',       'new',      'switch',
                 'assert',  'default', 'goto',      'package',  'synchronized',
                 'boolean', 'do',      'if',        'private',  'this',
                 'break',   'double',  'implements','protected','throw',
                 'byte',    'else',    'import',    'public',   'throws',
                 'case',    'enum',    'instanceof','return',   'transient',
                 'catch',   'extends', 'int',       'short',    'try',
                 'char',    'final',   'interface', 'static',   'void',
                 'class',   'finally', 'long',      'strictfp', 'volatile',
                 'const',   'float',   'native',    'super',    'while',
                 'null',    'true',    'false'].contains(name)
    }


    /**
     * Checks if the argument is a valid <i>QualifiedIdentifier</i>, as defined
     * in the <i>Java Language Specification</i>.
     *
     * @param name the name to check
     * @return <code>true</code> if <code>name</code> is a valid
     * 	       <i>QualifiedIdentifier</i>, else <code>false</code>
     */
    boolean validQualifiedIdentifier(String name) {
        if (name == null)
            return false
        int offset = 0
        int dot = 0
        while (dot >= 0) {
            dot = name.indexOf('.', offset)
            String id = name.substring(offset, dot < 0 ? name.length() : dot)
            if (!validIdentifier(id))
                return false;
            offset = dot + 1;
        }
        return true;
    }
}
