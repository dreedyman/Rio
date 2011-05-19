This directory contains platform configuration files. The Platform
configuration file is used as follows:

- Provides attributes for creating PlatformCapability classes and declaring
   what jars will be common across all service classloaders. If jars are common
   they are loaded by the CommonClassLoader, and are in the classpath of all
   child class loaders. By default Rio and Jini technology jars are common
   across all service classloaders.

   For platform capabilities that are not loaded by the common classloader,
   services that declare that dependency will have the capability loaded by
   the service's classloader.

 - Provides a manifest of the platform jars that can be loaded.

Platform configuration files are loaded when Rio starts. The contents of this
directory will be scanned at startup time.

You have a choice of using .xml or .groovy files to declare platform configurations.

Using XML
==========
Each .xml file will be parsed for <platform> declarations.

The structure of the documents are as follows:

<platform>
    <capability name="Foo" common="yes">
        <description>An optional description</description>
        <version>2.5</version>
        <manufacturer>An optional manufacturer</manufacturer>
        <classpath>Path separator (: or ;) delimited listing of directories and/or jars</classpath>
        <path>The location on the file system where the capability is installed</path>
        <native>Any native libraries that need to be loaded</native>
        <costmodel>The resource cost model class name</costmodel>
    </capability>
</platform>


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