/*
 * Copyright (C) 2012 The Guava Authors
 *
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

package com.google.common.reflect;

import static com.google.common.base.Charsets.US_ASCII;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.common.reflect.ClassPath.ResourceInfo;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Functional tests of {@link ClassPath}.
 */
public class ClassPathTest extends TestCase {
  private static final Logger log = Logger.getLogger(ClassPathTest.class.getName());

  public void testEquals() {
    new EqualsTester()
        .addEqualityGroup(classInfo(ClassPathTest.class), classInfo(ClassPathTest.class))
        .addEqualityGroup(classInfo(Test.class), classInfo(Test.class, getClass().getClassLoader()))
        .addEqualityGroup(
            new ResourceInfo("a/b/c.txt", getClass().getClassLoader()),
            new ResourceInfo("a/b/c.txt", getClass().getClassLoader()))
        .addEqualityGroup(
            new ResourceInfo("x.txt", getClass().getClassLoader()))
        .testEquals();
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_emptyURLClassLoader_noParent() {
    assertThat(ClassPath.Scanner.getClassPathEntries(new URLClassLoader(new URL[0], null)).keySet())
        .isEmpty();
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_URLClassLoader_noParent() throws Exception {
    URL url1 = new URL("file:/a");
    URL url2 = new URL("file:/b");
    URLClassLoader classloader = new URLClassLoader(new URL[] {url1, url2}, null);
    assertThat(ClassPath.Scanner.getClassPathEntries(classloader))
        .containsExactly(new File("/a"), classloader, new File("/b"), classloader);
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_URLClassLoader_withParent() throws Exception {
    URL url1 = new URL("file:/a");
    URL url2 = new URL("file:/b");
    URLClassLoader parent = new URLClassLoader(new URL[] {url1}, null);
    URLClassLoader child = new URLClassLoader(new URL[] {url2}, parent) {};
    assertThat(ClassPath.Scanner.getClassPathEntries(child))
        .containsExactly(new File("/a"), parent, new File("/b"), child)
        .inOrder();
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_duplicateUri_parentWins() throws Exception {
    URL url = new URL("file:/a");
    URLClassLoader parent = new URLClassLoader(new URL[] {url}, null);
    URLClassLoader child = new URLClassLoader(new URL[] {url}, parent) {};
    assertThat(ClassPath.Scanner.getClassPathEntries(child))
        .containsExactly(new File("/a"), parent);
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_notURLClassLoader_noParent() {
    assertThat(ClassPath.Scanner.getClassPathEntries(new ClassLoader(null) {})).isEmpty();
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_notURLClassLoader_withParent() throws Exception {
    URL url = new URL("file:/a");
    URLClassLoader parent = new URLClassLoader(new URL[] {url}, null);
    assertThat(ClassPath.Scanner.getClassPathEntries(new ClassLoader(parent) {}))
        .containsExactly(new File("/a"), parent);
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_notURLClassLoader_withParentAndGrandParent() throws Exception {
    URL url1 = new URL("file:/a");
    URL url2 = new URL("file:/b");
    URLClassLoader grandParent = new URLClassLoader(new URL[] {url1}, null);
    URLClassLoader parent = new URLClassLoader(new URL[] {url2}, grandParent);
    assertThat(ClassPath.Scanner.getClassPathEntries(new ClassLoader(parent) {}))
        .containsExactly(new File("/a"), grandParent, new File("/b"), parent);
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  public void testClassPathEntries_notURLClassLoader_withGrandParent() throws Exception {
    URL url = new URL("file:/a");
    URLClassLoader grandParent = new URLClassLoader(new URL[] {url}, null);
    ClassLoader parent = new ClassLoader(grandParent) {};
    assertThat(ClassPath.Scanner.getClassPathEntries(new ClassLoader(parent) {}))
        .containsExactly(new File("/a"), grandParent);
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  // https://github.com/google/guava/issues/2152
  public void testClassPathEntries_URLClassLoader_pathWithSpace() throws Exception {
    URL url = new URL("file:///c:/Documents and Settings/");
    URLClassLoader classloader = new URLClassLoader(new URL[] {url}, null);
    assertThat(ClassPath.Scanner.getClassPathEntries(classloader))
        .containsExactly(new File("/c:/Documents and Settings/"), classloader);
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  // https://github.com/google/guava/issues/2152
  public void testClassPathEntries_URLClassLoader_pathWithEscapedSpace() throws Exception {
    URL url = new URL("file:///c:/Documents%20and%20Settings/");
    URLClassLoader classloader = new URLClassLoader(new URL[] {url}, null);
    assertThat(ClassPath.Scanner.getClassPathEntries(classloader))
        .containsExactly(new File("/c:/Documents and Settings/"), classloader);
  }

  // https://github.com/google/guava/issues/2152
  public void testToFile() throws Exception {
    assertThat(ClassPath.toFile(new URL("file:///c:/Documents%20and%20Settings/")))
        .isEqualTo(new File("/c:/Documents and Settings/"));
    assertThat(ClassPath.toFile(new URL("file:///c:/Documents ~ Settings, or not/11-12 12:05")))
        .isEqualTo(new File("/c:/Documents ~ Settings, or not/11-12 12:05"));
  }

  // https://github.com/google/guava/issues/2152
  @AndroidIncompatible // works in newer Android versions but fails at the version we test with
  public void testToFile_AndroidIncompatible() throws Exception {
    assertThat(ClassPath.toFile(new URL("file:///c:\\Documents ~ Settings, or not\\11-12 12:05")))
        .isEqualTo(new File("/c:\\Documents ~ Settings, or not\\11-12 12:05"));
    assertThat(ClassPath.toFile(new URL("file:///C:\\Program Files\\Apache Software Foundation")))
        .isEqualTo(new File("/C:\\Program Files\\Apache Software Foundation/"));
    assertThat(ClassPath.toFile(new URL("file:///C:\\\u20320 \u22909")))  // Chinese Ni Hao
        .isEqualTo(new File("/C:\\\u20320 \u22909"));
  }

  @AndroidIncompatible // Android forbids null parent ClassLoader
  // https://github.com/google/guava/issues/2152
  public void testJarFileWithSpaces() throws Exception {
    URL url = makeJarUrlWithName("To test unescaped spaces in jar file name.jar");
    URLClassLoader classloader = new URLClassLoader(new URL[] {url}, null);
    assertThat(ClassPath.from(classloader).getTopLevelClasses()).isNotEmpty();
  }

  public void testScan_classPathCycle() throws IOException {
    File jarFile = File.createTempFile("with_circular_class_path", ".jar");
    try {
      writeSelfReferencingJarFile(jarFile, "test.txt");
      ClassPath.DefaultScanner scanner = new ClassPath.DefaultScanner();
      scanner.scan(jarFile, ClassPathTest.class.getClassLoader());
      assertThat(scanner.getResources()).hasSize(1);
    } finally {
      jarFile.delete();
    }
  }

  public void testScanFromFile_fileNotExists() throws IOException {
    ClassLoader classLoader = ClassPathTest.class.getClassLoader();
    ClassPath.DefaultScanner scanner = new ClassPath.DefaultScanner();
    scanner.scan(new File("no/such/file/anywhere"), classLoader);
    assertThat(scanner.getResources()).isEmpty();
  }

  public void testScanFromFile_notJarFile() throws IOException {
    ClassLoader classLoader = ClassPathTest.class.getClassLoader();
    File notJar = File.createTempFile("not_a_jar", "txt");
    ClassPath.DefaultScanner scanner = new ClassPath.DefaultScanner();
    try {
      scanner.scan(notJar, classLoader);
    } finally {
      notJar.delete();
    }
    assertThat(scanner.getResources()).isEmpty();
  }

  public void testGetClassPathEntry() throws MalformedURLException, URISyntaxException {
    assertEquals(new File("/usr/test/dep.jar").toURI(),
        ClassPath.Scanner.getClassPathEntry(
            new File("/home/build/outer.jar"), "file:/usr/test/dep.jar").toURI());
    assertEquals(new File("/home/build/a.jar").toURI(),
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "a.jar").toURI());
    assertEquals(new File("/home/build/x/y/z").toURI(),
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "x/y/z").toURI());
    assertEquals(new File("/home/build/x/y/z.jar").toURI(),
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "x/y/z.jar")
            .toURI());
    assertEquals("/home/build/x y.jar",
        ClassPath.Scanner.getClassPathEntry(new File("/home/build/outer.jar"), "x y.jar")
            .getFile());
  }

  public void testGetClassPathFromManifest_nullManifest() {
    assertThat(ClassPath.Scanner.getClassPathFromManifest(new File("some.jar"), null)).isEmpty();
  }

  public void testGetClassPathFromManifest_noClassPath() throws IOException {
    File jarFile = new File("base.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest("")))
        .isEmpty();
  }

  public void testGetClassPathFromManifest_emptyClassPath() throws IOException {
    File jarFile = new File("base.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifestClasspath("")))
        .isEmpty();
  }

  public void testGetClassPathFromManifest_badClassPath() throws IOException {
    File jarFile = new File("base.jar");
    Manifest manifest = manifestClasspath("nosuchscheme:an_invalid^path");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .isEmpty();
  }

  public void testGetClassPathFromManifest_pathWithStrangeCharacter() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:the^file.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("base/the^file.jar"));
  }

  public void testGetClassPathFromManifest_relativeDirectory() throws IOException {
    File jarFile = new File("base/some.jar");
    // with/relative/directory is the Class-Path value in the mf file.
    Manifest manifest = manifestClasspath("with/relative/dir");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("base/with/relative/dir"));
  }

  public void testGetClassPathFromManifest_relativeJar() throws IOException {
    File jarFile = new File("base/some.jar");
    // with/relative/directory is the Class-Path value in the mf file.
    Manifest manifest = manifestClasspath("with/relative.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("base/with/relative.jar"));
  }

  public void testGetClassPathFromManifest_jarInCurrentDirectory() throws IOException {
    File jarFile = new File("base/some.jar");
    // with/relative/directory is the Class-Path value in the mf file.
    Manifest manifest = manifestClasspath("current.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("base/current.jar"));
  }

  public void testGetClassPathFromManifest_absoluteDirectory() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:/with/absolute/dir");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("/with/absolute/dir"));
  }

  public void testGetClassPathFromManifest_absoluteJar() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:/with/absolute.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("/with/absolute.jar"));
  }

  public void testGetClassPathFromManifest_multiplePaths() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("file:/with/absolute.jar relative.jar  relative/dir");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(
            fullpath("/with/absolute.jar"),
            fullpath("base/relative.jar"),
            fullpath("base/relative/dir"))
        .inOrder();
  }

  public void testGetClassPathFromManifest_leadingBlanks() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath(" relative.jar");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("base/relative.jar"));
  }

  public void testGetClassPathFromManifest_trailingBlanks() throws IOException {
    File jarFile = new File("base/some.jar");
    Manifest manifest = manifestClasspath("relative.jar ");
    assertThat(ClassPath.Scanner.getClassPathFromManifest(jarFile, manifest))
        .containsExactly(fullpath("base/relative.jar"));
  }

  public void testGetClassName() {
    assertEquals("abc.d.Abc", ClassPath.getClassName("abc/d/Abc.class"));
  }

  public void testResourceInfo_of() {
    assertEquals(ClassInfo.class, resourceInfo(ClassPathTest.class).getClass());
    assertEquals(ClassInfo.class, resourceInfo(ClassPath.class).getClass());
    assertEquals(ClassInfo.class, resourceInfo(Nested.class).getClass());
  }

  public void testGetSimpleName() {
    ClassLoader classLoader = getClass().getClassLoader();
    assertEquals("Foo",
        new ClassInfo("Foo.class", classLoader).getSimpleName());
    assertEquals("Foo",
        new ClassInfo("a/b/Foo.class", classLoader).getSimpleName());
    assertEquals("Foo",
        new ClassInfo("a/b/Bar$Foo.class", classLoader).getSimpleName());
    assertEquals("",
        new ClassInfo("a/b/Bar$1.class", classLoader).getSimpleName());
    assertEquals("Foo",
        new ClassInfo("a/b/Bar$Foo.class", classLoader).getSimpleName());
    assertEquals("",
        new ClassInfo("a/b/Bar$1.class", classLoader).getSimpleName());
    assertEquals("Local",
        new ClassInfo("a/b/Bar$1Local.class", classLoader).getSimpleName());
  }

  public void testGetPackageName() {
    assertEquals("",
        new ClassInfo("Foo.class", getClass().getClassLoader()).getPackageName());
    assertEquals("a.b",
        new ClassInfo("a/b/Foo.class", getClass().getClassLoader()).getPackageName());
  }

  // Test that ResourceInfo.urls() returns identical content to ClassLoader.getResources()

  private static boolean contentEquals(URL left, URL right) throws IOException {
    return Resources.asByteSource(left).contentEquals(Resources.asByteSource(right));
  }

  private static class Nested {}

  public void testNulls() throws IOException {
    new NullPointerTester().testAllPublicStaticMethods(ClassPath.class);
    new NullPointerTester()
        .testAllPublicInstanceMethods(ClassPath.from(getClass().getClassLoader()));
  }

  public void testResourceScanner() throws IOException {
    ResourceScanner scanner = new ResourceScanner();
    scanner.scan(ClassLoader.getSystemClassLoader());
    assertThat(scanner.resources).contains("com/google/common/reflect/ClassPathTest.class");
  }

  public void testExistsThrowsSecurityException() throws IOException, URISyntaxException {
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      doTestExistsThrowsSecurityException();
    } finally {
      System.setSecurityManager(oldSecurityManager);
    }
  }

  private void doTestExistsThrowsSecurityException() throws IOException, URISyntaxException {
    URLClassLoader myLoader = (URLClassLoader) getClass().getClassLoader();
    URL[] urls = myLoader.getURLs();
    ImmutableList.Builder<File> filesBuilder = ImmutableList.builder();
    for (URL url : urls) {
      if (url.getProtocol().equalsIgnoreCase("file")) {
        filesBuilder.add(new File(url.toURI()));
      }
    }
    ImmutableList<File> files = filesBuilder.build();
    assertThat(files).isNotEmpty();
    SecurityManager disallowFilesSecurityManager = new SecurityManager() {
      @Override
      public void checkPermission(Permission p) {
        if (p instanceof FilePermission) {
          throw new SecurityException("Disallowed: " + p);
        }
      }
    };
    System.setSecurityManager(disallowFilesSecurityManager);
    try {
      files.get(0).exists();
      fail("Did not get expected SecurityException");
    } catch (SecurityException expected) {
    }
    ClassPath classPath = ClassPath.from(myLoader);
    assertThat(classPath.getResources()).isEmpty();
  }

  private static ClassPath.ClassInfo findClass(
      Iterable<ClassPath.ClassInfo> classes, Class<?> cls) {
    for (ClassPath.ClassInfo classInfo : classes) {
      if (classInfo.getName().equals(cls.getName())) {
        return classInfo;
      }
    }
    throw new AssertionError("failed to find " + cls);
  }

  private static ResourceInfo resourceInfo(Class<?> cls) {
    String resource = cls.getName().replace('.', '/') + ".class";
    ClassLoader loader = cls.getClassLoader();
    return ResourceInfo.of(resource, loader);
  }

  private static ClassInfo classInfo(Class<?> cls) {
    return classInfo(cls, cls.getClassLoader());
  }

  private static ClassInfo classInfo(Class<?> cls, ClassLoader classLoader) {
    String resource = cls.getName().replace('.', '/') + ".class";
    return new ClassInfo(resource, classLoader);
  }

  private static Manifest manifestClasspath(String classpath) throws IOException {
    return manifest("Class-Path: " + classpath + "\n");
  }

  private static void writeSelfReferencingJarFile(File jarFile, String... entries)
      throws IOException {
    Manifest manifest = new Manifest();
    // Without version, the manifest is silently ignored. Ugh!
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, jarFile.getName());

    Closer closer = Closer.create();
    try {
      FileOutputStream fileOut = closer.register(new FileOutputStream(jarFile));
      JarOutputStream jarOut = closer.register(new JarOutputStream(fileOut));
      for (String entry : entries) {
        jarOut.putNextEntry(new ZipEntry(entry));
        Resources.copy(ClassPathTest.class.getResource(entry), jarOut);
        jarOut.closeEntry();
      }
    } catch (Throwable e) {
      throw closer.rethrow(e);
    } finally {
      closer.close();
    }
  }

