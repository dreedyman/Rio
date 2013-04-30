#!/usr/bin/env groovy
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

/*
 * This script is used to compile service groovy configurations to class files to improve loading time.
 */
String rioHome=null

if (args.length ==0) {
    rioHome = System.getenv("RIO_HOME")
} else if(args.length==1) {
    rioHome = args[1]    
} else {
    StringBuilder builder = new StringBuilder()
    builder.append("When calling the compile script with arguments, you must provide the location ")
    builder.append("of Rio. ").append("\n")
    builder.append("You may alternately call the install script with no arguments, ")
    builder.append("and the environment will be checked ").append("\n")
    builder.append("for the existence of RIO_HOME").append("\n\n")
    builder.append("\tcompile.groovy [rio-home-location]")
    builder.append("\n")
    System.err.println(builder.toString())
    System.exit(2)
}

if (rioHome == null || rioHome.length() == 0) {
    System.err.println("The location of RIO_HOME must be set")
    System.exit(2)
}

if(!new File(rioHome, "lib").exists()) {
    System.err.println("Invalid location of Rio: "+rioHome)
    System.exit(2)
}


StringBuilder classPath = new StringBuilder()
File rioLib = new File(rioHome, "lib")

for(File f: rioLib.listFiles()) {
    classPath.append(File.pathSeparator)
    classPath.append(f.path)
}

File configDir = new File(rioHome, "config")
for(File config : configDir.listFiles()) {	
	if(config.isDirectory())
	    continue
	StringBuffer out = new StringBuffer()
    String targetDir
    if(config.name.startsWith("start")) {
        continue
    } else {
        targetDir = "${rioHome}/config/compiled/${config.name.substring(0, config.name.indexOf("."))}"
    }
    String groovyc = "groovyc -d ${targetDir} -classpath ${classPath.toString()} ${config.path}"
    Process process = groovyc.execute()
    process.consumeProcessOutputStream(out)
    process.consumeProcessErrorStream(out)
    process.waitFor()
    if(out.length()>0)
        println out.toString()
    else
        println "Compiled ${config.name} to ${targetDir}"
}