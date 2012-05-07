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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.jini.rio.tools.ant.ClassDepAndJarTask;

import java.io.*;
import java.util.*;

/**
 * This goal runs the <tt>ClassDepAndJarTask</tt> utility for a module. The
 * <tt>ClassDepAndJarTask</tt> creates a jar file from a list of classes generated
 * by the ClassDep tool. In particular it:
 * <ol>
 * <li>Searches and extracts classes directly from jar files that are specified
 * in the Maven project's classpath. Thus it is not necessary to unjar libraries
 * in order to include elements (classes).</li>
 * <li>Creates a Jar file directly; it is not an extension of the Maven Jar plugin.</li>
 * <li>Additional elements can be included in the jar file by configuring "includes" elements.</li>
 * </ol>
 *
 * @goal classdepandjar
 * @description Run classdepandjar for a module
 * @phase package
 * @requiresProject true
 * @requiresDependencyResolution
 */
public class ClassDepAndJarMojo extends AbstractMojo {
    /**
     * List of jars to create
     *
     * @parameter
     */
    private List<CreateJar> createJars;

    /**
     * Create a single jar
     *
     * @parameter
     */
    private CreateJar createJar;

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Dependency artifacts
     * 
     * @parameter expression="${project.dependencyArtifacts}"
     */
    private Collection dependencies;

    /**
	 * The Jar archiver.
	 *
	 * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
	 * @required
	 */
	protected JarArchiver jarArchiver;

	/**
	 * The maven archive configuration to use.
	 */
	protected MavenArchiveConfiguration archiveConfig = new MavenArchiveConfiguration();

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    protected Project antProject = new Project();

    public void execute() throws MojoExecutionException {
        doExecute(project, true);
    }

    protected void doExecute(MavenProject project, boolean setMainArtifact) throws MojoExecutionException {
        File contentDirectory = new File(project.getBuild().getOutputDirectory());
        if(getCreateJar()==null && getCreateJars()==null) {
            if (!contentDirectory.exists() ) {
                getLog().info("No content found, default project JAR will not be created");
                return;
            }
        }        
        String outputDir = project.getBuild().getDirectory();
        getLog().debug("Project Name    : "+project.getName());
        getLog().debug("Artifact ID     : "+project.getArtifactId());
        getLog().debug("Build Directory : "+outputDir)       ;
        getLog().debug("Project Base Dir : "+project.getBasedir());
        getLog().debug("Build Dir        : "+project.getBuild().getDirectory());
        getLog().debug("Build Output Dir : "+contentDirectory.getPath());
        Properties props = new Properties();
        props.setProperty("version", project.getVersion());
        props.setProperty("groupId", project.getGroupId());
        props.setProperty("artifactId", project.getArtifactId());
        File artifactResourcesDir;
        try {
            artifactResourcesDir = writeProjectInfo(props, project);
        } catch (IOException e) {
            throw new MojoExecutionException("Creating project artifacts", e);
        }

        boolean createDefaultProjectJar = true;
        if(getCreateJars()!=null) {
            for (CreateJar createJar : getCreateJars()) {
                if(createJar.getClassifier()==null) {
                    createDefaultProjectJar = false;
                    break;
                }
            }
        } else {
            if(createJar!=null && createJar.getClassifier()==null)
                createDefaultProjectJar = false;
        }

        /* If the packaging type is "jar", do not create a default project jar */
        if(createDefaultProjectJar)
            createDefaultProjectJar = !project.getPackaging().equals("jar");

        getLog().debug("Create default project jar: "+createDefaultProjectJar);
        if(createDefaultProjectJar) {
            MavenArchiver archiver = new MavenArchiver();
            archiver.setArchiver(getJarArchiver());
            File jarFile = new File(project.getBuild().getDirectory(),
                                    project.getBuild().getFinalName()+".jar");
            archiver.setOutputFile(jarFile);
            try {
                String[] includes = new String[]{"**/**"};
                String[] excludes = new String[]{"**/package.html"};
                archiver.getArchiver().addDirectory( contentDirectory, includes,  excludes);

                archiver.createArchive(project, archiveConfig);
            } catch (Exception e) {
                throw new MojoExecutionException("Failed creating default project jar",
                                                 e);
            }
            if(setMainArtifact)
                project.getArtifact().setFile(jarFile);
            else
                getProjectHelper().attachArtifact(project, "jar", "", jarFile);
        }
        if (getCreateJars() != null) {
            for (CreateJar createJar : getCreateJars())
                createJar(createJar,
                          antProject,
                          outputDir,
                          artifactResourcesDir,
                          setMainArtifact);
        } else {
            if(getCreateJar()!=null) {
                createJar(getCreateJar(),
                          antProject,
                          outputDir,
                          artifactResourcesDir,
                          setMainArtifact);
            }
        }

        StringBuffer sb = new StringBuffer();
        for(Object o : project.getAttachedArtifacts()) {
            sb.append("\t"+o.toString()+"\n");
        }
        getLog().debug("ATTACHED ARTIFACTS\n"+sb.toString());
    }

