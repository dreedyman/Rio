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
package org.rioproject.associations;

/**
 * The {@code AssociationType} defines the different types of an {@code Association}.
 * An {@code AssociationType} may be either:
 * <ul>
 * <li><b><u>Uses</u> </b> <br>
 * A weak association relationship where if A uses B exists then, then B may be
 * present for A
 * <li><b><u>Requires</u> </b> <br>
 * A stronger association relationship where if A requires B exists then B must
 * be present for A
 * <li><b><u>Colocated</u> </b> <br>
 * An association which requires that A be colocated with B in the same
 * JVM. If B does not exist, or cannot be located, A shall not be created
 * without B
 * <li><b><u>Opposed</u> </b> <br>
 * An association which requires that A exist in a different JVM then B.
 * <li><b><u>Isolated</u> </b> <br>
 * An association which requires that A exist in a different machine then B.
 * </ul>
 *
 * @author Dennis Reedy
 */
public enum AssociationType {
    USES, REQUIRES, COLOCATED, OPPOSED, ISOLATED;

    @Override
    public String toString() {
        String s;
        switch(this) {
            case REQUIRES:
                s = "requires";
                break;
            case COLOCATED:
                s = "colocated";
                break;
            case OPPOSED:
                s = "opposed";
                break;
            case ISOLATED:
                s = "isolated";
                break;
            default:
                s = "uses";
        }
        return "AssociationType="+s;
    }
    
}
