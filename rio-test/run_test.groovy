#!/usr/bin/env groovy


def repo = "${System.getProperty('user.home')}/.m2/repository"
cwd = System.getProperty('user.dir')
def rioDist = "distribution/target/rio-4.1"
def distLib = "${cwd}/../${rioDist}/lib"
def testClassDir = "${cwd}/target/test-classes"
def testResourcesDir = "${cwd}/src/test/resources"

println "===> ${System.getProperty("java.io.tmpdir")}"
final java_home = { ->
    javaHome = System.getProperty("JAVA_HOME")
    if(javaHome==null) {
        javaHome = System.getProperty("java.home")
        /* Make an attempt to find tools.jar */
        File jHome = new File(javaHome)
        File lib = new File(jHome, "lib")
        boolean foundTools = findFile(lib, "tools.jar")
        if(!foundTools) {
            File f = jHome.getParentFile()
            lib = new File(f, "lib")
            if(findFile(lib, "tools.jar")) {
                jHome = f
            }
        }
        javaHome = jHome.canonicalPath
    }
}

def findFile(File dir, String fName) {
    boolean found = false
    if(dir.exists()&& dir.isDirectory()) {
        for(String f : dir.list()) {
            if(f.equals(fName)) {
                found = true
                break
            }
        }
    }
    return found
}

final java  = { ->
    return "${java_home}${File.separator}bin${File.separator}java "
}

final java_tools = { ->
    String javaHome = "${java_home}"
    File dir = new File(javaHome)
    if(dir.name.endsWith('jre')) {
        javaHome = dir.getParentFile().canonicalPath
    }
    return "${javaHome}${File.separator}lib${File.separator}tools.jar"
}


final findTestClasses = {basedir ->
    List list = basedir.listFiles().grep(~/.*Test.java$/)
    list.addAll(basedir.listFiles().grep(~/.*Test.groovy$/))
    return list
}

final scanner = { basedir, pattern ->
    List list = new ArrayList()
    new File(basedir).eachFile { f ->
        for(p in pattern) {
            if(p.equals("*.jar") && f.name.endsWith('jar')) {
                list.add(f.canonicalPath)
            } else if(p == f.name) {
                list.add(f.canonicalPath)
            }
        }
    }
    return list
}

final classpath = { ->
    def cp = []
    /* Add tools.jar*/
    cp.add("${java_tools}")
    
    /* Add core jars */
    cp.addAll(scanner("${distLib}", ['jsk-platform.jar', 'jsk-lib.jar', 'start.jar']))
    cp.addAll(scanner("${distLib}", ['boot.jar', 'rio.jar', 'rio-test.jar', 'webster.jar', 'groovy-all.jar', 'cglib-nodep.jar', 'prefuse.jar', 'cybernode.jar', 'rio-cli.jar']))    

    /* Add JUnit and Hyperic jars */
    ["${repo}/junit/junit/4.6/"   : '*.jar',
     "${distLib}/hyperic" : '*.jar'].each {dir, pattern ->
        cp.addAll(scanner(dir, [pattern]))
    }

    /* Add test classes directory  */
    cp.add(new File("${testClassDir}").absolutePath)
    /* Add test resources directory  */
    cp.add(new File("${testResourcesDir}").absolutePath)

    StringBuilder sb = new StringBuilder()
    sb.append("-cp ")
    int i = 0
    for(f in cp) {
        if(i>0)
            sb.append(File.pathSeparator)
        sb.append(f)
        i++
    }
    if(System.getProperty('os.name').startsWith('Windows'))
        sb.append(File.pathSeparator)
    return sb.toString()
}

final systemProperties = { properties ->
    StringBuilder sysProps = new StringBuilder()
    properties.each { propertyKey, propertyValue ->
        sysProps.append("-D${propertyKey}=${propertyValue} ")
    }
    return sysProps.toString()
}

final runTest = { testClass, properties ->
    def mainClass = 'org.junit.runner.JUnitCore'
    def cmdLine = "${java} ${systemProperties(properties)} ${classpath} ${mainClass} ${testClass}"
    def cmd
    if(System.getProperty('os.name').startsWith('Windows'))
        cmd = ["cmd.exe", "/C", cmdLine]
    else
        cmd = ["bash", "-c", cmdLine]
    println "Exec command line: ${cmd}"
    Process process = cmd.execute()
    process.consumeProcessOutputStream(System.out)
    process.consumeProcessErrorStream(System.err)
    return process.waitFor()
}

testJVMProperties =
        ['java.security.policy'               : "${cwd}/../${rioDist}/policy/policy.all",
         'RIO_HOME'                           : "${cwd}/../${rioDist}",
         'RIO_TEST_HOME'                      : "${cwd}",
         'org.rioproject.test.manager.config' : "${cwd}/src/test/conf/manager-config.groovy",
         'org.rioproject.test.config'         : "${cwd}/src/test/conf/test-config.groovy"]

def testSelector() {
    def boolean valid = false
    def byte[] bytes = new byte[10];
    String selectedTestClass
    while (!valid) {
        def int counter = 0
        println "\nSelect a test to run:"
        tests.each { testClass ->
             println "        " + (++counter) + ".  " + testClass
         }
        
        print "\nSelect test number or 'q' to quit: "
        System.in.read(bytes);

        String rough = new String(bytes);
        String testNumber = rough.trim();
        if(testNumber=='q')
           return null

        try {
            int testInt = Integer.parseInt(testNumber)
            if(testInt < 1 || testInt > tests.size())
               println "${testNumber} is not a valid selection"
            else {
                selectedTestClass = tests[testInt-1]
                println "Selected ${selectedTestClass}"
                valid = true
            }
        } catch (NumberFormatException e) {
            println "${testNumber} is not a valid selection"
        }        
    }
    return selectedTestClass
}

def findDirs(File basedir) {
    def dirs = []
    if(basedir.isDirectory()) {
        if(basedir.name != ".svn") {
            dirs << basedir
            for(File f : basedir.listFiles()) {
                List result = findDirs(f)
                if(result.size()>0)
                    dirs.addAll(result)
            }
        }
    }
    return dirs
}

File basedir = new File(cwd, "src/test")
dirs = findDirs(basedir)
testClasses = []
for (dir in dirs) {
    testClasses.addAll(findTestClasses(dir))
}

excludes = ['org.rioproject.test.watch.WatchTest', 'org.rioproject.test.watch.ThresholdWatchTest']
tests = []
for (testClass in testClasses) {
    String s = testClass.path.substring(basedir.path.length()+1, testClass.path.length())
    int trimLen = s.endsWith(".java")?5:7
    /* Strip the extension and leading directory name, and replace File.separator with "." */
    s = s.replaceAll(File.separator, ".").substring(0, s.length()-trimLen).substring(trimLen)
    if(s in excludes)
        continue
    tests << "$s"
}
tests << "all"
String testClass = testSelector()
if(testClass!=null) {
    if(testClass=='all') {
        println "Running all tests..."
        tests.each { t ->
            if(t!='all')
                runTest t, testJVMProperties
        }
    } else {
        runTest testClass, testJVMProperties
    }
} else {
    println 'No test selected, exiting'
}



