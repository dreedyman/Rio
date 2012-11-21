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
    private final List<String> keywords = new ArrayList<String>();
    private final Map<String, Map<String, String>> operands = new HashMap<String, Map<String, String>>();

    public FilterParser() {
        keywords.add("=");
        keywords.add("is");
        keywords.add("contains");
        keywords.add("~");

        operands.put("type", new HashMap<String, String>());
        operands.put("desc", new HashMap<String, String>());
    }

    public FilterCriteria parse(String s) {
        FilterCriteria filterCriteria = null;
        if (s != null && s.length() > 0) {
            filterCriteria = new FilterCriteria();
            System.out.println(s);
            String[] statements = getStatements(s);
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
                    filterCriteria.addDescription(entry.getValue());
                }
            } else if(entry.getKey().equals("contains") || entry.getKey().equals("~")) {
                String value;
                if(entry.getValue().endsWith("*"))
                    value = entry.getValue();
                else
                    value = entry.getValue()+"*";
                if(operand.equals("type")) {
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
