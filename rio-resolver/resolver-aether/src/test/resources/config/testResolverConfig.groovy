/* test resolver configuration*/

package config

String hostAddress = InetAddress.getLocalHost().hostAddress

resolver {
    jar = "resolver-aether"
    repositories {
        remote = ["project": "http://${hostAddress}:${System.properties["resolver.config.test.port"]}"]
        flatDirs = [new File(System.getProperty("user.dir"), "target/flat")]
    }
}