  private static Manifest manifest(String content) throws IOException {
    InputStream in = new ByteArrayInputStream(content.getBytes(US_ASCII));
    Manifest manifest = new Manifest();
    manifest.read(in);
    return manifest;
  }

  private static File fullpath(String path) {
    return new File(new File(path).toURI());
  }

  private static class ResourceScanner extends ClassPath.Scanner {
    final Set<String> resources = new HashSet<String>();

    @Override protected void scanDirectory(ClassLoader loader, File root) throws IOException {
      URI base = root.toURI();
      for (File entry : Files.fileTreeTraverser().preOrderTraversal(root)) {
        String resourceName = new File(base.relativize(entry.toURI()).getPath()).getPath();
        resources.add(resourceName);
      }
    }

    @Override protected void scanJarFile(ClassLoader loader, JarFile file) throws IOException {
      Enumeration<JarEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        resources.add(entries.nextElement().getName());
      }
    }
  }

  private static URL makeJarUrlWithName(String name) throws IOException {
    File fullPath = new File(Files.createTempDir(), name);
    File jarFile = JarFileFinder.pickAnyJarFile();
    Files.copy(jarFile, fullPath);
    return fullPath.toURI().toURL();
  }

  private static final class JarFileFinder extends ClassPath.Scanner {

    private File found;

    static File pickAnyJarFile() throws IOException {
      JarFileFinder finder = new JarFileFinder();
      try {
        finder.scan(JarFileFinder.class.getClassLoader());
        throw new IllegalStateException("No jar file found!");
      } catch (StopScanningException expected) {
        return finder.found;
      }
    }

    @Override protected void scanJarFile(ClassLoader loader, JarFile file) throws IOException {
      this.found = new File(file.getName());
      throw new StopScanningException();
    }

    @Override protected void scanDirectory(ClassLoader loader, File root) {}

    // Special exception just to terminate the scanning when we get any jar file to use.
    private static final class StopScanningException extends RuntimeException {}
  }
}
