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
package org.rioproject.config

import groovy.util.logging.Slf4j
import net.jini.config.*
import org.codehaus.groovy.control.CompilerConfiguration

import java.lang.reflect.Constructor
/**
 * Provides support for Groovy based configuration.
 *
 * @author Dennis Reedy
 */
@Slf4j
class GroovyConfig implements net.jini.config.Configuration {
    private Map<String, GroovyObject> groovyConfigs = new HashMap<String, GroovyObject>()
    private ConfigurationFile configFile
    private List <String> visited = new ArrayList<String>()

    @SuppressWarnings("unused")
    GroovyConfig(String gFile) {
        File f = new File(gFile)
        GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader())
        parseAndLoad(new GroovyCodeSource(f), gcl)
    }

    /**
     * Constructor required for Jini Configuration
     */
    public GroovyConfig(String[] args, ClassLoader loader) {
        if(args==null || args.length==0) {
            configFile = new ConfigurationFile(args, loader)
        } else {
            if(args[0].endsWith(".config") || args[0].equals("-")) {
                configFile = new ConfigurationFile(args, loader)
            } else {
                /* Make sure we have all groovy files */
                checkInputs(args)
                traverseInputs(args, loader)
            }
        }
    }

    def checkInputs(String[] args) {
        for(String arg : args) {
            if(arg.endsWith(".groovy")) {
                log.trace(arg)
            } else if(arg.endsWith(".config")){
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
        ClassLoader cCL = Thread.currentThread().getContextClassLoader()
        if(loader==null)
            loader = cCL
        GroovyClassLoader gcl = new GroovyClassLoader(loader)
        Thread.currentThread().setContextClassLoader(gcl)
        try {
            for(String arg : args) {
                String groovySource = arg
                long t0 = System.currentTimeMillis()
                try {
                    GroovyCodeSource groovyCodeSource
                    if(groovySource.startsWith("jar:")) {
                        String resource = groovySource
                        int ndx = groovySource.indexOf("!")
                        if(ndx!=-1)
                            resource = groovySource.substring(ndx+2)
                        groovyCodeSource = new GroovyCodeSource(loader.getResource(resource))
                    } else {
                        if(groovySource.startsWith("file:") || groovySource.startsWith("http:") || groovySource.startsWith("https:")) {
                            groovyCodeSource = new GroovyCodeSource(new URL(groovySource))
                        } else {
                            File groovyFile = new File(groovySource)
                            if (groovyFile.exists()) {
                                groovyCodeSource = new GroovyCodeSource(groovyFile)
                            } else {
                                groovyCodeSource = new GroovyCodeSource((String)groovySource, "DynamicGroovyConfig", "groovy/script")
                            }
                        }
                    }
                    parseAndLoad(groovyCodeSource, gcl)
                } catch (FileNotFoundException e) {
                    throw new ConfigurationNotFoundException("The configuration file [${groovySource}] does not exist", e)
                } catch(Throwable t) {
                    throw new ConfigurationException("The configuration file [${groovySource}] could not be parsed", t)
                } finally {
                    log.debug "Time to parse ${groovySource} : ${(System.currentTimeMillis()-t0)} milliseconds"
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cCL)
            gcl = null
        }
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

    def parseAndLoad(GroovyCodeSource groovyCodeSource, GroovyClassLoader gcl) {
        if(groovyCodeSource.getName().endsWith(".class")) {
            GroovyClassLoader newCl
            try {
                CompilerConfiguration config = new CompilerConfiguration()
                config.classpath = groovyCodeSource.file.parentFile.path
                newCl = new GroovyClassLoader(gcl, config, true)
                String name = groovyCodeSource.file.name.substring(0, groovyCodeSource.file.name.indexOf("."))
                load(newCl.loadClass(name))
            } catch(Throwable t) {
                throw t
            } finally {
                newCl = null
            }
        } else {
            gcl.parseClass(groovyCodeSource)
            for(Class groovyClass : gcl.loadedClasses) {
                load(groovyClass)
            }
        }
    }

    def load(Class groovyClass)  {
        log.debug("Loading {}", groovyClass.name)
        if(visited.contains(groovyClass.name))
            return
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
        log.trace("component=${component}, name=${name}, type=${type.getName()}, defautValue=${defaultValue}, data=${data}")
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
                log.trace "Configuration entry [${component}.${name}] found in "+
                          "GroovyObject ${groovyConfig}, assign returned value: ${value}"
            } catch(MissingPropertyException e) {
                if(!e.getProperty().equals(name))
                    throw new ConfigurationException(e.getMessage(), e)

                log.trace("${e.getClass().getName()}: looking for configuration entry "+
                             "[${component}.${name}] in GroovyObject "+
                             groovyConfig)
                if(defaultValue==NO_DEFAULT) {
                    throw new NoSuchEntryException("entry not found for component: $component, name: $name", e);
                } else {
                    log.trace "Configuration entry [${component}.${name}] not found in "+
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
                        log.trace "Found matching method name [${methodName}], check for type match"
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
