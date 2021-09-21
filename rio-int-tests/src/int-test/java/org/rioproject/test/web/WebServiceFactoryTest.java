package org.rioproject.test.web;

import org.junit.*;
import org.rioproject.web.WebsterService;
import org.rioproject.web.WebsterServiceFactory;

import static org.junit.Assert.assertNotNull;

public class WebServiceFactoryTest {
    private WebsterService websterService;

    @After
    public void shutdown() {
        if (websterService != null) {
            websterService.terminate();
        }
    }

    @BeforeClass
    public static void setup() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
    }

    @Test
    public void createWebster() throws Exception {
        websterService = WebsterServiceFactory.createWebster(0, System.getProperty("user.dir"));
        assertNotNull(websterService);
    }

    @Test
    @Ignore
    public void createJetty() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        websterService = WebsterServiceFactory.createJetty(0, System.getProperty("user.dir"));
        assertNotNull(websterService);
    }
}
