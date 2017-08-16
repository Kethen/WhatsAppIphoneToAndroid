/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnnecessaryStaticImport}Test */
@RunWith(JUnit4.class)
public class UnnecessaryStaticImportTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(UnnecessaryStaticImport.class, getClass());
  }

  @Test
  public void positive() throws Exception {
    compilationHelper
        .addSourceLines(
            "b/B.java", "package b;", "public class B {", "  public static class Inner {}", "}")
        .addSourceLines(
            "Test.java",
            "// BUG: Diagnostic contains: import b.B.Inner;",
            "import static b.B.Inner;",
            "class Test {}")
        .doTest();
  }

  @Test
  public void positiveRename() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java", "package a;", "public class A {", "  public static class Inner {}", "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {}")
        .addSourceLines(
            "b/Test.java",
            "package b;",
            "// BUG: Diagnostic contains: import a.A.Inner;",
            "import static b.B.Inner;",
            "class Test {}")
        .doTest();
  }

  @Test
  public void negativeStaticMethod() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static class Inner {",
            "    public static void f() {}",
            "  }",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {}")
        .addSourceLines("b/Test.java", "package b;", "import static a.A.Inner.f;", "class Test {}")
        .doTest();
  }

  @Test
  public void negativeGenericTypeStaticMethod() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "package a;",
            "public class A {",
            "  public static class Inner<T> {",
            "    public static void f() {}",
            "  }",
            "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {}")
        .addSourceLines("b/Test.java", "package b;", "import static a.A.Inner.f;", "class Test {}")
        .doTest();
  }

  @Test
  public void negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java", "package a;", "public class A {", "  public static class Inner {}", "}")
        .addSourceLines("b/B.java", "package b;", "import a.A;", "public class B extends A {}")
        .addSourceLines("b/Test.java", "package b;", "import a.A.Inner;", "class Test {}")
        .doTest();
  }
}
