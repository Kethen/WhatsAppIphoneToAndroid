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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.ThrowTree;

/**
 * Matches a {@code throw} statement whose thrown expression is matched by the given matcher.
 *
 * @author schmitt@google.com (Peter Schmitt)
 */
public class Throws implements Matcher<StatementTree> {

  private final Matcher<? super ExpressionTree> thrownMatcher;

  /**
   * New matcher for a {@code throw} statement where the thrown item is matched by the passed {@code
   * thrownMatcher}.
   */
  public Throws(Matcher<? super ExpressionTree> thrownMatcher) {
    this.thrownMatcher = thrownMatcher;
  }

  @Override
  public boolean matches(StatementTree expressionTree, VisitorState state) {
    if (!(expressionTree instanceof ThrowTree)) {
      return false;
    }

    return thrownMatcher.matches(((ThrowTree) expressionTree).getExpression(), state);
  }
}
