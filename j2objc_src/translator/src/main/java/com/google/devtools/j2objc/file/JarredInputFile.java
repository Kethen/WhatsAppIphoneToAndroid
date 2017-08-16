/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.j2objc.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * A file inside a .jar file.
 *
 * @author Mike Thvedt
 */
public class JarredInputFile implements InputFile {
  private final String jarPath;
  private final String internalPath;

  /**
   * Create a new JarredSourceFile. The file's unit name will be the same as
   * the given internal path.
   * @param jarPath a filesystem path to the containing .jar
   * @param internalPath the file's path within the jar
   */
  public JarredInputFile(String jarPath, String internalPath) {
    assert !jarPath.endsWith(".java");
    this.jarPath = jarPath;
    this.internalPath = internalPath;
  }

  @Override
  public boolean exists() throws IOException {
    try (JarFile jarFile = new JarFile(jarPath)) {
      ZipEntry entry = jarFile.getEntry(internalPath);
      return entry != null;
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    final JarFile jarFile = new JarFile(jarPath);
    ZipEntry entry = jarFile.getEntry(internalPath);
    final InputStream entryStream = jarFile.getInputStream(entry);
    return new InputStream() {

      @Override
      public int read() throws IOException {
        return entryStream.read();
      }

      @Override
      public int read(byte[] buffer) throws IOException {
        return entryStream.read(buffer);
      }

      @Override
      public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        return entryStream.read(buffer, byteOffset, byteCount);
      }

      @Override
      public void close() throws IOException {
        entryStream.close();
        jarFile.close();
      }
    };
  }

  @Override
  public Reader openReader(Charset charset) throws IOException {
    return new InputStreamReader(getInputStream(), charset);
  }

  @Override
  public String getAbsolutePath() {
    return jarPath;
  }

  @Override
  public String getOriginalLocation() {
    return "jar:file:" + jarPath + "!" + internalPath;
  }

  @Override
  public String getUnitName() {
    return internalPath;
  }

  @Override
  public String getBasename() {
    return internalPath.substring(internalPath.lastIndexOf('/') + 1);
  }

  @Override
  public long lastModified() {
    return new File(jarPath).lastModified();
  }

  @Override
  public String toString() {
    return getOriginalLocation();
  }
}
