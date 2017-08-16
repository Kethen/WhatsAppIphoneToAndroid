/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
 * Unit tests for {@link ShouldHaveEvenArgs} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ShouldHaveEvenArgsTest {
  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ShouldHaveEvenArgs.class, getClass());
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("ShouldHaveEvenArgsPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("ShouldHaveEvenArgsNegativeCases.java").doTest();
  }

  @org.junit.Ignore("Public truth doesn't contain this method")
  @Test
  public void testPositiveCase_multimap() throws Exception {
    compilationHelper.addSourceFile("ShouldHaveEvenArgsMultimapPositiveCases.java").doTest();
  }

  @org.junit.Ignore("Public truth doesn't contain this method")
  @Test
  public void testNegativeCase_multimap() throws Exception {
    compilationHelper.addSourceFile("ShouldHaveEvenArgsMultimapNegativeCases.java").doTest();
  }
}
