package org.rioproject.deploy;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rioproject.security.SecureEnv;

import java.io.IOException;

import static org.junit.Assert.*;

public class StagedDataTest {

    @BeforeClass
    public static void setup() throws Exception {
        SecureEnv.setup();
    }

    @Test
    public void testGetDownloadSize() throws IOException {
        StagedData stagedData = new StagedData();
        stagedData.setLocation("https://us.mirrors.quenda.co/apache/tomcat/tomcat-9/v9.0.36/bin/apache-tomcat-9.0.36.zip");
        long size = stagedData.getDownloadSize();
        assertTrue(size != -1);
    }

}