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
package com.google.j2objc.java8;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Returns a suite of all small tests in this package.
 */
public class SmallTests {
  private static final Class<?>[] smallTestClasses = new Class[] {
    CreationReferenceTest.class,
    DefaultMethodsTest.class,
    ExpressionMethodReferenceTest.class,
    LambdaTest.class,
    SuperMethodReferenceTest.class,
    TypeMethodReferenceTest.class,
  };

  public static Test suite() {
    return new TestSuite(smallTestClasses);
  }
}
