import org.rioproject.config.Component

@Component('simple')
class SimpleConfig {

    String getSomething() {
        return 'something'
    }
}