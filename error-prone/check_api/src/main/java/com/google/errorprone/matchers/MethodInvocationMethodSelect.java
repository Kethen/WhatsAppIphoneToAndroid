/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Adapts a matcher on MethodInvocationTree to match the MethodSelect of the MethodInvocation.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class MethodInvocationMethodSelect implements Matcher<MethodInvocationTree> {
  private final Matcher<ExpressionTree> methodSelectMatcher;

  public MethodInvocationMethodSelect(Matcher<ExpressionTree> methodSelectMatcher) {
    this.methodSelectMatcher = methodSelectMatcher;
  }

  @Override
  public boolean matches(MethodInvocationTree item, VisitorState state) {
    return methodSelectMatcher.matches(item.getMethodSelect(), state);
  }
}
