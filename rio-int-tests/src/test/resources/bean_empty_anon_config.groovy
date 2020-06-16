
String getConfigEntry() {
    String entry
    String value = System.getProperty('rio.test.config')
    if(value.equals('anon-groovy'))
        entry = '''
            @org.rioproject.config.Component('simple')
            class SimpleConfig {
                String getSomething() {
                    return 'something'
                }
            }
        '''
    else if(value.equals('anon-groovy-2'))
        entry = '''
            class simple {
                String getSomething() {
                    return 'something'
                }
            }
        '''
    else
        entry = '''
            simple {
                something = "something";
            }
        '''

    return entry
}

deployment(name:'Test') {

    configuration getConfigEntry()

    service(name: 'Test') {
        interfaces {
            classes 'java.lang.Object'
        }
        implementation(class: 'org.rioproject.test.bean.Service')

        maintain 1

    }
}
