/*
 * Configuration for a Provision Monitor
 */

import net.jini.core.discovery.LookupLocator
import net.jini.export.Exporter
import net.jini.jrmp.JrmpExporter

import org.rioproject.config.Component
import org.rioproject.config.Constants
import org.rioproject.opstring.ClassBundle
import org.rioproject.monitor.selectors.LeastActiveSelector
import org.rioproject.monitor.selectors.ServiceResourceSelector
import org.rioproject.fdh.FaultDetectionHandlerFactory
import org.rioproject.resources.client.JiniClient
import net.jini.security.BasicProxyPreparer
import net.jini.core.constraint.InvocationConstraints
import net.jini.constraint.BasicMethodConstraints
import net.jini.core.constraint.ConnectionRelativeTime
import net.jini.security.ProxyPreparer
import net.jini.core.constraint.MethodConstraints
import net.jini.core.entry.Entry
import org.rioproject.boot.PortUtil
import org.rioproject.entry.UIDescriptorFactory
import org.rioproject.RioVersion
import net.jini.lookup.ui.MainUI
import org.rioproject.serviceui.UIFrameFactory

/*
* Declare Provision Monitor properties
*/
@Component('org.rioproject.monitor')
class MonitorConfig {
    String serviceName = 'Monitor'
    String serviceComment = 'Dynamic Provisioning Agent'
    String jmxName = 'org.rioproject.monitor:type=Monitor'

    String[] getInitialLookupGroups() {
        def groups = [System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                      System.getProperty('user.name'))]
        return groups as String[]
    }

    LookupLocator[] getInitialLookupLocators() {
        String locators = System.getProperty(Constants.LOCATOR_PROPERTY_NAME)
        if(locators!=null) {
            def lookupLocators = JiniClient.parseLocators(locators)
            return lookupLocators as LookupLocator[]
        } else {
            return null
        }
    }

    ServiceResourceSelector getServiceResourceSelector() {
        return new LeastActiveSelector()
    }

    Entry[] getServiceUIs(String codebase) {
        String uiClass = 'org.rioproject.tools.ui.ServiceUIWrapper'
        URL url = new URL("artifact:org.rioproject:rio-ui:${RioVersion.VERSION}")
        def entry = [UIDescriptorFactory.getUIDescriptor(MainUI.ROLE, new UIFrameFactory(url, uiClass))]
        return entry as Entry[]
    }

    /*
     * Use a JrmpExporter for the OpStringManager.
     */
    Exporter getOpStringManagerExporter() {
        int port = 0
        String portRange = System.getProperty(Constants.PORT_RANGE)
        if(portRange!=null)
            port = PortUtil.getPortFromRange(portRange)
        return new JrmpExporter(port)
    }
    
    ProxyPreparer getInstantiatorPreparer() {
        MethodConstraints serviceListenerConstraints =
                new BasicMethodConstraints(new InvocationConstraints(new ConnectionRelativeTime(30000),
                                                                     null))
        return  new BasicProxyPreparer(false, serviceListenerConstraints, null);        
    }
}
