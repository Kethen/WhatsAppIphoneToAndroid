/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author gak@google.com (Gregory Kick) */
@RunWith(JUnit4.class)
public class RemoveUnusedImportsTest {
  private BugCheckerRefactoringTestHelper testHelper;

  @Before
  public void setUp() {
    this.testHelper =
        BugCheckerRefactoringTestHelper.newInstance(new RemoveUnusedImports(), getClass());
  }

  @Test
  public void basicUsageTest() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import static java.util.Collections.emptyList;",
            "import static java.util.Collections.emptySet;",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "",
            "import java.util.ArrayList;",
            "import java.util.Collection;",
            "import java.util.Collections;",
            "import java.util.HashSet;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.Set;",
            "import java.util.UUID;",
            "public class Test {",
            "  private final Object object;",
            "",
            "  Test(Object object) {",
            "    this.object = checkNotNull(object);",
            "  }",
            "",
            "  Set<UUID> someMethod(Collection<UUID> collection) {",
            "    if (collection.isEmpty()) {",
            "      return emptySet();",
            "    }",
            "    return new HashSet<>(collection);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.util.Collections.emptySet;",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "",
            "import java.util.Collection;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "import java.util.UUID;",
            "public class Test {",
            "  private final Object object;",
            "",
            "  Test(Object object) {",
            "    this.object = checkNotNull(object);",
            "  }",
            "",
            "  Set<UUID> someMethod(Collection<UUID> collection) {",
            "    if (collection.isEmpty()) {",
            "      return emptySet();",
            "    }",
            "    return new HashSet<>(collection);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void useInSelect() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Map;",
            "import java.util.Map.Entry;",
            "public class Test {",
            "  Map.Entry<String, String> e;",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Map;",
            "public class Test {",
            "  Map.Entry<String, String> e;",
            "}")
        .doTest();
  }

  @Test
  public void useInJavadocSee() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import java.util.Map;",
            "/** @see Map */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocSeeSelect() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import java.util.Map;",
            "/** @see Map#get */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocLink() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "import java.util.Map;",
            "/** {@link Map} */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocLink_selfReferenceDoesNotBreak() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java", //
            "/** {@link #blah} */",
            "public class Test {",
            "  void blah() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useInJavadocLinkSelect() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.Map;",
            "/** {@link Map#get} */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void diagnosticPosition() throws IOException {
    CompilationTestHelper.newInstance(RemoveUnusedImports.class, getClass())
        .addSourceLines(
            "Test.java",
            "package test;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "// BUG: Diagnostic contains:",
            "import java.util.LinkedList;",
            "public class Test {",
            "  List<String> xs = new ArrayList<>();",
            "}")
        .doTest();
  }

  @Test
  public void useInJavadocParameter() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.List;",
            "import java.util.Collection;",
            "/** {@link List#containsAll(Collection)}  */",
            "public class Test {}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void qualifiedJavadoc() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.Map.Entry;",
            "/** {@link java.util.List} {@link Map.Entry} */",
            "public class Test {}")
        .addOutputLines(
            "out/Test.java",
            "import java.util.Map;",
            "/** {@link java.util.List} {@link Map.Entry} */",
            "public class Test {}")
        .doTest();
  }

  @Test
  public void parameterErasure() throws IOException {
    testHelper
        .addInputLines(
            "in/A.java",
            "import java.util.Collection;",
            "public class A<T extends Collection> {",
            "  public void foo(T t) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/B.java",
            "import java.util.Collection;",
            "import java.util.List;",
            "public class B extends A<List> {",
            "  /** {@link #foo(Collection)} {@link #foo(List)} */",
            "  public void foo(List t) {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void atSee() throws IOException {
    testHelper
        .addInputLines(
            "Lib.java",
            "import java.nio.file.Path;",
            "class Lib {",
            "  static void f(Path... ps) {}",
            "}")
        .expectUnchanged()
        .addInputLines(
            "in/Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  /** @see Lib#f(Path[]) */",
            "  void f() {}",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void multipleTopLevelClasses() throws IOException {
    CompilationTestHelper.newInstance(RemoveUnusedImports.class, getClass())
        .addSourceLines(
            "MultipleTopLevelClasses.java",
            "import java.util.List;",
            "import java.util.Set;",
            "public class MultipleTopLevelClasses { List x; }",
            "class Evil { Set x; }")
        .doTest();
  }
}
