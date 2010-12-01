/*
 * This configuration is used by the com.sun.jini.start utility to start an
 * Activatable Cybernode
 */
import org.rioproject.config.Component
import org.rioproject.boot.RioActivatableServiceDescriptor
import com.sun.jini.start.SharedActivationGroupDescriptor
import com.sun.jini.start.ServiceDescriptor

@Component('com.sun.jini.start')
class StartActivatableCybernodeConfig extends ActivatableConfig {

    ServiceDescriptor[] getServiceDescriptors() {
        String cybernodeClasspath = "${rioHome}/lib/cybernode.jar${File.pathSeparator}${rioHome}/lib/resolver.jar"
        String cybernodeCodebase =
            getServiceCodebase(['cybernode-dl.jar', 'rio-dl.jar', 'jsk-dl.jar'])            
            
        def configArgs = ["${rioHome}/config/cybernode.groovy",
                          "${rioHome}/config/persistent_cybernode.groovy",
                          "${rioHome}/config/compute_resource.groovy"]

        def serviceDescriptors = [
            new SharedActivationGroupDescriptor(policyFile,
                                                groupClasspath,
                                                getGroupPersistenceDirectory('act-cybernode.log'),
                                                null,         /* serverCommand */
                                                null,         /* serverOptions */
                                                getSystemProperties()),  /* serverProperties */

            new RioActivatableServiceDescriptor(cybernodeCodebase,
                                                policyFile,
                                                cybernodeClasspath,
                                                "org.rioproject.cybernode.CybernodeImpl",
                                                getGroupPersistenceDirectory('act-cybernode.log'),
                                                (String[])configArgs,
                                                true)         /* restart */
        ]
        return (ServiceDescriptor[])serviceDescriptors
    }
}
