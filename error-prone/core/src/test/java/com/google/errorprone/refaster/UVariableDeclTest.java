/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UVariableDecl}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UVariableDeclTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(UVariableDecl.create("foo", UClassIdent.create("java.lang.String"), null))
        .addEqualityGroup(
            UVariableDecl.create(
                "foo", UClassIdent.create("java.lang.String"), ULiteral.stringLit("bar")))
        .addEqualityGroup(
            UVariableDecl.create("foo", UClassIdent.create("java.lang.Integer"), null))
        .addEqualityGroup(UVariableDecl.create("baz", UClassIdent.create("java.lang.String"), null))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UVariableDecl.create(
            "foo", UClassIdent.create("java.lang.String"), ULiteral.stringLit("bar")));
  }
}
