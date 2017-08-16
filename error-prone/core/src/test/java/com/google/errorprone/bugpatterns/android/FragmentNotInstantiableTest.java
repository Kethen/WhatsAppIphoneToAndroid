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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.Category.ANDROID;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author avenet@google.com (Arnaud J. Venet) */
@RunWith(JUnit4.class)
public class FragmentNotInstantiableTest {
  /** Used for testing a custom FragmentNotInstantiable. */
  @BugPattern(
    name = "CustomFragmentNotInstantiable",
    summary =
        "Subclasses of CustomFragment must be instantiable via Class#newInstance():"
            + " the class must be public, static and have a public nullary constructor",
    category = ANDROID,
    severity = WARNING
  )
  public static class CustomFragmentNotInstantiable extends FragmentNotInstantiable {
    public CustomFragmentNotInstantiable() {
      super(ImmutableSet.of("com.google.errorprone.bugpatterns.android.testdata.CustomFragment"));
    }
  }

  @Test
  public void testPositiveCases() throws Exception {
    createCompilationTestHelper(FragmentNotInstantiable.class)
        .addSourceFile("FragmentNotInstantiablePositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    createCompilationTestHelper(FragmentNotInstantiable.class)
        .addSourceFile("FragmentNotInstantiableNegativeCases.java")
        .doTest();
  }

  @Test
  public void testPositiveCases_custom() throws Exception {
    createCompilationTestHelper(CustomFragmentNotInstantiable.class)
        .addSourceFile("FragmentNotInstantiablePositiveCases.java")
        .addSourceFile("CustomFragment.java")
        .addSourceFile("CustomFragmentNotInstantiablePositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase_custom() throws Exception {
    createCompilationTestHelper(CustomFragmentNotInstantiable.class)
        .addSourceFile("FragmentNotInstantiableNegativeCases.java")
        .addSourceFile("CustomFragment.java")
        .addSourceFile("CustomFragmentNotInstantiableNegativeCases.java")
        .doTest();
  }

  private CompilationTestHelper createCompilationTestHelper(
      Class<? extends FragmentNotInstantiable> bugCheckerClass) {
    return CompilationTestHelper.newInstance(bugCheckerClass, getClass());
  }
}
