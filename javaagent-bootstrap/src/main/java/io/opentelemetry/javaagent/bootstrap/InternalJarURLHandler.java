/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.javaagent.bootstrap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalJarURLHandler extends URLStreamHandler {

  private static final Logger log = LoggerFactory.getLogger(InternalJarURLHandler.class);

  private static final WeakReference<ResolvedJarEntry> NULL = new WeakReference<>(null);

  private final String name;
  private final FileNotInInternalJar notFound;
  private final JarFile bootstrapJarFile;

  private WeakReference<ResolvedJarEntry> cache = NULL;

  public InternalJarURLHandler(String internalJarFileName, URL bootstrapJarLocation) {
    name = internalJarFileName;
    notFound = new FileNotInInternalJar(internalJarFileName);
    JarFile jarFile = null;
    try {
      if (bootstrapJarLocation != null) {
        jarFile = new JarFile(new File(bootstrapJarLocation.toURI()), false);
      }
    } catch (URISyntaxException | IOException e) {
      log.error("Unable to read internal jar", e);
    }

    bootstrapJarFile = jarFile;
  }

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    String filename = url.getFile();
    if ("/".equals(filename)) {
      // "/" is used as the default url of the jar
      // This is called by the SecureClassLoader trying to obtain permissions

      // nullInputStream() is not available until Java 11
      return new InternalJarURLConnection(url, new ByteArrayInputStream(new byte[0]));
    }
    // believe it or not, we're going to get called twice for this,
    // and the key will be a new object each time.
    ResolvedJarEntry pair = cache.get();
    if (null == pair || !filename.equals(pair.filename)) {
      JarEntry entry = bootstrapJarFile.getJarEntry(getResourcePath(filename));
      if (null != entry) {
        pair = new ResolvedJarEntry(filename, entry);
        // the cache field is not volatile as a performance optimization
        // in the rare event this write is not visible to another thread,
        // it just means the same work is recomputed but does not affect consistency
        cache = new WeakReference<>(pair);
      } else {
        throw notFound;
      }
    } else {
      // hack: just happen to know this only ever happens twice,
      // so dismiss cache after a hit
      cache = NULL;
    }
    return new InternalJarURLConnection(url, bootstrapJarFile.getInputStream(pair.entry));
  }

  private String getResourcePath(String filename) {
    boolean isClass = filename.endsWith(".class");
    int length = name.length() + filename.length();
    if (isClass) {
      length += 4; // length of text "data" appended below
    }
    StringBuilder sb = new StringBuilder(length);
    sb.append(name).append(filename);
    if (isClass) {
      sb.append("data");
    }
    return sb.toString();
  }

  private static class InternalJarURLConnection extends URLConnection {
    private final InputStream inputStream;

    private InternalJarURLConnection(URL url, InputStream inputStream) {
      super(url);
      this.inputStream = inputStream;
    }

    @Override
    public void connect() {
      connected = true;
    }

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }

    @Override
    public Permission getPermission() {
      // No permissions needed because all classes are in memory
      return null;
    }
  }

  private static class FileNotInInternalJar extends IOException {

    public FileNotInInternalJar(String jarName) {
      super("class not found in " + jarName);
    }

    @Override
    public Throwable fillInStackTrace() {
      return this;
    }
  }

  private static class ResolvedJarEntry {
    private final String filename;
    private final JarEntry entry;

    private ResolvedJarEntry(String filename, JarEntry entry) {
      this.filename = filename;
      this.entry = entry;
    }
  }
}
