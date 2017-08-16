/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.errorprone.CodeTransformer;
import com.google.testing.compile.JavaFileObjects;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.io.IOException;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for Refaster templates.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class TemplateIntegrationTest extends CompilerBasedTest {
  private CodeTransformer extractRefasterRule(JavaFileObject object) {
    compile(object);
    ClassTree classTree =
        Iterables.getOnlyElement(
            FluentIterable.from(compilationUnits)
                .transformAndConcat(
                    new Function<CompilationUnitTree, Iterable<? extends Tree>>() {
                      @Override
                      public Iterable<? extends Tree> apply(CompilationUnitTree input) {
                        return input.getTypeDecls();
                      }
                    })
                .filter(ClassTree.class));
    return Iterables.getOnlyElement(RefasterRuleBuilderScanner.extractRules(classTree, context));
  }

  private void expectTransforms(
      CodeTransformer transformer, JavaFileObject input, JavaFileObject expectedOutput)
      throws IOException {
    JavaFileObject transformedInput =
        CodeTransformerTestHelper.create(transformer).transform(input);

    // TODO(lowasser): modify compile-testing to enable direct tree comparison
    assert_().about(javaSource()).that(transformedInput).compilesWithoutError();
    String expectedSource = expectedOutput.getCharContent(false).toString();
    String actualSource = transformedInput.getCharContent(false).toString();
    assertThat(CharMatcher.whitespace().collapseFrom(actualSource, ' '))
        .isEqualTo(CharMatcher.whitespace().collapseFrom(expectedSource, ' '));
  }

  private static final String TEMPLATE_DIR = "com/google/errorprone/refaster/testdata/template";
  private static final String INPUT_DIR = "com/google/errorprone/refaster/testdata/input";
  private static final String OUTPUT_DIR = "com/google/errorprone/refaster/testdata/output";

  private void runTest(String testName) throws IOException {
    CodeTransformer transformer =
        extractRefasterRule(
            JavaFileObjects.forResource(String.format("%s/%s.java", TEMPLATE_DIR, testName)));

    JavaFileObject input =
        JavaFileObjects.forResource(String.format("%s/%sExample.java", INPUT_DIR, testName));
    JavaFileObject output =
        JavaFileObjects.forResource(String.format("%s/%sExample.java", OUTPUT_DIR, testName));
    expectTransforms(transformer, input, output);
  }

  @Test
  public void binary() throws IOException {
    runTest("BinaryTemplate");
  }

  @Test
  public void parenthesesOptional() throws IOException {
    runTest("ParenthesesOptionalTemplate");
  }

  @Test
  public void multipleReferencesToIdentifier() throws IOException {
    runTest("MultipleReferencesToIdentifierTemplate");
  }

  @Test
  public void methodInvocation() throws IOException {
    runTest("MethodInvocationTemplate");
  }

  @Test
  public void explicitTypesPreserved() throws IOException {
    runTest("ExplicitTypesPreservedTemplate");
  }

  @Test
  public void implicitTypesInlined() throws IOException {
    runTest("ImplicitTypesInlinedTemplate");
  }

  @Test
  public void autoboxing() throws IOException {
    runTest("AutoboxingTemplate");
  }

  @Test
  public void array() throws IOException {
    runTest("ArrayTemplate");
  }

  @Test
  public void precedenceSensitive() throws IOException {
    runTest("PrecedenceSensitiveTemplate");
  }

  @Test
  public void staticField() throws IOException {
    runTest("StaticFieldTemplate");
  }

  @Test
  public void isInstance() throws IOException {
    runTest("IsInstanceTemplate");
  }

  @Test
  public void anyOf() throws IOException {
    runTest("AnyOfTemplate");
  }

  @Test
  public void repeated() throws IOException {
    runTest("VarargTemplate");
  }

  @Test
  public void ifTemplate() throws IOException {
    runTest("IfTemplate");
  }

  @Test
  public void variableDecl() throws IOException {
    runTest("VariableDeclTemplate");
  }

  @Test
  public void inferredThis() throws IOException {
    runTest("InferredThisTemplate");
  }

  @Test
  public void twoLinesToOne() throws IOException {
    runTest("TwoLinesToOneTemplate");
  }

  @Test
  public void oneLineToTwo() throws IOException {
    runTest("OneLineToTwoTemplate");
  }

  @Test
  public void tryCatch() throws IOException {
    runTest("TryTemplate");
  }

  @Test
  public void tryMultiCatch() throws IOException {
    runTest("TryMultiCatchTemplate");
  }

  @Test
  public void wildcard() throws IOException {
    runTest("WildcardTemplate");
  }

  @Test
  public void freeIdentWildcardCapture() throws IOException {
    runTest("WildcardUnificationTemplate");
  }

  @Test
  public void labeledStatements() throws IOException {
    runTest("LabelTemplate");
  }

  @Test
  public void expressionPlaceholder() throws IOException {
    runTest("PlaceholderTemplate");
  }

  @Test
  public void expressionPlaceholderAllowsIdentity() throws IOException {
    runTest("PlaceholderAllowsIdentityTemplate");
  }

  @Test
  public void blockPlaceholder() throws IOException {
    runTest("BlockPlaceholderTemplate");
  }

  @Test
  public void genericPlaceholder() throws IOException {
    runTest("GenericPlaceholderTemplate");
  }

  @Test
  public void mayOptionallyUse() throws IOException {
    runTest("MayOptionallyUseTemplate");
  }

  @Test
  public void comparisonChain() throws IOException {
    runTest("ComparisonChainTemplate");
  }

  @Test
  public void multibound() throws IOException {
    runTest("MultiBoundTemplate");
  }

  @Test
  public void topLevel() throws IOException {
    runTest("TopLevelTemplate");
  }

  @Test
  public void diamond() throws IOException {
    runTest("DiamondTemplate");
  }

  @Test
  public void anonymousClass() throws IOException {
    runTest("AnonymousClassTemplate");
  }

  @Test
  public void returnPlaceholder() throws IOException {
    runTest("ReturnPlaceholderTemplate");
  }

  @Test
  public void literal() throws IOException {
    runTest("LiteralTemplate");
  }

  @Test
  public void importClassDirectly() throws IOException {
    runTest("ImportClassDirectlyTemplate");
  }

  @Test
  public void assertions() throws IOException {
    runTest("AssertTemplate");
  }

  @Test
  public void samePackageImports() throws IOException {
    runTest("SamePackageImportsTemplate");
  }

  @Test
  public void ifFallthrough() throws IOException {
    runTest("IfFallthroughTemplate");
  }

  @Test
  public void emitCommentBefore() throws IOException {
    runTest("EmitCommentBeforeTemplate");
  }

  @Test
  public void emitComment() throws IOException {
    runTest("EmitCommentTemplate");
  }

  @Test
  public void nestedClass() throws IOException {
    runTest("NestedClassTemplate");
  }

  @Test
  public void inferLambdaType() throws IOException {
    runTest("InferLambdaType");
  }

  @Test
  public void lambdaImplicitType() throws IOException {
    runTest("LambdaImplicitType");
  }

  @Test
  public void inferLambdaBodyType() throws IOException {
    runTest("InferLambdaBodyType");
  }

  @Test
  public void asVarargs() throws IOException {
    runTest("AsVarargsTemplate");
  }

  @Test
  public void placeholderAllowedVars() throws IOException {
    runTest("PlaceholderAllowedVarsTemplate");
  }
}
