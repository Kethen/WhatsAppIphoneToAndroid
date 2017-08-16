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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.devtools.j2objc.ast.AbstractTypeDeclaration;
import com.google.devtools.j2objc.ast.AnnotationTypeDeclaration;
import com.google.devtools.j2objc.ast.Assignment;
import com.google.devtools.j2objc.ast.Block;
import com.google.devtools.j2objc.ast.BodyDeclaration;
import com.google.devtools.j2objc.ast.CatchClause;
import com.google.devtools.j2objc.ast.CompilationUnit;
import com.google.devtools.j2objc.ast.EnumDeclaration;
import com.google.devtools.j2objc.ast.Expression;
import com.google.devtools.j2objc.ast.ExpressionStatement;
import com.google.devtools.j2objc.ast.FieldAccess;
import com.google.devtools.j2objc.ast.FieldDeclaration;
import com.google.devtools.j2objc.ast.ForStatement;
import com.google.devtools.j2objc.ast.IfStatement;
import com.google.devtools.j2objc.ast.InfixExpression;
import com.google.devtools.j2objc.ast.MethodDeclaration;
import com.google.devtools.j2objc.ast.MethodInvocation;
import com.google.devtools.j2objc.ast.NullLiteral;
import com.google.devtools.j2objc.ast.PackageDeclaration;
import com.google.devtools.j2objc.ast.ParenthesizedExpression;
import com.google.devtools.j2objc.ast.PropertyAnnotation;
import com.google.devtools.j2objc.ast.QualifiedName;
import com.google.devtools.j2objc.ast.SimpleName;
import com.google.devtools.j2objc.ast.SingleVariableDeclaration;
import com.google.devtools.j2objc.ast.Statement;
import com.google.devtools.j2objc.ast.ThrowStatement;
import com.google.devtools.j2objc.ast.TreeNode;
import com.google.devtools.j2objc.ast.TreeUtil;
import com.google.devtools.j2objc.ast.TryStatement;
import com.google.devtools.j2objc.ast.Type;
import com.google.devtools.j2objc.ast.TypeDeclaration;
import com.google.devtools.j2objc.ast.UnitTreeVisitor;
import com.google.devtools.j2objc.ast.VariableDeclarationExpression;
import com.google.devtools.j2objc.ast.VariableDeclarationFragment;
import com.google.devtools.j2objc.ast.VariableDeclarationStatement;
import com.google.devtools.j2objc.types.ExecutablePair;
import com.google.devtools.j2objc.types.GeneratedVariableElement;
import com.google.devtools.j2objc.util.ElementUtil;
import com.google.devtools.j2objc.util.ErrorUtil;
import com.google.devtools.j2objc.util.TypeUtil;
import com.google.j2objc.annotations.AutoreleasePool;
import com.google.j2objc.annotations.Weak;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Rewrites the Java AST to replace difficult to translate code with methods
 * that are more Objective C/iOS specific. For example, Objective C doesn't have
 * the concept of class variables, so they need to be replaced with static
 * accessor methods referencing private static data.
 *
 * @author Tom Ball
 */
public class Rewriter extends UnitTreeVisitor {

  public Rewriter(CompilationUnit unit) {
    super(unit);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    ExecutableElement element = node.getExecutableElement();
    if (ElementUtil.hasAnnotation(element, AutoreleasePool.class)) {
      if (TypeUtil.isReferenceType(element.getReturnType())) {
        ErrorUtil.warning(
            "Ignoring AutoreleasePool annotation on method with retainable return type");
      } else if (node.getBody() != null) {
        node.getBody().setHasAutoreleasePool(true);
      }
    }

    if (ElementUtil.hasNullableAnnotation(element) || ElementUtil.hasNonnullAnnotation(element)) {
      unit.setHasNullabilityAnnotations();
    }
    return true;
  }

  @Override
  public void endVisit(ForStatement node) {
    // It should not be possible to have multiple VariableDeclarationExpression
    // nodes in the initializers.
    if (node.getInitializers().size() == 1) {
      Object initializer = node.getInitializer(0);
      if (initializer instanceof VariableDeclarationExpression) {
        List<VariableDeclarationFragment> fragments =
            ((VariableDeclarationExpression) initializer).getFragments();
        for (VariableDeclarationFragment fragment : fragments) {
          if (ElementUtil.hasAnnotation(fragment.getVariableElement(), AutoreleasePool.class)) {
            Statement loopBody = node.getBody();
            if (!(loopBody instanceof Block)) {
              Block block = new Block();
              node.setBody(block);
              block.addStatement(loopBody);
            }
            ((Block) node.getBody()).setHasAutoreleasePool(true);
          }
        }
      }
    }
  }

