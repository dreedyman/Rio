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
package org.rioproject.associations;

import java.io.Serializable;

/**
 * Just a dummy class.
 */
public class DummyImpl implements Dummy, Serializable {
    int index;
    String name;

    DummyImpl(int index) {
        this.index = index;
    }

    DummyImpl(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DummyImpl dummy = (DummyImpl) o;

        if (index != dummy.index)
            return false;
        if (name != null ? !name.equals(dummy.name) : dummy.name != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

