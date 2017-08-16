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

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class SelfAssignmentTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(SelfAssignment.class, getClass());
  }

  @Test
  public void testPositiveCases1() throws Exception {
    compilationHelper.addSourceFile("SelfAssignmentPositiveCases1.java").doTest();
  }

  @Test
  public void testPositiveCases2() throws Exception {
    compilationHelper.addSourceFile("SelfAssignmentPositiveCases2.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("SelfAssignmentNegativeCases.java").doTest();
  }
}
