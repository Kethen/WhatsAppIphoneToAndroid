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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.parser.Tokens.TokenKind;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "MixedArrayDimensions",
  category = JDK,
  summary = "C-style array declarations should not be used",
  severity = SUGGESTION,
  linkType = CUSTOM,
  tags = StandardTags.STYLE,
  link = "https://google.github.io/styleguide/javaguide.html#s4.8.3.2-array-declarations"
)
public class MixedArrayDimensions extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return checkArrayDimensions(tree, tree.getReturnType(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return checkArrayDimensions(tree, tree.getType(), state);
  }

  private Description checkArrayDimensions(Tree tree, Tree type, VisitorState state) {
    if (!(type instanceof ArrayTypeTree)) {
      return NO_MATCH;
    }
    CharSequence source = state.getSourceCode();
    for (; type instanceof ArrayTypeTree; type = ((ArrayTypeTree) type).getType()) {
      Tree elemType = ((ArrayTypeTree) type).getType();
      int start = state.getEndPosition(elemType);
      int end = state.getEndPosition(type);
      if (start >= end) {
        continue;
      }
      String dim = source.subSequence(start, end).toString();
      if (dim.isEmpty()) {
        continue;
      }
      ImmutableList<ErrorProneToken> tokens = ErrorProneTokens.getTokens(dim.trim(), state.context);
      if (tokens.size() > 2 && tokens.get(0).kind() == TokenKind.IDENTIFIER) {
        int nonWhitespace = CharMatcher.isNot(' ').indexIn(dim);
        int idx = dim.indexOf("[]", nonWhitespace);
        if (idx > nonWhitespace) {
          String replacement = dim.substring(idx, dim.length()) + dim.substring(0, idx);
          return describeMatch(tree, SuggestedFix.replace(start, end, replacement));
        }
      }
    }
    return NO_MATCH;
  }
}
