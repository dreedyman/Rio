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

import java.util.*;

/**
 * Parses a filter string.
 *
 * @author Dennis Reedy
 */
public class FilterParser {
    private final List<String> operands = new ArrayList<String>();
    private final Map<String, Map<String, String>> keywords = new HashMap<String, Map<String, String>>();

    public FilterParser() {
        operands.add("=");
        operands.add("is");
        operands.add("contains");
        operands.add("~");

        keywords.put("type", new HashMap<String, String>());
        keywords.put("desc", new HashMap<String, String>());
    }

    public FilterCriteria parse(String s) {
        FilterCriteria filterCriteria = null;
        if (s != null && s.length() > 0) {
            filterCriteria = new FilterCriteria();
            String[] statements = getStatements(s);
            String[] parsed = s.split(" ");
            if(parsed.length<3) {
                System.out.println("The filter ["+s+"] is not a valid filter, you must declare at least <keyword> <operand> <value>");
                return null;
            }
            Map<String, String> map = null;
            for(int i=0; i<parsed.length; i++) {
                if(map==null) {
                    for(Map.Entry<String, Map<String, String>> entry : keywords.entrySet()) {
                        if(entry.getKey().equals(parsed[i])) {
                            map = entry.getValue();
                            break;
                        }
                    }
                }
                if(operands.contains(parsed[i]) && map!=null){
                    String key = parsed[i];
                    if(i<parsed.length){
                        String value = parsed[i+1];
                        map.put(key, value);
                    }
                    map = null;
                }
            }

            for(Map.Entry<String, Map<String, String>> entry : keywords.entrySet()) {
                processMap(entry.getValue(), entry.getKey(), filterCriteria);
            }

            keywords.get("type").clear();
            keywords.get("desc").clear();
        }
        return filterCriteria;
    }

    private void processMap(Map<String, String> map, String keyword, FilterCriteria filterCriteria) {
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(entry.getKey().equals("=") || entry.getKey().equals("is")) {
                if(keyword.equals("type")) {
                    filterCriteria.addEventType(entry.getValue());
                } else {
                    filterCriteria.addDescription(entry.getValue());
                }
            } else if(entry.getKey().equals("contains") || entry.getKey().equals("~")) {
                String value;
                if(entry.getValue().endsWith("*"))
                    value = entry.getValue();
                else
                    value = entry.getValue()+"*";
                if(keyword.equals("type")) {
                    filterCriteria.addEventType(value);
                } else {
                    filterCriteria.addDescription(value);
                }
            } else {
                System.out.println("Unknown keyword "+entry.getKey());
            }
        }
    }

    /*
     *
     */
    private String[] getStatements(String s) {
        List<String> statements = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(s, ";\n");
        while (tokenizer.hasMoreTokens()) {
            statements.add(tokenizer.nextToken());
        }
        return statements.toArray(new String[statements.size()]);
    }
}
