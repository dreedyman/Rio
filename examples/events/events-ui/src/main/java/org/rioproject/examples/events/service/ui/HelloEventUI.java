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
package org.rioproject.examples.events.service.ui;

import org.rioproject.examples.events.Hello;
import net.jini.core.lookup.ServiceItem;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A Simple Service UI for the Hello event example
 */
public class HelloEventUI extends JPanel {
    private Hello service;
    private JTextField helloField;
    private int invocationCount = 0;

    /**
     * Creates new HelloEventUI
     *
     * @param obj The provided ServiceItem as an object
     */
    public HelloEventUI(Object obj) {
        super();
        getAccessibleContext().setAccessibleName("Hello World");

        try {
            ServiceItem item = (ServiceItem) obj;
            helloField = new JTextField(40);
            add(helloField);

            JButton button = new JButton("sayHello()");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    doSayHello();
                }
            });
            add(button);
            this.service = (Hello) item.service;
            doSayHello();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void doSayHello() {
        try {
            invocationCount++;
            service.sayHello("From UI"+invocationCount);
            helloField.setText(Integer.toString(invocationCount));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
