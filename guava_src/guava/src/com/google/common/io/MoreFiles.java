/*
 * Copyright (C) 2013 The Guava Authors
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

package com.google.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeTraverser;
import com.google.common.io.ByteSource.AsCharSource;
import com.google.j2objc.annotations.J2ObjCIncompatible;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Static utilities for use with {@link Path} instances, intended to complement {@link Files}.
 *
 * <p>Many methods provided by Guava's {@code Files} class for {@link java.io.File} instances are
 * now available via the JDK's {@link java.nio.file.Files} class for {@code Path} - check the JDK's
 * class if a sibling method from {@code Files} appears to be missing from this class.
 *
 * @since 21.0
 * @author Colin Decker
 */
@Beta
@GwtIncompatible
@J2ObjCIncompatible // java.nio.file
public final class MoreFiles {

  private MoreFiles() {}

  /**
   * Returns a view of the given {@code path} as a {@link ByteSource}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned source and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#READ READ} option.
   */
  public static ByteSource asByteSource(Path path, OpenOption... options) {
    return new PathByteSource(path, options);
  }

  private static final class PathByteSource extends ByteSource {

    private static final LinkOption[] FOLLOW_LINKS = {};

    private final Path path;
    private final OpenOption[] options;
    private final boolean followLinks;

    private PathByteSource(Path path, OpenOption... options) {
      this.path = checkNotNull(path);
      this.options = options.clone();
      this.followLinks = followLinks(this.options);
      // TODO(cgdecker): validate the provided options... for example, just WRITE seems wrong
    }

    private static boolean followLinks(OpenOption[] options) {
      for (OpenOption option : options) {
        if (option == NOFOLLOW_LINKS) {
          return false;
        }
      }
      return true;
    }

    @Override
    public InputStream openStream() throws IOException {
      return Files.newInputStream(path, options);
    }

    private BasicFileAttributes readAttributes() throws IOException {
      return Files.readAttributes(
          path, BasicFileAttributes.class,
          followLinks ? FOLLOW_LINKS : new LinkOption[] { NOFOLLOW_LINKS });
    }

    @Override
    public Optional<Long> sizeIfKnown() {
      BasicFileAttributes attrs;
      try {
        attrs = readAttributes();
      } catch (IOException e) {
        // Failed to get attributes; we don't know the size.
        return Optional.absent();
      }

      // Don't return a size for directories or symbolic links; their sizes are implementation
      // specific and they can't be read as bytes using the read methods anyway.
      if (attrs.isDirectory() || attrs.isSymbolicLink()) {
        return Optional.absent();
      }

      return Optional.of(attrs.size());
    }

    @Override
    public long size() throws IOException {
      BasicFileAttributes attrs = readAttributes();

      // Don't return a size for directories or symbolic links; their sizes are implementation
      // specific and they can't be read as bytes using the read methods anyway.
      if (attrs.isDirectory()) {
        throw new IOException("can't read: is a directory");
      } else if (attrs.isSymbolicLink()) {
        throw new IOException("can't read: is a symbolic link");
      }

      return attrs.size();
    }

    @Override
    public byte[] read() throws IOException {
      try (SeekableByteChannel channel = Files.newByteChannel(path, options)) {
        return com.google.common.io.Files.readFile(
            Channels.newInputStream(channel), channel.size());
      }
    }

    @Override
    public CharSource asCharSource(Charset charset) {
      if (options.length == 0) {
        // If no OpenOptions were passed, delegate to Files.lines, which could have performance
        // advantages. (If OpenOptions were passed we can't, because Files.lines doesn't have an
        // overload taking OpenOptions, meaning we can't guarantee the same behavior w.r.t. things
        // like following/not following symlinks.
        return new AsCharSource(charset) {
          @SuppressWarnings("FilesLinesLeak") // the user needs to close it in this case
          @Override
          public Stream<String> lines() throws IOException {
            return Files.lines(path, charset);
          }
        };
      }

      return super.asCharSource(charset);
    }

    @Override
    public String toString() {
      return "MoreFiles.asByteSource(" + path + ", " + Arrays.toString(options) + ")";
    }
  }

