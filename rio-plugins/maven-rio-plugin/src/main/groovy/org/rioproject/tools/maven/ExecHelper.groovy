/*
 * Copyright to the original author or authors.
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
package org.rioproject.tools.maven

import java.util.concurrent.Future
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService;

/**
 * Provides utility for starting processes
 */
class ExecHelper {
    public static void doExec(String command) {
        doExec(command, true)
    }

    public static void doExec(String command, boolean wait) {
        doExec(command, wait, false)
    }

    public static void doExec(String command, boolean wait, boolean redirect) {
        Process process = command.execute()
        if(wait) {
            process.consumeProcessOutputStream(System.out)
            process.consumeProcessErrorStream(System.err)
            if(redirect) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                final Future piper = executor.submit(new Piper(process))
                process.waitFor()
                piper.cancel(true)
                executor.shutdownNow()
            }
        }
    }

    public static void doExec(String command, List<String> envp, File workingDir, boolean wait) {
        Process process = command.execute(envp, workingDir)
        if(wait) {
            process.consumeProcessOutputStream(System.out)
            process.consumeProcessErrorStream(System.err)
            process.waitFor()
        }
    }

    private static class Piper implements Runnable {
        Process process;

        Piper(Process process) {
            this.process = process
        }

        void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while(!Thread.currentThread().isInterrupted()) {
                String input = br.readLine();
                if(input != null && input.length()>0) {
                    process.withWriter { writer ->
                        writer << input
                    }
                }
            } 
        }
    }
    
}