    protected MavenProject getMavenProject() {
        return project;
    }

    protected Collection getDependencies() {
        return dependencies;
    }

    protected MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    protected List<CreateJar> getCreateJars() {
        return createJars;
    }

    protected CreateJar getCreateJar() {
        return createJar;
    }

    protected JarArchiver getJarArchiver() {
        return jarArchiver;
    }

    protected void parseDependenciesAndBuildClasspath(Path classpath) {
        for (Object dependency : getDependencies()) {
            Artifact artifact = (Artifact) dependency;
            if ("pom".equalsIgnoreCase(artifact.getType())) {
                getLog().debug("Skipping pom artifact element " + artifact.getFile());
                continue;
            }
            getLog().debug("Found artifact element " + artifact.getFile());
            if (artifact.getFile() != null)
                classpath.append(new Path(antProject,
                                          artifact.getFile().toString()));
        }
    }

    protected void createJar(CreateJar createJar,
                             Project antProject,
                             final String outputDir,
                             File artifactResourcesDir,
                             boolean setMainArtifact) throws  MojoExecutionException {
        
        List<String> ins = createJar.getIns();
        List<String> outs = createJar.getOuts();
        List<String> skips = createJar.getSkips();
        List<String> topclasses = createJar.getTopclasses();
        String preferredlist = createJar.getPreferredlist();
        String jarname = createJar.getJarname();
        Map<String, String> manifest = createJar.getManifest();

        String classifier;
        if(createJar.getClassifier()==null || createJar.getClassifier().length()==0)
            classifier = "";
        else
            classifier = "-"+createJar.getClassifier();
        MavenProject mProj = getMavenProject();
        if(jarname==null) {
            jarname = mProj.getArtifactId()+"-"+mProj.getVersion()+classifier+".jar";
        } else {
            if(!jarname.endsWith(".jar")) {
                jarname = jarname+classifier+".jar";
            } else {
                jarname = jarname.substring(0, jarname.length()-4)+classifier+".jar";
            }
        }

        StringBuilder jarNameBuilder = new StringBuilder();
        jarNameBuilder.append(outputDir);
        if(!outputDir.endsWith(File.separator))
            jarNameBuilder.append(File.separator);
        jarNameBuilder.append(jarname);
        String jarFileName =  jarNameBuilder.toString();

        // build the compilation classpath as a Ant path
        Path classpath = new Path(new Project());
        List classpaths;

        try {
            classpaths = mProj.getCompileClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Can't get compilation classpath",
                                             e);
        }
        for (Object o : classpaths) {
            getLog().debug("Found classpath element " + o.toString());
            classpath.append(new Path(antProject, o.toString()));
        }
        /*for (Object dependency : getDependencies()) {
            Artifact artifact = (Artifact) dependency;
            if ("pom".equalsIgnoreCase(artifact.getType())) {
                getLog().debug("Skipping pom artifact element " + artifact.getFile());
                continue;
            }
            getLog().debug("Found artifact element " + artifact.getFile());
            if (artifact.getFile() != null)
                classpath.append(new Path(antProject,
                                          artifact.getFile().toString()));
        }*/
        parseDependenciesAndBuildClasspath(classpath);

