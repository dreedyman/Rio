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
package org.rioproject.bean.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a way to compare method signatures as well as package a method for
 * serialization
 */
public class PackagedMethod implements Serializable {
    private static final long serialVersionUID = 1L;
    String name;
    final List<Class> parameterList = new ArrayList<Class>();

    public PackagedMethod(Method method) {
        name = method.getName();
        parameterList.addAll(Arrays.asList(method.getParameterTypes()));
    }

    public void clear() {
        parameterList.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PackagedMethod that = (PackagedMethod) o;
        return (
            !(name != null ? !name.equals(that.name) : that.name != null) &&
            !(parameterList != null ?
              !parameterList.equals(that.parameterList) :
              that.parameterList != null));

    }

    @Override
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result =
            31 * result +
            (parameterList != null ? parameterList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PackagedMethod{" +
               "name='" + name + '\'' +
               ", parameterList=" + parameterList +
               '}';
    }
}
