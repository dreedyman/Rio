/* test resolver configuration*/

package config

String hostAddress = InetAddress.getLocalHost().hostAddress

resolver {
    jar = "resolver-aether"
    repositories = ["project": "http://${hostAddress}:${System.properties["resolver.config.test.port"]}"]
}