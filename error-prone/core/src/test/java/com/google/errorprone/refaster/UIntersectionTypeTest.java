/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.refaster;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link UIntersectionType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class UIntersectionTypeTest {
  @Test
  public void equality() {
    new EqualsTester()
        .addEqualityGroup(
            UIntersectionType.create(
                UClassIdent.create("java.lang.CharSequence"),
                UClassIdent.create("java.io.Serializable")))
        .addEqualityGroup(
            UIntersectionType.create(
                UClassIdent.create("java.lang.Number"), UClassIdent.create("java.io.Serializable")))
        .testEquals();
  }

  @Test
  public void serialization() {
    SerializableTester.reserializeAndAssert(
        UIntersectionType.create(
            UClassIdent.create("java.lang.CharSequence"),
            UClassIdent.create("java.io.Serializable")));
  }
}
