import org.rioproject.config.Component

/* The script has errors, we have a missing property */
@Component('foo.bar')
class bad_config2 {

    String getBaz() {
        String baz = "$badProperty} oops"
        return baz
    }
      
}
