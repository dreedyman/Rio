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
package org.rioproject.logging.jul;

/**
 * Provides support for ansi colorization fo a string.
 */
public class StringColorizer {
    public enum Color {
        LIGHT_RED(31), LIGHT_GREEN(32), LIGHT_YELLOW(33), LIGHT_BLUE(34), LIGHT_MAGENTA(35), LIGHT_CYAN(36),
        RED(91), GREEN(92), YELLOW(93), BLUE(94), MAGENTA(95), CYAN(96);

        private int code;

        private Color(int c) {
            code = c;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * Get the matching {@code Color} for the provided string.
     *
     * @param s The string to use for getting the {@code Color}. Must not be {@code null}.
     *
     * @return The matching {@code Color}, or {@code null} if not found.
     *
     * @throws IllegalArgumentException if the parameter is {@code null}
     */
    public static Color getColor(String s) {
        Color color = null;
        for(Color c : Color.values()) {
            if(s==null)
                throw new IllegalArgumentException("The string can not be null");
            if(c.name().equals(s)) {
                color = c;
                break;
            }
        }
        return color;
    }

    /**
     * Provide the string you want colorized.
     *
     * @param s The string you want colorized. Must not be {@code null}.
     * @param color The color to use. Must not be {@code null}.
     *
     * @return The string with ansi color encoding.
     *
     * @throws IllegalArgumentException if either of the parameters are {@code null}
     */
    public static String colorize(String s, Color color) {
        if(s==null)
            throw new IllegalArgumentException("The string to colorize can not be null");
        if(color==null)
            throw new IllegalArgumentException("The color to use for colorization can not be null");
        StringBuilder colorBuilder = new StringBuilder();
        String PREFIX = "\033[0;";
        String SUFFIX = "m";
        String RESET = "\033[0;0m";
        colorBuilder.append(PREFIX).append(color.getCode()).append(SUFFIX);
        colorBuilder.append(s);
        colorBuilder.append(RESET);
        return colorBuilder.toString();
    }
}
