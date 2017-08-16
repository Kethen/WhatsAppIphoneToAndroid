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
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class OvershadowingSubclassFieldsPositiveCases2 {

  /**
   * ClassA extends a class from a different file and ClassA has a member with the same name as its
   * parent
   */
  public class ClassA extends OvershadowingSubclassFieldsPositiveCases1.ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    private int varTwo;
  }

  /**
   * ClassB extends a class from a different file and ClassB has a member with the same name as its
   * grandparent
   */
  public class ClassB extends OvershadowingSubclassFieldsPositiveCases1.ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    public int varOne = 2;
  }
}
