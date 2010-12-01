/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.test.simple;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.rioproject.bean.Initialized;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleImpl implements Simple {
    String food;
    int visitorNumber = 1;
    Random rand = new Random(60);
    static final Logger logger = Logger.getLogger("bean.service");

    @Initialized
    public void checkConfigs() {
        if(food==null)
            throw new RuntimeException("Have not been initialized");
        if(logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("***********\n");
            sb.append("Initialized\n");
            sb.append("***********");
            logger.fine(sb.toString());
        }
    }

    public void setConfiguration(Configuration config) {
        try {
            food = (String)config.getEntry("bean", "food", String.class);
            logger.fine("\n############\nBean gets to eat "+food+"\n############");
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void setParameters(Map<String, Object> params) {
        if(logger.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("***********\n");
            for(Map.Entry<String, Object> entry : params.entrySet()) {
                sb.append(entry.getKey()+" = "+entry.getValue()+"\n");
            }
        }
    }

    public String getFood() {
        return food;
    }

    public String hello(String message) throws RemoteException {
        System.out.println("Client says hello : "+message);
        return("Hello vistor : "+visitorNumber++);
    }

    public int getGauge() {
        int val = rand.nextInt();
        return(val);
    }

    public String goodbye() {
        return "goobye "+visitorNumber++;
    }
}
