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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class that parses logged {@link Calculable} instances.
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("unused")
public class CalculablesFromLog {

    /**
     * Parse a location for {@code Calculable}s.
     *
     * @param url The {@code URL} to load and parse, must not be {@code null}.
     * @return A {@link List} of {@code Calculable}s. If the {@code URL} is opened and parsed, and contains no
     *         parse-able {@code Calculable}s, an empty list is returned. A new list is created each time.
     */
    public static List<Calculable> parse(URL url) throws IOException {
        List<Calculable> calculables = new LinkedList<Calculable>();
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String VALUE_INDEX = "value: [";
        String ELAPSED_INDEX = "elapsed: [";
        String DETAIL_INDEX = "detail: [";
        final String ID_INDEX = "id: [";
        final String END_INDEX = "]";

        String line;
        try {
            while ((line = in.readLine()) != null) {
                int ndx = line.indexOf("-");
                String dateString = line.substring(0, ndx).trim();
                ndx = line.indexOf(ID_INDEX);
                if(ndx==-1) {
                    System.err.println(String.format("The %s has an unrecognized format", url.toExternalForm()));
                    break;
                }
                String id = line.substring(ndx+ID_INDEX.length(), line.indexOf(END_INDEX)).trim();
                int valueIndex = line.indexOf(VALUE_INDEX);
                int indexLength = VALUE_INDEX.length();
                /* We may be parsing a line produced from a StopWatch, check for elapsed */
                if(valueIndex==-1) {
                    valueIndex = line.indexOf(ELAPSED_INDEX);
                    indexLength = ELAPSED_INDEX.length();
                }
                if(valueIndex==-1) {
                    System.err.println(String.format("The %s has an unrecognized format", url.toExternalForm()));
                    break;
                }
                String choppedString = line.substring(valueIndex+indexLength);
                String value = choppedString.substring(0, choppedString.indexOf(END_INDEX));
                int detailIndex = line.indexOf(DETAIL_INDEX);
                String detail = null;
                if(detailIndex>=0) {
                    detail = line.substring(detailIndex+DETAIL_INDEX.length(), line.lastIndexOf(END_INDEX));
                }
                Date date;
                try {
                    date = Calculable.DATE_FORMATTER.parse(dateString);
                } catch (ParseException e) {
                    continue;
                }
                Calculable calculable = new Calculable(id, Double.parseDouble(value), date.getTime());
                calculable.setDetail(detail);
                calculables.add(calculable);
            }
        } finally {
            in.close();
        }
        return calculables;
    }
}