  @Override
  public void endVisit(InfixExpression node) {
    InfixExpression.Operator op = node.getOperator();
    if (typeUtil.isString(node.getTypeMirror()) && op == InfixExpression.Operator.PLUS) {
      rewriteStringConcat(node);
    } else if (op == InfixExpression.Operator.CONDITIONAL_AND) {
      // Avoid logical-op-parentheses compiler warnings.
      if (node.getParent() instanceof InfixExpression) {
        InfixExpression parent = (InfixExpression) node.getParent();
        if (parent.getOperator() == InfixExpression.Operator.CONDITIONAL_OR) {
          ParenthesizedExpression.parenthesizeAndReplace(node);
        }
      }
    } else if (op == InfixExpression.Operator.AND) {
      // Avoid bitwise-op-parentheses compiler warnings.
      if (node.getParent() instanceof InfixExpression
          && ((InfixExpression) node.getParent()).getOperator() == InfixExpression.Operator.OR) {
        ParenthesizedExpression.parenthesizeAndReplace(node);
      }
    }

    // Avoid lower precedence compiler warnings.
    if (op == InfixExpression.Operator.AND || op == InfixExpression.Operator.OR) {
      for (Expression operand : node.getOperands()) {
        if (operand instanceof InfixExpression) {
          ParenthesizedExpression.parenthesizeAndReplace(operand);
        }
      }
    }
  }

  private void rewriteStringConcat(InfixExpression node) {
    // Collect all non-string operands that precede the first string operand.
    // If there are multiple such operands, move them into a sub-expression.
    List<Expression> nonStringOperands = new ArrayList<>();
    TypeMirror nonStringExprType = null;
    for (Expression operand : node.getOperands()) {
      TypeMirror operandType = operand.getTypeMirror();
      if (typeUtil.isString(operandType)) {
        break;
      }
      nonStringOperands.add(operand);
      nonStringExprType = getAdditionType(nonStringExprType, operandType);
    }

    if (nonStringOperands.size() < 2) {
      return;
    }

    InfixExpression nonStringExpr =
        new InfixExpression(nonStringExprType, InfixExpression.Operator.PLUS);
    for (Expression operand : nonStringOperands) {
      nonStringExpr.addOperand(TreeUtil.remove(operand));
    }
    node.addOperand(0, nonStringExpr);
  }

  private TypeKind getPrimitiveKind(TypeMirror t) {
    if (t == null) {
      return null;
    }
    if (t.getKind().isPrimitive()) {
      return t.getKind();
    }
    PrimitiveType p = typeUtil.unboxedType(t);
    return p != null ? p.getKind() : null;
  }

  private PrimitiveType getAdditionType(TypeMirror aType, TypeMirror bType) {
    TypeKind aKind = getPrimitiveKind(aType);
    TypeKind bKind = getPrimitiveKind(bType);
    if (aKind == TypeKind.DOUBLE || bKind == TypeKind.DOUBLE) {
      return typeUtil.getDouble();
    }
    if (aKind == TypeKind.FLOAT || bKind == TypeKind.FLOAT) {
      return typeUtil.getFloat();
    }
    if (aKind == TypeKind.LONG || bKind == TypeKind.LONG) {
      return typeUtil.getLong();
    }
    return typeUtil.getInt();
  }

  @Override
  public void endVisit(SingleVariableDeclaration node) {
    if (node.getExtraDimensions() > 0) {
      node.setType(Type.newType(node.getVariableElement().asType()));
      node.setExtraDimensions(0);
    }

    VariableElement var = node.getVariableElement();
    if (ElementUtil.hasNullableAnnotation(var) || ElementUtil.hasNonnullAnnotation(var)) {
      unit.setHasNullabilityAnnotations();
    }
  }

  @Override
  public void endVisit(VariableDeclarationStatement node) {
    if (options.isJDT()) {
      ListMultimap<Integer, VariableDeclarationFragment> newDeclarations =
          rewriteExtraDimensions(node.getFragments());
      if (newDeclarations != null) {
        List<Statement> statements = ((Block) node.getParent()).getStatements();
        int location = 0;
        while (location < statements.size() && !node.equals(statements.get(location))) {
          location++;
        }
        for (Integer dimensions : newDeclarations.keySet()) {
          List<VariableDeclarationFragment> fragments = newDeclarations.get(dimensions);
          VariableDeclarationStatement newDecl = new VariableDeclarationStatement(fragments.get(0));
          newDecl.getFragments().addAll(fragments.subList(1, fragments.size()));
          statements.add(++location, newDecl);
        }
      }
    }
  }

