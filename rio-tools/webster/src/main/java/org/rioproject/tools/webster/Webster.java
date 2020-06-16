/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package org.rioproject.tools.webster;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.rioproject.config.Constants;
import org.rioproject.net.HostUtil;
import org.rioproject.net.PortRangeServerSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ServerSocketFactory;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Webster is a HTTP server which can serve code from multiple codebases.
 * Environment variables used to control Webster are as follows: 
 * 
 * <table BORDER COLS=3 WIDTH="100%" >
 ** <tr>
 * <td>org.rioproject.tools.webster.port</td>
 * <td>Sets the port for webster to use</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td>org.rioproject.tools.webster.port</td>
 * <td>Sets the port for webster to use</td>
 * <td>0</td>
 * </tr>
 * <td>org.rioproject.tools.webster.root</td>
 * <td>Root directory to serve code from. Webster supports multiple root
 * directories which are separated by a <code>;</code></td>
 * <td>System.getProperty(user.home)</td>
 * </tr>
 * 
 * </table>
 *
 * @author Dennis Reedy
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class Webster implements Runnable {
    static final int DEFAULT_MAX_THREADS = 10;
    private ServerSocket ss;
    private int port;
    private boolean run = true;
    private static Properties MimeTypes = new Properties();
    private String[] websterRoot;
    private ThreadPoolExecutor pool;
    private int maxThreads = DEFAULT_MAX_THREADS;
    private int soTimeout = 0;
    private static Logger logger = LoggerFactory.getLogger("org.rioproject.tools.webster");
    private com.sun.jini.start.LifeCycle lifeCycle;
    private boolean debug = false;
    private ServerSocketFactory socketFactory;
    private String putDirectory;
    private static String SERVER_DESCRIPTION=Webster.class.getName();

    /**
     * Create a new Webster using a Groovy config file
     *
     * @param websterConfig The Groovy configuration file.
     *
     * <p>The Webster will be serving code as determined by the either the
     * org.rioproject.tools.webster.root system property (if set) or defaulting to
     * the user.dir system property.
     *
     * @throws BindException if Webster cannot create a socket
     */
    @SuppressWarnings("unchecked")
    public Webster(final File websterConfig) throws BindException, MalformedURLException {
        ConfigObject config = new ConfigSlurper().parse(websterConfig.toURI().toURL());
        Map flattened = config.flatten();
        port = (Integer)flattened.get("webster.port");
        StringBuilder websterRoots = new StringBuilder();
        for(String root : (List<String>)flattened.get("webster.roots")) {
            if(websterRoots.length()>0)
                websterRoots.append(";");
            websterRoots.append(root);
        }
        String bindAddress = (String) flattened.get("webster.address");
        if(flattened.get("webster.putDirectory")!=null) {
            putDirectory = (String) flattened.get("webster.putDirectory");
        }
        initialize(websterRoots.toString(), bindAddress);
    }

    /**
     * Create a new Webster
     * 
     * @param port The port to use
     * @param roots The root(s) to serve code from. This is a semi-colin
     * delimited list of directories
     *
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port, String roots) throws BindException {
        this(port, roots, null);
    }

    /**
     * Create a new Webster
     * 
     * @param port The port to use
     * @param roots The root(s) to serve code from. This is a semi-colin
     * delimited list of directories
     * @param bindAddress TCP/IP address which Webster should bind to (null
     * implies no specific address)
     *
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(int port, String roots, String bindAddress) throws BindException {
        this.port = port;
        initialize(roots, bindAddress);
    }

    /**
     * Create a new Webster
     *
     * @param socketFactory The ServerSocketFactory to use when creating the socket the Webster will be bound to
     * @param roots The root(s) to serve code from. This is a semi-colin
     * delimited list of directories
     * @param bindAddress TCP/IP address which Webster should bind to (null
     * implies no specific address)
     *
     * @throws BindException if Webster cannot create a socket
     */
    public Webster(ServerSocketFactory socketFactory, String roots, String bindAddress) throws BindException {
        this.socketFactory = socketFactory;
        initialize(roots, bindAddress);
    }

    /**
     * Create a new Webster, compatible with the ServiceStarter mechanism in
     * Apache River
     * 
     * @param options String[] of options. Valid options are [-port port],
     * [-roots list-of-roots], [-bindAddress address],
     * [-maxThreads maxThreads] [-soTimeout soTimeout] [-portRange range].
     * Note -port and -portRange are mutually exclusive
     * @param lifeCycle The LifeCycle object, may be null
     *
     * @throws BindException if Webster cannot create a socket
     * @throws IllegalArgumentException if both -port and -portRange are provided
     * @throws NumberFormatException if the ports cannot be parsed into an integer
     */
    public Webster(String[] options, com.sun.jini.start.LifeCycle lifeCycle) throws BindException {
        if(options == null)
            throw new IllegalArgumentException("options are null");
        this.lifeCycle = lifeCycle;
        String roots = null;
        String bindAddress = null;
        boolean parsedPort = false;
        for(int i = 0; i < options.length; i++) {
            String option = options[i];
            if("-port".equals(option)) {
                i++;
                this.port = Integer.parseInt(options[i]);
                parsedPort = true;
            } else if("-portRange".equals(option)) {
                if(parsedPort)
                    throw new IllegalArgumentException("both -port and -portRange " +
                                                       "cannot be provided, choose one or the other");
                i++;
                socketFactory = parsePortRange(options[i]);
            } else if("-roots".equals(option)) {
                i++;
                roots = options[i];
            } else if("-bindAddress".equals(option)) {
                i++;
                bindAddress = options[i];
            } else if("-maxThreads".equals(option)) {
                i++;
                maxThreads = Integer.parseInt(options[i]);
            } else if("-soTimeout".equals(option)) {
                i++;
                soTimeout = Integer.parseInt(options[i]);
            } else if("-putDirectory".equals(option)) {
                i++;
                putDirectory = options[i];
            } else {
                throw new IllegalArgumentException(option);
            }
        }
        initialize(roots, bindAddress);
    }

    /*
     * Parse portRange string and return a ServerSocketFactory to deal with the port range
     */
    private ServerSocketFactory parsePortRange(String portRange) {
        String[] range = portRange.split("-");
        int startRange = Integer.parseInt(range[0]);
        int endRange = Integer.parseInt(range[1]);
        /* reset the port to '0', this way the range will be used as intended */
        port = 0;
        return new PortRangeServerSocketFactory(startRange, endRange);
    }

    /*
     * Initialize Webster
     * 
     * @param roots The root(s) to serve code from. This is a semi-colin
     * delimited list of directories
     */
    private void initialize(String roots, String bindAddress) throws BindException {
        String d = System.getProperty("webster.debug");
        if(d != null)
            debug = true;
        String str = System.getProperty("webster.put.dir");
        if (str != null) {
            putDirectory = str;
            if (debug)
                System.out.println("putDirectory: " + putDirectory);
        }
        setupRoots(roots);
        try {
            InetAddress address;
            if(bindAddress==null) {
                address = HostUtil.getInetAddressFromProperty(Constants.RMI_HOST_ADDRESS);
            } else {
                address = InetAddress.getByName(bindAddress);
            }
            if(socketFactory==null) {
                ss = new ServerSocket(port, 0, address);
            } else {
                ss = socketFactory.createServerSocket(port, 0, address);
            }
            if(debug)
                System.out.println("Webster serving on : "+ss.getInetAddress().getHostAddress()+":"+port);
            if(logger.isDebugEnabled())
                logger.debug("Webster serving on : {}:{}", ss.getInetAddress().getHostAddress(), port);

            port = ss.getLocalPort();
        } catch(IOException ioe) {
            throw new BindException("Could not start Webster.");
        }
        if(debug)
            System.out.println("Webster listening on port : " + port);
        if(logger.isDebugEnabled())
            logger.debug("Webster listening on port : " + port);
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
        if(debug)
            System.out.println("Webster maxThreads ["+maxThreads+"]");
        if(logger.isDebugEnabled())
            logger.debug("Webster maxThreads [{}]", maxThreads);
        if(soTimeout>0) {
            if(debug)
                System.out.println("Webster Socket SO_TIMEOUT set to ["+soTimeout+"] millis");
            if(logger.isDebugEnabled())
                logger.debug("Webster Socket SO_TIMEOUT set to [{}]] millis", soTimeout);
        }
        if(putDirectory!=null && debug) {
            System.out.println("putDirectory: " + putDirectory);
        }

        /* Set system properties */
        if (logger.isDebugEnabled()) {
            logger.debug("Setting WEBSTER property to: " + "http://"+ss.getInetAddress().getHostAddress()+":"+port);
        }
        System.setProperty(Constants.WEBSTER, "http://"+ss.getInetAddress().getHostAddress()+":"+port);
        System.setProperty(Constants.WEBSTER_ROOTS, roots);

        Thread runner = new Thread(this, "Webster");
        runner.setDaemon(true);
        runner.start();
    }

    /**
     * Get the roots Webster is serving
     *
     * @return The roots Webster is serving as a semicolon delimited String
     */
    public String getRoots() {
        StringBuilder buffer = new StringBuilder();
        for(int i = 0; i < websterRoot.length; i++) {
            if(i > 0)
                buffer.append(";");
            buffer.append(websterRoot[i]);
        }
        return (buffer.toString());
    }

    /**
     * Get address that Webster is bound to
     * 
     * @return The host address the server socket Webster is using is bound to.
     * If the socket is null, return null.
     */
    public String getAddress() {
        if(ss==null)
            return(null);
        return(ss.getInetAddress().getHostAddress());
    }

    /*
     * Setup the websterRoot property
     */
    private void setupRoots(String roots) {
        if(roots == null)
            throw new IllegalArgumentException("roots is null");
        StringTokenizer tok = new StringTokenizer(roots, ";");
        websterRoot = new String[tok.countTokens()];
        if(websterRoot.length > 1) {
            for(int j = 0; j < websterRoot.length; j++) {
                websterRoot[j] = tok.nextToken();
                if(debug)
                    System.out.println("Root " + j + " = " + websterRoot[j]);
                if(logger.isDebugEnabled())
                    logger.debug("Root " + j + " = " + websterRoot[j]);
            }
        } else {
            websterRoot[0] = roots;
            if(debug)
                System.out.println("Root  = " + websterRoot[0]);
            if(logger.isDebugEnabled())
                logger.debug("Root  = " + websterRoot[0]);
        }
    }

    /**
     * Terminate a running Webster instance
     */
    public void terminate() {
        run = false;
        if(ss!=null) {
            try {
                ss.close();
            } catch(IOException e) {
                logger.warn("Exception closing Webster ServerSocket");
            }
        }
        if(lifeCycle != null)
            lifeCycle.unregister(this);
        
        if(pool!=null)
            pool.shutdownNow();
    }

    /**
     * Get the port Webster is bound to
     *
     * @return Te port Webster is bound to
     */
    public int getPort() {
        return (port);
    }

    /*
     * Read up to CRLF, return false if EOF
     */
    private String readRequest(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        int read;
        int prev = -1;
        while ((read = inputStream.read()) != -1) {
            if (read != '\n' && read != '\r')
                sb.append((char) read);
            if (read == '\n' && prev == '\r') {
                break;
            }
            if (read == '\r' && prev == '0') {
                //sb.delete(0, sb.length());
                break;
            }
            prev = read;
        }
        return sb.toString();
    }

    public void run() {
        Socket s  ;
        try {
            loadMimes();
            String fileName;
            while (run) {
                s = ss.accept(); // accept incoming requests
                if(soTimeout>0) {
                    s.setSoTimeout(soTimeout);
                }
                String line;
                Properties header = new Properties();
                DataInputStream inputStream;
                try {
                    inputStream = new DataInputStream(s.getInputStream());
                    StringBuilder lineBuilder = new StringBuilder();
                    StringTokenizer tokenizer;
                    while ((line = readRequest(inputStream)).length() != 0) {
                        if (lineBuilder.length() > 0)
                            lineBuilder.append("\n");
                        lineBuilder.append(line);
                        tokenizer = new StringTokenizer(line, ":");
                        String aToken = tokenizer.nextToken().trim();
                        if (tokenizer.hasMoreTokens()) {
                            header.setProperty(aToken, tokenizer.nextToken().trim());
                        }
                    }
                    line = lineBuilder.toString();
                    int port = s.getPort();
                    String from = s.getInetAddress().getHostAddress() + ":" + port;
                    if (debug) {
                        StringBuilder buff = new StringBuilder();
                        buff.append("From: ").append(from).append(", ");
                        if (soTimeout > 0)
                            buff.append("SO_TIMEOUT: ").append(soTimeout).append(", ");
                        buff.append("Request: ").append(line);
                        System.out.println("\n"+buff.toString());
                    }
                    if(logger.isDebugEnabled()) {
                        StringBuilder buff = new StringBuilder();
                        buff.append("From: ").append(from).append(", ");
                        if(soTimeout > 0)
                            buff.append("SO_TIMEOUT: ").append(soTimeout).append(", ");
                        buff.append("Request: ").append(line);
                        logger.debug(buff.toString());
                    }
                    if (line.length()>0) {
                        tokenizer = new StringTokenizer(line, " ");
                        if (!tokenizer.hasMoreTokens())
                            break;
                        String token = tokenizer.nextToken();
                        fileName = tokenizer.nextToken();
                        if (fileName.startsWith("/"))
                            fileName = fileName.substring(1);
                        if ("GET".equals(token)) {
                            header.setProperty("GET", fileName);
                        } else if ("PUT".equals(token)) {
                            header.setProperty("PUT", fileName);
                        } else if ("DELETE".equals(token)) {
                            header.setProperty("DELETE", fileName);
                        } else if ("HEAD".equals(token)) {
                            header.setProperty("HEAD", fileName);
                        }
                        while (tokenizer.hasMoreTokens()) {
                            String aToken = tokenizer.nextToken().trim();
                            if (tokenizer.hasMoreTokens()) {
                                header.setProperty(aToken, tokenizer.nextToken().trim());
                            }
                        }
                        if (header.getProperty("GET") != null) {
                            pool.execute(new GetFile(s, fileName));
                        } else if (header.getProperty("PUT") != null) {
                            if(putDirectory!=null) {
                                pool.execute(new PutFile(s, fileName, header, inputStream));
                            } else {
                                DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                                clientStream.writeBytes("HTTP/1.1 405 Method Not Allowed\nWebster is in read-only mode\r\n\r\n");
                                clientStream.flush();
                                clientStream.close();
                            }
                        } else if (header.getProperty("DELETE") != null) {
                            pool.execute(new DelFile(s, fileName));
                        } else if (header.getProperty("HEAD") != null) {
                            pool.execute(new Head(s, fileName));
                        } else {
                            if (debug)
                                System.out.println("bad request [" + line + "] from " + from);
                            if (logger.isDebugEnabled())
                                logger.debug("bad request [{}] from {}", line, from);
                            DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                            clientStream.writeBytes("HTTP/1.1 400 Bad Request\r\n\r\n");
                            clientStream.flush();
                            clientStream.close();
                        }
                    } /* if line != null */
                } catch (Exception e) {
                    DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                    clientStream.writeBytes("HTTP/1.1 500 Internal Server Error\n"+
                                            "MIME-Version: 1.0\n"+
                                            "Server: "+SERVER_DESCRIPTION+"\n"+
                                            "\n\n<H1>500 Internal Server Error</H1>\n"
                                            +e);
                    clientStream.flush();
                    clientStream.close();
                    logger.warn("Getting Request", e);
                }
            }
        } catch(Exception e) {
            if(run) {
                logger.warn("Processing HTTP Request", e);
            }
        }
    }

    // load the properties file
    void loadMimes() throws IOException {
        if(debug)
            System.out.println("Loading mimetypes ... ");
        if(logger.isDebugEnabled())
            logger.debug("Loading mimetypes ... ");
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        URL fileURL = ccl.getResource("org/rioproject/tools/webster/mimetypes.properties");
        if(fileURL != null) {
            try {
                InputStream is = fileURL.openStream();
                MimeTypes.load(is);
                close(is);
                if(debug)
                    System.out.println("Mimetypes loaded");
                if(logger.isDebugEnabled())
                    logger.debug("Mimetypes loaded");
            } catch(IOException ioe) {
                logger.error("Loading Mimetypes", ioe);
            }
        } else {
            if(debug)
                System.out.println("mimetypes.properties not found, loading defaults");
            if(logger.isDebugEnabled())
                logger.debug("mimetypes.properties not found, loading defaults");
            MimeTypes.put("jpg", "image/jpg");
            MimeTypes.put("jpeg", "image/jpg");
            MimeTypes.put("jpe", "image/jpg");
            MimeTypes.put("gif", "image/gif");
            MimeTypes.put("htm", "text/html");
            MimeTypes.put("html", "text/html");
            MimeTypes.put("txt", "text/plain");
            MimeTypes.put("qt", "video/quicktime");
            MimeTypes.put("mov", "video/quicktime");
            MimeTypes.put("class", "application/octet-stream");
            MimeTypes.put("mpg", "video/mpeg");
            MimeTypes.put("mpeg", "video/mpeg");
            MimeTypes.put("mpe", "video/mpeg");
            MimeTypes.put("au", "audio/basic");
            MimeTypes.put("snd", "audio/basic");
            MimeTypes.put("wav", "audio/x-wave");
            MimeTypes.put("JNLP", "application/x-java-jnlp-file");
            MimeTypes.put("jnlp", "application/x-java-jnlp-file");
            MimeTypes.put("java", "application/java");
            MimeTypes.put("jar", "application/java");
            MimeTypes.put("JAR", "application/java");
        }
    } // end of loadMimes

    protected File parseFileName(final String filename) {
        String fileNameWithSpacesHandled = filename.replace("%20", " ");
        StringBuilder fn = new StringBuilder(fileNameWithSpacesHandled);
        for (int i = 0; i < fn.length(); i++) {
            if (fn.charAt(i) == '/')
                fn.replace(i, i + 1, File.separator);
        }
        File f = null;
        String[] roots = expandRoots();
        for (String root : roots) {
            f = new File(root, fn.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("Looking for {} in {}, found? {}", fn, root, f.exists());
            }
            if (f.exists()) {
                return (f);
            }
        }
        return (f);
    }

    protected String[] expandRoots() {
        List<String> expandedRoots = new LinkedList<>();
        if(hasWildcard()) {
            String[] rawRoots = websterRoot;
            for (String root : rawRoots) {
                int wildcard;
                if ((wildcard = root.indexOf('*')) != -1) {
                    String prefix = root.substring(0, wildcard);
                    File prefixFile = new File(prefix);
                    if (prefixFile.exists()) {
                        String suffix = (wildcard < (root.length() - 1)) ? root.substring(wildcard + 1) : "";
                        String[] children = prefixFile.list();
                        for (String aChildren : children) {
                            expandedRoots.add(prefix + aChildren + suffix);
                        }
                    }
                    // Eat the root entry if it's wildcarded and doesn't exist
                } else {
                    expandedRoots.add(root);
                }
            }
        }
        String[] roots;
        if(!expandedRoots.isEmpty()) {
            roots = expandedRoots.toArray(new String[expandedRoots.size()]);
        } else {
            roots = websterRoot;
        }
        return(roots);
    }

    /*
     * See if the root is using a wildcard
     */
    boolean hasWildcard() {
        boolean wildcarded = false;
        for (String root : websterRoot) {
            if ((root.indexOf('*')) != -1) {
                wildcarded = true;
                break;
            }
        }
        return(wildcarded);
    }

    void close(Closeable c) {
        if(c!=null) {
            try {
                c.close();
            } catch (IOException e) {
                logger.warn("Closing Closeable", e);
            }
        }
    }

    class Head implements Runnable {
        private final Socket client;
        private final String fileName;

        Head(Socket s, String fileName) {
            client = s;
            this.fileName = fileName;
        }

        public void run() {
            StringBuilder dirData = new StringBuilder();
            StringBuilder logData = new StringBuilder();
            try {
                File getFile = parseFileName(fileName);
                logData.append("Do HEAD: input=")
                    .append(fileName)
                    .append(", parsed=")
                    .append(getFile)
                    .append(", ");
                int fileLength;
                String header;
                if(getFile.isDirectory()) {
                    logData.append("directory located");
                    String files[] = getFile.list();
                    for (String file : files) {
                        File f = new File(getFile, file);
                        dirData.append(f.toString().substring(getFile.getParent().length()));
                        dirData.append("\t");
                        if (f.isDirectory())
                            dirData.append("d");
                        else
                            dirData.append("f");
                        dirData.append("\t");
                        dirData.append(f.length());
                        dirData.append("\t");
                        dirData.append(f.lastModified());
                        dirData.append("\n");
                    }
                    fileLength = dirData.length();
                    String fileType = MimeTypes.getProperty("txt");
                    if(fileType==null)
                        fileType = "application/java";
                    header = "HTTP/1.1 200 OK\n"+
                             "Allow: GET\nMIME-Version: 1.0\n"+
                             "Server: "+SERVER_DESCRIPTION+"\n"+
                             "Content-Type: "+ fileType+ "\n"+
                             "Content-Length: "+ fileLength + "\r\n\r\n";
                } else if(getFile.exists()) {
                    DataInputStream requestedFile = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(getFile)));
                    fileLength = requestedFile.available();
                    String fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
                    fileType = MimeTypes.getProperty(fileType);
                    logData.append("file size: [").append(fileLength).append("]");
                    header = "HTTP/1.1 200 OK\n"
                             + "Allow: GET\nMIME-Version: 1.0\n"
                             + "Server: "+SERVER_DESCRIPTION+"\n"
                             + "Content-Type: "
                             + fileType
                             + "\n"
                             + "Content-Length: "
                             + fileLength
                             + "\r\n\r\n";
                    close(requestedFile);
                } else {
                    header = "HTTP/1.1 404 Not Found\r\n\r\n";
                    logData.append("not found");
                }

                if(debug)
                    System.out.println(logData.toString());
                if(logger.isDebugEnabled())
                    logger.debug(logData.toString());

                DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                clientStream.writeBytes(header);
                clientStream.flush();
                close(clientStream);
            } catch(IOException e) {
                logger.warn("Error closing Socket", e);
            } finally {
                try {
                    client.close();
                } catch(IOException e2) {
                    logger.warn("Closing incoming socket", e2);
                }
            }
        } // end of Head
    }

    class GetFile implements Runnable {
        private final Socket client;
        private final String fileName;
        private int fileLength;

        GetFile(Socket s, String fileName) {
            client = s;
            this.fileName = fileName;
        }

        public void run() {
            StringBuilder dirData = new StringBuilder();
            StringBuilder logData = new StringBuilder();
            DataInputStream requestedFile = null;
            try {
                File getFile = parseFileName(fileName);
                logData.append("Do GET: input=")
                    .append(fileName)
                    .append(", " + "parsed=")
                    .append(getFile)
                    .append(", ");
                String header;
                if(getFile.isDirectory()) {
                    logData.append("directory located");
                    String files[] = getFile.list();
                    for (String file : files) {
                        File f = new File(getFile, file);
                        dirData.append(f.toString().substring(getFile.getParent().length()));
                        dirData.append("\t");
                        if (f.isDirectory())
                            dirData.append("d");
                        else
                            dirData.append("f");
                        dirData.append("\t");
                        dirData.append(f.length());
                        dirData.append("\t");
                        dirData.append(f.lastModified());
                        dirData.append("\n");
                    }
                    fileLength = dirData.length();
                    String fileType = MimeTypes.getProperty("txt");
                    if(fileType == null)
                        fileType = "application/java";
                    header = "HTTP/1.1 200 OK\n"
                             + "Allow: GET\nMIME-Version: 1.0\n"
                             + "Server: "+SERVER_DESCRIPTION+"\n"
                             + "Content-Type: "
                             + fileType
                             + "\n"
                             + "Content-Length: "
                             + fileLength
                             + "\r\n\r\n";
                } else if(getFile.exists()) {
                    requestedFile = new DataInputStream(new BufferedInputStream(new FileInputStream(getFile)));
                    fileLength = requestedFile.available();
                    String fileType = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
                    fileType = MimeTypes.getProperty(fileType);
                    header = "HTTP/1.1 200 OK\n"
                             + "Allow: GET\nMIME-Version: 1.0\n"
                             + "Server: "+SERVER_DESCRIPTION+"\n"
                             + "Content-Type: "
                             + fileType
                             + "\n"
                             + "Content-Length: "
                             + fileLength
                             + "\r\n\r\n";
                } else {
                    header = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
                DataOutputStream clientStream =new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                clientStream.writeBytes(header);

                if(getFile.isDirectory()) {
                    clientStream.writeBytes(dirData.toString());
                } else if(getFile.exists() && requestedFile!=null) {
                    byte[] buffer = new byte[fileLength];
                    requestedFile.readFully(buffer);
                    logData.append("file size: [").append(fileLength).append("]");
                    try {
                        clientStream.write(buffer);
                    } catch(Exception e) {
                        String s = "Sending ["+
                                   getFile.getAbsolutePath()+"], "+
                                   "size ["+fileLength+"], "+
                                   "to client at "+
                                   "["+
                                   client.getInetAddress().getHostAddress()+
                                   "]";
                        if(logger.isDebugEnabled())
                            logger.debug(s, e);
                        if(debug) {
                            System.out.println(s);
                            e.printStackTrace();
                        }
                    }
                } else {
                    logData.append("not found");
                }
                if(debug)
                    System.out.println(logData.toString());
                if(logger.isDebugEnabled())
                    logger.debug(logData.toString());
                clientStream.flush();
                clientStream.close();
            } catch(IOException e) {
                logger.warn("Closing Socket", e);
            } finally {
                try {
                    close(requestedFile);
                    client.close();
                } catch(IOException e2) {
                    logger.warn("Closing incoming socket", e2);
                }
            }
        } // end of GetFile
    }

    class PutFile implements Runnable {
        private Socket client;
        private String fileName;
        private Properties rheader;
        private InputStream inputStream;
        final int BUFFER_SIZE = 4096;

        PutFile(Socket s, String fileName, Properties header, InputStream fromClient) {
            rheader = header;
            client = s;
            this.fileName = fileName;
            this.inputStream = fromClient;
        }

        public void run() {

            String s = ignoreCaseProperty(rheader, "Content-Length");
            if (s == null) {
                try {
                    sendResponse("HTTP/1.0 411 OK\n"
                                 + "Allow: PUT\n"
                                 + "MIME-Version: 1.0\n"
                                 + "Server: " + SERVER_DESCRIPTION + "\n"
                                 + "\n\n <H1>411 Webster refuses to accept the out request for " + fileName + " " +
                                 "without a defined Content-Length.</H1>\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String header;
                OutputStream requestedFileOutputStream = null;
                try {
                    // check to see if the file exists if it does the return code
                    // will be 200 if it doesn't it will be 201
                    File putFile = new File(putDirectory + File.separator + fileName);
                    if (debug)
                        System.out.println("tempDir: " + putDirectory + ", fileName: " + fileName + ", putFile: " + putFile.getPath());

                    if (putFile.exists()) {
                        header = "HTTP/1.0 200 OK\n"
                                 + "Allow: PUT\n"
                                 + "MIME-Version: 1.0\n"
                                 + "Server: " + SERVER_DESCRIPTION + "\n"
                                 + "\n\n <H1>200 PUT File " + fileName + " updated</H1>\n";
                        if (debug)
                            System.out.println("updated putFile: " + putFile);
                    } else {
                        header = "HTTP/1.0 201 Created\n"
                                 + "Allow: PUT\n"
                                 + "MIME-Version: 1.0\n"
                                 + "Server: " + SERVER_DESCRIPTION + "\n"
                                 + "\n\n <H1>201 PUT File " + fileName + " Created</H1>\n";
                        File parentDir = putFile.getParentFile();
                        System.out.println("Parent: " + parentDir.getPath() + ", exists? " + parentDir.exists());
                        if (!parentDir.exists()) {
                            if (parentDir.mkdirs() && debug) {
                                System.out.println("Created " + parentDir.getPath());
                            }
                        }
                        if (debug)
                            System.out.println("Created putFile: " + putFile + ", exists? " + putFile.exists());
                    }

                    int length = Integer.parseInt(ignoreCaseProperty(rheader, "Content-Length"));
                    if (debug)
                        System.out.println("Putting " + fileName + " size: " + length + ", header: " + rheader);
                    try {
                        requestedFileOutputStream = new DataOutputStream(new FileOutputStream(putFile));
                        int read;
                        long amountRead = 0;
                        byte[] buffer = new byte[length < BUFFER_SIZE ? length : BUFFER_SIZE];
                        while (amountRead < length) {
                            read = inputStream.read(buffer);
                            requestedFileOutputStream.write(buffer, 0, read);
                            amountRead += read;
                        }
                        requestedFileOutputStream.flush();
                        System.out.println("Wrote: " + putFile.getPath() + " size: " + putFile.length());
                    } catch (IOException e) {
                        e.printStackTrace();
                        header = "HTTP/1.0 500 Internal Server Error\n"
                                 + "Allow: PUT\n"
                                 + "MIME-Version: 1.0\n"
                                 + "Server: " + SERVER_DESCRIPTION + "\n"
                                 + "\n\n <H1>500 Internal Server Error</H1>\n"
                                 + e;
                    } finally {
                        if (requestedFileOutputStream != null)
                            requestedFileOutputStream.close();
                        sendResponse(header);
                    }

                } catch (Exception e) {
                    logger.warn( "Closing Socket", e);
                } finally {
                    try {
                        if (requestedFileOutputStream != null)
                            requestedFileOutputStream.close();
                        client.close();
                    } catch (IOException e2) {
                    logger.warn("Closing incoming socket", e2);
                    }
                }
            }
        }

        void sendResponse(String header) throws IOException {
            DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
            clientStream.writeBytes(header);
            clientStream.flush();
            clientStream.close();
        }

        public String ignoreCaseProperty(Properties props, String field) {
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                String propName = (String) names.nextElement();
                if (field.equalsIgnoreCase(propName)) {
                    return (props.getProperty(propName));
                }
            }
            return (null);
        }
    }

    class DelFile implements Runnable {
        private final Socket client;
        private final String fileName;

        DelFile(Socket s, String fileName) {
            client = s;
            this.fileName = fileName;
        }

        public void run() {
            try {
                File putFile = parseFileName(fileName);
                String header;
                if(!putFile.exists()) {
                    header = "HTTP/1.1 404 File not found\n"
                             + "Allow: GET\n"
                             + "MIME-Version: 1.0\n"
                             +"Server: "+SERVER_DESCRIPTION+"\n"
                             + "\n\n <H1>404 File not Found</H1>\n"
                             + "<BR>";
                } else if(putFile.delete()) {
                    header = "HTTP/1.1 200 OK\n"
                             + "Allow: PUT\n"
                             + "MIME-Version: 1.0\n"
                             +"Server: "+SERVER_DESCRIPTION+"\n"
                             + "\n\n <H1>200 File succesfully deleted</H1>\n";
                } else {
                    header = "HTTP/1.1 500 Internal Server Error\n"
                             + "Allow: PUT\n"
                             + "MIME-Version: 1.0\n"
                             +"Server: "+SERVER_DESCRIPTION+"\n"
                             + "\n\n <H1>500 File could not be deleted</H1>\n";
                }
                DataOutputStream clientStream = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                clientStream.writeBytes(header);
                clientStream.flush();
                close(clientStream);
            } catch(IOException e) {
                logger.warn("Closing Socket", e);
            } finally {
                try {
                    client.close();
                } catch(IOException e2) {
                    logger.warn("Closing incoming socket", e2);
                }
            }
        }
    }
}
