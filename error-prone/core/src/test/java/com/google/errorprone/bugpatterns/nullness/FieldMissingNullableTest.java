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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author kmb@google.com (Kevin Bierhoff) */
@RunWith(JUnit4.class)
public class FieldMissingNullableTest {

  @Test
  public void testLiteralNullAssignment() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "  public void reset() {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testDefiniteNullAssignment() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.message = message != null ? null : message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMaybeNullAssignment() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = x >= 0 ? null : \"negative\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignNullableMethodResult() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableMethodCallTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = toSignString(x);",
            "  }",
            "",
            "  @Nullable",
            "  private String toSignString(int x) {",
            "    return x < 0 ? \"negative\" : \"positive\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignNullableMethodCall_alternativeAnnotation() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/anno/my/Nullable.java",
            "package com.google.anno.my;",
            "public @interface Nullable {}")
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableMethodCallTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NullableMethodCallTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    message = toSignString(x);",
            "  }",
            "",
            "  @com.google.anno.my.Nullable",
            "  private String toSignString(int x) {",
            "    return x < 0 ? \"negative\" : \"positive\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignNullableField() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableFieldTest {",
            "  @Nullable private String message;",
            "  private String previous = \"\";",
            "  public void setMessage(String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    previous = this.message;",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testAssignNullableParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private String message = \"hello\";",
            "  public void apply(@Nullable String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testInitializeWithNullableParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private final String message;",
            "  public NullableParameterTest(@Nullable String message) {",
            "    // BUG: Diagnostic contains: @Nullable",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNullInitializer() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  // BUG: Diagnostic contains: @Nullable",
            "  public static final String MESSAGE = null;",
            "}")
        .doTest();
  }

  @Test
  public void testMaybeNullAssignmentInLambda() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private String message = \"hello\";",
            "  public void setMessageIfPresent(java.util.Optional<String> message) {",
            // Note this code is bogus: s is guaranteed non-null...
            "    // BUG: Diagnostic contains: @Nullable",
            "    message.ifPresent(s -> { this.message = s != null ? s : null; });",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_alreadyAnnotated() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class FieldMissingNullTest {",
            "  @Nullable String message;",
            "  public void reset() {",
            "    this.message = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_initializeWithNonNullLiteral() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private final String message;",
            "  public FieldMissingNullTest() {",
            "    message = \"hello\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullInitializer() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/FieldMissingNullTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class FieldMissingNullTest {",
            "  private String message = \"hello\";",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullMethod() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullMethodTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullMethodTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(int x) {",
            "    message = String.valueOf(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullField() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullFieldTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullFieldTest {",
            "  private String message = \"hello\";",
            "  private String previous = \"\";",
            "  public void save() {",
            "    previous = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_nonNullParameter() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NonNullParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class NonNullParameterTest {",
            "  private String message = \"hello\";",
            "  public void setMessage(String message) {",
            "    this.message = message;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_this() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/ThisTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class ThisTest {",
            "  private static ThisTest theInstance = new ThisTest();",
            "  public void makeDefault() {",
            "    this.theInstance = this;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Makes sure the check never flags methods returning a primitive. Returning null from them is a
   * bug, of course, but we're not trying to find those bugs in this check.
   */
  @Test
  public void testNegativeCases_primitiveFieldType() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/PrimitiveReturnTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "public class PrimitiveReturnTest {",
            "  private int count = (Integer) null;",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_initializeWithLambda() throws Exception {
    createCompilationTestHelper()
        .addSourceLines(
            "com/google/errorprone/bugpatterns/nullness/NullableParameterTest.java",
            "package com.google.errorprone.bugpatterns.nullness;",
            "import javax.annotation.Nullable;",
            "public class NullableParameterTest {",
            "  private String message = \"hello\";",
            "  public void setMessageIfPresent(java.util.Optional<String> message) {",
            "    message.ifPresent(s -> { this.message = s; });",
            "  }",
            "}")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper() {
    return CompilationTestHelper.newInstance(FieldMissingNullable.class, getClass());
  }
}
