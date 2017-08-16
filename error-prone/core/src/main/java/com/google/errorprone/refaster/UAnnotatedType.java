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

import static com.google.errorprone.refaster.Unifier.unifications;
import static com.google.errorprone.refaster.Unifier.unifyList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.util.List;

/**
 * {@code UTree} representation of an {@code AnnotatedTypeTree}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UAnnotatedType extends UExpression implements AnnotatedTypeTree {
  public static UAnnotatedType create(Iterable<UAnnotation> annotations, UExpression type) {
    return new AutoValue_UAnnotatedType(ImmutableList.copyOf(annotations), type);
  }

  @Override
  public Kind getKind() {
    return Kind.ANNOTATED_TYPE;
  }

  @Override
  public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
    return visitor.visitAnnotatedType(this, data);
  }

  @Override
  public Choice<Unifier> visitAnnotatedType(AnnotatedTypeTree node, Unifier unifier) {
    return unifyList(unifier, getAnnotations(), node.getAnnotations())
        .thenChoose(unifications(getUnderlyingType(), node.getUnderlyingType()));
  }

  @Override
  public JCAnnotatedType inline(Inliner inliner) throws CouldNotResolveImportException {
    return inliner
        .maker()
        .AnnotatedType(
            List.convert(JCAnnotation.class, inliner.inlineList(getAnnotations())),
            getUnderlyingType().inline(inliner));
  }

  @Override
  public abstract ImmutableList<UAnnotation> getAnnotations();

  @Override
  public abstract UExpression getUnderlyingType();
}