  @Override
  public void endVisit(FieldDeclaration node) {
    if (options.isJDT()) {
      ListMultimap<Integer, VariableDeclarationFragment> newDeclarations =
          rewriteExtraDimensions(node.getFragments());
      if (newDeclarations != null) {
        List<BodyDeclaration> bodyDecls = TreeUtil.asDeclarationSublist(node);
        for (Integer dimensions : newDeclarations.keySet()) {
          List<VariableDeclarationFragment> fragments = newDeclarations.get(dimensions);
          FieldDeclaration newDecl = new FieldDeclaration(fragments.get(0));
          newDecl.getFragments().addAll(fragments.subList(1, fragments.size()));
          bodyDecls.add(newDecl);
        }
      }
    }
  }

  @Override
  public boolean visit(QualifiedName node) {
    VariableElement var = TreeUtil.getVariableElement(node);
    Expression qualifier = node.getQualifier();
    if (var != null && var.getKind().isField() && TreeUtil.getVariableElement(qualifier) != null) {
      // FieldAccess nodes are more easily mutated than QualifiedName.
      FieldAccess fieldAccess =
          new FieldAccess(var, node.getTypeMirror(), TreeUtil.remove(qualifier));
      node.replaceWith(fieldAccess);
      fieldAccess.accept(this);
      return false;
    }
    return true;
  }

  private ListMultimap<Integer, VariableDeclarationFragment> rewriteExtraDimensions(
      List<VariableDeclarationFragment> fragments) {
    // Removes extra dimensions on variable declaration fragments and creates extra field
    // declaration nodes if necessary.
    // eg. "int i1, i2[], i3[][];" becomes "int i1; int[] i2; int[][] i3".
    ListMultimap<Integer, VariableDeclarationFragment> newDeclarations = null;
    int masterDimensions = -1;
    Iterator<VariableDeclarationFragment> iter = fragments.iterator();
    while (iter.hasNext()) {
      VariableDeclarationFragment frag = iter.next();
      int dimensions = frag.getExtraDimensions();
      if (masterDimensions == -1) {
        masterDimensions = dimensions;
      } else if (dimensions != masterDimensions) {
        if (newDeclarations == null) {
          newDeclarations = LinkedListMultimap.create();
        }
        VariableDeclarationFragment newFrag = new VariableDeclarationFragment(
            frag.getVariableElement(), TreeUtil.remove(frag.getInitializer()));
        newDeclarations.put(dimensions, newFrag);
        iter.remove();
      } else {
        frag.setExtraDimensions(0);
      }
    }
    return newDeclarations;
  }

  /**
   * Verify, update property attributes. Accessor methods are not checked since a
   * property annotation may apply to separate variables in a field declaration, so
   * each variable needs to be checked separately during generation.
   */
  @Override
  public void endVisit(PropertyAnnotation node) {
    FieldDeclaration field = (FieldDeclaration) node.getParent();
    TypeMirror fieldType = field.getTypeMirror();
    VariableDeclarationFragment firstVarNode = field.getFragment(0);
    if (typeUtil.isString(fieldType)) {
      node.addAttribute("copy");
    } else if (ElementUtil.hasAnnotation(firstVarNode.getVariableElement(), Weak.class)) {
      if (node.hasAttribute("strong")) {
        ErrorUtil.error(field, "Weak field annotation conflicts with strong Property attribute");
        return;
      }
      node.addAttribute("weak");
    }

    node.removeAttribute("readwrite");
    node.removeAttribute("strong");
    node.removeAttribute("atomic");

    // Make sure attempt isn't made to specify an accessor method for fields with multiple
    // fragments, since each variable needs unique accessors.
    String getter = node.getGetter();
    String setter = node.getSetter();
    if (field.getFragments().size() > 1) {
      if (getter != null) {
        ErrorUtil.error(field, "@Property getter declared for multiple fields");
        return;
      }
      if (setter != null) {
        ErrorUtil.error(field, "@Property setter declared for multiple fields");
        return;
      }
    } else {
      // Check that specified accessors exist.
      TypeElement enclosingType = TreeUtil.getEnclosingTypeElement(node);
      if (getter != null) {
        if (ElementUtil.findMethod(enclosingType, getter) == null) {
          ErrorUtil.error(field, "Non-existent getter specified: " + getter);
        }
      }
      if (setter != null) {
        if (ElementUtil.findMethod(
            enclosingType, setter, TypeUtil.getQualifiedName(fieldType)) == null) {
          ErrorUtil.error(field, "Non-existent setter specified: " + setter);
        }
      }
    }
  }

  @Override
  public void endVisit(AnnotationTypeDeclaration node) {
    checkForNullabilityAnnotation(node);
  }

  @Override
  public void endVisit(EnumDeclaration node) {
    checkForNullabilityAnnotation(node);
  }

  @Override
  public void endVisit(TypeDeclaration node) {
    checkForNullabilityAnnotation(node);
  }

  private void checkForNullabilityAnnotation(AbstractTypeDeclaration node) {
    if (ElementUtil.hasAnnotation(node.getTypeElement(), ParametersAreNonnullByDefault.class)) {
      unit.setHasNullabilityAnnotations();
    }
  }

