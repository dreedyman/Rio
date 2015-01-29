/* test resolver configuration*/

String hostName = InetAddress.getLocalHost().getHostName()
String address = InetAddress.getLocalHost().getHostAddress()
println "hostName: ${hostName}, address: ${address}"
boolean onEnclave = (hostName.endsWith("wpafb.af.mil") || address.startsWith("10.131"))

resolver {
    jar = "resolver-aether"

    if (onEnclave) {
        repositories = ["repo": "http://10.131.7.138:7001"]
    } else {
        repositories = ["mine"   : "http://10.0.1.9:9010",
                        "central": "http://repo1.maven.org/maven2"]
    }
}