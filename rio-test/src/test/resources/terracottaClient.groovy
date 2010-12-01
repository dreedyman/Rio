deployment(name:'Terracotta-SharedEditor') {
    groups('rio')
    serviceExec(name:'SharedEditor') {
        software(name:'Terracotta', version:'2.4.8', removeOnDestroy: true)
        execute inDirectory: 'samples/pojo/sharededitor', command: 'run.sh'
        maintain 1
        maxPerMachine type:'physical', 1
    }
}
