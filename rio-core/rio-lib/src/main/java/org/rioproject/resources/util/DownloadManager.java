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
package org.rioproject.resources.util;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.rioproject.deploy.DownloadRecord;
import org.rioproject.deploy.StagedData;
import org.rioproject.deploy.StagedSoftware;
import org.rioproject.deploy.StagedSoftware.PostInstallAttributes;
import org.rioproject.exec.ExecDescriptor;
import org.rioproject.exec.ProcessManager;
import org.rioproject.exec.ServiceExecutor;
import org.rioproject.exec.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * The DownloadManager class provides support to manage the download and
 * installation of artifacts
 *
 * @author Dennis Reedy
 */
public class DownloadManager {
    /** The root directory to download the software */
    private String installPath;
    /** The Download */
    private StagedData stagedData;
    /** The DownloadRecord of the downloaded software */
    private DownloadRecord downloadRecord;
    /** The post-install DownloadRecord */
    private DownloadRecord postInstallRecord;
    /** The files extracted during post-install */
    private List<File> postInstallExtractList;
    private boolean showDownloadTo = true;
    /** A suitable Logger */
    private static final Logger logger = LoggerFactory.getLogger(DownloadManager.class.getName());

    /**
     * Create an instance of the DownloadManager
     *
     * @param stagedData The Download
     */
    public DownloadManager(StagedData stagedData) {
        if(stagedData == null)
            throw new IllegalArgumentException("stagedData is null");
        this.stagedData = stagedData;
    }

    /**
     * Create an instance of the DownloadManager
     *
     * @param installPath The root directory to download the software. If the
     * directory does not exist, it will be created. Read and write access
     * permissions are required to the directory
     * @param stagedData The Download
     */
    public DownloadManager(String installPath, StagedData stagedData) {
        if(installPath == null)
            throw new IllegalArgumentException("installPath is null");
        if(stagedData == null)
            throw new IllegalArgumentException("stagedData is null");
        this.installPath = installPath;
        this.stagedData = stagedData;
    }

    /**
     * Performs software download for a Download
     * 
     * @return The DownloadRecord based on
     * attributes from the downloaded software
     * 
     * @throws IOException if there are errors accessing the file system
     */
    public DownloadRecord download() throws IOException {
        if(downloadRecord != null)
            return (downloadRecord);
        return doDownload(stagedData, false);
    }

    /**
     * Whether to emit logger statements documenting where the data is being
     * downloaded to
     *
     * @param show If true emit (default)
     */
    public void setShowDownloadTo(boolean show) {
        this.showDownloadTo= show;
    }

