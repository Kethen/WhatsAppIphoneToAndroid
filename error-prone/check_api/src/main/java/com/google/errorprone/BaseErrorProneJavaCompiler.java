/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.RefactoringCollection.RefactoringResult;
import com.google.errorprone.scanner.ErrorProneScannerTransformer;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.WriterKind;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/** An Error Prone compiler that implements {@link javax.tools.JavaCompiler}. */
public class BaseErrorProneJavaCompiler implements JavaCompiler {

  private final JavaCompiler javacTool;
  private final ScannerSupplier scannerSupplier;

  public BaseErrorProneJavaCompiler(ScannerSupplier scannerSupplier) {
    this(JavacTool.create(), scannerSupplier);
  }

  BaseErrorProneJavaCompiler(JavaCompiler javacTool, ScannerSupplier scannerSupplier) {
    this.javacTool = javacTool;
    this.scannerSupplier = scannerSupplier;
  }

  @Override
  public CompilationTask getTask(
      Writer out,
      JavaFileManager fileManager,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      Iterable<String> options,
      Iterable<String> classes,
      Iterable<? extends JavaFileObject> compilationUnits) {
    ErrorProneOptions errorProneOptions = ErrorProneOptions.processArgs(options);
    List<String> remainingOptions = Arrays.asList(errorProneOptions.getRemainingArgs());
    ImmutableList<String> javacOpts = ImmutableList.copyOf(remainingOptions);
    javacOpts = defaultToLatestSupportedLanguageLevel(javacOpts);
    javacOpts = setCompilePolicyToByFile(javacOpts);
    final JavacTaskImpl task =
        (JavacTaskImpl)
            javacTool.getTask(
                out, fileManager, diagnosticListener, javacOpts, classes, compilationUnits);
    setupMessageBundle(task.getContext());
    RefactoringCollection[] refactoringCollection = {null};
    task.addTaskListener(
        createAnalyzer(
            scannerSupplier, errorProneOptions, task.getContext(), refactoringCollection));
    if (refactoringCollection[0] != null) {
      task.addTaskListener(new RefactoringTask(task.getContext(), refactoringCollection[0]));
    }
    return task;
  }

  @Override
  public StandardJavaFileManager getStandardFileManager(
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      Locale locale,
      Charset charset) {
    return javacTool.getStandardFileManager(diagnosticListener, locale, charset);
  }

  @Override
  public int isSupportedOption(String option) {
    int numberOfArgs = javacTool.isSupportedOption(option);
    if (numberOfArgs != -1) {
      return numberOfArgs;
    }
    return ErrorProneOptions.isSupportedOption(option);
  }

  @Override
  public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
    return javacTool.run(in, out, err, arguments);
  }

  @Override
  public Set<SourceVersion> getSourceVersions() {
    Set<SourceVersion> filtered = EnumSet.noneOf(SourceVersion.class);
    for (SourceVersion version : javacTool.getSourceVersions()) {
      if (version.compareTo(SourceVersion.RELEASE_6) >= 0) {
        filtered.add(version);
      }
    }
    return filtered;
  }

  /**
   * Default to compiling with the same -source and -target as the host's javac.
   *
   * <p>This prevents, e.g., targeting Java 8 by default when using error-prone on JDK7.
   */
  private static ImmutableList<String> defaultToLatestSupportedLanguageLevel(
      ImmutableList<String> args) {

    String overrideLanguageLevel;
    switch (JAVA_SPECIFICATION_VERSION.value()) {
      case "1.7":
        overrideLanguageLevel = "7";
        break;
      case "1.8":
        overrideLanguageLevel = "8";
        break;
      default:
        return args;
    }

    return ImmutableList.<String>builder()
        .add(
            // suppress xlint 'options' warnings to avoid diagnostics like:
            // 'bootstrap class path not set in conjunction with -source 1.7'
            "-Xlint:-options", "-source", overrideLanguageLevel, "-target", overrideLanguageLevel)
        .addAll(args)
        .build();
  }

  /**
   * Sets javac's {@code -XDcompilePolicy} flag to ensure that all classes in a file are attributed
   * before any of them are lowered. Error Prone depends on this behavior when analyzing files that
   * contain multiple top-level classes.
   *
   * @throws InvalidCommandLineOptionException if the {@code -XDcompilePolicy} flag is passed in the
   *     existing arguments with an unsupported value
   */
  private static ImmutableList<String> setCompilePolicyToByFile(ImmutableList<String> args) {
    for (String arg : args) {
      if (arg.startsWith("-XDcompilePolicy")) {
        String value = arg.substring(arg.indexOf('=') + 1);
        switch (value) {
          case "byfile":
          case "simple":
            break;
          default:
            throw new InvalidCommandLineOptionException(
                String.format("-XDcompilePolicy=%s is not supported by Error Prone", value));
        }
        // don't do anything if a valid policy is already set
        return args;
      }
    }
    return ImmutableList.<String>builder().addAll(args).add("-XDcompilePolicy=byfile").build();
  }

  /** Registers our message bundle. */
  public static void setupMessageBundle(Context context) {
    ResourceBundle bundle = ResourceBundle.getBundle("com.google.errorprone.errors");
    JavacMessages.instance(context).add(l -> bundle);
  }

  static ErrorProneAnalyzer createAnalyzer(
      ScannerSupplier scannerSupplier,
      ErrorProneOptions epOptions,
      Context context,
      RefactoringCollection[] refactoringCollection) {
    if (!epOptions.patchingOptions().doRefactor()) {
      return ErrorProneAnalyzer.createByScanningForPlugins(scannerSupplier, epOptions, context);
    }
    refactoringCollection[0] = RefactoringCollection.refactor(epOptions.patchingOptions());

    // Refaster refactorer or using builtin checks
    CodeTransformer codeTransformer =
        epOptions
            .patchingOptions()
            .customRefactorer()
            .or(
                () -> {
                  ScannerSupplier toUse = ErrorPronePlugins.loadPlugins(scannerSupplier, context);
                  Set<String> namedCheckers = epOptions.patchingOptions().namedCheckers();
                  if (!namedCheckers.isEmpty()) {
                    toUse =
                        toUse.filter(bci -> namedCheckers.contains(bci.canonicalName()));
                  }
                  return ErrorProneScannerTransformer.create(toUse.applyOverrides(epOptions).get());
                })
            .get();

    return ErrorProneAnalyzer.createWithCustomDescriptionListener(
        codeTransformer, epOptions, context, refactoringCollection[0]);
  }

  static class RefactoringTask implements TaskListener {

    private final Context context;
    private final RefactoringCollection refactoringCollection;

    public RefactoringTask(Context context, RefactoringCollection refactoringCollection) {
      this.context = context;
      this.refactoringCollection = refactoringCollection;
    }

    @Override
    public void started(TaskEvent event) {}

    @Override
    public void finished(TaskEvent event) {
      if (event.getKind() != Kind.GENERATE) {
        return;
      }
      RefactoringResult refactoringResult;
      try {
        refactoringResult = refactoringCollection.applyChanges(event.getSourceFile().toUri());
      } catch (Exception e) {
        PrintWriter out = Log.instance(context).getWriter(WriterKind.ERROR);
        out.println(e.getMessage());
        out.flush();
        return;
      }
      if (refactoringResult.type() == RefactoringCollection.RefactoringResultType.CHANGED) {
        PrintWriter out = Log.instance(context).getWriter(WriterKind.NOTICE);
        out.println(refactoringResult.message());
        out.flush();
      }
    }
  }
}
