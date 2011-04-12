import java.util.logging.Level
import net.jini.space.JavaSpace
import net.jini.core.transaction.server.TransactionManager

deployment(name:'Grid') {

    include 'outrigger.groovy'
    
    groups('rio')


    service(name: 'Worker') {
        interfaces {
            classes 'tutorial.grid.Task'
            resources 'compute-grid/lib/grid-dl.jar'
        }
        implementation(class: 'tutorial.grid.TaskServer') {
            resources 'compute-grid/lib/grid.jar'
        }
        associations {
            association name: 'JavaSpace', type: 'requires',
                        serviceType: JavaSpace.name,
                        property: 'space', matchOnName: false
            association name: 'Transaction TestManager', type: 'requires',
                        serviceType: TransactionManager.name,
                        property: 'transactionManager', matchOnName: false
        }
        logging {
            logger('tutorial.grid', Level.ALL)

        }
        
        maintain 5
        maxPerMachine 10
    }

}