    /*
     * Performs software stagedData for StagedData
     * 
     * @param dAttrs The StagedData
     * @param postInstall Whether this is for the post-install task
     * @return The DownloadRecord based on
     * attributes from the downloaded software.
     *
     * @throws IOException if there are errors accessing the file system
     */
    private DownloadRecord doDownload(StagedData dAttrs, boolean postInstall) throws IOException {
        String installRoot  ;
        int extractedSize = 0;
        long extractTime = 0;
        boolean unarchived = false;
        boolean createdTargetPath = false;

        if(dAttrs == null)
            throw new IllegalArgumentException("dAttrs is null");

        URL location = dAttrs.getLocationURL();
        String extension = dAttrs.getInstallRoot();
        boolean unarchive = dAttrs.unarchive();
        if(extension.indexOf("/") != -1)
            installRoot = extension.replace('/', File.separatorChar);
        else
            installRoot = extension.replace('\\', File.separatorChar);
        File targetPath = new File(FileUtils.makeFileName(installPath, installRoot));
        if(!targetPath.exists()) {
            if(targetPath.mkdirs()) {
                logger.trace("Created {}", targetPath.getPath());
            }
            if(!targetPath.exists())
                throw new IOException("Failed to create: " + installPath);
            createdTargetPath = true;
        }
        if(!targetPath.canWrite())
            throw new IOException("Can not write to : " + installPath);
        String source = location.toExternalForm();
        int index = source.lastIndexOf("/");
        if(index == -1)
            throw new IllegalArgumentException("Don't know how to install : "+ source);
        String software = source.substring(index + 1);
        String target = FileUtils.getFilePath(targetPath);
        File targetFile = new File(FileUtils.makeFileName(target, software));
        if (targetFile.exists()) {
            if(!dAttrs.overwrite()) {
                logger.warn("{} exists, stagedData attributes indicate to not overwrite file",
                            FileUtils.getFilePath(targetFile));
                return null;
            } else {
                if(showDownloadTo)
                    logger.info("Overwriting {} with {}", FileUtils.getFilePath(targetFile), location);
            }
        } else {
            if(showDownloadTo)
                logger.info("Downloading {} to {}", location, FileUtils.getFilePath(targetFile));
        }
        long t0 = System.currentTimeMillis();
        URLConnection con = location.openConnection();
        int downloadedSize = writeFileFromInputStream(con.getInputStream(),
                                                      targetFile,
                                                      con.getContentLength(),
                                                      System.console()!=null);
        long t1 = System.currentTimeMillis();
        long downloadTime = t1 - t0;
        long downloadSecs = downloadTime/1000;
        Date downloadDate = new Date();
        ExtractResults results;
        logger.info("Wrote {}K in {} seconds", (downloadedSize/1024), (downloadSecs<1?"< 1":downloadSecs));
        String extractedToPath = null;
        if(unarchive) {
            t0 = System.currentTimeMillis();
            results = extract(targetPath, targetFile);
            t1 = System.currentTimeMillis();
            extractedSize = results.extractedSize;
            if(postInstall)
                postInstallExtractList = results.postInstallExtractList;
            extractTime = t1 - t0;
            unarchived = true;
            extractedToPath = results.extractedToPath;
            if(extractedToPath==null) {
                extractedToPath = FileUtils.getFilePath(targetPath);
            }
        }
        downloadRecord = new DownloadRecord(location,
                                            target,
                                            software,
                                            downloadDate,
                                            downloadedSize,
                                            extractedSize,
                                            extractedToPath,
                                            unarchived,
                                            downloadTime,
                                            extractTime);
        downloadRecord.setCreatedParentDirectory(createdTargetPath);
        return (downloadRecord);
    }

