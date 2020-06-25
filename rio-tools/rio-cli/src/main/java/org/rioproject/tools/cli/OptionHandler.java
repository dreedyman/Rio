/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.tools.cli;

import java.io.BufferedReader;
import java.io.PrintStream;

/**
 * Define plugin interface for CLI option handlers. An OptionHandler is
 * responsible for providing a option, or activity, that will be used through
 * the CLI.
 *
 * @author Dennis Reedy
 */
public interface OptionHandler {
    /**
     * Process the option.
     *
     * @param input Parameters for the option, may be null
     * @param br An optional BufferdReader, used if the option requires input.
     * if this is null, the option handler may create a BufferedReader to
     * handle the input 
     * @param out The PrintStream to use if the option prints results or
     * choices for the user. Must not be null
     *
     * @return The result of the action.
     */
    String process(String input, BufferedReader br, PrintStream out);

    /**
     * Get the usage of the command
     *
     * @return Command usage
     */
    String getUsage();
}
