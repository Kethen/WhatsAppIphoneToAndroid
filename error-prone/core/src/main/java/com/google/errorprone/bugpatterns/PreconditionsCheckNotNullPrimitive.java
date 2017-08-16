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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks that the 1st argument to Preconditions.checkNotNull() isn't a primitive. The primitive
 * would be autoboxed to a non-null boxed type, and the check would trivially pass.
 *
 * <p>In our experience, most of these errors are from copied-and-pasted code and should simply be
 * removed.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(
  name = "PreconditionsCheckNotNullPrimitive",
  summary =
      "First argument to `Preconditions.checkNotNull()` is a primitive rather "
          + "than an object reference",
  explanation =
      "`Preconditions.checkNotNull()` takes as an argument a reference that should be "
          + "non-null. Often a primitive is passed as the argument to check. The primitive "
          + "will be [autoboxed]"
          + "(http://docs.oracle.com/javase/7/docs/technotes/guides/language/autoboxing.html) "
          + "into a boxed object, which is non-null, causing the check to "
          + "always pass without the condition being evaluated.\n\n"
          + "If the intent was to ensure that the primitive met some criterion (e.g., a boolean "
          + "that should be non-null), please use `Preconditions.checkState()` or "
          + "`Preconditions.checkArgument()` instead.",
  category = GUAVA,
  severity = ERROR
)
public class PreconditionsCheckNotNullPrimitive extends BugChecker
    implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (allOf(
            staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
            argument(0, Matchers.<ExpressionTree>isPrimitiveType()))
        .matches(methodInvocationTree, state)) {
      return describe(methodInvocationTree, state);
    }
    return Description.NO_MATCH;
  }

  /**
   * If the call to Preconditions.checkNotNull is part of an expression (assignment, return, etc.),
   * we substitute the argument for the method call. E.g.: {@code bar =
   * Preconditions.checkNotNull(foo); ==> bar = foo;}
   *
   * <p>If the argument to Preconditions.checkNotNull is a comparison using == or != and one of the
   * operands is null, we call checkNotNull on the non-null operand. E.g.: {@code checkNotNull(a ==
   * null); ==> checkNotNull(a);}
   *
   * <p>If the argument is a method call or binary tree and its return type is boolean, change it to
   * a checkArgument/checkState. E.g.: {@code Preconditions.checkNotNull(foo.hasFoo()) ==>
   * Preconditions.checkArgument(foo.hasFoo())}
   *
   * <p>Otherwise, delete the checkNotNull call. E.g.: {@code Preconditions.checkNotNull(foo); ==>
   * [delete the line]}
   */
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree arg1 = methodInvocationTree.getArguments().get(0);
    Tree parent = state.getPath().getParentPath().getLeaf();

    // Assignment, return, etc.
    if (parent.getKind() != Kind.EXPRESSION_STATEMENT) {
      return describeMatch(arg1, SuggestedFix.replace(methodInvocationTree, arg1.toString()));
    }

    // Comparison to null
    if (arg1.getKind() == Kind.EQUAL_TO || arg1.getKind() == Kind.NOT_EQUAL_TO) {
      BinaryTree binaryExpr = (BinaryTree) arg1;
      if (binaryExpr.getLeftOperand().getKind() == Kind.NULL_LITERAL) {
        return describeMatch(
            arg1, SuggestedFix.replace(arg1, binaryExpr.getRightOperand().toString()));
      }
      if (binaryExpr.getRightOperand().getKind() == Kind.NULL_LITERAL) {
        return describeMatch(
            arg1, SuggestedFix.replace(arg1, binaryExpr.getLeftOperand().toString()));
      }
    }

    if ((arg1 instanceof BinaryTree
            || arg1.getKind() == Kind.METHOD_INVOCATION
            || arg1.getKind() == Kind.LOGICAL_COMPLEMENT)
        && ((JCExpression) arg1).type == state.getSymtab().booleanType) {
      return describeMatch(arg1, createCheckArgumentOrStateCall(methodInvocationTree, state, arg1));
    }

    return describeMatch(arg1, SuggestedFix.delete(parent));
  }

  /**
   * Creates a SuggestedFix that replaces the checkNotNull call with a checkArgument or checkState
   * call.
   */
  private Fix createCheckArgumentOrStateCall(
      MethodInvocationTree methodInvocationTree, VisitorState state, ExpressionTree arg1) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String replacementMethod = "checkState";
    if (hasMethodParameter(state.getPath(), arg1)) {
      replacementMethod = "checkArgument";
    }

    StringBuilder replacement = new StringBuilder();

    // Was the original call to Preconditions.checkNotNull a static import or not?
    if (methodInvocationTree.getMethodSelect().getKind() == Kind.IDENTIFIER) {
      fix.addStaticImport("com.google.common.base.Preconditions." + replacementMethod);
    } else {
      replacement.append("Preconditions.");
    }
    replacement.append(replacementMethod).append('(');

    Joiner.on(", ").appendTo(replacement, methodInvocationTree.getArguments());

    replacement.append(")");
    fix.replace(methodInvocationTree, replacement.toString());
    return fix.build();
  }

  /**
   * Determines whether the expression contains a reference to one of the enclosing method's
   * parameters.
   *
   * <p>TODO(eaftan): Extract this to ASTHelpers.
   *
   * @param path the path to the current tree node
   * @param tree the node to compare against the parameters
   * @return whether the argument is a parameter to the enclosing method
   */
  private static boolean hasMethodParameter(TreePath path, ExpressionTree tree) {
    Set<Symbol> symbols = new HashSet<>();
    for (IdentifierTree ident : getVariableUses(tree)) {
      Symbol sym = ASTHelpers.getSymbol(ident);
      if (sym.isLocal()) {
        symbols.add(sym);
      }
    }

    // Find enclosing method declaration.
    while (path != null && !(path.getLeaf() instanceof MethodTree)) {
      path = path.getParentPath();
    }
    if (path == null) {
      throw new IllegalStateException("Should have an enclosing method declaration");
    }
    MethodTree methodDecl = (MethodTree) path.getLeaf();
    for (VariableTree param : methodDecl.getParameters()) {
      if (symbols.contains(ASTHelpers.getSymbol(param))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Find the root variable identifiers from an arbitrary expression.
   *
   * <p>Examples: a.trim().intern() ==> {a} a.b.trim().intern() ==> {a} this.intValue.foo() ==>
   * {this} this.foo() ==> {this} intern() ==> {} String.format() ==> {} java.lang.String.format()
   * ==> {} x.y.z(s.t) ==> {x,s}
   */
  static List<IdentifierTree> getVariableUses(ExpressionTree tree) {
    final List<IdentifierTree> freeVars = new ArrayList<>();

    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree node, Void v) {
        if (((JCIdent) node).sym instanceof VarSymbol) {
          freeVars.add(node);
        }
        return super.visitIdentifier(node, v);
      }
    }.scan(tree, null);

    return freeVars;
  }
}
