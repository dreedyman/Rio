package config

def appenders = []

/*appender("MEM", org.rioproject.start.MemoryAppender) {
    if (!System.getProperty("os.name").startsWith("Windows")) {
        withJansi = true

        encoder(PatternLayoutEncoder) {
            pattern = "%highlight(%-5level) %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
        }
    } else {
        encoder(PatternLayoutEncoder) {
            pattern = "%-5level %d{HH:mm:ss.SSS} %logger{36} - %msg%n%rEx"
        }
    }
}
appenders << "MEM"*/

logger("org.rioproject.config.RioPropertiesTest", OFF)

//root(INFO, appenders)
