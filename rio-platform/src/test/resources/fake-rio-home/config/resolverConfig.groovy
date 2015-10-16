import org.rioproject.RioVersion

/* test resolver configuration*/

String hostName = InetAddress.getLocalHost().getHostName()
String address = InetAddress.getLocalHost().getHostAddress()
println "hostName: ${hostName}, address: ${address}"
boolean onEnclave = (hostName.endsWith("wpafb.af.mil") || address.startsWith("10.131"))

resolver {
    jar = "${System.properties['rio.home']}/lib/resolver/resolver-aether-${RioVersion.VERSION}.jar"

    repositories {
        if (onEnclave) {
            remote = ["repo": "http://10.131.7.138:7001"]
        } else {
            remote = ["mine"   : "http://10.0.1.9:9010",
                      "central": "http://repo1.maven.org/maven2"]
        }
    }
}