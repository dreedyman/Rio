/**
 * Created by IntelliJ IDEA.
 *
 * User: dreedy
 * Date: Oct 19, 2009
 * Time: 4:07:34 PM
 */
deployment(name:'Calculator') {
    groups 'rio'

    artifact id:'impl.artifact',   'org.rioproject.examples:calculator:1.0'
    artifact id:'client.artifact', 'org.rioproject.examples:calculator:dl:1.0'

    service(name: 'Calculator') {
        interfaces {
            classes 'calculator.Calculator'
            artifact 'org.rioproject.examples:calculator:dl:1.0'
        }
        implementation(class:'calculator.service.CalculatorImpl') {
            artifact ref:'impl.artifact'
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
                artifact ref:'client.artifact'
            }
            implementation(class: "calculator.service.${s}Impl") {
                artifact ref:'impl.artifact'
            }
            maintain 1
        }
    }
}
