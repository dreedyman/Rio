#!/usr/bin/env groovy

if (args.length !=2) {
    System.err.println("You must provide the location of Rio and Java home")
    System.exit(2)
}

String javaHome = args[0]
String rioHome = args[1]

if (javaHome == null || javaHome.length() == 0) {
    System.err.println("The location of Java must be set")
    System.exit(2)
}
StringBuilder java = new StringBuilder()
java.append(javaHome)
if(!javaHome.endsWith(File.separator))
    java.append(File.separator)
java.append("bin").append(File.separator).append("java")
if(System.getProperty("os.name").startsWith("Windows"))
    java.append(".exe")
if(!new File(java.toString()).exists()) {
    System.err.println("The java executable not found in provided path: "+java)
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
classPath.append(new File(rioHome, "lib/groovy-all.jar").path)
classPath.append(File.pathSeparator)
classPath.append(new File(rioHome, "lib/rio-start.jar").path)
classPath.append(File.pathSeparator)
File resolverLibs = new File(rioHome, "lib/resolver")
for(File f: resolverLibs.listFiles()) {
    classPath.append(File.pathSeparator)
    classPath.append(f.path)
}

StringBuffer out = new StringBuffer()
long installDate = System.currentTimeMillis()
String install = "${java.toString()} -Djava.security.policy=${rioHome}/policy/policy.all -DRIO_HOME=$rioHome -classpath ${classPath.toString()} org.rioproject.install.Installer"
Process process = install.execute()
process.consumeProcessOutputStream(out)
process.consumeProcessErrorStream(out)
process.waitFor()

if(out.length()>0) {
    File logDir = new File(rioHome+File.separator+"logs")
    if(!logDir.exists())
        logDir.mkdirs()
    File installerLog = new File(logDir, "install.log")
    if(!installerLog.exists())
        installerLog.createNewFile()
    StringBuilder builder = new StringBuilder()
    builder.append("===============================================\n")
    builder.append("Installer").append("\n")
    builder.append("Log creation : ").append(new Date(installDate).toString()).append("\n")
    builder.append("Operator : ").append(System.getProperty("user.name")).append("\n")
    builder.append("===============================================\n")
    installerLog.append(builder.toString())
    installerLog.append(out.toString())
}

