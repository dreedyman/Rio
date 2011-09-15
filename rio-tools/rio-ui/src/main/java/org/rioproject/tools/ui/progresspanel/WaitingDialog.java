/*
 * Copyright 2008 to the original author or authors.
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
package org.rioproject.tools.ui.progresspanel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A dialog that incorporates a <tt>SingleComponentInfiniteProgress</tt>
 * 
 * @author Dennis Reedy
 */
public class WaitingDialog extends JDialog {
    private JFrame frame;
    private SingleComponentInfiniteProgress spinnyThing;
    private ScheduledExecutorService ses =
        Executors.newSingleThreadScheduledExecutor();

    public WaitingDialog(JFrame frame, String waitFor, long delay) {
        super(frame);
        this.frame = frame;
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(8, 8));

        spinnyThing = new SingleComponentInfiniteProgress(false);
        spinnyThing.setResizeRatio(0.25);
        spinnyThing.setPreferredSize(new Dimension(40, 20));
        JLabel waitingFor = new JLabel("<html><center>"+waitFor+"</center></html>");
        pane.setBackground(Color.WHITE);
        pane.add(spinnyThing, BorderLayout.WEST);
        pane.add(waitingFor, BorderLayout.CENTER);
        getContentPane().add(pane);
        if(delay>0) {
            ses.schedule(new DisplayDialogTask(this, waitFor),
                         delay,
                         TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if(visible) {
            int width = 350;
            int height = 100;
            pack();
            setSize(width, height);
            setLocationRelativeTo(frame);
            spinnyThing.start();
        } else {
            spinnyThing.stop();
            ses.shutdownNow();
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        setVisible(false);
        super.dispose();
    }

    class DisplayDialogTask implements Runnable {
        String waitMessage;
        String hostName;
        String hostAddress;
        JDialog dialog;

        DisplayDialogTask(JDialog dialog, String waitMessage) {
            super();
            this.waitMessage = waitMessage;
            this.dialog = dialog;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            if(!dialog.isVisible())
                dialog.setVisible(true);
        }
    }
}
