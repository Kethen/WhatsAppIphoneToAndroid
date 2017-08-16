/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
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

package com.google.devtools.j2objc.gen;

import com.google.devtools.j2objc.GenerationTest;
import com.google.devtools.j2objc.ast.Statement;

import java.util.List;

/**
 * Verifies that primitive arrays such as byte[] are converted into IOS
 * equivalents, and their access and methods mapped correctly.
 *
 * @author Tom Ball
 */
public class PrimitiveArrayTest extends GenerationTest {

  public void testBooleanArrayType() {
    testArrayType("boolean[] foo;", "IOSBooleanArray *foo;");
  }

  public void testByteArrayType() {
    testArrayType("byte[] foo;", "IOSByteArray *foo;");
  }

  public void testCharArrayType() {
    testArrayType("char[] foo;", "IOSCharArray *foo;");
  }

  public void testDoubleArrayType() {
    testArrayType("double[] foo;", "IOSDoubleArray *foo;");
  }

  public void testFloatArrayType() {
    testArrayType("float[] foo;", "IOSFloatArray *foo;");
  }

  public void testIntArrayType() {
    testArrayType("int[] foo;", "IOSIntArray *foo;");
  }

  public void testLongArrayType() {
    testArrayType("long[] foo;", "IOSLongArray *foo;");
  }

  public void testShortArrayType() {
    testArrayType("short[] foo;", "IOSShortArray *foo;");
  }

  public void testObjectArrayType() {
    testArrayType("Object[] foo;", "IOSObjectArray *foo;");
    testArrayType("Exception[] foo;", "IOSObjectArray *foo;");
  }

  private void testArrayType(String source, String expectedResult) {
    List<Statement> stmts = translateStatements(source);
    assertEquals(1, stmts.size());
    String result = generateStatement(stmts.get(0));
    assertEquals(expectedResult, result);
  }

  public void testByteType() {
    List<Statement> stmts = translateStatements("byte[] foo;");
    assertEquals(1, stmts.size());
    String result = generateStatement(stmts.get(0));
    assertEquals("IOSByteArray *foo;", result);
  }

  public void testBooleanArrayAccess() {
    List<Statement> stmts =
        translateStatements("boolean[] foo = new boolean[3]; boolean b = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jboolean b = IOSBooleanArray_Get(foo, 1);", result);
  }

  public void testByteArrayAccess() {
    List<Statement> stmts = translateStatements("byte[] foo = new byte[3]; byte b = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jbyte b = IOSByteArray_Get(foo, 1);", result);
  }

  public void testCharArrayAccess() {
    List<Statement> stmts = translateStatements("char[] foo = new char[3]; char c = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jchar c = IOSCharArray_Get(foo, 1);", result);
  }

  public void testDoubleArrayAccess() {
    List<Statement> stmts = translateStatements(
        "double[] foo = new double[3]; double d = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jdouble d = IOSDoubleArray_Get(foo, 1);", result);
  }

  public void testFloatArrayAccess() {
    List<Statement> stmts = translateStatements("float[] foo = new float[3]; float f = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jfloat f = IOSFloatArray_Get(foo, 1);", result);
  }

  public void testIntArrayAccess() {
    List<Statement> stmts = translateStatements("int[] foo = new int[3]; int i = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jint i = IOSIntArray_Get(foo, 1);", result);
  }

  public void testLongArrayAccess() {
    List<Statement> stmts = translateStatements("long[] foo = new long[3]; long l = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jlong l = IOSLongArray_Get(foo, 1);", result);
  }

  public void testShortArrayAccess() {
    List<Statement> stmts = translateStatements("short[] foo = new short[3]; short s = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("jshort s = IOSShortArray_Get(foo, 1);", result);
  }

  public void testObjectArrayAccess() {
    List<Statement> stmts = translateStatements(
        "Object[] foo = new Object[3]; Object o = foo[1];");
    assertEquals(2, stmts.size());
    String result = generateStatement(stmts.get(1));
    assertEquals("id o = IOSObjectArray_Get(foo, 1);", result);

    stmts = translateStatements("Exception[] foo = new Exception[3]; Exception o = foo[1];");
    assertEquals(2, stmts.size());
    result = generateStatement(stmts.get(1));
    assertEquals("JavaLangException *o = IOSObjectArray_Get(foo, 1);", result);
  }
}
