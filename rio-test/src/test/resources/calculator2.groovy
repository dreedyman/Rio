deployment(name:'Calculator') {
    groups 'rio'

    resources id:'impl.jars', 'calculator/lib/calculator.jar'
    resources id:'client.jars', 'calculator/lib/calculator-dl.jar'

    service(name: 'Calculator') {
        interfaces {
            classes 'calculator.Calculator'
            resources ref:'client.jars'
        }
        implementation(class:'calculator.service.CalculatorImpl') {
            resources ref:'impl.jars'
        }
        associations {
            association name:'Add', type:'requires', property:'add'
            association name:'Subtract', type:'requires', property:'subtract'
            association name:'Multiply', type:'requires', property:'multiply'
            association name:'Divide', type:'requires', property:'divide'
        }
        maintain 1
    }

    ['Add', 'Subtract', 'Multiply', 'Divide'].each { s ->
        service(name: s) {
            interfaces {
                classes "calculator.$s"
                resources ref:'client.jars'
            }
            implementation(class: "calculator.service.${s}Impl") {
                resources ref:'impl.jars'
            }
            maintain 1
        }
    }
}
