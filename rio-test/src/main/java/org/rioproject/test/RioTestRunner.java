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
package org.rioproject.test;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.rioproject.RioVersion;
import org.rioproject.logging.LoggingSystem;
import org.rioproject.url.artifact.ArtifactURLStreamHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

/**
 * RioTestRunner is a custom extension of {@link BlockJUnit4ClassRunner}
 *
 * <p>
 * <em>NOTE</em>: RioTestRunner requires JUnit 4.5. or later
 */
public class RioTestRunner extends BlockJUnit4ClassRunner {
    static final Logger logger = LoggerFactory.getLogger(RioTestRunner.class.getName());
    static {
        if (System.getSecurityManager() == null) {
            Utils.checkSecurityPolicy();
            System.setSecurityManager(new SecurityManager());
        }
        if(LoggingSystem.usingJUL()) {
            if(System.getProperty("java.util.logging.config.file")==null) {
                LogManager logManager = LogManager.getLogManager();
                try {
                    logManager.readConfiguration(Thread.currentThread().getClass().getResourceAsStream("/default-logging.properties"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        /* If the artifact URL has not been configured, set it up */
        try {
            new URL("artifact:foo");
        } catch (MalformedURLException e) {
            URL.setURLStreamHandlerFactory(new ArtifactURLStreamHandlerFactory());
        }
    }
    TestManager testManager;
    TestConfig testConfig;

    /**
     * Constructs a new <code>RioTestRunner</code>
     *
     * @param clazz the Class object corresponding to the test class to be run
     *
     * @throws org.junit.runners.model.InitializationError If the class
     * cannot be created
     */
    public RioTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        logger.debug("TestRunner constructor called with [{}].", clazz);
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement afterStatement = super.withAfterClasses(statement);
        return new TestCaseShutdownStatement(afterStatement, testManager);
    }

    /**
     * Check whether the test is enabled in the first place. This prevents classes with
     * a non-matching <code>@IfPropertySet</code> annotation from running altogether,
     * even skipping the execution of <code>prepareTestInstance</code> listener methods.
     *
     * This method also creates and initializes a {@link TestManager} providing
     * Rio testing functionality to standard JUnit tests.
     *
     * @see #createTestConfig(String)
     */
    @Override
    public void run(RunNotifier notifier) {
        if (!isTestClassEnabled(getTestClass().getJavaClass())) {
            logger.info("Test class {}  is not enabled, skipping", getTestClass().getJavaClass().getName());
            notifier.fireTestIgnored(getDescription());
            return;
        }

        Utils.setEnvironment();
        testConfig = createTestConfig(getTestClass().getName());
        testManager = testConfig.getTestManager();
        testManager.init(testConfig);
        
        for (FrameworkMethod fMethod : getTestClass().getAnnotatedMethods(SetTestManager.class)) {
            Method method = fMethod.getMethod();
            if(Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                try {
                    method.invoke(null, getManager());
                } catch (Exception e) {
                    logger.warn("Invoking static method [{}] with declared annotation {}",
                                method.getName(), SetTestManager.class.getName(),
                                e);
                }
                //break;
            }
        }
        for(Field field : getAnnotatedFields(getTestClass().getJavaClass(),
                                             SetTestManager.class)) {
            field.setAccessible(true);
            if(Modifier.isStatic(field.getModifiers())) {
                try {
                    field.set(null, getManager());
                } catch (IllegalAccessException e) {
                    logger.warn("Invoking static field [{}] with declared annotation {}",
                                field.getName(), SetTestManager.class.getName(),
                                e);
                }
                //break;
            }
        }
        super.run(notifier);
    }

    /**
     * Delegates to {@link BlockJUnit4ClassRunner#createTest()} to create the test
     * {@link BlockJUnit4ClassRunner#createTest()}, first checks for TestManager injection
     */
    @Override
    protected Object createTest() throws Exception {
        Object testInstance = super.createTest();

        for (FrameworkMethod fMethod : getTestClass().getAnnotatedMethods(SetTestManager.class)) {
            Method method = fMethod.getMethod();
            if(!Modifier.isStatic(method.getModifiers())) {
                method.setAccessible(true);
                try {
                    method.invoke(null, getManager());
                } catch (Exception e) {
                    logger.warn("Invoking method [{}] with declared annotation {}",
                                method.getName(), SetTestManager.class.getName(),
                                e);
                }
                break;
            }
        }
        for(Field field : getAnnotatedFields(testInstance.getClass(),
                                             SetTestManager.class)) {
            field.setAccessible(true);
            if(!Modifier.isStatic(field.getModifiers())) {
                try {
                    field.set(testInstance, getManager());
                } catch (IllegalAccessException e) {
                    logger.warn("Invoking field [{}] with declared annotation {}",
                                field.getName(), SetTestManager.class.getName(),
                                e);
                }
                break;
            }
        }        
        return testInstance;
    }

    @Override
    protected Statement withPotentialTimeout(FrameworkMethod method,
                                             Object target,
                                             Statement next) {
        Test testAnnotation = method.getAnnotation(Test.class);
        long junitTimeout = 0;
        if(testAnnotation != null && testAnnotation.timeout() > 0)
            junitTimeout = testAnnotation.timeout();

        long testTimeout = testManager.getTestConfig().getTimeout();
        if (junitTimeout > 0) {
            if(testTimeout > 0) {
                logger.info("Test method [{}] has a default test configuration timeout value of {} "+
                            "and JUnit's @Test(timeout={}) annotation. The JUnit declaration takes precedence " +
                            "over the configured default timeout.", method.getMethod(), testTimeout, junitTimeout);
            }
			return super.withPotentialTimeout(method, target, next);
		}
        if(testTimeout>0)
            return new FailOnTimeout(next, testTimeout);

        return super.withPotentialTimeout(method, target, next);
    }

    /**
     * Creates A testConfig. Can be overridden by subclasses.
     *
     * @param testClassName The name of the test class to be run
     *
     * @return A TestConfig
     */
    protected TestConfig createTestConfig(String testClassName) {
        TestConfig testConfig = new TestConfig(testClassName);
        String testConfigLocation = System.getProperty("org.rioproject.test.config");

        /* If the property isnt declared look for the test configuration
        in the expected place. If found, use it */
        if(testConfigLocation==null) {
            File tc = new File(System.getProperty("user.dir"),
                               "src"+File.separator+
                               "test"+File.separator+
                               "conf"+File.separator+
                               "test-config.groovy");
            if(tc.exists())
                testConfigLocation = tc.getPath();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Test Configuration for ").append(testClassName);
        sb.append("\n");
        sb.append("==========================================");
        sb.append("\n");
        sb.append("Rio ");
        sb.append(RioVersion.VERSION);
        sb.append(" build ");
        sb.append(RioVersion.getBuildNumber());
        sb.append("\n");
        sb.append("==========================================");
        sb.append("\n");
        sb.append("JAVA_HOME:     ");
        sb.append(System.getProperty("JAVA_HOME"));
        sb.append("\n");
        sb.append("RIO_HOME:      ");
        sb.append(System.getProperty("RIO_HOME"));
        sb.append("\n");

        if(testConfigLocation!=null) {
            sb.append("test-config:   ").append(testConfigLocation);
            sb.append("\n");
            testConfig.loadConfig(testConfigLocation);
            sb.append("groups:        ").append((testConfig.getGroups()==null?
                                         "<not declared>":testConfig.getGroups()));
            sb.append("\n");
            sb.append("locators:      ").append((testConfig.getLocators()==null?
                                         "<not declared>":testConfig.getLocators()));
            sb.append("\n");
            sb.append("numCybernodes: ").append(testConfig.getNumCybernodes());
            sb.append("\n");
            sb.append("numMonitors:   ").append(testConfig.getNumMonitors());
            sb.append("\n");
            if(testConfig.getNumLookups()>0) {
                sb.append("numLookups:    ").append(testConfig.getNumLookups());
                sb.append("\n");
            }
            sb.append("opstring:      ").append((testConfig.getOpString()==null?
                                         "<not declared>":testConfig.getOpString()));
            sb.append("\n");
            sb.append("autoDeploy:    ").append(testConfig.autoDeploy());
            sb.append("\n");

            if(testConfig.autoDeploy())
                Assert.assertNotNull("You have declared that the test case " +
                                     "["+testClassName+"] have it's " +
                                     "OperationalString automatically " +
                                     "deployed, but have not declared an " +
                                     "OperationalString. You must declare " +
                                     "an OperationalString using the " +
                                     "opstring property for the test case " +
                                     "configuration",
                                     testConfig.getOpString());
        }
        sb.append("testManager:   ").append(testConfig.getTestManager().getClass().getName());
        sb.append("\n");
        sb.append("harvest:       ").append(testConfig.runHarvester());
        sb.append("\n");
        sb.append("loggingSystem: ").append(testConfig.getLoggingSystem().name().toLowerCase());
        sb.append("\n");
        sb.append("timeout:       ").append((testConfig.getTimeout()==0?
                                     "<not declared>":testConfig.getTimeout()));
        sb.append("\n");
        sb.append("==========================================");
        logger.info(sb.toString());
        return testConfig;
    }

    /**
     * Get the {@link TestManager}  associated with this runner.
     * @return The created Manager
     */
    protected final TestManager getManager() {
        return this.testManager;
    }

    /*
     * Invokes the supplied {@link Method test method} and notifies the supplied
     * {@link RunNotifier} of the appropriate events.
     * @see #createTest()
     * @see BlockJUnit4ClassRunner#invokeTestMethod(Method, RunNotifier)
     */
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        logger.debug("Invoking test method [{}]", method.getMethod().toGenericString());
        if (!isTestMethodEnabled(method.getMethod())) {
            logger.info("Test method {} is not enabled, skipping", method.getMethod().toGenericString());
            notifier.fireTestIgnored(getDescription());
            return;
        }
        super.runChild(method, notifier);
    }

    boolean isTestClassEnabled(Class<?> testClass) {
		return isPropertySet(testClass.getAnnotation(IfPropertySet.class));
    }

    boolean isTestMethodEnabled(Method testMethod) {
		return isPropertySet(testMethod.getAnnotation(IfPropertySet.class));
    }

    boolean isPropertySet(IfPropertySet ifPropertySet) {
        if (ifPropertySet == null) {
			return true;
		}
        boolean isSet = false;
        String value = System.getProperty(ifPropertySet.name());
        if(value==null)
            return false;
        String propertyValue = ifPropertySet.value();
        String notPropertyValue = ifPropertySet.notvalue();
        boolean wildcard = false;
        if(propertyValue.length()>0) {
            if(propertyValue.endsWith("*")) {
                propertyValue = propertyValue.substring(0, propertyValue.length()-1);
                wildcard = true;
            }
            isSet = (wildcard?value.startsWith(propertyValue):value.equals(propertyValue));
        }

        if(notPropertyValue.length()>0) {
            if(notPropertyValue.endsWith("*")) {
                notPropertyValue = notPropertyValue.substring(0, notPropertyValue.length()-1);
                wildcard = true;
            }
            isSet =
                !(wildcard?value.startsWith(notPropertyValue):value.equals(notPropertyValue));
        }
        return isSet;
    }

    List<Field> getAnnotatedFields(Class<?> tClass,
                                   Class<? extends Annotation> annotationClass) {
		List<Field> results= new ArrayList<Field>();
		for (Class<?> eachClass : getSuperClasses(tClass)) {
			Field[] fields= eachClass.getDeclaredFields();
			for (Field field : fields) {
				Annotation annotation= field.getAnnotation(annotationClass);
				if (annotation != null)
					results.add(field);
			}
		}
		return results;
    }

    private List<Class<?>> getSuperClasses(Class< ?> testClass) {
		ArrayList<Class<?>> results= new ArrayList<Class<?>>();
		Class<?> current= testClass;
		while (current != null) {
			results.add(current);
			current= current.getSuperclass();
		}
		return results;
	}

    class InitStatement extends Statement {
        Statement next;
        TestConfig testConfig;

        InitStatement(Statement next, TestConfig testConfig) {
            this.next = next;
            this.testConfig = testConfig;
        }

        @Override
        public void evaluate() throws Throwable {
            testManager.init(testConfig);
            next.evaluate();
        }
    }


    /**
     * A custom JUnit 4.5+ {@link Statement} that first checks if Harvester
     * should be run. This allows Harvester to be plugged ino the JUnit test
     * execution chain, executing after all <tt>@AfterClass</tt> invocations
     * have been run. This statement also undeploys deployed opstrings prior to
     * returning.
     */
    class TestCaseShutdownStatement extends Statement {
        Statement next;
        TestManager testManager;

        TestCaseShutdownStatement(Statement next, TestManager testManager) {
            this.next = next;
            this.testManager = testManager;
        }

        @Override
        public void evaluate() throws Throwable {
            List<Throwable> errors = new ArrayList<Throwable>();
            try {
                next.evaluate();
            } catch (Throwable e) {
                errors.add(e);
            }

            try {
                testManager.maybeRunHarvester();
            } catch (Exception e) {
                errors.add(e);
            }

            if(testManager.getDeployedOperationalStringManager()!=null) {
                try {
                    String undeploy =
                        testManager.getDeployedOperationalStringManager()
                            .getOperationalString().getName();
                    testManager.undeploy(undeploy);
                } catch (Throwable t) {
                    System.err.println("Failed automated undeployment as part of test case shutdown for " +
                                       "["+testManager.getOpStringToDeploy()+"], "+
                                       t.getClass().getName()+": "+t.getMessage());
                }
            }
            
            if (errors.isEmpty())
                return;

            if (errors.size() == 1)
                throw errors.get(0);

            throw new MultipleFailureException(errors);
        }
    }
}
