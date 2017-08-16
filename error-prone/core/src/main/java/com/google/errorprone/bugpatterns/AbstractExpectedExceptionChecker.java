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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFix.Builder;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** @author cushon@google.com (Liam Miller-Cushon) */
public abstract class AbstractExpectedExceptionChecker extends BugChecker
    implements MethodTreeMatcher {

  static final Matcher<StatementTree> MATCHER =
      expressionStatement(
          instanceMethod()
              .onExactClass("org.junit.rules.ExpectedException")
              .withNameMatching(Pattern.compile("expect.*")));

  static final Matcher<ExpressionTree> IS_A =
      staticMethod()
          .onClassAny("org.hamcrest.Matchers", "org.hamcrest.CoreMatchers")
          .withSignature("<T>isA(java.lang.Class<T>)");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getBody() == null) {
      return NO_MATCH;
    }
    tree.getBody()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitBlock(BlockTree block, Void unused) {
                Description description = scanBlock(tree, block, state);
                if (description != NO_MATCH) {
                  state.reportMatch(description);
                }
                return super.visitBlock(block, unused);
              }
            },
            null);
    return NO_MATCH;
  }

  Description scanBlock(MethodTree tree, BlockTree block, VisitorState state) {
    PeekingIterator<? extends StatementTree> it =
        Iterators.peekingIterator(block.getStatements().iterator());
    while (it.hasNext() && !MATCHER.matches(it.peek(), state)) {
      it.next();
    }
    List<Tree> expectations = new ArrayList<>();
    while (it.hasNext() && MATCHER.matches(it.peek(), state)) {
      expectations.add(it.next());
    }
    if (expectations.isEmpty()) {
      return NO_MATCH;
    }
    List<StatementTree> suffix = new ArrayList<>();
    Iterators.addAll(suffix, it);
    return handleMatch(tree, state, expectations, suffix);
  }

  /**
   * Handle a method that contains a use of {@code ExpectedException}.
   *
   * @param tree the method
   * @param state the visitor state
   * @param expectations the statements for the call to {@code thrown.except(...)}, and any
   *     additional assertions
   * @param suffix the statements after the assertions, which are expected to throw
   */
  protected abstract Description handleMatch(
      MethodTree tree, VisitorState state, List<Tree> expectations, List<StatementTree> suffix);

  protected BaseFix buildBaseFix(VisitorState state, List<Tree> expectations) {
    String exceptionClass = "Throwable";
    // additional assertions to perform on the captured exception (if any)
    List<String> newAsserts = new ArrayList<>();
    Builder fix = SuggestedFix.builder();
    for (Tree expectation : expectations) {
      MethodInvocationTree invocation =
          (MethodInvocationTree) ((ExpressionStatementTree) expectation).getExpression();
      MethodSymbol symbol = ASTHelpers.getSymbol(invocation);
      Symtab symtab = state.getSymtab();
      List<? extends ExpressionTree> args = invocation.getArguments();
      switch (symbol.getSimpleName().toString()) {
        case "expect":
          Type type = ASTHelpers.getType(getOnlyElement(invocation.getArguments()));
          if (isSubtype(type, symtab.classType, state)) {
            // expect(Class<?>)
            exceptionClass = state.getSourceForNode(getReceiver(getOnlyElement(args)));
          } else if (isSubtype(type, state.getTypeFromString("org.hamcrest.Matcher"), state)) {
            Type matcherType =
                state.getTypes().asSuper(type, state.getSymbolFromString("org.hamcrest.Matcher"));
            if (!matcherType.getTypeArguments().isEmpty()) {
              Type matchType = getOnlyElement(matcherType.getTypeArguments());
              if (isSubtype(matchType, symtab.throwableType, state)) {
                exceptionClass = SuggestedFixes.qualifyType(state, fix, matchType);
              }
            }
            // expect(Matcher)
            fix.addStaticImport("org.hamcrest.MatcherAssert.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown, %s);", state.getSourceForNode(getOnlyElement(args))));
          }
          break;
        case "expectCause":
          ExpressionTree matcher = getOnlyElement(invocation.getArguments());
          if (IS_A.matches(matcher, state)) {
            fix.addStaticImport("com.google.common.truth.Truth.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown).hasCauseThat().isInstanceOf(%s);",
                    state.getSourceForNode(
                        getOnlyElement(((MethodInvocationTree) matcher).getArguments()))));
          } else {
            fix.addStaticImport("org.hamcrest.MatcherAssert.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown.getCause(), %s);",
                    state.getSourceForNode(getOnlyElement(args))));
          }
          break;
        case "expectMessage":
          if (isSubtype(
              getOnlyElement(symbol.getParameters()).asType(), symtab.stringType, state)) {
            // expectedMessage(String)
            fix.addStaticImport("com.google.common.truth.Truth.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown).hasMessageThat().contains(%s);",
                    state.getSourceForNode(getOnlyElement(args))));
          } else {
            // expectedMessage(Matcher)
            fix.addStaticImport("org.hamcrest.MatcherAssert.assertThat");
            newAsserts.add(
                String.format(
                    "assertThat(thrown.getMessage(), %s);",
                    state.getSourceForNode(getOnlyElement(args))));
          }
          break;
        default:
          throw new AssertionError("unknown expect method: " + symbol.getSimpleName());
      }
    }
    // remove all interactions with the ExpectedException rule
    fix.replace(
        ((JCTree) expectations.get(0)).getStartPosition(),
        state.getEndPosition(getLast(expectations)),
        "");
    return new BaseFix(fix.build(), exceptionClass, newAsserts);
  }

  /** A partially assembled fix. */
  protected static class BaseFix {

    final SuggestedFix baseFix;
    final String exceptionClass;
    final List<String> newAsserts;

    BaseFix(SuggestedFix baseFix, String exceptionClass, List<String> newAsserts) {
      this.baseFix = baseFix;
      this.exceptionClass = exceptionClass;
      this.newAsserts = newAsserts;
    }

    public Fix build(List<? extends StatementTree> throwingStatements) {
      if (throwingStatements.isEmpty()) {
        return baseFix;
      }
      SuggestedFix.Builder fix = SuggestedFix.builder().merge(baseFix);
      StringBuilder fixPrefix = new StringBuilder();
      if (newAsserts.isEmpty()) {
        fix.addStaticImport("org.junit.Assert.assertThrows");
        fixPrefix.append("assertThrows");
      } else {
        fix.addStaticImport("org.junit.Assert.expectThrows");
        fixPrefix.append(String.format("%s thrown = expectThrows", exceptionClass));
      }
      fixPrefix.append(String.format("(%s.class, () -> ", exceptionClass));
      boolean useExpressionLambda =
          throwingStatements.size() == 1
              && getOnlyElement(throwingStatements).getKind() == Kind.EXPRESSION_STATEMENT;
      if (!useExpressionLambda) {
        fixPrefix.append("{");
      }
      fix.prefixWith(throwingStatements.get(0), fixPrefix.toString());
      if (useExpressionLambda) {
        fix.postfixWith(((ExpressionStatementTree) throwingStatements.get(0)).getExpression(), ")");
        fix.postfixWith(getLast(throwingStatements), Joiner.on('\n').join(newAsserts));
      } else {
        fix.postfixWith(getLast(throwingStatements), "});\n" + Joiner.on('\n').join(newAsserts));
      }
      return fix.build();
    }
  }
}
