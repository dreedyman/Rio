import org.rioproject.config.Constants

deployment(name:'Calculator') {
    groups System.getProperty(Constants.GROUPS_PROPERTY_NAME,
                              System.getProperty('user.name'))
    
    artifact id:'service', 'org.rioproject.examples.calculator:calculator-service:2.0'
    artifact id:'service-dl', 'org.rioproject.examples.calculator:calculator-api:2.0'

    service(name: 'Calculator') {
        interfaces {
            classes 'org.rioproject.examples.calculator.Calculator'
            artifact ref:'service-dl'
        }
        implementation(class:'org.rioproject.examples.calculator.service.CalculatorImpl') {
            artifact ref:'service'
        }
        associations {
            ['Add', 'Subtract', 'Multiply', 'Divide'].each { s ->
                association name:"$s", type:'requires', property:"${s.toLowerCase()}"
            }
        }
        maintain 1
    }

    ['Add', 'Subtract', 'Multiply', 'Divide'].each { s ->
        service(name: s) {
            interfaces {
                classes "org.rioproject.examples.calculator.$s"
                artifact ref:'service-dl'
            }
            implementation(class: "org.rioproject.examples.calculator.service.${s}Impl") {
                artifact ref:'service'
            }
            maintain 1
        }
    }
}

