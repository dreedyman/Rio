### Overview  ![build status](https://travis-ci.org/dreedyman/Rio.svg?branch=develop)
Rio is an open source technology that provides a dynamic framework for developing, deploying and managing distributed systems composed of services.

Rio turns a network of compute resources into a dynamic service, providing a policy based approach for fault detection and recovery, scalability and dynamic deployment. Key to the architecture are a set of dynamic capabilities and reliance on policy-based and SLA mechanisms.

Developing services for use with Rio is simple. Rio provides a non-intrusive model that removes the complexity surrounding service development and deployment. Build your services using Maven and deploy using service artifacts.

Key features include:

* Dynamic architecture for distributed systems
* Built-in fault detection and recovery for your services
* Policy based SLA enforcement
* Support for external systems, encapsulate the control and monitoring of other frameworks
* Built-in support for Maven artifact resolution at deploy time
* Extensible service development & deployment support:
	* POJOs
	* Spring
	* Maven based artifact resolution


### Building
Rio is built using Gradle. 

* Run `./gradlew build`. This will run unit and integration tests, building a distribution in the `distribution/build` directory
*  To create a distribution only, run `./gradlew distribution`

### Use in your project
First, look at the [Rio Examples repository](https://github.com/dreedyman/rio-examples) for how to use Rio in your project.

Until getting synched with jcenter(), add the following repository declaration into your build:  

```
repositories {
   maven { url "https://dl.bintray.com/dreedyman/Rio" }
}
```
You can then declare a dependency on the distribution artifact: ` org.rioproject:distribution:${rioVersion}@zip`

### Documentation
See the [Wiki](https://github.com/dreedyman/Rio/wiki) for documentation, examples, operational guidelines and other information.

### Issue Tracking
Issues, bugs, and feature requests should be submitted to the [issue tracking system](https://github.com/dreedyman/Rio/issues) for this project.

### Getting Help
Feel free to ask questions on Rio's [mailing list](http://groups.google.com/group/rio-users).

### License

Rio is licensed under the Apache License, Version 2.0. You may obtain a copy of the license
[here](http://www.apache.org/licenses/LICENSE-2.0).
