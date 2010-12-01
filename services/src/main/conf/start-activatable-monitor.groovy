/*
 * This configuration is used by the com.sun.jini.start utility to start an
 * Activatable ProvisionMonitor
 */
import org.rioproject.boot.RioActivatableServiceDescriptor
import org.rioproject.config.Component
import com.sun.jini.start.ServiceDescriptor
import com.sun.jini.start.SharedActivationGroupDescriptor
import com.sun.jini.start.SharedActivatableServiceDescriptor

@Component('com.sun.jini.start')
class StartActivatableMonitorConfig extends ActivatableConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String monitorCodebase =
            getServiceCodebase(['monitor-dl.jar', 'rio-dl.jar', 'jsk-dl.jar'])
        def monitorConfigs = ["${rioHome}/config/monitor.groovy",
                              "${rioHome}/config/persistent_monitor.groovy"]
        String reggieCodebase =
            getServiceCodebase(['reggie-dl.jar', 'rio-dl.jar', 'jsk-dl.jar'])
        def reggieConfigs = ["${rioHome}/config/reggie.groovy",
                             "${rioHome}/config/persistent_reggie.groovy"]

        def serviceDescriptors = [
            new SharedActivationGroupDescriptor(policyFile,
                                                groupClasspath,
                                                getGroupPersistenceDirectory('act-monitor.log'),
                                                null,         /* serverCommand */
                                                null,         /* serverOptions */
                                                getSystemProperties()),  /* serverProperties */

            new RioActivatableServiceDescriptor(monitorCodebase,
                                                policyFile,
                                                "${rioHome}/lib/monitor.jar${File.pathSeparator}${rioHome}/lib/resolver.jar",
                                                'org.rioproject.monitor.ProvisionMonitorImpl',
                                                getGroupPersistenceDirectory('act-monitor.log'),
                                                (String[])monitorConfigs,
                                                true),        /* restart */

            new SharedActivatableServiceDescriptor(reggieCodebase,
                                                   policyFile,
                                                   "${jiniHome}/lib/reggie.jar",
                                                   'com.sun.jini.reggie.PersistentRegistrarImpl',
                                                   getGroupPersistenceDirectory('act-monitor.log'),
                                                   (String[])reggieConfigs,
                                                   true)      /* restart */
        ]
        return (ServiceDescriptor[])serviceDescriptors
    }


}
