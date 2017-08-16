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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ImmutableEnumChecker}Test */
@RunWith(JUnit4.class)
public class ImmutableEnumCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ImmutableEnumChecker.class, getClass());

  @Test
  public void nonFinalField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "enum Enum {",
            "  ONE(1), TWO(2);",
            "  // BUG: Diagnostic contains: final int x;'",
            "  int x;",
            "  private Enum(int x) {",
            "    this.x = x;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableEnum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "enum Enum {",
            "  ONE(1), TWO(2);",
            "  final ImmutableSet<Integer> xs;",
            "  private Enum(Integer... xs) {",
            "    this.xs = ImmutableSet.copyOf(xs);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalMutableField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "enum Enum {",
            "  ONE(1), TWO(2);",
            "  // BUG: Diagnostic contains: "
                + "enums should only have immutable fields, 'Set' is mutable",
            "  final Set<Integer> xs;",
            "  private Enum(Integer... xs) {",
            "    this.xs = new HashSet<>(Arrays.asList(xs));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotatedEnum() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "// BUG: Diagnostic contains: enums are immutable by default",
            "@Immutable",
            "enum Enum {",
            "  ONE, TWO",
            "}")
        .doTest();
  }

  @Test
  public void annotatedEnumThatImplementsImmutableInterface() {
    compilationHelper
        .addSourceLines(
            "MyInterface.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface MyInterface {}")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable",
            "enum Enum implements MyInterface {",
            "  ONE, TWO",
            "}")
        .doTest();
  }

  @Test
  public void annotatedEnumThatImplementsImmutableInterfaceWithOverrides() {
    compilationHelper
        .addSourceLines(
            "MyInterface.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable interface MyInterface { void bar(); }")
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Immutable;",
            "@Immutable",
            "enum Enum implements MyInterface {",
            "  ONE { public void bar() {} },",
            "  TWO { public void bar() {} }",
            "}")
        .doTest();
  }

  @Test
  public void mutableFieldType() {
    compilationHelper
        .addSourceLines("Foo.java", "class Foo {", "}")
        .addSourceLines(
            "Test.java",
            "import java.util.Arrays;",
            "import java.util.HashSet;",
            "import java.util.Set;",
            "enum Enum {",
            "  ONE(new Foo()), TWO(new Foo());",
            "  // BUG: Diagnostic contains:"
                + " the declaration of type 'Foo' is not annotated @Immutable",
            "  final Foo f;",
            "  private Enum(Foo f) {",
            "    this.f = f;",
            "  }",
            "}")
        .doTest();
  }
}
