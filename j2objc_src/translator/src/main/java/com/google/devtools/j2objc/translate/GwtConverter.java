/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
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

package com.google.devtools.j2objc.translate;

import com.google.common.annotations.GwtIncompatible;
import com.google.common.collect.Sets;
import com.google.devtools.j2objc.ast.Annotation;
import com.google.devtools.j2objc.ast.Block;
import com.google.devtools.j2objc.ast.BooleanLiteral;
import com.google.devtools.j2objc.ast.CompilationUnit;
import com.google.devtools.j2objc.ast.ConditionalExpression;
import com.google.devtools.j2objc.ast.EmptyStatement;
import com.google.devtools.j2objc.ast.Expression;
import com.google.devtools.j2objc.ast.IfStatement;
import com.google.devtools.j2objc.ast.MethodDeclaration;
import com.google.devtools.j2objc.ast.MethodInvocation;
import com.google.devtools.j2objc.ast.SingleMemberAnnotation;
import com.google.devtools.j2objc.ast.Statement;
import com.google.devtools.j2objc.ast.StringLiteral;
import com.google.devtools.j2objc.ast.TreeUtil;
import com.google.devtools.j2objc.ast.UnitTreeVisitor;
import com.google.devtools.j2objc.types.ExecutablePair;
import com.google.devtools.j2objc.util.ElementUtil;
import com.google.devtools.j2objc.util.TypeUtil;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;

/**
 * Updates the Java AST to remove code bound by GWT.isClient and
 * GWT.isScript tests, and translate GWT.create(Class) invocations
 * into Class.newInstance().
 *
 * @author Tom Ball
 */
public class GwtConverter extends UnitTreeVisitor {

  private static final String GWT_CLASS = "com.google.gwt.core.client.GWT";

  /**
   * The list of APIs that can be translated by J2ObjC, but not by GWT. These
   * strings were found by scanning common sources like the Google Guava
   * library.
   *
   * Note: this list is neither exhaustive, nor guaranteed to match a
   * specified GwtIncompatible value, since it takes unchecked strings.
   */
  private static final Set<String> compatibleAPIs = Sets.newHashSet(
      "Array.newArray(Class, int)", "Array.newInstance(Class, int)", "Class.isInstance",
      "Class.isAssignableFrom", "CopyOnWriteArraySet", "InputStream", "java.io.BufferedReader",
      "java.io.Closeable,java.io.Flushable", "java.io.Writer", "java.lang.reflect",
      "java.lang.String.getBytes()", "java.lang.System#getProperty", "java.util.ArrayDeque",
      "java.util.BitSet", "java.util.Locale", "java.util.regex", "java.util.regex.Pattern",
      "java.util.String(byte[], Charset)", "MapMakerInternalMap", "NavigableMap", "NavigableAsMap",
      "NavigableSet", "Non-UTF-8 Charset", "OutputStream", "proto", "protos", "Readable",
      "Reader", "Reader,InputStream", "reflection", "regular expressions", "String.format()",
      "uses NavigableMap", "Writer", "Writer,OutputStream");

  public GwtConverter(CompilationUnit unit) {
    super(unit);
  }

  @Override
  public boolean visit(ConditionalExpression node) {
    if (isGwtTest(node.getExpression())) {
      // Replace this node with the else expression, removing this conditional.
      node.replaceWith(TreeUtil.remove(node.getElseExpression()));
    }
    node.getElseExpression().accept(this);
    return false;
  }

  @Override
  public boolean visit(IfStatement node) {
    if (isGwtTest(node.getExpression())) {
      if (node.getElseStatement() != null) {
        // Replace this node with the else statement.
        Statement replacement = TreeUtil.remove(node.getElseStatement());
        node.replaceWith(replacement);
        replacement.accept(this);
      } else {
        // No else statement, so remove this if statement or replace it
        // with an empty statement.
        if (node.getParent() instanceof Block) {
          node.remove();
        } else {
          node.replaceWith(new EmptyStatement());
        }
      }
      return false;
    }
    return true;
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    if (options.stripGwtIncompatibleMethods() && isIncompatible(node.getAnnotations())) {
      // Remove method from its declaring class.
      node.remove();
      return false;
    }
    return true;
  }

  @Override
  public boolean visit(MethodInvocation node) {
    ExecutableElement method = node.getExecutableElement();
    List<Expression> args = node.getArguments();
    if (ElementUtil.getName(method).equals("create")
        && ElementUtil.getQualifiedName(ElementUtil.getDeclaringClass(method)).equals(GWT_CLASS)
        && args.size() == 1) {
      // Convert GWT.create(Foo.class) to Foo.class.newInstance().
      ExecutableElement newMethod = ElementUtil.findMethod(typeUtil.getJavaClass(), "newInstance");
      Expression clazz = args.remove(0);
      node.setExpression(clazz);
      node.setExecutablePair(new ExecutablePair(newMethod));
    } else if (isGwtTest(node)) {
      node.replaceWith(new BooleanLiteral(false, typeUtil));
    }
    return true;
  }

  /**
   * Returns true if expression is a method invocation, and that method refers to GWT.isClient() or
   * GWT.isScript().
   *
   * NOTE: this method only checks for GWT.isClient() method invocations, not any other boolean
   * expression that might contain them. For example, !GWT.isClient() won't detect the method, since
   * that expression won't have a method element. The isGwtTest() call in visit(MethodInvocation)
   * warns when code like this is translated.
   */
  private boolean isGwtTest(Expression node) {
    ExecutableElement method = TreeUtil.getExecutableElement(node);
    if (method != null) {
      if (ElementUtil.getQualifiedName(ElementUtil.getDeclaringClass(method)).equals(GWT_CLASS)) {
        String name = ElementUtil.getName(method);
        return name.equals("isClient") || name.equals("isScript");
      }
    }
    return false;
  }

  private boolean isIncompatible(List<Annotation> annotations) {
    // Annotation mirrors don't have the annotation's package.
    for (Annotation annotationNode : annotations) {
      String annotationName =
          TypeUtil.getQualifiedName(annotationNode.getAnnotationMirror().getAnnotationType());
      if (!annotationName.equals(GwtIncompatible.class.getName())) {
        continue;
      }
      if (annotationNode.isSingleMemberAnnotation()) {
        Expression value = ((SingleMemberAnnotation) annotationNode).getValue();
        if (value instanceof StringLiteral) {
          if (compatibleAPIs.contains(((StringLiteral) value).getLiteralValue())) {
            // Pretend incompatible annotation isn't present, since what it's
            // flagging is J2ObjC-compatible.
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }
}
