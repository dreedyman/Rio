import org.rioproject.config.Component

@Component('simple2')
class SimpleConfig2 {

    String getSomething() {
        return 'something-else'
    }
}
