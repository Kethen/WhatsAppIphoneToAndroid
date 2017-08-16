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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.Suppressibility.UNSUPPRESSIBLE;
import static com.google.errorprone.DiagnosticTestHelper.diagnosticMessage;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.errorprone.bugpatterns.ArrayEquals;
import com.google.errorprone.bugpatterns.BadShiftAmount;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.ChainingConstructorIgnoresParameter;
import com.google.errorprone.bugpatterns.Finally;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import java.io.ByteArrayOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class ErrorProneJavaCompilerTest {

  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void testIsSupportedOption() {
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler();

    // javac options should be passed through
    assertThat(compiler.isSupportedOption("-source")).isEqualTo(1);

    // error-prone options should be handled
    assertThat(compiler.isSupportedOption("-Xep:")).isEqualTo(0);
    assertThat(compiler.isSupportedOption("-XepIgnoreUnknownCheckNames")).isEqualTo(0);
    assertThat(compiler.isSupportedOption("-XepDisableWarningsInGeneratedCode")).isEqualTo(0);

    // old-style error-prone options are not supported
    assertThat(compiler.isSupportedOption("-Xepdisable:")).isEqualTo(-1);
  }

  interface JavaFileObjectDiagnosticListener extends DiagnosticListener<JavaFileObject> {}

  @Test
  public void testGetStandardJavaFileManager() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    JavaFileObjectDiagnosticListener listener = mock(JavaFileObjectDiagnosticListener.class);
    Locale locale = Locale.CANADA;

    compiler.getStandardFileManager(listener, locale, null);
    verify(mockCompiler).getStandardFileManager(listener, locale, null);
  }

  @Test
  public void testRun() {
    JavaCompiler mockCompiler = mock(JavaCompiler.class);
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler(mockCompiler);

    InputStream in = mock(InputStream.class);
    OutputStream out = mock(OutputStream.class);
    OutputStream err = mock(OutputStream.class);
    String[] arguments = {"-source", "8", "-target", "8"};

    compiler.run(in, out, err, arguments);
    verify(mockCompiler).run(in, out, err, arguments);
  }

  @Test
  public void testSourceVersion() {
    ErrorProneJavaCompiler compiler = new ErrorProneJavaCompiler();
    assertThat(compiler.getSourceVersions()).contains(SourceVersion.latest());
    assertThat(compiler.getSourceVersions()).doesNotContain(SourceVersion.RELEASE_5);
  }

  @Test
  public void fileWithErrorIntegrationTest() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[SelfAssignment]")));
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testWithDisabledCheck() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Arrays.asList("-Xep:SelfAssignment:OFF"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
  }

  @Test
  public void testWithCheckPromotedToError() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/WaitNotInLoopPositiveCases.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[WaitNotInLoop]")));
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/WaitNotInLoopPositiveCases.java"),
            Arrays.asList("-Xep:WaitNotInLoop:ERROR"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testWithCheckDemotedToWarning() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[SelfAssignment]")));
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Arrays.asList("-Xep:SelfAssignment:WARN"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testWithNonDefaultCheckOn() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/EmptyIfStatementPositiveCases.java"),
            Collections.<String>emptyList(),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics()).isEmpty();

    result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/EmptyIfStatementPositiveCases.java"),
            Arrays.asList("-Xep:EmptyIf"),
            Collections.<Class<? extends BugChecker>>emptyList());
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[EmptyIf]")));
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testBadFlagThrowsException() throws Exception {
    try {
      doCompile(
          Arrays.asList("bugpatterns/testdata/EmptyIfStatementPositiveCases.java"),
          Arrays.asList("-Xep:foo:bar:baz"),
          Collections.<Class<? extends BugChecker>>emptyList());
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected.getMessage()).contains("invalid flag");
    }
  }

  @BugPattern(
    name = "ArrayEquals",
    summary = "Reference equality used to compare arrays",
    explanation = "",
    category = JDK,
    severity = ERROR,
    suppressibility = UNSUPPRESSIBLE
  )
  public static class UnsuppressibleArrayEquals extends ArrayEquals {}

  @Test
  public void testCantDisableNonDisableableCheck() throws Exception {
    try {
      doCompile(
          Arrays.asList("bugpatterns/testdata/ArrayEqualsPositiveCases.java"),
          Arrays.asList("-Xep:ArrayEquals:OFF"),
          ImmutableList.<Class<? extends BugChecker>>of(UnsuppressibleArrayEquals.class));
      fail();
    } catch (RuntimeException expected) {
      assertThat(expected.getMessage()).contains("ArrayEquals may not be disabled");
    }
  }

  @Test
  public void testWithCustomCheckPositive() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/BadShiftAmountPositiveCases.java"),
            Collections.<String>emptyList(),
            Arrays.<Class<? extends BugChecker>>asList(BadShiftAmount.class));
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics().size()).isGreaterThan(0);
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[BadShiftAmount]")));
    assertTrue(matcher.matches(result.diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testWithCustomCheckNegative() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("bugpatterns/testdata/SelfAssignmentPositiveCases1.java"),
            Collections.<String>emptyList(),
            Arrays.<Class<? extends BugChecker>>asList(Finally.class));
    assertThat(result.succeeded).isTrue();
    assertThat(result.diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @Test
  public void testSeverityResetsAfterOverride() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();
    JavaCompiler errorProneJavaCompiler = new ErrorProneJavaCompiler();
    List<String> args =
        Lists.newArrayList(
            "-d",
            tempDir.getRoot().getAbsolutePath(),
            "-proc:none",
            "-Xep:ChainingConstructorIgnoresParameter:WARN");
    List<JavaFileObject> sources =
        fileManager.forResources(
            ChainingConstructorIgnoresParameter.class,
            "testdata/ChainingConstructorIgnoresParameterPositiveCases.java");
    fileManager.close();

    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter, fileManager, diagnosticHelper.collector, args, null, sources);
    boolean succeeded = task.call();
    assertThat(succeeded).isTrue();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[ChainingConstructorIgnoresParameter]")));
    assertTrue(matcher.matches(diagnosticHelper.getDiagnostics()));

    // reset state between compilations
    diagnosticHelper.clearDiagnostics();
    fileManager = new ErrorProneInMemoryFileManager();
    sources =
        fileManager.forResources(
            ChainingConstructorIgnoresParameter.class,
            "testdata/ChainingConstructorIgnoresParameterPositiveCases.java");
    fileManager.close();
    args.remove("-Xep:ChainingConstructorIgnoresParameter:WARN");

    task =
        errorProneJavaCompiler.getTask(
            printWriter, fileManager, diagnosticHelper.collector, args, null, sources);
    succeeded = task.call();
    assertThat(succeeded).isFalse();
    assertTrue(matcher.matches(diagnosticHelper.getDiagnostics()));
  }

  @Test
  public void testMaturityResetsAfterOverride() throws Exception {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();
    JavaCompiler errorProneJavaCompiler = new ErrorProneJavaCompiler();
    List<String> args =
        Lists.newArrayList("-d", tempDir.getRoot().getAbsolutePath(), "-proc:none", "-Xep:EmptyIf");
    List<JavaFileObject> sources =
        fileManager.forResources(
            BadShiftAmount.class, "testdata/EmptyIfStatementPositiveCases.java");
    fileManager.close();

    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter, null, diagnosticHelper.collector, args, null, sources);
    boolean succeeded = task.call();
    assertThat(succeeded).isFalse();
    Matcher<? super Iterable<Diagnostic<? extends JavaFileObject>>> matcher =
        hasItem(diagnosticMessage(containsString("[EmptyIf]")));
    assertTrue(matcher.matches(diagnosticHelper.getDiagnostics()));

    diagnosticHelper.clearDiagnostics();
    args.remove("-Xep:EmptyIf");
    task =
        errorProneJavaCompiler.getTask(
            printWriter, null, diagnosticHelper.collector, args, null, sources);
    fileManager.close();
    succeeded = task.call();
    assertThat(succeeded).isTrue();
    assertThat(diagnosticHelper.getDiagnostics()).isEmpty();
  }

  @BugPattern(
    name = "DeleteMethod",
    summary =
        "You appear to be using methods; prefer to implement all program logic inside the main"
            + " function by flipping bits in a single long[].",
    explanation = "",
    category = JDK,
    severity = ERROR,
    suppressibility = UNSUPPRESSIBLE
  )
  public static class DeleteMethod extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      MethodTree ctor = (MethodTree) Iterables.getOnlyElement(tree.getMembers());
      Preconditions.checkArgument(ASTHelpers.isGeneratedConstructor(ctor));
      return describeMatch(tree, SuggestedFix.delete(ctor));
    }
  }

  @Test
  public void testFixGeneratedConstructor() throws Exception {
    CompilationResult result =
        doCompile(
            Arrays.asList("testdata/DeleteGeneratedConstructorTestCase.java"),
            Collections.<String>emptyList(),
            ImmutableList.<Class<? extends BugChecker>>of(DeleteMethod.class));
    assertThat(result.succeeded).isFalse();
    assertThat(result.diagnosticHelper.getDiagnostics()).hasSize(1);
    assertThat(
            Iterables.getOnlyElement(result.diagnosticHelper.getDiagnostics())
                .getMessage(Locale.ENGLISH))
        .contains("AssertionError: Cannot edit synthetic AST nodes");
  }

  private static class CompilationResult {
    public final boolean succeeded;
    public final DiagnosticTestHelper diagnosticHelper;

    public CompilationResult(boolean succeeded, DiagnosticTestHelper diagnosticHelper) {
      this.succeeded = succeeded;
      this.diagnosticHelper = diagnosticHelper;
    }
  }

  private CompilationResult doCompile(
      List<String> fileNames,
      List<String> extraArgs,
      List<Class<? extends BugChecker>> customCheckers) {
    DiagnosticTestHelper diagnosticHelper = new DiagnosticTestHelper();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, UTF_8), true);
    ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

    List<String> args = Lists.newArrayList("-d", tempDir.getRoot().getAbsolutePath(), "-proc:none");
    args.addAll(extraArgs);

    JavaCompiler errorProneJavaCompiler =
        (customCheckers.isEmpty())
            ? new ErrorProneJavaCompiler()
            : new ErrorProneJavaCompiler(ScannerSupplier.fromBugCheckerClasses(customCheckers));
    JavaCompiler.CompilationTask task =
        errorProneJavaCompiler.getTask(
            printWriter,
            fileManager,
            diagnosticHelper.collector,
            args,
            null,
            fileManager.forResources(getClass(), fileNames.toArray(new String[0])));

    try {
      fileManager.close();
    } catch (IOException e) {
      throw new IOError(e);
    }
    return new CompilationResult(task.call(), diagnosticHelper);
  }
}