  /**
   * Returns a view of the given {@code path} as a {@link ByteSink}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned sink and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
   * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
   * WRITE} options.
   */
  public static ByteSink asByteSink(Path path, OpenOption... options) {
    return new PathByteSink(path, options);
  }

  private static final class PathByteSink extends ByteSink {

    private final Path path;
    private final OpenOption[] options;

    private PathByteSink(Path path, OpenOption... options) {
      this.path = checkNotNull(path);
      this.options = options.clone();
      // TODO(cgdecker): validate the provided options... for example, just READ seems wrong
    }

    @Override
    public OutputStream openStream() throws IOException {
      return Files.newOutputStream(path, options);
    }

    @Override
    public String toString() {
      return "MoreFiles.asByteSink(" + path + ", " + Arrays.toString(options) + ")";
    }
  }

  /**
   * Returns a view of the given {@code path} as a {@link CharSource} using the given {@code
   * charset}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned source and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#READ READ} option.
   */
  public static CharSource asCharSource(Path path, Charset charset, OpenOption... options) {
    return asByteSource(path, options).asCharSource(charset);
  }

  /**
   * Returns a view of the given {@code path} as a {@link CharSink} using the given {@code
   * charset}.
   *
   * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
   * and may affect the behavior of the returned sink and the streams it provides. See {@link
   * StandardOpenOption} for the standard options that may be provided. Providing no options is
   * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
   * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
   * WRITE} options.
   */
  public static CharSink asCharSink(Path path, Charset charset, OpenOption... options) {
    return asByteSink(path, options).asCharSink(charset);
  }