    /**
     * Given an InputStream this method will write the contents to the desired
     * File.
     * 
     * @param in InputStream
     * @param file The File object to write to
     * @param total The total length
     * @param show Whether to print out progress
     *
     * @return The size of what was written
     *
     * @throws IOException if there are errors accessing the file system
     */
    private static int writeFileFromInputStream(InputStream in, File file, long total, boolean show) throws IOException {
        int totalWrote = 0;
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            int read;
            byte[] buf = new byte[2048];
            while((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                totalWrote += read;
                if (show)
                    showTransferStatus(total, totalWrote);
            }
            if(show) {
                String l = total >= 1024 ? ( total / 1024 ) + "K" : total + "b";
                logger.info( l + " downloaded ("+file.getName()+")");
            }
        } catch(FileNotFoundException e) {
            // catch so we can delete the file
            if(file.delete()) {
                logger.trace("Deleted {}", file.getName());
            }
            throw e;
        } catch(IOException e) {
            // catch so we can delete the file
            if(file.delete()) {
                logger.trace("Deleted {}", file.getName());
            }
            throw e;
        } finally {
            try {
                if(in != null)
                    in.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
            try {
                if(out != null)
                    out.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return (totalWrote);
    }

    private static void showTransferStatus(long total, long complete) {
        if (total >= 1024) {
            System.out.print((complete/1024)+"/"+(total==-1?"?":(total/1024)+"K" )+"\r");
        } else {
            System.out.print(complete+"/"+(total==-1 ?"?":total+"b")+"\r" );
        }
    }

    /**
     * Extract an archive
     *
     * @param directory the directory to extract to
     * @param archive The archive to extract
     *
     * @return An ExtractResults class detailing what was extracted
     *
     * @throws IOException if there are errors extracting the archive
     */
    @SuppressWarnings("PMD.AvoidReassigningParameters")
    public static ExtractResults extract(File directory, File archive) throws IOException {

        String extractedToPath = null;
        int extractSize = 0;
        ZipFile zipFile = null;
        List<File> extractList = new ArrayList<File>();
        if(archive.getName().endsWith(".gz") ||
           archive.getName().endsWith(".gzip")) {
            archive = dealWithGZIP(archive);
        }
        if(archive.getName().endsWith("tar")) {
            unTar(archive, directory);
            return (new ExtractResults(extractedToPath,
                                       extractSize, 
                                       extractList));
        }
        try {
            zipFile = new ZipFile(archive);
            Enumeration zipEntries = zipFile.entries();
            while(zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();
                if(zipEntry.isDirectory()) {
                    File file = makeChildFile(directory, zipEntry.getName());
                    if(file.mkdirs()) {
                        logger.trace("Created {}", file.getPath());
                    }
                    if(extractedToPath==null) {
                        extractedToPath = getExtractedToPath(file, directory);
                    }
                } else {
                    File file = makeChildFile(directory, zipEntry.getName());
                    //System.out.println("Writing : "+file.getCanonicalPath());
                    extractList.add(file);
                    String fullPath = FileUtils.getFilePath(file);
                    int index = fullPath.lastIndexOf(File.separatorChar);
                    String installPath = fullPath.substring(0, index);
                    File targetPath = new File(installPath);
                    if(!targetPath.exists()) {
                        if(targetPath.mkdirs()) {
                            logger.trace("Created {}", file.getPath());
                        }
                        if(!targetPath.exists())
                            throw new IOException("Failed to create : "+ installPath);
                    }
                    if(!targetPath.canWrite())
                        throw new IOException("Can not write to : "+ installPath);
                    InputStream in = zipFile.getInputStream(zipEntry);
                    extractSize += writeFileFromInputStream(in, file, archive.length(), false);
                }
            }
        } finally {
            if(zipFile != null)
                zipFile.close();
        }
        return (new ExtractResults(extractedToPath, extractSize, extractList));
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private static String getExtractedToPath(File path, File rootDir) {
        File parent;
        do {
            parent = path.getParentFile();
            if(parent==null) {
                logger.warn("No parent for {}", FileUtils.getFilePath(path));
                break;
            }
            if(!parent.equals(rootDir))
                path = parent;
        } while(!parent.equals(rootDir));

        return FileUtils.getFilePath(path);
    }

    private static File makeChildFile(File parent, String name) {
        return new File(parent, name);
    }

    private static File dealWithGZIP(File gzip) throws IOException {
        int ndx = gzip.getName().lastIndexOf(".");
        File output = new File(gzip.getParentFile().getPath(),
                               gzip.getName().substring(0, ndx));
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(gzip));
        logger.info("Writing {} ...", FileUtils.getFilePath(output));
        long t0 = System.currentTimeMillis();

        int downloadedSize = writeFileFromInputStream(gzipInputStream, output, gzip.length(), System.console()!=null);
        long t1 = System.currentTimeMillis();
        long downloadTime = t1 - t0;
        long downloadSecs = downloadTime/1000;
        logger.info("Wrote {}K in {} millis", (downloadedSize/1024), (downloadSecs<1?"< 1":downloadSecs));

        return output;
    }

    private static void unTar(File tarFile, File target) throws IOException {
        InputStream is = new FileInputStream(tarFile);
        ArchiveInputStream in;
        try {
            in = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        } catch (ArchiveException e) {
            IOException ioe = new IOException("Unarchving "+tarFile.getName());
            ioe.initCause(e);
            throw ioe;
        }
        try {
            TarArchiveEntry entry;
            while((entry = (TarArchiveEntry)in.getNextEntry())!=null) {
                File f = new File(target, entry.getName());
                if(entry.isDirectory()) {
                    if(f.mkdirs()) {
                        logger.trace("Created directory {}", f.getPath());
                    }
                } else {
                    if(!f.getParentFile().exists()) {
                        if(f.getParentFile().mkdirs()) {
                            logger.trace("Created {}", f.getParentFile().getPath());
                        }
                    }
                    if(f.createNewFile()) {
                        logger.trace("Created {}", f.getName());
                    }
                    OutputStream out = new FileOutputStream(f);
                    IOUtils.copy(in, out);
                    out.close();
                }
                setPerms(f, entry.getMode());
            }
        } finally {
            in.close();
        }
    }

    private static void setPerms(File f, int mode) {
        String jvmVersion = System.getProperty("java.version");
        if(jvmVersion.contains("1.5"))
            return;
        
        int ownerPerm = mode >> 6 & 007;
        int groupPerm = mode >> 3 & 007;
        int userPerm = mode & 007;

        /* Check read */
        boolean ownerOnly = true;
        if(userPerm>=4) {
            userPerm-=4;
            ownerOnly = false;
        }
        if(groupPerm>=4) {
            groupPerm-=4;
            ownerOnly = false;
        }
        if(ownerPerm>=4) {
            ownerPerm-=4;
            setFileAccess(f, "setReadable", true, ownerOnly);
        }
        /* Check write */
        ownerOnly = true;
        if(userPerm>=2) {
            userPerm-=2;
            ownerOnly = false;
        }
        if(groupPerm>=2) {
            groupPerm-=2;
            ownerOnly = false;
        }
        if(ownerPerm>=2) {
            ownerPerm-=2;
            setFileAccess(f, "setWritable", true, ownerOnly);
        }
        /* Check execute */
        ownerOnly = true;
        if(userPerm==1 || groupPerm==1)
            ownerOnly = false;
        if(ownerPerm==1) {
            setFileAccess(f, "setExecutable", true, ownerOnly);
        }        
    }

    /*
     * Use reflection so this can be compiled using 1.5
     */
    private static void setFileAccess(File f,
                                      String setter,
                                      boolean allow,
                                      boolean ownerOnly) {
        try {
            Method m = f.getClass().getMethod(setter, boolean.class, boolean.class);
            m.invoke(f, allow, ownerOnly);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void removeExtractedTarFiles(File root, File tarFile) throws IOException {
        InputStream is = new FileInputStream(tarFile);
        ArchiveInputStream in;
        try {
            in = new ArchiveStreamFactory().createArchiveInputStream("tar", is);
        } catch (ArchiveException e) {
            IOException ioe = new IOException("Removing "+tarFile.getName());
            ioe.initCause(e);
            throw ioe;
        }
        File parent = null;
        try {
            TarArchiveEntry entry;
            while((entry = (TarArchiveEntry)in.getNextEntry())!=null) {
                File f = new File(root, entry.getName());
                if(parent==null) {
                    parent = new File(getExtractedToPath(f, root));
                }
                FileUtils.remove(f);

            }
        } finally {
            in.close();
        }
        FileUtils.remove(parent);
    }

    /*
     * Container class holding results of the extract
     */
    public static class ExtractResults {
        String extractedToPath;
        int extractedSize;
        List<File> postInstallExtractList;

        ExtractResults(String extractedToPath,
                       int extractedSize,
                       List<File>postInstallExtractList) {
            this.extractedToPath = extractedToPath;
            this.extractedSize = extractedSize;
            this.postInstallExtractList = postInstallExtractList;
        }
    }

    /**
     * Perform post-install task(s) as described by the StagedSoftware object
     * 
     * @return A DownloadRecord for the post
     * install if software was downloaded to perform the post install task(s).
     * If no software was downloaded to perform the task(s), return null
     *
     * @throws IOException if there are errors accessing the file system
     */
    public DownloadRecord postInstall() throws IOException {
        if(downloadRecord == null)
            throw new IllegalStateException("software has not been downloaded");
        if(!(stagedData instanceof StagedSoftware))
            return null;
        PostInstallAttributes postInstall =
            ((StagedSoftware) stagedData).getPostInstallAttributes();
        if(postInstall == null)
            return (null);
        String path = downloadRecord.getPath();
        try {
            StagedData dAttrs = postInstall.getStagedData();
            if(dAttrs != null) {
                postInstallRecord = doDownload(dAttrs, true);
                path = postInstallRecord.getPath();
            }
            ExecDescriptor execDesc = postInstall.getExecDescriptor();
            if(execDesc != null) {
                if(!execDesc.getCommandLine().startsWith(File.separator))
                    execDesc = Util.extendCommandLine(path, execDesc);
                ServiceExecutor svcExecutor = new ServiceExecutor();
                ProcessManager manager = svcExecutor.exec(execDesc);
                manager.manage();
                //manager.waitFor();
                manager.destroy(false);
            }
            if(postInstall.getStagedData()!=null &&
               postInstall.getStagedData().removeOnDestroy()) {
                if(postInstallRecord != null) {
                    FileUtils.remove(new File(FileUtils.makeFileName(postInstallRecord.getPath(),
                                                 postInstallRecord.getName())));
                }
                if(postInstallExtractList != null) {
                    for (File file : postInstallExtractList)
                        FileUtils.remove(file);
                }
            }
        } catch(IOException e) {
            if(postInstallRecord != null)
                remove(postInstallRecord);
            throw e;
        }
        return (postInstallRecord);
    }

    /**
     * Remove installed software
     */
    public void remove() {
        if(downloadRecord == null)
            throw new IllegalStateException("software has not been downloaded");
        remove(downloadRecord);
        if(postInstallRecord != null)
            remove(postInstallRecord);
    }

    /**
     * Remove installed software
     * 
     * @param record The DownloadRecord to remove
     *
     * @return the top-most directory/file that was removed
     */
    public static String remove(DownloadRecord record) {
        if(record == null)
            throw new IllegalArgumentException("record is null");
        File software = new File(FileUtils.makeFileName(record.getPath(),
                                                        record.getName()));
        if(!software.exists()) {
            logger.debug("Software recorded at [{}] does not exist or has already been removed "+
                            "from the file system, removal aborted", FileUtils.getFilePath(software));
            return null;
        }
        String removed;
        if(record.unarchived()) {
            if(software.getName().endsWith(".gz") || software.getName().endsWith(".gzip")) {
                FileUtils.remove(software);
                /* Strip off the extension and see if we still have
                 * something to remove */
                int ndx = software.getName().lastIndexOf(".");
                if(ndx!=-1) {
                    String newName = software.getName().substring(0, ndx);
                    software = new File(FileUtils.makeFileName(record.getPath(), newName));
                }
            }
            if(software.getName().endsWith("tar")) {
                try {
                    File root = new File(record.getPath());
                    removeExtractedTarFiles(root, software);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ZipFile zipFile = null;
                try {
                    zipFile = new ZipFile(software);
                    Enumeration zipEntries = zipFile.entries();
                    while(zipEntries.hasMoreElements()) {
                        ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();
                        File file = new File(record.getPath() + File.separator
                                             + zipEntry.getName());
                        FileUtils.remove(file);
                    }
                } catch (ZipException e) {
                    logger.error("Error in opening zip file {}", FileUtils.getFilePath(software), e);
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    if(zipFile!=null) {
                        try {
                            zipFile.close();
                        } catch (IOException e) {
                            logger.error("Could not close zip file", e);
                        }
                    }
                }
            }
            removed = FileUtils.getFilePath(software);
            FileUtils.remove(software);
            File softwareDirectory = new File(record.getPath());
            String[] list = softwareDirectory.list();
            if(list.length == 0
                    && !softwareDirectory.getName().equals("native"))
                FileUtils.remove(softwareDirectory);
        } else {
            if(record.createdParentDirectory()) {
                removed = record.getPath();
                FileUtils.remove(new File(record.getPath()));
            } else {
                removed = FileUtils.getFilePath(software);
                FileUtils.remove(software);
            }
        }

        return removed;
    }

    public static void main(String args[]) {
        try {
            if(args.length < 2) {
                System.out.println(
                    "Usage: org.rioproject.resources.util.DownloadManager " +
                    "download-URL install-root");
                System.exit(-1);
            }
            String downloadFrom = args[0];
            String installPath = args[1];
            System.setSecurityManager(new java.rmi.RMISecurityManager());
            StagedSoftware download = new StagedSoftware();
            download.setLocation(downloadFrom);
            download.setInstallRoot(installPath);
            download.setUnarchive(true);
            DownloadManager slm = new DownloadManager(installPath, download);
            DownloadRecord record = slm.download();
            System.out.println("Details");
            System.out.println("-------");
            System.out.println(record.toString());
            //DownloadManager.remove(record);
        } catch(Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
