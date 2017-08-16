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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import java.util.List;

/** @author mariasam@google.com (Maria Sam) on 6/27/17. */
@BugPattern(
  name = "CollectionToArraySafeParameter",
  summary =
      "The type of the array parameter of Collection.toArray "
          + "needs to be compatible with the array type",
  category = JDK,
  severity = WARNING
)
public class CollectionToArraySafeParameter extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> TO_ARRAY_MATCHER =
      instanceMethod().onDescendantOf("java.util.Collection").withSignature("<T>toArray(T[])");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState visitorState) {

    if (TO_ARRAY_MATCHER.matches(methodInvocationTree, visitorState)) {
      ArrayType firstArgType =
          (ArrayType) ASTHelpers.getType(methodInvocationTree.getArguments().get(0));
      Type variableType = firstArgType.getComponentType();

      ClassType classTypeSub = (ClassType) ASTHelpers.getReceiverType(methodInvocationTree);

      Types types = visitorState.getTypes();
      Type classType =
          types.asSuper(classTypeSub, visitorState.getSymbolFromString("java.util.Collection"));
      List<Type> typeArguments = classType.getTypeArguments();

      if (!typeArguments.isEmpty()
          && !types.isCastable(types.erasure(variableType), types.erasure(typeArguments.get(0)))) {
        return describeMatch(methodInvocationTree);
      }
    }
    return Description.NO_MATCH;
  }
}
