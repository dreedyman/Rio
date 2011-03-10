package org.rioproject.resolver.maven2

import java.util.logging.Logger
import java.util.logging.Level
import org.rioproject.resolver.Resolver
import org.rioproject.resolver.Artifact
import org.rioproject.resolver.Dependency
import org.rioproject.resources.util.DownloadManager
import org.rioproject.core.provision.StagedData
import org.rioproject.core.provision.DownloadRecord
import net.jini.url.httpmd.HttpmdUtil
import org.rioproject.resolver.maven2.filters.ExclusionFilter
import org.rioproject.resolver.maven2.filters.DependencyFilter
import org.rioproject.resolver.maven2.filters.ClassifierFilter

import org.rioproject.opstring.OAR

import org.rioproject.config.maven2.Repository
import org.rioproject.resolver.RemoteRepository
import org.rioproject.config.maven2.Settings
import org.rioproject.config.maven2.SettingsParser
import org.rioproject.resources.util.FileUtils

class SimpleResolver implements Resolver {
    static Logger logger = Logger.getLogger(SimpleResolver.class.getName());
    Settings m2settings
    List<RemoteRepository> repositories = []
    private File repoDir = Repository.getLocalRepository()
    private ArtifactUtils artifactUtils = new ArtifactUtils()

    def SimpleResolver() {
    }

    public static void main(String... args) {
        if (args.length != 2) {
            println "you must provide an artifactID and the directory of the pom"
            System.exit(-1);
        }
        SimpleResolver r = new SimpleResolver()
        println "Classpath for artifact ${args[0]} is "+
                "[${r.getClassPathFor(args[0], new File(args[1]), true)}]"
    }

    Collection<RemoteRepository> getRemoteRepositories() {
        List<RemoteRepository> remotes = []
        remotes.addAll(repositories)
        return remotes
    }

    public URL getLocation(String a, String aType,  File pomFile) {
        if (a == null)
            throw new IllegalArgumentException("An artifact must be provided")

        loadSettings()
        Artifact artifact = new Artifact(a)
        if(pomFile!=null) {
            for(RemoteRepository r : Repository.getRemoteRepositories(pomFile)) {
                if(!repositories.contains(r))
                    repositories.add(r)
            }
        }        
        String type = aType==null?"jar":(aType.length()==0?"jar":aType)
        URL loc = artifactUtils.getArtifactURLFromRepository(artifact,
                                               type,
                                               repoDir,
                                               repositories)
        return loc
    }

    public String[] getClassPathFor(String s, RemoteRepository[] remote, boolean download) {

        for(RemoteRepository rr: remote) {
            if(!repositories.contains(rr))
                repositories << rr
        }
        if(logger.isLoggable(Level.FINE)) {
            StringBuffer sb = new StringBuffer()
            for(RemoteRepository rr: repositories) {
                if(sb.length()>0)
                    sb.append(", ")
                sb.append(rr.url)
            }
            println "Artifact: $s, using Remote repositories: [$sb]"
        }
        return getClassPathFor(s, (File)null, download)
    }

    public String[] getClassPathFor(String s, File pom, boolean download) {
        return doResolve(s, pom, download)
    }

    protected String[] doResolve(String s, File pom, boolean download) {
        if (s == null)
            throw new IllegalArgumentException("An artifact must be provided")

        loadSettings()
        List<String> classPath = new ArrayList<String>()        
        Artifact artifact = new Artifact(s)
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(artifact.toString())
        }

