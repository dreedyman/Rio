import java.util.concurrent.TimeUnit

deployment(name:'association stuff') {
    groups('rio')

    resources(id: 'impl.jars', 'calculator/lib/calculator.jar')
    resources(id: 'client.jars', 'calculator/lib/calculator-dl.jar')

    service(name: 'Calculator') {
        interfaces {
            classes('calculator.Calculator')
            resources(ref: 'client.jars')
        }
        implementation(class: 'calculator.service.CalculatorImpl') {
            resources(ref: 'impl.jars')
        }
        associations {
            association(name: 'Add', type: 'requires', property: 'add') {
                management inject: 'eager',
                           proxy: "net.foo.space.SomeProxy",
                           strategy: 'org.rioproject.associations.strategy.Utilization',
                           serviceDiscoveryTimeout: 15,
                           serviceDiscoveryTimeoutUnits: TimeUnit.SECONDS
            }
        }
        maintain 1
    }

    service(name: 'Add') {
        interfaces {
            classes('calculator.Add')
            resources(ref: 'client.jars')
        }
        implementation(class: 'calculator.service.AddImpl') {
            resources(ref: 'impl.jars')
        }
        maintain 1
    }

}
