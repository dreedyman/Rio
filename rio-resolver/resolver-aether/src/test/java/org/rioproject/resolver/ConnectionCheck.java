package org.rioproject.resolver;

import java.net.HttpURLConnection;
import java.net.URL;

class ConnectionCheck {
    static boolean connected() {
        boolean online = true;
        try {
            URL url = new URL("http://www.rio-project.org/maven2");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.getResponseCode();
        } catch(Exception e) {
            online = false;
        }
        return online;
    }
}
