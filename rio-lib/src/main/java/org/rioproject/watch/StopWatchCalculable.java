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
package org.rioproject.watch;

/**
 * A {@code Calculable} for use with a {@code StopWatch}.
 *
 * @author Dennis Reedy
 */
public class StopWatchCalculable extends Calculable {
    private final long start;

    public StopWatchCalculable(String id, long elapsed, long end) {
        super(id, elapsed, end);
        this.start = end - elapsed;
    }

    @Override
    public String toString() {
        String s;
        if (getDetail() != null) {
            s = String.format("%s - id: [%s], start: [%s], elapsed: [%s], detail: [%s]",
                              getFormattedDate(), getId(), start, getValue(), getDetail());
        } else {
            s = String.format("%s - id: [%s], start: [%s], elapsed: [%s]",
                              getFormattedDate(), getId(), start, getValue());
        }
        return s;
    }
}
