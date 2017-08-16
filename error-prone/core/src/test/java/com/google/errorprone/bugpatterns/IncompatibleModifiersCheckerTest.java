/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

/**
 * Unit tests for {@link IncompatibleModifiersChecker}.
 *
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class IncompatibleModifiersCheckerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(IncompatibleModifiersChecker.class, getClass())
            .addSourceLines(
                "test/NotPrivateOrFinal.java",
                "package test;",
                "import static javax.lang.model.element.Modifier.FINAL;",
                "import static javax.lang.model.element.Modifier.PRIVATE;",
                "import com.google.errorprone.annotations.IncompatibleModifiers;",
                "@IncompatibleModifiers({PRIVATE, FINAL})",
                "public @interface NotPrivateOrFinal {",
                "}")
            .addSourceLines(
                "test/NotPublicOrFinal.java",
                "package test;",
                "import static javax.lang.model.element.Modifier.FINAL;",
                "import static javax.lang.model.element.Modifier.PUBLIC;",
                "import com.google.errorprone.annotations.IncompatibleModifiers;",
                "@IncompatibleModifiers({PUBLIC, FINAL})",
                "public @interface NotPublicOrFinal {",
                "}")
            .addSourceLines(
                "test/NotAbstract.java",
                "package test;",
                "import static javax.lang.model.element.Modifier.ABSTRACT;",
                "import com.google.errorprone.annotations.IncompatibleModifiers;",
                "@IncompatibleModifiers(ABSTRACT)",
                "public @interface NotAbstract {",
                "}");
  }

  @Test
  public void testAnnotationWithIncompatibleModifierOnClassFails() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/IncompatibleModifiersTestCase.java",
            "package test;",
            "import test.NotAbstract;",
            "// BUG: Diagnostic contains: The annotation '@NotAbstract' has specified that it"
                + " should not be used together with the following modifiers: [abstract]",
            "@NotAbstract abstract class IncompatibleModifiersTestCase {",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotationWithIncompatibleModifierOnFieldFails() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/IncompatibleModifiersTestCase.java",
            "package test;",
            "import test.NotPrivateOrFinal;",
            "public class IncompatibleModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@NotPrivateOrFinal' has specified that"
                + " it should not be used together with the following modifiers: [final]",
            "  @NotPrivateOrFinal public final int n = 0;",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotationWithIncompatibleModifierOnMethodFails() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/IncompatibleModifiersTestCase.java",
            "package test;",
            "import test.NotPrivateOrFinal;",
            "public class IncompatibleModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@NotPrivateOrFinal' has specified that"
                + " it should not be used together with the following modifiers: [private]",
            "  @NotPrivateOrFinal private void foo(){}",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotationWithTwoIncompatibleModifiersFails() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/IncompatibleModifiersTestCase.java",
            "package test;",
            "import test.NotPublicOrFinal;",
            "public class IncompatibleModifiersTestCase {",
            "  // BUG: Diagnostic contains: The annotation '@NotPublicOrFinal' has specified that"
                + " it should not be used together with the following modifiers: [public, final]",
            "  @NotPublicOrFinal public static final int FOO = 0;",
            "}")
        .doTest();
  }

  @Test
  public void testAnnotationWithCompatibleModifiersSucceeds() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/IncompatibleModifiersTestCase.java",
            "package test;",
            "import test.NotAbstract;",
            "public class IncompatibleModifiersTestCase {}")
        .doTest();
  }

  @Test
  public void testGuavaAnnotation() throws Exception {
    compilationHelper
        .addSourceLines(
            "com/google/common/annotations/IncompatibleModifiers.java",
            "package com.google.common.annotations;",
            "import javax.lang.model.element.Modifier;",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target(ElementType.ANNOTATION_TYPE)",
            "public @interface IncompatibleModifiers {",
            "  Modifier[] value();",
            "}")
        .addSourceLines(
            "test/NotAbstract.java",
            "package test;",
            "import static javax.lang.model.element.Modifier.ABSTRACT;",
            "import com.google.common.annotations.IncompatibleModifiers;",
            "@IncompatibleModifiers(ABSTRACT)",
            "public @interface NotAbstract {",
            "}")
        .addSourceLines(
            "test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.NotAbstract;",
            "// BUG: Diagnostic contains: The annotation '@NotAbstract' has specified that it"
                + " should not be used together with the following modifiers: [abstract]",
            "@NotAbstract public abstract class RequiredModifiersTestCase {",
            "}")
        .doTest();
  }

  @Test
  public void testGuavaAnnotationOK() throws Exception {
    compilationHelper
        .addSourceLines(
            "com/google/common/annotations/IncompatibleModifiers.java",
            "package com.google.common.annotations;",
            "import javax.lang.model.element.Modifier;",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target(ElementType.ANNOTATION_TYPE)",
            "public @interface IncompatibleModifiers {",
            "  Modifier[] value();",
            "}")
        .addSourceLines(
            "test/NotAbstract.java",
            "package test;",
            "import static javax.lang.model.element.Modifier.ABSTRACT;",
            "import com.google.common.annotations.IncompatibleModifiers;",
            "@IncompatibleModifiers(ABSTRACT)",
            "public @interface NotAbstract {",
            "}")
        .addSourceLines(
            "test/RequiredModifiersTestCase.java",
            "package test;",
            "import test.NotAbstract;",
            "@NotAbstract public class RequiredModifiersTestCase {",
            "}")
        .doTest();
  }

  // Regression test for #313
  @Test
  public void negativeNestedAnnotations() throws Exception {
    compilationHelper
        .addSourceLines(
            "test/Test.java",
            "package test;",
            "@interface Foos {",
            "  Foo[] value();",
            "}",
            "@interface Foo {",
            "}",
            "@Foos({@Foo, @Foo}) public class Test {",
            "}")
        .doTest();
  }

  // Regression test for #313
  @Test
  public void negativePackageAnnotation() throws Exception {
    compilationHelper
        .addSourceLines(
            "testdata/Anno.java",
            "package testdata;",
            "import java.lang.annotation.Target;",
            "import java.lang.annotation.ElementType;",
            "@Target(ElementType.PACKAGE)",
            "public @interface Anno {",
            "}")
        .addSourceLines("testdata/package-info.java", "@Anno", "package testdata;")
        .doTest();
  }
}
