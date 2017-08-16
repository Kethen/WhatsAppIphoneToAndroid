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
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author avenet@google.com (Arnaud J. Venet) */
@RunWith(JUnit4.class)
public class EqualsIncompatibleTypeTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(EqualsIncompatibleType.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("EqualsIncompatibleTypePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("EqualsIncompatibleTypeNegativeCases.java").doTest();
  }

  @Test
  public void testPrimitiveBoxingIntoObject() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void something(boolean b, Object o) {",
            "     o.equals(b);",
            "  }",
            "}")
        .setArgs(Arrays.asList("-source", "1.6", "-target", "1.6"))
        .doTest();
  }

  @Test
  public void i547() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface B {}",
            "  <T extends B> void t(T x) {",
            "    // BUG: Diagnostic contains: T and String",
            "    x.equals(\"foo\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void prettyNameForConflicts() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface B {}",
            "  interface String {}",
            "  void t(String x) {",
            "    // BUG: Diagnostic contains: types Test.String and java.lang.String",
            "    x.equals(\"foo\");",
            "  }",
            "}")
        .doTest();
  }
}
