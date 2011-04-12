deployment(name:'Terracotta-Server Group') {
    groups('rio')
    serviceExec(name:'Terracotta-Server') {
        software(name:'Terracotta', version:'latest', removeOnDestroy: true)
        execute command: 'bin/start-tc-server.sh -f /Users/tgautier/rio-test/tc-config.xml -n server2'
        cluster '10.0.4.222', '10.0.4.224'
        maintain 2
        maxPerMachine type:'physical', 1
    }
}
