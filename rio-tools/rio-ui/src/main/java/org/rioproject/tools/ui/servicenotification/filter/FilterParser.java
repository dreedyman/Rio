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
package org.rioproject.tools.ui.servicenotification.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a filter string.
 *
 * @author Dennis Reedy
 */
public class FilterParser {
    private final List<String> keywords = new ArrayList<String>();
    private final Map<String, Map<String, String>> operands = new HashMap<String, Map<String, String>>();

    public FilterParser() {
        keywords.add("=");
        keywords.add("is");
        keywords.add("contains");

        operands.put("type", new HashMap<String, String>());
        operands.put("desc", new HashMap<String, String>());
    }

    public FilterCriteria parse(String s) {
        FilterCriteria filterCriteria = null;
        if (s != null && s.length() > 0) {
            filterCriteria = new FilterCriteria();
            System.out.println(s);
            String[] parsed = s.split(" ");
            Map<String, String> map = null;
            for(int i=0; i<parsed.length; i++) {
                if(map==null) {
                    for(Map.Entry<String, Map<String, String>> entry : operands.entrySet()) {
                        if(entry.getKey().equals(parsed[i])) {
                            map = entry.getValue();
                            break;
                        }
                    }
                }
                if(keywords.contains(parsed[i]) && map!=null){
                    String key = parsed[i];
                    if(i<parsed.length){
                        String value = parsed[i+1];
                        map.put(key, value);
                    }
                    map = null;
                }
            }

            for(Map.Entry<String, Map<String, String>> entry : operands.entrySet()) {
                processMap(entry.getValue(), entry.getKey(), filterCriteria);
            }
            System.out.println(filterCriteria);
            System.out.println("======================");
            operands.get("type").clear();
            operands.get("desc").clear();
        }
        return filterCriteria;
    }

    private void processMap(Map<String, String> map, String operand, FilterCriteria filterCriteria) {
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(entry.getKey().equals("=") || entry.getKey().equals("is")) {
                if(operand.equals("type")) {
                    filterCriteria.addEventType(entry.getValue());
                } else {
                    filterCriteria.addContains(entry.getValue());
                }
            } else if(entry.getKey().equals("contains")) {
                if(operand.equals("type")) {
                    filterCriteria.addEventType(entry.getValue()+"*");
                } else {
                    filterCriteria.addContains(entry.getValue());
                }
            } else {
                System.out.println("Unknown keyword "+entry.getKey());
            }
        }
    }
}
