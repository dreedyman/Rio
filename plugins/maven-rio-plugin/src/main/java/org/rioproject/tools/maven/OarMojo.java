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

import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.artifact.AttachedArtifact;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.ZipFileSet;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Properly packages an OAR (Operational String Archive).
 *
 * @goal oar
 * @description Build & deploy a Rio OAR
 * @phase package
 * @requiresProject true
 * @requiresDependencyResolution
 */
public class OarMojo extends ClassDepAndJarMojo {

    /**
     * Name of the OAR.
     *
     * @parameter
     */
    private String oarName;

    /**
     * The OAR to generate.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}.oar"
     * @required
     */
    private String oarFileName;

    /**
     * OpString to deploy.
     *
     * @parameter
     * @required
     */
    private String opstring;

    /**
     * OAR activation. Either "Automatic" or "Manual"
     *
     * @parameter expression="Automatic"
     */
    private String activation;

    /**
     * @parameter expression="${project.dependencyArtifacts}"
     */
    private Collection dependencies;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    public void execute() throws MojoExecutionException {
        File opstringFile = getOpStringFile();
        if(!opstringFile.exists()) {
            getLog().debug("The opstring ["+opstring+"] does not exist, cannot build OAR");
            return;
        }
        doExecute(getMavenProject(), false);                
        getLog().info("Building OAR: "+getOarFileName());
        if(getOarName()==null)
            oarName = getMavenProject().getArtifactId();
        Jar oar = new Jar();
        oar.setProject(antProject);
        File oarFile = new File(getOarFileName());
        oar.setDestFile(oarFile);

        // add the pom
        File projectPath = getMavenProject().getBasedir();
        String pomName = getMavenProject().getBuild().getFinalName();
        File tempPom = new File(System.getProperty("java.io.tmpdir"), pomName+".pom");
        tempPom.deleteOnExit();
        File projectPom = new File(projectPath, "pom.xml");
        try {
            FileUtils.copyFile(projectPom, tempPom);
        } catch (IOException e) {
            throw new MojoExecutionException("could not create temp pom", e);
        }
        ZipFileSet fileSetPom = new ZipFileSet();
        fileSetPom.setDir(tempPom.getParentFile());
        fileSetPom.setIncludes(tempPom.getName());
        oar.addZipfileset(fileSetPom);

        // add the opstring
        ZipFileSet fileSetOpstring = new ZipFileSet();
        fileSetOpstring.setDir(opstringFile.getParentFile());
        fileSetOpstring.setIncludes(opstringFile.getName());
        oar.addZipfileset(fileSetOpstring);

        List<String> attached = new ArrayList<String>();
        for(Object o : getMavenProject().getAttachedArtifacts()) {
            AttachedArtifact a = (AttachedArtifact)o;
            StringBuilder sb = new StringBuilder();
            sb.append(a.getGroupId());
            sb.append(":");
            sb.append(a.getArtifactId());
            sb.append(":");
            if(a.getClassifier()!=null && a.getClassifier().length()>0) {
                sb.append(a.getClassifier());
                sb.append(":");
            }
            sb.append(a.getVersion());
            attached.add(sb.toString());
        }
        
        // add the manifest
        Manifest manifest = new Manifest();
        try {
            manifest.addConfiguredAttribute(
                new Manifest.Attribute("OAR-Name", oarName));
            manifest.addConfiguredAttribute(
                new Manifest.Attribute("OAR-Version", getMavenProject().getVersion()));
            manifest.addConfiguredAttribute(
                new Manifest.Attribute("OAR-OperationalString",
                                       getOpStringFile().getName()));
            manifest.addConfiguredAttribute(
                new Manifest.Attribute("OAR-Activation", activation));
            if(attached.size()>0) {
                StringBuilder sb = new StringBuilder();
                for(String s: attached) {
                    if(sb.length()>0)
                        sb.append(",");
                    sb.append(s);
                }
                manifest.addConfiguredAttribute(
                    new Manifest.Attribute("OAR-Artifacts", sb.toString()));
            }
        } catch (ManifestException e) {
            getLog().error("Can't generate OAR manifest", e);
            return;
        }
        try {
            oar.addConfiguredManifest(manifest);
        } catch (ManifestException e) {
            getLog().error("Can't add manifest into OAR", e);
            return;
        }

        /* Make sure target directory exists */
        File target = oarFile.getParentFile();
        if(!target.exists()) {
            if(target.mkdirs())
                getLog().debug("Created "+target.getPath());
        }
        /* Execute the Ant task */
        oar.execute();
        if(!project.getPackaging().equals("oar")) {
            getLog().info("Attaching artifact "+oarFile.getName()+" as type oar");
            getProjectHelper().attachArtifact(project, "oar", null, oarFile);
            for(Object o : project.getAttachedArtifacts()) {
                if(o instanceof AttachedArtifact) {
                    AttachedArtifact a = (AttachedArtifact)o;
                    if(a.getType().equals("oar")) {
                        a.setArtifactHandler(new DefaultArtifactHandler("oar"));
                        getLog().debug("Artifact : "+a+", type: "+a.getType()+", " +
                                       "artifact handler extension: "+
                                       a.getArtifactHandler().getExtension());
                    }
                }
            }
        } else {
            getMavenProject().getArtifact().setFile(oarFile);
        }
    }

    protected String getOarName() {
        return oarName;
    }

    protected String getOarFileName() {
        return oarFileName;
    }

    @Override
    protected MavenProject getMavenProject() {
        return project;
    }

    @Override
    protected Collection getDependencies() {
        return dependencies;
    }

    @Override
    protected MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    protected String getOpstring() {
        return opstring;
    }

    private File getOpStringFile() {
        File f;
        if(!getOpstring().startsWith(File.separator)) {
            f = new File(project.getBasedir(), getOpstring());
        } else {
            f = new File(getOpstring());
        }
        return f;
    }

}
