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
import org.rioproject.config.Component

/*
 * Configure the invocation delay duration for the JMXFaultDetectionHandler.
 * This is used (by default) for a forked service to monitor the presence of the
 * Cybernode that created it. If the forked service detects that it's parent
 * Cybernode has orphaned it, it will terminate.
 */
@Component('org.rioproject.fdh.JMXFaultDetectionHandler')
class JMXFaultDetectionHandlerConfig {
    /*
     * Set the invocation delay (in milliseconds) to be 5 seconds. Default is 60 seconds.
     */
    long invocationDelay = 5000;

    /*
     * Set the number of times to retry connecting to the service. If the service cannot
     * be reached within the retry count specified the service will be determined to be
     * unreachable. Default is 3
     */
    //int retryCount = 3

    /*
     * Set the amount of time to wait between retries (in milliseconds).
     * Set how long to wait between retries (in milliseconds). This value
     * will be used between retry attempts, waiting the specified amount of
     * time to retry. Default is 1 second (1000 milliseconds)
     */
    //long retryTimeout = 1000
}

