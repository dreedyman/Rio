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
package org.rioproject.examples.workflow;

enum State  {
    NEW, PENDING, OPEN, CLOSED;

    public State next() {
        State next=null;
        switch(this) {
            case NEW:
                next = PENDING;
                System.out.println("Next -> PENDING");
                break;
            case PENDING:
                next = OPEN;
                System.out.println("Next -> OPEN");
                break;
            case OPEN:
                next = CLOSED;
                System.out.println("Next -> CLOSED");
                break;
            case CLOSED:
                next = CLOSED;
                break;
        }
        return(next);
    }
}
