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
package org.rioproject.tools.ui;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Fetch ServiceItems
 *
 * @author Dennis Reedy
 */
public class ServiceItemFetchQ  {
    private static final BlockingQueue<Request> serviceItemFetchQ = new LinkedBlockingQueue<Request>();

    public static void write(GraphNode node, GraphView graphView) {
        serviceItemFetchQ.add(new Request(node, graphView));
    }

    public static void write(Request request) {
        serviceItemFetchQ.add(request);
    }

    public static Request take() throws InterruptedException {
        return serviceItemFetchQ.take();
    }

    static class Request {
        final GraphNode node;
        final GraphView graphView;

        Request(GraphNode node, GraphView graphView) {
            this.node = node;
            this.graphView = graphView;
        }
    }
}
