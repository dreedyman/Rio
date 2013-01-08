deployment(name:'Empty') {
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
            association(name: 'Add', type: 'requires', property: 'add') { }
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
