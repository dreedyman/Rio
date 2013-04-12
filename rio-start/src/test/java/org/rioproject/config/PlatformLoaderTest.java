package org.rioproject.config;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * @author Rafał Krupiński
 */
public class PlatformLoaderTest {
	@org.junit.Test
	public void testParsePlatform() throws Exception {
		PlatformLoader pl = new PlatformLoader();
		PlatformCapabilityConfig[] platform = pl.parsePlatform(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile(), "org/rioproject/config").getPath());
		assertTrue(platform.length == 1);
		PlatformCapabilityConfig capability = platform[0];
		String[] classpath = capability.getClasspath();
		assertTrue(classpath.length == 1);
		String entry = classpath[0];
		assertTrue(entry.endsWith("net/jini/jini-core/2.1/jini-core-2.1.jar"));
		assertTrue(new File(entry).exists());
	}
}
