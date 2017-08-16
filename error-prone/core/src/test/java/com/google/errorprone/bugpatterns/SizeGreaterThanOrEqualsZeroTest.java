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

/** Unit tests for {@link com.google.errorprone.bugpatterns.SizeGreaterThanOrEqualsZero} */
@RunWith(JUnit4.class)
public class SizeGreaterThanOrEqualsZeroTest {
  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() throws Exception {
    compilationHelper =
        CompilationTestHelper.newInstance(SizeGreaterThanOrEqualsZero.class, getClass());
  }

  @Test
  public void testCollectionSizePositiveCases() throws Exception {
    compilationHelper.addSourceFile("SizeGreaterThanOrEqualsZeroPositiveCases.java").doTest();
  }

  @Test
  public void testCollectionSizeNegativeCases() throws Exception {
    compilationHelper.addSourceFile("SizeGreaterThanOrEqualsZeroNegativeCases.java").doTest();
  }
}