        getLog().debug("Build ant path: " + classpath.toString());
        getLog().info("Building jar: " + jarFileName);

        ClassDepAndJarTask classDepAndJarTask = new ClassDepAndJarTask();
        classDepAndJarTask.setProject(antProject);
        classDepAndJarTask.setClasspath(classpath);
        classDepAndJarTask.setFiles(true);
        File resourcesDirectory = new File(mProj.getBasedir(),
                                           "src" + File.separatorChar + "main" +
                                           File.separatorChar + "resources");
        if (resourcesDirectory.exists() && createJar.includeResources()) {
            FileSet resourcesFileSet = new FileSet();
            resourcesFileSet.setDir(resourcesDirectory);
            resourcesFileSet.setIncludes("**/*");
            classDepAndJarTask.addFileset(resourcesFileSet);
        }

        FileSet artifactResourcesSet = new FileSet();
        artifactResourcesSet.setDir(artifactResourcesDir);
        artifactResourcesSet.setIncludes("**/*");
        classDepAndJarTask.addFileset(artifactResourcesSet);

        if (preferredlist != null && !"".equals(preferredlist))
            classDepAndJarTask.setPreferredlist(new File(preferredlist));
        classDepAndJarTask.setJarfile(new File(jarFileName));

        if(manifest!=null) {
            org.apache.tools.ant.taskdefs.Manifest m =
                new org.apache.tools.ant.taskdefs.Manifest();
            for(Map.Entry<String, String> entry : manifest.entrySet())
                try {
                    m.addConfiguredAttribute(new Manifest.Attribute(entry.getKey(),
                                                                    entry.getValue()));
                } catch (ManifestException e) {
                    throw new MojoExecutionException("Constructing Manifest", e);
                }
            classDepAndJarTask.addConfiguredManifest(m);
            getLog().debug("Added Manifest: "+m.toString());
        }

        if(ins!=null) {
            for (String in : ins) {
                classDepAndJarTask.setIn(in);
            }
        }
        for (String out : outs) {
            classDepAndJarTask.setOut(out);
        }
        for (String skip : skips) {
            classDepAndJarTask.setSkip(skip);
        }
        for (String topclass : topclasses) {
            classDepAndJarTask.setTopclass(topclass);
        }

        classDepAndJarTask.execute();

        if(createJar.getClassifier()==null && setMainArtifact) {
            getLog().info("Set project artifact as "+jarFileName);
            mProj.getArtifact().setFile(new File(jarFileName));
        } else {
            getProjectHelper().attachArtifact(mProj,
                                              "jar",
                                              createJar.getClassifier(),
                                              new File(jarFileName));
        }
    }

    protected File writeProjectInfo(Properties props, MavenProject project) throws IOException {
        String path = project.getBuild().getDirectory();
        if(!path.endsWith("/"))
            path = path+"/";
        String root = path+"classdepandjar";
        File rootDir = new File(root);
        if(!rootDir.exists()) {
            if(rootDir.mkdirs())
                getLog().debug("Created "+rootDir.getPath());
        }
        File mavenResourceDir = new File(rootDir, "META-INF/maven");
        if(!mavenResourceDir.exists()) {
            if(mavenResourceDir.mkdirs())
                getLog().debug("Created "+mavenResourceDir.getPath());
        }
        File groupIdDir = new File(mavenResourceDir, project.getGroupId());
        if(groupIdDir.mkdirs()) {
            getLog().debug("Created "+groupIdDir.getPath());
        }
        File artifactIdDir = new File(groupIdDir, project.getArtifactId());
        if(artifactIdDir.mkdirs()) {
            getLog().debug("Created "+artifactIdDir.getPath());
        }
        File propFile = new File(artifactIdDir, "pom.properties");

        props.store(new FileOutputStream(propFile), null);

        File projDir = project.getBasedir();
        File pom = new File(projDir, "pom.xml");
        copy(pom, new File(artifactIdDir, "pom.xml"));
        return rootDir;
    }

    private void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}
