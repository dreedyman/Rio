import java.util.logging.Level

String getConfigEntry() {
    String entry
    String value = System.getProperty('rio.test.config')
    if(value.equals('useclasspath'))
        entry = 'classpath:bogus.groovy'
    else if(value.equals('usebadfile'))
        entry = 'bogus.groovy'
    else if(value.equals('use-multi-classpath'))
        entry = 'classpath:simple_config.groovy,simple_config2.groovy'
    else if(value.equals('use-multi-file'))
        entry = 'src/test/resources/simple_config.groovy,src/test/resources/simple_config2.groovy'
    else
        entry = 'src/test/resources/simple_config.groovy'
    return entry
}

deployment(name:'Test') {

    configuration file: getConfigEntry()

    service(name: 'Test') {
        interfaces {
            classes 'java.lang.Object'
        }
        implementation(class: 'org.rioproject.test.bean.Service')

        maintain 1

    }
}
