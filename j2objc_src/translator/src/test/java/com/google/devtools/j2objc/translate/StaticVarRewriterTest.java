/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.translate;

import com.google.devtools.j2objc.GenerationTest;

import java.io.IOException;

/**
 * Tests for {@link StaticVarRewriter}.
 *
 * @author Keith Stanger
 */
public class StaticVarRewriterTest extends GenerationTest {

  public void testRewriteChildOfQualifiedName() throws IOException {
    String translation = translateSourceFile(
        "class Test { static Test test = new Test(); Object obj = new Object();"
        + "static class Other { void test() { test.obj.toString(); test.obj.toString(); } } }",
        "Test", "Test.m");
    assertTranslatedLines(translation,
        "[nil_chk(((Test *) nil_chk(JreLoadStatic(Test, test)))->obj_) description];",
        "[nil_chk(((Test *) nil_chk(JreLoadStatic(Test, test)))->obj_) description];");
  }

  public void testAssinmentToNewObject() throws IOException {
    addSourceFile("class A { static Object o; }", "A.java");
    String translation = translateSourceFile(
        "class Test { void test() { A.o = new Object(); } }", "Test", "Test.m");
    assertTranslation(translation,
        "JreStrongAssignAndConsume(JreLoadStaticRef(A, o), new_NSObject_init());");
  }

  public void testFieldAccessRewriting() throws IOException {
    String translation = translateSourceFile(
        "class Test { static int i = 5; static Test getTest() { return null; } "
        + " static void test() { Test t = new Test(); int a = t.i; int b = getTest().i; "
        + " int c = getTest().i++; int d = getTest().i = 6; } }", "Test", "Test.m");
    assertTranslatedLines(translation,
        "jint a = Test_i;",
        "jint b = (Test_getTest(), Test_i);",
        "jint c = (*(Test_getTest(), &Test_i))++;",
        "jint d = *(Test_getTest(), &Test_i) = 6;");
  }

  public void testFieldAccessRewritingWithStaticLoads() throws IOException {
    String translation = translateSourceFile(
        "class Test { static int i = 5; static class Inner { "
        + " static Test getTest() { return null; } "
        + " static void test() { Test t = new Test(); int a = t.i; int b = getTest().i; "
        + " int c = getTest().i++; int d = getTest().i = 6; } } }", "Test", "Test.m");
    assertTranslatedLines(translation,
        "jint a = JreLoadStatic(Test, i);",
        "jint b = (Test_Inner_getTest(), JreLoadStatic(Test, i));",
        "jint c = (*(Test_Inner_getTest(), JreLoadStaticRef(Test, i)))++;",
        "jint d = *(Test_Inner_getTest(), JreLoadStaticRef(Test, i)) = 6;");
  }

  public void testStaticLoadWithArrayAccess() throws IOException {
    String translation = translateSourceFile(
        "class Test { static class Inner { static int[] ints; } "
        + " int test() { Inner.ints[0] = 1; Inner.ints[0] += 2; return Inner.ints[0]; } }",
        "Test", "Test.m");
    assertTranslatedLines(translation,
        "*IOSIntArray_GetRef(nil_chk(JreLoadStatic(Test_Inner, ints)), 0) = 1;",
        "*IOSIntArray_GetRef(JreLoadStatic(Test_Inner, ints), 0) += 2;",
        "return IOSIntArray_Get(JreLoadStatic(Test_Inner, ints), 0);");
  }
}