        URL pomURL
        if(pom!=null)
            pomURL = pom.toURI().toURL()
        else
            pomURL = artifactUtils.getArtifactPomURLFromRepository(artifact,
                                                                   repoDir,
                                                                   repositories)
        artifact.pomURL = pomURL
        PomParser p = new PomParser()
        p.artifactUtils = artifactUtils
        p.remoteRepositories = repositories
        p.localRepository = repoDir
        p.settings = m2settings
        DependencyFilter filter = getDependencyFilter(artifact)
        ResolutionResult result = p.parse(artifact, pomURL, filter)
        if(result!=null) {
            repositories = p.remoteRepositories
            resolve(result.artifact, filter, classPath, download)
            StringBuffer sb = new StringBuffer()
            sb.append "${result.artifact.groupId}:${result.artifact.artifactId}:${result.artifact.version}"
            sb.append "\n"
            for(Dependency dep : result.dependencies) {
                sb.append parseDependency(dep, filter, classPath, p.excludes, 1, download)
            }
            if(logger.isLoggable(Level.FINE))
                logger.fine sb.toString()
        }
        //println s+": "+result.dependencies
        return classPath.toArray(new String[classPath.size()])
    }

    private String parseDependency(Dependency dep,
                                   DependencyFilter filter,
                                   List<String> cp,
                                   def excludes,
                                   int indent,
                                   boolean download) {
        StringBuffer sb = new StringBuffer()
        if(!dep.scope.equals("test")) {
            sb.append printDependency(dep, indent)
            int i = indent
            i++
            PomParser p = new PomParser()
            p.artifactUtils = artifactUtils
            p.remoteRepositories = repositories
            p.localRepository = repoDir
            p.settings = m2settings
            p.excludes = excludes
            ResolutionResult result = p.parse(dep, filter)
            repositories = p.remoteRepositories
            //println "===> ${dumpRemoteRepositories()}"
            if(result && result.artifact) {
                resolve(result.artifact, filter, cp, download)
                for(Dependency d : result.dependencies) {
                    sb.append parseDependency(d, filter, cp, p.excludes, i, download)
                }
            }
        }
        return sb.toString()
    }

    private void resolve(Artifact a,
                         DependencyFilter filter,
                         List<String> cp,
                         boolean download) {
        if(logger.isLoggable(Level.FINE))
            logger.fine "Resolve ${a.getGAV()}, install?: $download"
        if(!a.pom.exists()) {
            if(a.pomURL!=null && download)
                install(a.pomURL.toExternalForm(), installTo(a.pom.absolutePath))
        }
        if(download) {
            boolean downloaded = true
            if(logger.isLoggable(Level.FINE))
                logger.fine "Install ${a.getGAV()}, jar: ${a.jar}"
            if(a.jar!=null) {
                if(logger.isLoggable(Level.FINE))
                    logger.fine "${a.getGAV()}, jar exists? ${a.jar.exists()}"
                if(!a.jar.exists()) {                    
                    if(a.pomURL==null || (a.pomURL.protocol=="file")) {
                        a.pomURL = artifactUtils.getArtifactPomURLFromRepository(a,
                                                                                 null,
                                                                                 repositories)
                    }
                    URL aJarURL
                    if(a.isSnapshot() && a.jar.name.endsWith("jar")) {
                        aJarURL =
                            artifactUtils.getArtifactURLFromRepository(a,
                                                                       "jar",
                                                                       repoDir,
                                                                       repositories)
                    } else {
                        aJarURL = a.getJarURL()
                    }
                    if(logger.isLoggable(Level.FINE))
                        logger.fine "${a.getGAV()}, jar URL ${aJarURL}"
                    if(aJarURL!=null) {
                        DownloadRecord record =
                            install(aJarURL.toExternalForm(), installTo(a.jar.absolutePath))
                        if(record==null) {
                            downloaded = false
                        } else {
                            /* make sure that the snapshot artifact's version does
                             * not need to be applied */
                            if(a.isSnapshot() && record!=null) {
                                if(!a.jar.exists()) {
                                    String newVersion = record.name.substring(a.artifactId.length()+1)
                                    int ndx = newVersion.lastIndexOf(".")
                                    a.version = newVersion.substring(0, ndx)
                                    a.jar = new File(a.jar.parent, (String)record.name)
                                }
                            }
                        }
                    } else {
                        downloaded = false
                        println "[WARNING] Could not resolve artifact "+
                                a.getGAV()+" using "+
                                "these repositories\n"+
                                dumpRemoteRepositories()
                    }
                }
                if(downloaded) {
                    processOARArtifacts(a, filter, cp, download)
                    addToClasspath(cp, a.jar.absolutePath)
                }
            }
        } else {
            if(a.jar!=null && a.jarURL!=null) {
                processOARArtifacts(a, filter, cp, download)
                addToClasspath(cp, a.jarURL.toExternalForm())
            }
        }
    }

    private void loadSettings() {
        File settingsFile = new File(System.getProperty("user.home"),
                                     ".m2/settings.xml");
        SettingsParser settingsParser = new SettingsParser()
        m2settings = settingsParser.parse(settingsFile)
        for(RemoteRepository rr: m2settings.getRemoteRepositories()) {
            if(!repositories.contains(rr))
                repositories << rr
        }
    }

    private void addToClasspath(List<String> cp, String add) {
        if(add.endsWith(".oar"))
            return
        if(add.startsWith("file:"))
            add = add.substring(5)
        if(!cp.contains(add))
            cp.add(add)
    }

    private void processOARArtifacts(Artifact a,
                                     DependencyFilter filter,
                                     List<String> cp,
                                     boolean download) {
        if(a.jar.name.endsWith(".oar") && a.jar.exists()) {
            String[] arts = getOARArtifacts(a.jar)
            if(arts) {
                for(String oa : arts) {
                    Dependency oarDep = new Dependency(oa)
                    oarDep.pom = a.pom
                    oarDep.pomURL = a.pomURL
                    oarDep.loadFromProject = a.loadFromProject
                    oarDep.jar = ArtifactUtils.getLocalFile(oarDep,
                                                            repoDir.absolutePath,
                                                            "jar")
                    if(filter.include(oarDep))
                        resolve(oarDep, filter, cp, download)
                }
            }
        }
    }

    private String installTo(String filePath) {
        int ndx = filePath.lastIndexOf(File.separator)
        return filePath.substring(0, ndx)
    }

    private DownloadRecord install(String source, String location) {
        if(source.startsWith("file:")) {
            File sourceFile = new File(new URL(source).toURI())
            File targetDir = new File(location)
            if(!targetDir.exists())
                targetDir.mkdirs()
            File targetFile = new File(targetDir, sourceFile.name)
            FileUtils.copy sourceFile, targetFile
            return
        }
        int retryCount = 3
        DownloadRecord downloadRecord = null
        String digest
        String remoteDigest = getRemoteDigest(source)
        String ext = null
        String failed = null
        if(remoteDigest!=null) {
            int ndx = remoteDigest.indexOf(":")
            ext = remoteDigest.substring(0, ndx)
            remoteDigest = remoteDigest.substring(ndx+1)
        }
        String tmpDir = System.getProperty("java.io.tmpdir")
        StagedData sData = new StagedData(source, tmpDir, false)
        for(int i=0; i<retryCount; i++) {
            DownloadManager dMgr = new DownloadManager(sData)
            dMgr.showDownloadTo = false
            println "[INFO] Downloading "+source
            try {
                downloadRecord = dMgr.download()
                File downloadedFile = new File(downloadRecord.path, downloadRecord.name)
                if(remoteDigest!=null) {
                    digest = HttpmdUtil.computeDigest(downloadedFile.toURI().toURL(), ext)
                    if(digest.equals(remoteDigest)) {
                        failed = null
                        moveFile(downloadedFile, location)
                        File checksum = new File(location, downloadRecord.name+"."+ext)
                        checksum.write(digest)
                        break
                    } else {
                        failed = downloadedFile.name
                        downloadedFile.delete()
                        if(i<2)
                            println "[WARNING] *** CHECKSUM FAILED - "+
                                    "Checksum failed on download: "+
                                    "local = [${digest}]; remote = [${remoteDigest}] "+
                                    "- RETRYING"
                    }
                } else {
                    moveFile(downloadedFile, location)
                }
            } catch(FileNotFoundException e) {
                println "[WARNING] *** FAILED downloading $source, "+
                        "${e.getClass().name}: ${e.message}"
                break
            }
        }
        if(failed)
            println "[WARNING] *** CHECKSUM FAILED - "+
                    "Checksum failed on download: "+
                    "local = ${digest}; remote = ${failed} "+
                    "- SKIPPING"
        return downloadRecord
    }

    private boolean isUpdated(File local, URL remote) {
        boolean updated = false
        if(local!=null && local.exists()) {
            if(remote!=null) {
                String remoteDigest = getRemoteDigest(remote.toExternalForm())
                boolean useFileLength = true
                if(remoteDigest!=null) {
                    int ndx = remoteDigest.indexOf(":")
                    String ext = remoteDigest.substring(0, ndx)
                    remoteDigest = remoteDigest.substring(ndx+1)
                    File f = new File(local.parentFile, local.name+"."+ext)
                    if(f.exists()) {
                        useFileLength = false
                        String localDigest = f.text
                        updated = !remoteDigest.equals(localDigest)
                    }
                }
                if(useFileLength) {
                    URLConnection conn = remote.openConnection()
                    updated = conn.contentLength!=local.length()
                }
            }
        }
        return updated
    }

    private void moveFile(File f, String to) {
        File toDir = new File(to)
        if(!toDir.exists())
            toDir.mkdirs()

        File target = new File(toDir, f.name)
        try {
            FileUtils.copy f, target
            FileUtils.remove f
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                       "*** INSTALL FAILURE: "+f.name+" was NOT moved to "+to,
                       e)

        }
        //if(!f.renameTo(new File(toDir, f.name)))
        //    logger.severe "*** INSTALL FAILURE: "+f.name+" was NOT moved to "+to
    }

    private String getRemoteDigest(String source) {
        /* Try sha1 first */
        String ext = "sha1"
        String digest = getTextFromURL(new URL(source+"."+ext))
        /* Try md5 if sha1 file not found */
        if(digest==null) {
            ext = "md5"
            digest = getTextFromURL(new URL(source+"."+ext))
        }
        if(digest!=null)
            digest = parseDigest(digest)

        return digest==null?null:ext+":"+digest
    }

    private String parseDigest(String rawChecksumString) {
        String trimmedChecksum = rawChecksumString.replace( '\n', ' ' ).trim()
        String[] parts = trimmedChecksum.split(" ")
        return parts[0]
    }

    private String getTextFromURL(URL u) {
        String s = null
        try {
            s = u.openStream().text
        } catch(Exception e) {

        }
        return s
    }

    private String printDependency(Dependency d, int indent) {
        String scope = ""
        if(d.scope)
            scope = "<"+d.scope+">"
        return getPadding(indent)+"- "+d.groupId+":"+d.artifactId+":"+d.version+" "+scope+"\n"
    }

    private String getPadding(int count) {
        StringBuffer sb = new StringBuffer()
        for(int i=0; i<count; i++)
            sb.append("\t")
        return sb.toString()
    }

    protected DependencyFilter getDependencyFilter(Artifact a) {
        DependencyFilter filter
        if(a.classifier && a.classifier=="dl")
            filter = new ClassifierFilter(a.classifier)
        else
            filter = new ExclusionFilter()
        return filter
    }

    private String[] getOARArtifacts(File oarFile) {
        OAR oar = new OAR(oarFile)
        return toArray(oar.getArtifacts(), " ,")
    }

    private String[] toArray(String arg) {
        return toArray(arg, " ,"+File.pathSeparator)
    }

    private String[] toArray(String arg, String delim) {
        if(arg==null)
            return new String[0]
        StringTokenizer tok = new StringTokenizer(arg, delim);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }

    private String dumpRemoteRepositories() {
        StringBuilder sb = new StringBuilder()
        for(RemoteRepository rr : repositories) {
            if(sb.length()>0)
                sb.append("\n")
            sb.append("\t"+rr.url)
        }
        return sb.toString()
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SimpleResolver ");
        sb.append("{repositories=").append(repositories);
        sb.append('}');
        return sb.toString();
    }
}
