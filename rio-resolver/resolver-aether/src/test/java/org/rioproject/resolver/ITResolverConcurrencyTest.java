/*
 * Copyright 2012 to the original author or authors.
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
package org.rioproject.resolver;

import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.resolver.aether.AetherResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Dennis Reedy
 */
public class ITResolverConcurrencyTest {

    @Test
    public void testConcurrentAccess() throws ExecutionException, InterruptedException {
        Resolver r = new AetherResolver();
        ExecutorService resolverExecutor = Executors.newCachedThreadPool();
        System.out.println("Create 100 threads");
        long t0 = System.currentTimeMillis();
        List<Future<String[]>> futures = new ArrayList<Future<String[]>>();
        for(int i=0; i<100; i++) {
            futures.add(resolverExecutor.submit(new Request(r)));
        }
        Assert.assertTrue(futures.size() == 100);
        for(Future<String[]> future : futures) {
            String[] classPath = future.get();
            Assert.assertTrue("Expected 7 jars, got " + classPath.length, classPath.length == 7);
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Complete, took "+(t1-t0)+" millis");
    }

    @Test
    public void testConcurrentAccess2() throws ExecutionException, InterruptedException {
        Resolver r = new AetherResolver();
        ExecutorService resolverExecutor = Executors.newCachedThreadPool();
        System.out.println("Create 100 threads");
        long t0 = System.currentTimeMillis();
        List<Future<String[]>> futures = new ArrayList<Future<String[]>>();
        RemoteRepository repository = new RemoteRepository();
        repository.setUrl("http://repo1.maven.org/maven2/");
        repository.setId("central");
        for(int i=0; i<100; i++) {
            futures.add(resolverExecutor.submit(new Request(r, new RemoteRepository[]{repository})));
        }
        Assert.assertTrue(futures.size() == 100);
        for(Future<String[]> future : futures) {
            String[] classPath = future.get();
            Assert.assertTrue("Expected 7 jars, got "+classPath.length, classPath.length==7);
        }
        long t1 = System.currentTimeMillis();
        System.out.println("Complete, took "+(t1-t0)+" millis");
    }

    class Request implements Callable<String[]> {
        Resolver resolver;
        RemoteRepository[] repositories;

        Request(Resolver resolver) {
            this.resolver = resolver;
        }

        Request(Resolver resolver, RemoteRepository[] repositories) {
            this.resolver = resolver;
            this.repositories = repositories;
        }

        public String[] call() throws ResolverException {
            String[] classPath;
            if(repositories==null)
                classPath = resolver.getClassPathFor("org.apache.maven:maven-settings-builder:3.0.3");
            else
                classPath = resolver.getClassPathFor("org.apache.maven:maven-settings-builder:3.0.3", repositories);
            return classPath;

        }
    }
}
