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
package org.rioproject.tools.maven;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Holds attributes required for creating a jar using classdedpandjar
 */
public class CreateJar {
    /**
     * @parameter
     */
    private List<String> ins = new ArrayList<String>();

    /**
     * @parameter
     */
    private String in;

    /**
     * @parameter
     */
    private String out;

    /**
     * @parameter
     */
    private List<String> outs = new ArrayList<String>();

    /**
     * @parameter
     */
    private List<String> skips = new ArrayList<String>();

    /**
     * @parameter
     */
    private List<String> topclasses = new ArrayList<String>();

    /**
     * @parameter
     */
    private String topclass;

    /**
     * @parameter
     */
    private String preferredlist;

    /**
     * Name of the service implementation JAR.
     *
     * @parameter
     */
    private String jarname;

    /**
     * Whether to include resources
     *
     * @parameter
     */
    private boolean includeResources = true;

    /**
     * @parameter
     */
    private Map<String, String> manifest;

    /**
     * Artifact Classifier
     *
     * @parameter default-value=""
     */
    private String classifier;

    public List<String> getIns() {
        if(ins.isEmpty() && in!=null)
            ins.add(in);
        return ins;
    }

    public List<String> getOuts() {
        if(outs.isEmpty() && out!=null)
            outs.add(out);
        return outs;
    }

    public List<String> getSkips() {
        return skips;
    }

    public List<String> getTopclasses() {
        if(topclasses.isEmpty() && topclass!=null)
            topclasses.add(topclass);
        return topclasses;
    }

    public String getPreferredlist() {
        return preferredlist;
    }

    public String getJarname() {
        return jarname;
    }

    public Map<String, String> getManifest() {
        return manifest;
    }

    public boolean includeResources() {
        return includeResources;
    }

    public String getClassifier() {
        return classifier;
    }

    void setIns(List<String> ins) {
        this.ins = ins;
    }

    void setJarname(String jarname) {
        this.jarname = jarname;
    }

    public void setTopclass(String topclass) {
        this.topclass = topclass;
    }
}
