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
 * A {@code ThresholdListener} that provides capability to set an identifier and a {@code ThresholdManager}.
 *
 * @author Dennis Reedy
 */
public interface SettableThresholdListener extends ThresholdListener {
    /**
     * Get the ID of the ThresholdWatch the ThresholdListener is associated to
     *
     * @return The identifier (ID) of the ThresholdWatch the
     * ThresholdListener is associated to
     */
    String getID();

    /**
     * Set the ThresholdManager and connect to the ThresholdManager
     *
     * @param thresholdManager The ThresholdManager to connect to
     */
    void setThresholdManager(ThresholdManager thresholdManager);
}
