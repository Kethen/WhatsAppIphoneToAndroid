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

/** @author scottjohnson@google.com (Scott Johnson) */
@RunWith(JUnit4.class)
public class NumericEqualityTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(NumericEquality.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("NumericEqualityPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("NumericEqualityNegativeCases.java").doTest();
  }

  // regression test for #415
  @Test
  public void testParenthesized() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    final Long constValue = new Long(1000L);",
            "    Long assignedValue;",
            "    // BUG: Diagnostic contains:"
                + " (!(assignedValue = new Long(1000L)).equals(constValue))",
            "    boolean retVal = ((assignedValue = new Long(1000L)) != constValue);",
            "  }",
            "}")
        .doTest();
  }
}
