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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * A copy of the information in {@link com.google.errorprone.annotations.Immutable}.
 *
 * <p>Useful for providing information for immutable classes we can't easily annotate, e.g. those in
 * the JDK.
 */
@AutoValue
public abstract class ImmutableAnnotationInfo {
  public abstract String typeName();

  public abstract ImmutableSet<String> containerOf();

  public static ImmutableAnnotationInfo create(String typeName, Iterable<String> containerOf) {
    return new AutoValue_ImmutableAnnotationInfo(typeName, ImmutableSet.copyOf(containerOf));
  }

  public static ImmutableAnnotationInfo create(String typeName) {
    return create(typeName, ImmutableSet.<String>of());
  }
}
