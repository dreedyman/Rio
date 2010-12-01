deployment(name:'Calculator') {
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
            association(name: 'Add', type: 'requires', property: 'add')
            association(name: 'Subtract', type: 'requires', property: 'subtract')
            association(name: 'Multiply', type: 'requires', property: 'multiply')
            association(name: 'Divide', type: 'requires', property: 'divide')
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

    service(name: 'Subtract') {
        interfaces {
            classes('calculator.Subtract')
            resources(ref: 'client.jars')
        }
        implementation(class: 'calculator.service.SubtractImpl') {
            resources(ref: 'impl.jars')
        }
        maintain 1
    }

    service(name: 'Multiply') {
        interfaces {
            classes('calculator.Multiply')
            resources(ref: 'client.jars')
        }
        implementation(class: 'calculator.service.MultiplyImpl') {
            resources(ref: 'impl.jars')
        }
        maintain 1
    }

    service(name: 'Divide') {
        interfaces {
            classes('calculator.Divide')
            resources(ref: 'client.jars')
        }
        implementation(class: 'calculator.service.DivideImpl') {
            resources(ref: 'impl.jars')
        }
        maintain 1
    }
}
