import org.rioproject.config.Component

/* Use a reserved Java keyword for a component name, will result in an
 * IllegalArgumentException */
@Component('finally')
public class bad_config {

}