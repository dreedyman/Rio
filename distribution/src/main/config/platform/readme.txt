This directory contains platform configuration files. The Platform
configuration file is used as follows:

- Provides attributes for creating PlatformCapability classes and declaring
   what jars will be common across all service classloaders. If jars are common
   they are loaded by the CommonClassLoader, and are in the classpath of all
   child class loaders. By default Rio and River technology jars are common
   across all service classloaders.

   For platform capabilities that are not loaded by the common classloader,
   services that declare that dependency will have the capability loaded by
   the service's classloader.

 - Provides a manifest of the platform jars that can be loaded.

Platform configuration files are loaded when Rio starts. The contents of this
directory will be scanned at startup time.

Groovy is used to declare platform configurations.

Using Groovy
=============
The Groovy class must provide a either a property called platformCapabilityConfig or
a method called getPlatformCapabilityConfigs. Depending on whether you are returning
a single PlatformCapabilityConfig or a Collection of PlatformCapabilityConfig objects.
An example is shown here:

Providing multiple PlatformCapabilityConfig objects:

class PlatformConfig  {

    def getPlatformCapabilityConfigs() {
        def configs = []
        configs << new PlatformCapabilityConfig("Foo",
                                     "1.0",
                                     "Description",
                                     "Manufacturer",
                                     "classpath-directory${File.separator}foo.jar")
        return configs
    }
}

Providing a single PlatformCapabilityConfig object:

class PlatformConfig  {
    def platformCapabilityConfig = new PlatformCapabilityConfig("Foo",
                                     "1.0",
                                     "Description",
                                     "Manufacturer",
                                     "classpath-directory${File.separator}foo.jar")
}

You can also declare an artifact, the classpath will be resolved when the PlatformCapabilityConfig is created

class PlatformConfig  {

    def getPlatformCapabilityConfigs() {
        def configs = []
        configs << new PlatformCapabilityConfig("Foo",
                                     "1.0",
                                     "Description",
                                     "Manufacturer",
                                     "group:artifact:version")
        return configs
    }
}

