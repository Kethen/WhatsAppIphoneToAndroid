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

/** Test cases for {@link MisusedWeekYear}. */
@RunWith(JUnit4.class)
public class MisusedWeekYearTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(MisusedWeekYear.class, getClass());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper.addSourceFile("MisusedWeekYearPositiveCases.java").doTest();
  }

  @Test
  public void testPositiveCases2() throws Exception {
    compilationHelper.addSourceFile("MisusedWeekYearPositiveCases2.java").doTest();
  }

  @Test
  public void testNegativeCases() throws Exception {
    compilationHelper.addSourceFile("MisusedWeekYearNegativeCases.java").doTest();
  }
}