  @Override
  public void endVisit(PackageDeclaration node) {
    String pkgName = node.getName().toString();
    if (options.getPackageInfoLookup().hasParametersAreNonnullByDefault(pkgName)) {
      unit.setHasNullabilityAnnotations();
    }
  }

  @Override
  public boolean visit(TryStatement node) {
    // This visit rewrites try-with-resources constructs into regular try statements according to
    // JLS 14.20.3. The rewriting is done in a visit instead of endVisit because the mutations may
    // result in more try-with-resources constructs that need to be rewritten recursively.
    List<VariableDeclarationExpression> resources = node.getResources();
    if (resources.isEmpty()) {
      return true;
    }

    if (!node.getCatchClauses().isEmpty() || node.getFinally() != null) {
      // Extended try-with-resources. (JLS 14.20.3.2)
      // The new innerTry statement will be a "Basic try-with-resources" and will be rewritten by
      // the code below when it is visited.
      TryStatement innerTry = new TryStatement().setBody(TreeUtil.remove(node.getBody()));;
      TreeUtil.moveList(resources, innerTry.getResources());
      node.setBody(new Block().addStatement(innerTry));
      return true;
    }

    // Basic try-with-resources. (JLS 14.20.3.1)
    DeclaredType throwableType = (DeclaredType) typeUtil.getJavaThrowable().asType();
    VariableElement primaryException = GeneratedVariableElement.newLocalVar(
        "__primaryException" + resources.size(), throwableType, null);

    List<VariableDeclarationFragment> resourceFrags = resources.remove(0).getFragments();
    assert resourceFrags.size() == 1;
    VariableDeclarationFragment resourceFrag = resourceFrags.get(0);
    VariableElement resourceVar = resourceFrag.getVariableElement();

    DeclaredType closeableType =
        typeUtil.findSupertype(resourceVar.asType(), "java.lang.AutoCloseable");
    ExecutablePair closeMethod = typeUtil.findMethod(closeableType, "close");
    ExecutablePair addSuppressedMethod =
        typeUtil.findMethod(throwableType, "addSuppressed", "java.lang.Throwable");

    Block block = new Block();
    block.addStatement(new VariableDeclarationStatement(
        resourceVar, TreeUtil.remove(resourceFrag.getInitializer())));

    block.addStatement(new VariableDeclarationStatement(
        primaryException, new NullLiteral(typeUtil.getNull())));

    // If the current try node is the only statement in its parent block then replace the parent
    // block instead of the try node to avoid extra nesting of braces.
    TreeNode parent = node.getParent();
    if (parent instanceof Block && ((Block) parent).getStatements().size() == 1) {
      parent.replaceWith(block);
    } else {
      node.replaceWith(block);
    }
    block.addStatement(TreeUtil.remove(node));

    VariableElement caughtException =
        GeneratedVariableElement.newLocalVar("e", throwableType, null);
    Block catchBlock = new Block()
        .addStatement(new ExpressionStatement(new Assignment(
            new SimpleName(primaryException), new SimpleName(caughtException))))
        .addStatement(new ThrowStatement(new SimpleName(caughtException)));
    node.addCatchClause(new CatchClause()
        .setException(new SingleVariableDeclaration(caughtException))
        .setBody(catchBlock));

    Statement closeResource = new ExpressionStatement(new MethodInvocation(
        closeMethod, typeUtil.getVoid(), new SimpleName(resourceVar)));
    VariableElement suppressedException =
        GeneratedVariableElement.newLocalVar("e", throwableType, null);
    node.setFinally(new Block().addStatement(new IfStatement()
        .setExpression(new InfixExpression(
            typeUtil.getBoolean(), InfixExpression.Operator.NOT_EQUALS,
            new SimpleName(resourceVar), new NullLiteral(typeUtil.getNull())))
        .setThenStatement(new Block().addStatement(new IfStatement()
            .setExpression(new InfixExpression(
                typeUtil.getBoolean(), InfixExpression.Operator.NOT_EQUALS,
                new SimpleName(primaryException), new NullLiteral(typeUtil.getNull())))
            .setThenStatement(new Block().addStatement(new TryStatement()
                .setBody(new Block().addStatement(closeResource))
                .addCatchClause(new CatchClause()
                    .setException(new SingleVariableDeclaration(suppressedException))
                    .setBody(new Block().addStatement(new ExpressionStatement(new MethodInvocation(
                        addSuppressedMethod, typeUtil.getVoid(), new SimpleName(primaryException))
                        .addArgument(new SimpleName(suppressedException))))))))
            .setElseStatement(new Block().addStatement(closeResource.copy()))))));

    // Visit the new block instead of the current node because some of content of the node (eg. the
    // resource initializer) has been moved outside of the try node.
    block.accept(this);
    return false;
  }
}