  /**
   * Returns an immutable list of paths to the files contained in the given directory.
   *
   * @throws NoSuchFileException if the file does not exist <i>(optional specific exception)</i>
   * @throws NotDirectoryException if the file could not be opened because it is not a directory
   *     <i>(optional specific exception)</i>
   * @throws IOException if an I/O error occurs
   */
  public static ImmutableList<Path> listFiles(Path dir) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      return ImmutableList.copyOf(stream);
    } catch (DirectoryIteratorException e) {
      throw e.getCause();
    }
  }

  /**
   * Returns a {@link TreeTraverser} for traversing a directory tree. The returned traverser
   * attempts to avoid following symbolic links to directories. However, the traverser cannot
   * guarantee that it will not follow symbolic links to directories as it is possible for a
   * directory to be replaced with a symbolic link between checking if the file is a directory and
   * actually reading the contents of that directory.
   *
   * <p>Note that if the {@link Path} passed to one of the traversal methods does not exist, no
   * exception will be thrown and the returned {@link Iterable} will contain a single element: that
   * path.
   *
   * <p>{@link DirectoryIteratorException}  may be thrown when iterating {@link Iterable} instances
   * created by this traverser if an {@link IOException} is thrown by a call to
   * {@link #listFiles(Path)}.
   */
  public static TreeTraverser<Path> directoryTreeTraverser() {
    return DirectoryTreeTraverser.INSTANCE;
  }

  private static final class DirectoryTreeTraverser extends TreeTraverser<Path> {

    private static final DirectoryTreeTraverser INSTANCE = new DirectoryTreeTraverser();

    @Override
    public Iterable<Path> children(Path dir) {
      if (Files.isDirectory(dir, NOFOLLOW_LINKS)) {
        try {
          return listFiles(dir);
        } catch (IOException e) {
          // the exception thrown when iterating a DirectoryStream if an I/O exception occurs
          throw new DirectoryIteratorException(e);
        }
      }
      return ImmutableList.of();
    }
  }

  /**
   * Returns a predicate that returns the result of {@link java.nio.file.Files#isDirectory(Path,
   * LinkOption...)} on input paths with the given link options.
   */
  public static Predicate<Path> isDirectory(LinkOption... options) {
    final LinkOption[] optionsCopy = options.clone();
    return new Predicate<Path>() {
      @Override
      public boolean apply(Path input) {
        return Files.isDirectory(input, optionsCopy);
      }

      @Override
      public String toString() {
        return "MoreFiles.isDirectory(" + Arrays.toString(optionsCopy) + ")";
      }
    };
  }

  /**
   * Returns a predicate that returns the result of {@link java.nio.file.Files#isRegularFile(Path,
   * LinkOption...)} on input paths with the given link options.
   */
  public static Predicate<Path> isRegularFile(LinkOption... options) {
    final LinkOption[] optionsCopy = options.clone();
    return new Predicate<Path>() {
      @Override
      public boolean apply(Path input) {
        return Files.isRegularFile(input, optionsCopy);
      }

      @Override
      public String toString() {
        return "MoreFiles.isRegularFile(" + Arrays.toString(optionsCopy) + ")";
      }
    };
  }

  /**
   * Returns true if the files located by the given paths exist, are not directories, and contain
   * the same bytes.
   *
   * @throws IOException if an I/O error occurs
   * @since 22.0
   */
  public static boolean equal(Path path1, Path path2) throws IOException {
    checkNotNull(path1);
    checkNotNull(path2);
    if (Files.isSameFile(path1, path2)) {
      return true;
    }

    /*
     * Some operating systems may return zero as the length for files denoting system-dependent
     * entities such as devices or pipes, in which case we must fall back on comparing the bytes
     * directly.
     */
    ByteSource source1 = asByteSource(path1);
    ByteSource source2 = asByteSource(path2);
    long len1 = source1.sizeIfKnown().or(0L);
    long len2 = source2.sizeIfKnown().or(0L);
    if (len1 != 0 && len2 != 0 && len1 != len2) {
      return false;
    }
    return source1.contentEquals(source2);
  }

  /**
   * Like the unix command of the same name, creates an empty file or updates the last modified
   * timestamp of the existing file at the given path to the current system time.
   */
  public static void touch(Path path) throws IOException {
    checkNotNull(path);

    try {
      Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
    } catch (NoSuchFileException e) {
      try {
        Files.createFile(path);
      } catch (FileAlreadyExistsException ignore) {
        // The file didn't exist when we called setLastModifiedTime, but it did when we called
        // createFile, so something else created the file in between. The end result is
        // what we wanted: a new file that probably has its last modified time set to approximately
        // now. Or it could have an arbitrary last modified time set by the creator, but that's no
        // different than if another process set its last modified time to something else after we
        // created it here.
      }
    }
  }

  /**
   * Creates any necessary but nonexistent parent directories of the specified path. Note that if
   * this operation fails, it may have succeeded in creating some (but not all) of the necessary
   * parent directories. The parent directory is created with the given {@code attrs}.
   *
   * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
   *                     directories of the specified file could not be created.
   */
  public static void createParentDirectories(
      Path path, FileAttribute<?>... attrs) throws IOException {
    // Interestingly, unlike File.getCanonicalFile(), Path/Files provides no way of getting the
    // canonical (absolute, normalized, symlinks resolved, etc.) form of a path to a nonexistent
    // file. getCanonicalFile() can at least get the canonical form of the part of the path which
    // actually exists and then append the normalized remainder of the path to that.
    Path normalizedAbsolutePath = path.toAbsolutePath().normalize();
    Path parent = normalizedAbsolutePath.getParent();
    if (parent == null) {
       // The given directory is a filesystem root. All zero of its ancestors exist. This doesn't
       // mean that the root itself exists -- consider x:\ on a Windows machine without such a
       // drive -- or even that the caller can create it, but this method makes no such guarantees
       // even for non-root files.
      return;
    }

    // Check if the parent is a directory first because createDirectories will fail if the parent
    // exists and is a symlink to a directory... we'd like for this to succeed in that case.
    // (I'm kind of surprised that createDirectories would fail in that case; doesn't seem like
    // what you'd want to happen.)
    if (!Files.isDirectory(parent)) {
      Files.createDirectories(parent, attrs);
      if (!Files.isDirectory(parent)) {
        throw new IOException("Unable to create parent directories of " + path);
      }
    }
  }

  /**
   * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> for
   * the file at the given path, or the empty string if the file has no extension. The result does
   * not include the '{@code .}'.
   *
   * <p><b>Note:</b> This method simply returns everything after the last '{@code .}' in the file's
   * name as determined by {@link Path#getFileName}. It does not account for any filesystem-specific
   * behavior that the {@link Path} API does not already account for. For example, on NTFS it will
   * report {@code "txt"} as the extension for the filename {@code "foo.exe:.txt"} even though NTFS
   * will drop the {@code ":.txt"} part of the name when the file is actually created on the
   * filesystem due to NTFS's <a href="https://goo.gl/vTpJi4">Alternate Data Streams</a>.
   */
  public static String getFileExtension(Path path) {
    Path name = path.getFileName();

    // null for empty paths and root-only paths
    if (name == null) {
      return "";
    }

    String fileName = name.toString();
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
  }

  /**
   * Returns the file name without its
   * <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
   * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
   */
  public static String getNameWithoutExtension(Path path) {
    Path name = path.getFileName();

    // null for empty paths and root-only paths
    if (name == null) {
      return "";
    }

    String fileName = name.toString();
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
  }

  /**
   * Deletes the file or directory at the given {@code path} recursively. Deletes symbolic links,
   * not their targets (subject to the caveat below).
   *
   * <p>If an I/O exception occurs attempting to read, open or delete any file under the given
   * directory, this method skips that file and continues. All such exceptions are collected and,
   * after attempting to delete all files, an {@code IOException} is thrown containing those
   * exceptions as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
   *
   * <h2>Warning: Security of recursive deletes</h2>
   *
   * <p>On a file system that supports symbolic links and does <i>not</i> support
   * {@link SecureDirectoryStream}, it is possible for a recursive delete to delete files and
   * directories that are <i>outside</i> the directory being deleted. This can happen if, after
   * checking that a file is a directory (and not a symbolic link), that directory is replaced by a
   * symbolic link to an outside directory before the call that opens the directory to read its
   * entries.
   *
   * <p>By default, this method throws {@link InsecureRecursiveDeleteException} if it can't
   * guarantee the security of recursive deletes. If you wish to allow the recursive deletes
   * anyway, pass {@link RecursiveDeleteOption#ALLOW_INSECURE} to this method to override that
   * behavior.
   *
   * @throws NoSuchFileException if {@code path} does not exist <i>(optional specific
   *     exception)</i>
   * @throws InsecureRecursiveDeleteException if the security of recursive deletes can't be
   *     guaranteed for the file system and {@link RecursiveDeleteOption#ALLOW_INSECURE} was not
   *     specified
   * @throws IOException if {@code path} or any file in the subtree rooted at it can't be deleted
   *     for any reason
   */
  public static void deleteRecursively(
      Path path, RecursiveDeleteOption... options) throws IOException {
    Path parentPath = getParentPath(path);
    if (parentPath == null) {
      throw new FileSystemException(path.toString(), null, "can't delete recursively");
    }

    Collection<IOException> exceptions = null; // created lazily if needed
    try {
      boolean sdsSupported = false;
      try (DirectoryStream<Path> parent = Files.newDirectoryStream(parentPath)) {
        if (parent instanceof SecureDirectoryStream) {
          sdsSupported = true;
          exceptions = deleteRecursivelySecure(
              (SecureDirectoryStream<Path>) parent, path.getFileName());
        }
      }

      if (!sdsSupported) {
        checkAllowsInsecure(path, options);
        exceptions = deleteRecursivelyInsecure(path);
      }
    } catch (IOException e) {
      if (exceptions == null) {
        throw e;
      } else {
        exceptions.add(e);
      }
    }

    if (exceptions != null) {
      throwDeleteFailed(path, exceptions);
    }
  }

  /**
   * Deletes all files within the directory at the given {@code path}
   * {@linkplain #deleteRecursively recursively}. Does not delete the directory itself. Deletes
   * symbolic links, not their targets (subject to the caveat below). If {@code path} itself is
   * a symbolic link to a directory, that link is followed and the contents of the directory it
   * targets are deleted.
   *
   * <p>If an I/O exception occurs attempting to read, open or delete any file under the given
   * directory, this method skips that file and continues. All such exceptions are collected and,
   * after attempting to delete all files, an {@code IOException} is thrown containing those
   * exceptions as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
   *
   * <h2>Warning: Security of recursive deletes</h2>
   *
   * <p>On a file system that supports symbolic links and does <i>not</i> support
   * {@link SecureDirectoryStream}, it is possible for a recursive delete to delete files and
   * directories that are <i>outside</i> the directory being deleted. This can happen if, after
   * checking that a file is a directory (and not a symbolic link), that directory is replaced by a
   * symbolic link to an outside directory before the call that opens the directory to read its
   * entries.
   *
   * <p>By default, this method throws {@link InsecureRecursiveDeleteException} if it can't
   * guarantee the security of recursive deletes. If you wish to allow the recursive deletes
   * anyway, pass {@link RecursiveDeleteOption#ALLOW_INSECURE} to this method to override that
   * behavior.
   *
   * @throws NoSuchFileException if {@code path} does not exist <i>(optional specific
   *     exception)</i>
   * @throws NotDirectoryException if the file at {@code path} is not a directory <i>(optional
   *     specific exception)</i>
   * @throws InsecureRecursiveDeleteException if the security of recursive deletes can't be
   *     guaranteed for the file system and {@link RecursiveDeleteOption#ALLOW_INSECURE} was not
   *     specified
   * @throws IOException if one or more files can't be deleted for any reason
   */
  public static void deleteDirectoryContents(
      Path path, RecursiveDeleteOption... options) throws IOException {
    Collection<IOException> exceptions = null; // created lazily if needed
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      if (stream instanceof SecureDirectoryStream) {
        SecureDirectoryStream<Path> sds = (SecureDirectoryStream<Path>) stream;
        exceptions = deleteDirectoryContentsSecure(sds);
      } else {
        checkAllowsInsecure(path, options);
        exceptions = deleteDirectoryContentsInsecure(stream);
      }
    } catch (IOException e) {
      if (exceptions == null) {
        throw e;
      } else {
        exceptions.add(e);
      }
    }

    if (exceptions != null) {
      throwDeleteFailed(path, exceptions);
    }
  }

  /**
   * Secure recursive delete using {@code SecureDirectoryStream}. Returns a collection of
   * exceptions that occurred or null if no exceptions were thrown.
   */
  @Nullable
  private static Collection<IOException> deleteRecursivelySecure(
      SecureDirectoryStream<Path> dir, Path path) {
    Collection<IOException> exceptions = null;
    try {
      if (isDirectory(dir, path, NOFOLLOW_LINKS)) {
        try (SecureDirectoryStream<Path> childDir = dir.newDirectoryStream(path, NOFOLLOW_LINKS)) {
          exceptions = deleteDirectoryContentsSecure(childDir);
        }

        // If exceptions is not null, something went wrong trying to delete the contents of the
        // directory, so we shouldn't try to delete the directory as it will probably fail.
        if (exceptions == null) {
          dir.deleteDirectory(path);
        }
      } else {
        dir.deleteFile(path);
      }

      return exceptions;
    } catch (IOException e) {
      return addException(exceptions, e);
    }
  }

  /**
   * Secure method for deleting the contents of a directory using {@code SecureDirectoryStream}.
   * Returns a collection of exceptions that occurred or null if no exceptions were thrown.
   */
  @Nullable
  private static Collection<IOException> deleteDirectoryContentsSecure(
      SecureDirectoryStream<Path> dir) {
    Collection<IOException> exceptions = null;
    try {
      for (Path path : dir) {
        exceptions = concat(exceptions, deleteRecursivelySecure(dir, path.getFileName()));
      }

      return exceptions;
    } catch (DirectoryIteratorException e) {
      return addException(exceptions, e.getCause());
    }
  }

  /**
   * Insecure recursive delete for file systems that don't support {@code SecureDirectoryStream}.
   * Returns a collection of exceptions that occurred or null if no exceptions were thrown.
   */
  @Nullable
  private static Collection<IOException> deleteRecursivelyInsecure(Path path) {
    Collection<IOException> exceptions = null;
    try {
      if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
          exceptions = deleteDirectoryContentsInsecure(stream);
        }
      }

      // If exceptions is not null, something went wrong trying to delete the contents of the
      // directory, so we shouldn't try to delete the directory as it will probably fail.
      if (exceptions == null) {
        Files.delete(path);
      }

      return exceptions;
    } catch (IOException e) {
      return addException(exceptions, e);
    }
  }

  /**
   * Simple, insecure method for deleting the contents of a directory for file systems that don't
   * support {@code SecureDirectoryStream}. Returns a collection of exceptions that occurred or
   * null if no exceptions were thrown.
   */
  @Nullable
  private static Collection<IOException> deleteDirectoryContentsInsecure(
      DirectoryStream<Path> dir) {
    Collection<IOException> exceptions = null;
    try {
      for (Path entry : dir) {
        exceptions = concat(exceptions, deleteRecursivelyInsecure(entry));
      }

      return exceptions;
    } catch (DirectoryIteratorException e) {
      return addException(exceptions, e.getCause());
    }
  }

  /**
   * Returns a path to the parent directory of the given path. If the path actually has a parent
   * path, this is simple. Otherwise, we need to do some trickier things. Returns null if the path
   * is a root or is the empty path.
   */
  @Nullable
  private static Path getParentPath(Path path) {
    Path parent = path.getParent();

    // Paths that have a parent:
    if (parent != null) {
      // "/foo" ("/")
      // "foo/bar" ("foo")
      // "C:\foo" ("C:\")
      // "\foo" ("\" - current drive for process on Windows)
      // "C:foo" ("C:" - working dir of drive C on Windows)
      return parent;
    }

    // Paths that don't have a parent:
    if (path.getNameCount() == 0) {
      // "/", "C:\", "\" (no parent)
      // "" (undefined, though typically parent of working dir)
      // "C:" (parent of working dir of drive C on Windows)
      //
      // For working dir paths ("" and "C:"), return null because:
      //   A) it's not specified that "" is the path to the working directory.
      //   B) if we're getting this path for recursive delete, it's typically not possible to
      //      delete the working dir with a relative path anyway, so it's ok to fail.
      //   C) if we're getting it for opening a new SecureDirectoryStream, there's no need to get
      //      the parent path anyway since we can safely open a DirectoryStream to the path without
      //      worrying about a symlink.
      return null;
    } else {
      // "foo" (working dir)
      return path.getFileSystem().getPath(".");
    }
  }

  /**
   * Checks that the given options allow an insecure delete, throwing an exception if not.
   */
  private static void checkAllowsInsecure(
      Path path, RecursiveDeleteOption[] options) throws InsecureRecursiveDeleteException {
    if (!Arrays.asList(options).contains(RecursiveDeleteOption.ALLOW_INSECURE)) {
      throw new InsecureRecursiveDeleteException(path.toString());
    }
  }

  /**
   * Returns whether or not the file with the given name in the given dir is a directory.
   */
  private static boolean isDirectory(
      SecureDirectoryStream<Path> dir, Path name, LinkOption... options) throws IOException {
    return dir.getFileAttributeView(name, BasicFileAttributeView.class, options)
        .readAttributes()
        .isDirectory();
  }

  /**
   * Adds the given exception to the given collection, creating the collection if it's null.
   * Returns the collection.
   */
  private static Collection<IOException> addException(
      @Nullable Collection<IOException> exceptions, IOException e) {
    if (exceptions == null) {
      exceptions = new ArrayList<>(); // don't need Set semantics
    }
    exceptions.add(e);
    return exceptions;
  }

  /**
   * Concatenates the contents of the two given collections of exceptions. If either collection is
   * null, the other collection is returned. Otherwise, the elements of {@code other} are added to
   * {@code exceptions} and {@code exceptions} is returned.
   */
  @Nullable
  private static Collection<IOException> concat(
      @Nullable Collection<IOException> exceptions, @Nullable Collection<IOException> other) {
    if (exceptions == null) {
      return other;
    } else if (other != null) {
      exceptions.addAll(other);
    }
    return exceptions;
  }

  /**
   * Throws an exception indicating that one or more files couldn't be deleted. The thrown
   * exception contains all the exceptions in the given collection as suppressed exceptions.
   */
  private static void throwDeleteFailed(
      Path path, Collection<IOException> exceptions) throws FileSystemException {
    // TODO(cgdecker): Should there be a custom exception type for this?
    // Also, should we try to include the Path of each file we may have failed to delete rather
    // than just the exceptions that occurred?
    FileSystemException deleteFailed = new FileSystemException(path.toString(), null,
        "failed to delete one or more files; see suppressed exceptions for details");
    for (IOException e : exceptions) {
      deleteFailed.addSuppressed(e);
    }
    throw deleteFailed;
  }
}
