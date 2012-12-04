import org.rioproject.tools.harvest.Harvester
import org.rioproject.config.Constants

def getLogDir() {
    String logDir = "/tmp/${System.getProperty('user.name')}/logs"
    String opSys = System.getProperty('os.name')
    if(opSys.startsWith("Windows")) {
        logDir = '${java.io.tmpdir}'
    }
    String logRoot = System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                                        System.getProperty('user.name'))
    return logDir+'${/}rio${/}'+logRoot
}

deployment(name:'Harvester') {

    groups System.getProperty('org.rioproject.groups')
    
    service(name: 'Harvester Agent', type: 'fixed') {
                
        implementation(class: 'org.rioproject.tools.harvest.HarvesterAgent')

        maintain 1

        association(type: "uses", serviceType: Harvester.name,
                    name: "Harvester", property: "harvester")

        parameters {
            parameter name: "match", value: "*.log, *.wds, *-1"
            parameter name: "directories",
                      //value: '${java.io.tmpdir}/service_logs'
                      value: getLogDir()
        }

        maxPerMachine type:'physical', 1
    }
}
