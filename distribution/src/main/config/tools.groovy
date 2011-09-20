/*
 * This configuration is used to configure Rio tools (UI & CLI)
 */

import org.rioproject.config.Component

@Component('net.jini.discovery.LookupDiscovery')
class ClientDiscoveryConfig {
    long multicastAnnouncementInterval=5000
}