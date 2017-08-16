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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Commented;
import com.google.errorprone.util.Comments;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ParameterComment",
  category = JDK,
  summary = "Non-standard parameter comment; prefer `/*paramName=*/ arg`",
  severity = SUGGESTION,
  tags = StandardTags.STYLE
)
public class ParameterComment extends BugChecker implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    MethodSymbol symbol = ASTHelpers.getSymbol(tree);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    forEachPair(
        Comments.findCommentsForArguments(tree, state).stream(),
        Stream.concat(
            symbol.getParameters().stream(),
            Stream.iterate(getLast(symbol.getParameters()), x -> x)),
        (commented, param) ->
            commented
                .afterComments()
                .stream()
                .filter(c -> matchingParamComment(c, param))
                .findFirst()
                .ifPresent(c -> fixParamComment(fix, commented, param, c)));
    return fix.isEmpty() ? NO_MATCH : describeMatch(tree, fix.build());
  }

  private static boolean matchingParamComment(Comment c, VarSymbol param) {
    String text = Comments.getTextFromComment(c).trim();
    if (text.endsWith("=")) {
      text = text.substring(0, text.length() - "=".length()).trim();
    }
    return param.getSimpleName().contentEquals(text);
  }

  private static void fixParamComment(
      SuggestedFix.Builder fix, Commented<ExpressionTree> commented, VarSymbol param, Comment c) {
    fix.prefixWith(commented.tree(), String.format("/* %s= */ ", param.getSimpleName()))
        .replace(c.getSourcePos(0), c.getSourcePos(0) + c.getText().length(), "");
  }

  // TODO(cushon): use Streams.forEach when guava 22 is available
  static <A, B> void forEachPair(Stream<A> xs, Stream<B> bx, BiConsumer<A, B> c) {
    BiFunction<A, B, Void> f =
        (a, b) -> {
          c.accept(a, b);
          return null;
        };
    long unused = Streams.zip(xs, bx, f).count();
  }
}
