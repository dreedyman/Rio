/*
 * This configuration is used by the com.sun.jini.start utility to start a 
 * service that will exec a single service bean
 */

import org.rioproject.boot.RioServiceDescriptor
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import java.util.logging.FileHandler
import org.rioproject.config.Constants
import java.util.logging.Handler

@Component('com.sun.jini.start')
class StartServiceBeanExecConfig {

    private void setFileHandler() {
        String name = System.getProperty(Constants.SERVICE_BEAN_EXEC_NAME)
        name = name.substring(name.indexOf("/") + 1)
        String logDir = System.getProperty("RIO_LOG_DIR")
        File logDirectory = new File(logDir)
        if (!logDirectory.exists())
            logDirectory.mkdirs()
        String logFileName = logDir + name + ".log"

        // log file max size 500K, 3 rolling files
        int limit = 1024 * 500
        int rollingFileCount = 3
        RedirectingFileHandler fileHandler =
            new RedirectingFileHandler(logFileName, limit, rollingFileCount)
        fileHandler.setFormatter(new SimpleFormatter())

        Logger rootLogger = Logger.getLogger("")
        for(Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler)
        }
        rootLogger.addHandler(fileHandler)
    }

    ServiceDescriptor[] getServiceDescriptors() {
        //setFileHandler()
        String rioHome = System.getProperty('RIO_HOME')
        String codebase = "file://${rioHome}/lib-dl/cybernode-dl.jar"
        String sep = File.pathSeparator
        String classpath = "${rioHome}/lib/cybernode.jar${sep}"
        if (System.getProperty("RIO_TEST_ATTACH") != null)
            classpath = "$classpath${sep}${rioHome}/lib/rio-test.jar"

        classpath = "$classpath${sep}${rioHome}/lib/resolver.jar"
        
        String policyFile = rioHome + '/policy/policy.all'
        def configArgs = [rioHome + '/config/cybernode.groovy',
            rioHome + '/config/compute_resource.groovy']

        def serviceDescriptors = [
            new RioServiceDescriptor(codebase,
                                     policyFile,
                                     classpath,
                                     'org.rioproject.cybernode.exec.ServiceBeanExec',
                                     (String[]) configArgs)
        ]

        return (ServiceDescriptor[]) serviceDescriptors
    }
}

class RedirectingFileHandler extends FileHandler {

    def RedirectingFileHandler(String pattern, int limit, int count) {
        super(pattern, limit, count)
    }

    protected synchronized void setOutputStream(OutputStream outputStream) {
        System.setErr(new PrintStream(outputStream, true))
        System.setOut(new PrintStream(outputStream, true))
    }
}