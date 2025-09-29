/*
 * Decompiled with CFR 0.152.
 */
package com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby;

import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.Library;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.classloader.IsolatedClassLoader;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.LogLevel;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.Logger;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.logging.adapters.LogAdapter;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.relocation.Relocation;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.relocation.RelocationHelper;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class LibraryManager {
    protected final Logger logger;
    protected final Path saveDirectory;
    private final Set<String> repositories = new LinkedHashSet<String>();
    private RelocationHelper relocator;
    private final Map<String, IsolatedClassLoader> isolatedLibraries = new HashMap<String, IsolatedClassLoader>();

    @Deprecated
    protected LibraryManager(LogAdapter logAdapter, Path dataDirectory) {
        this.logger = new Logger(Objects.requireNonNull(logAdapter, "logAdapter"));
        this.saveDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().resolve("lib");
    }

    protected LibraryManager(LogAdapter logAdapter, Path dataDirectory, String directoryName) {
        this.logger = new Logger(Objects.requireNonNull(logAdapter, "logAdapter"));
        this.saveDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory").toAbsolutePath().resolve(Objects.requireNonNull(directoryName, "directoryName"));
    }

    protected abstract void addToClasspath(Path var1);

    protected void addToIsolatedClasspath(Library library, Path file) {
        String id = library.getId();
        IsolatedClassLoader classLoader = id != null ? this.isolatedLibraries.computeIfAbsent(id, s -> new IsolatedClassLoader(new URL[0])) : new IsolatedClassLoader(new URL[0]);
        classLoader.addPath(file);
    }

    public IsolatedClassLoader getIsolatedClassLoaderOf(String libraryId) {
        return this.isolatedLibraries.get(libraryId);
    }

    public LogLevel getLogLevel() {
        return this.logger.getLevel();
    }

    public void setLogLevel(LogLevel level) {
        this.logger.setLevel(level);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Collection<String> getRepositories() {
        LinkedList<String> urls;
        Set<String> set = this.repositories;
        synchronized (set) {
            urls = new LinkedList<String>(this.repositories);
        }
        return Collections.unmodifiableList(urls);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addRepository(String url) {
        String repo = Objects.requireNonNull(url, "url").endsWith("/") ? url : url + '/';
        Set<String> set = this.repositories;
        synchronized (set) {
            this.repositories.add(repo);
        }
    }

    public void addMavenLocal() {
        this.addRepository(Paths.get(System.getProperty("user.home"), new String[0]).resolve(".m2/repository").toUri().toString());
    }

    public void addMavenCentral() {
        this.addRepository("https://repo1.maven.org/maven2/");
    }

    public void addSonatype() {
        this.addRepository("https://oss.sonatype.org/content/groups/public/");
    }

    public void addJCenter() {
        this.addRepository("https://jcenter.bintray.com/");
    }

    public void addJitPack() {
        this.addRepository("https://jitpack.io/");
    }

    public Collection<String> resolveLibrary(Library library) {
        String url;
        LinkedHashSet<String> urls = new LinkedHashSet<String>(Objects.requireNonNull(library, "library").getUrls());
        boolean snapshot = library.isSnapshot();
        for (String repository : library.getRepositories()) {
            if (snapshot) {
                url = this.resolveSnapshot(repository, library);
                if (url == null) continue;
                urls.add(repository + url);
                continue;
            }
            urls.add(repository + library.getPath());
        }
        for (String repository : this.getRepositories()) {
            if (snapshot) {
                url = this.resolveSnapshot(repository, library);
                if (url == null) continue;
                urls.add(repository + url);
                continue;
            }
            urls.add(repository + library.getPath());
        }
        return Collections.unmodifiableSet(urls);
    }

    private String resolveSnapshot(String repository, Library library) {
        String string;
        block15: {
            String url = Objects.requireNonNull(repository, "repository") + Objects.requireNonNull(library, "library").getPartialPath() + "maven-metadata.xml";
            URLConnection connection = new URL(Objects.requireNonNull(url, "url")).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "libby/1.3.0");
            InputStream in = connection.getInputStream();
            try {
                string = this.getURLFromMetadata(in, library);
                if (in == null) break block15;
            }
            catch (Throwable throwable) {
                try {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
                catch (IOException e) {
                    if (e instanceof FileNotFoundException) {
                        this.logger.debug("File not found: " + url);
                    } else if (e instanceof SocketTimeoutException) {
                        this.logger.debug("Connect timed out: " + url);
                    } else if (e instanceof UnknownHostException) {
                        this.logger.debug("Unknown host: " + url);
                    } else {
                        this.logger.debug("Unexpected IOException", e);
                    }
                    return null;
                }
            }
            in.close();
        }
        return string;
    }

    private String getURLFromMetadata(InputStream inputStream, Library library) throws IOException {
        String buildNumber;
        String timestamp;
        Objects.requireNonNull(inputStream, "inputStream");
        Objects.requireNonNull(library, "library");
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputStream);
            doc.getDocumentElement().normalize();
            NodeList nodes = doc.getElementsByTagName("snapshot");
            if (nodes.getLength() == 0) {
                return null;
            }
            Node snapshot = nodes.item(0);
            if (snapshot.getNodeType() != 1) {
                return null;
            }
            Node timestampNode = ((Element)snapshot).getElementsByTagName("timestamp").item(0);
            if (timestampNode == null || timestampNode.getNodeType() != 1) {
                return null;
            }
            Node buildNumberNode = ((Element)snapshot).getElementsByTagName("buildNumber").item(0);
            if (buildNumberNode == null || buildNumberNode.getNodeType() != 1) {
                return null;
            }
            Node timestampChild = timestampNode.getFirstChild();
            if (timestampChild == null || timestampChild.getNodeType() != 3) {
                return null;
            }
            Node buildNumberChild = buildNumberNode.getFirstChild();
            if (buildNumberChild == null || buildNumberChild.getNodeType() != 3) {
                return null;
            }
            timestamp = timestampChild.getNodeValue();
            buildNumber = buildNumberChild.getNodeValue();
        }
        catch (ParserConfigurationException | SAXException e) {
            this.logger.debug("Invalid maven-metadata.xml", e);
            return null;
        }
        String version = library.getVersion();
        if (version.endsWith("-SNAPSHOT")) {
            version = version.substring(0, version.length() - "-SNAPSHOT".length());
        }
        String url = library.getPartialPath() + library.getArtifactId() + '-' + version + '-' + timestamp + '-' + buildNumber;
        if (library.hasClassifier()) {
            url = url + '-' + library.getClassifier();
        }
        return url + ".jar";
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private byte[] downloadLibrary(String url) {
        try {
            URLConnection connection = new URL(Objects.requireNonNull(url, "url")).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "libby/1.3.0");
            InputStream in = connection.getInputStream();
            try {
                byte[] buf = new byte[8192];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
                catch (SocketTimeoutException e) {
                    this.logger.warn("Download timed out: " + connection.getURL());
                    byte[] byArray2 = null;
                    if (in == null) return byArray2;
                    in.close();
                    return byArray2;
                }
                this.logger.info("Downloaded library " + connection.getURL());
                byte[] byArray = out.toByteArray();
                return byArray;
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    }
                    catch (Throwable throwable2) {
                        Throwable throwable;
                        throwable.addSuppressed(throwable2);
                    }
                }
            }
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                this.logger.debug("File not found: " + url);
                return null;
            }
            if (e instanceof SocketTimeoutException) {
                this.logger.debug("Connect timed out: " + url);
                return null;
            }
            if (e instanceof UnknownHostException) {
                this.logger.debug("Unknown host: " + url);
                return null;
            }
            this.logger.debug("Unexpected IOException", e);
            return null;
        }
    }

    public Path downloadLibrary(Library library) {
        Collection<String> urls;
        Path file = this.saveDirectory.resolve(Objects.requireNonNull(library, "library").getPath());
        if (Files.exists(file, new LinkOption[0])) {
            if (!library.isSnapshot()) {
                return file;
            }
            try {
                Files.delete(file);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if ((urls = this.resolveLibrary(library)).isEmpty()) {
            throw new RuntimeException("Library '" + library + "' couldn't be resolved, add a repository");
        }
        MessageDigest md = null;
        if (library.hasChecksum()) {
            try {
                md = MessageDigest.getInstance("SHA-256");
            }
            catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        Path out = file.resolveSibling(file.getFileName() + ".tmp");
        out.toFile().deleteOnExit();
        try {
            Files.createDirectories(file.getParent(), new FileAttribute[0]);
            for (String url : urls) {
                byte[] checksum;
                byte[] bytes = this.downloadLibrary(url);
                if (bytes == null) continue;
                if (md != null && !Arrays.equals(checksum = md.digest(bytes), library.getChecksum())) {
                    this.logger.warn("*** INVALID CHECKSUM ***");
                    this.logger.warn(" Library :  " + library);
                    this.logger.warn(" URL :  " + url);
                    this.logger.warn(" Expected :  " + Base64.getEncoder().encodeToString(library.getChecksum()));
                    this.logger.warn(" Actual :  " + Base64.getEncoder().encodeToString(checksum));
                    continue;
                }
                Files.write(out, bytes, new OpenOption[0]);
                Files.move(out, file, new CopyOption[0]);
                Path path = file;
                return path;
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        finally {
            try {
                Files.deleteIfExists(out);
            }
            catch (IOException iOException) {}
        }
        throw new RuntimeException("Failed to download library '" + library + "'");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Path relocate(Path in, String out, Collection<Relocation> relocations) {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(relocations, "relocations");
        Path file = this.saveDirectory.resolve(out);
        if (Files.exists(file, new LinkOption[0])) {
            return file;
        }
        Path tmpOut = file.resolveSibling(file.getFileName() + ".tmp");
        tmpOut.toFile().deleteOnExit();
        Object object = this;
        synchronized (object) {
            if (this.relocator == null) {
                this.relocator = new RelocationHelper(this);
            }
        }
        try {
            this.relocator.relocate(in, tmpOut, relocations);
            Files.move(tmpOut, file, new CopyOption[0]);
            this.logger.info("Relocations applied to " + this.saveDirectory.getParent().relativize(in));
            object = file;
            return object;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        finally {
            try {
                Files.deleteIfExists(tmpOut);
            }
            catch (IOException iOException) {}
        }
    }

    public void loadLibrary(Library library) {
        Path file = this.downloadLibrary(Objects.requireNonNull(library, "library"));
        if (library.hasRelocations()) {
            file = this.relocate(file, library.getRelocatedPath(), library.getRelocations());
        }
        if (library.isIsolatedLoad()) {
            this.addToIsolatedClasspath(library, file);
        } else {
            this.addToClasspath(file);
        }
    }
}

