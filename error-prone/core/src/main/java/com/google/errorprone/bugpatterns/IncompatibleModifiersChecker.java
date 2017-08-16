/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.IncompatibleModifiers;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
@BugPattern(
  name = "IncompatibleModifiers",
  summary =
      "This annotation has incompatible modifiers as specified by its "
          + "@IncompatibleModifiers annotation",
  explanation =
      "The @IncompatibleModifiers annotation declares that the target annotation "
          + "is incompatible with a set of provided modifiers. This check ensures that all "
          + "annotations respect their @IncompatibleModifiers specifications.",
  linkType = NONE,
  category = JDK,
  severity = WARNING,
  tags = StandardTags.LIKELY_ERROR
)

// TODO(cushon): merge the implementation with RequiredModifiersChecker
public class IncompatibleModifiersChecker extends BugChecker implements AnnotationTreeMatcher {

  private static final String MESSAGE_TEMPLATE =
      "%s has specified that it should not be used" + " together with the following modifiers: %s";

  // TODO(cushon): deprecate and remove
  private static final String GUAVA_ANNOTATION =
      "com.google.common.annotations.IncompatibleModifiers";

  private static final Function<Attribute.Enum, Modifier> TO_MODIFIER =
      new Function<Attribute.Enum, Modifier>() {
        @Override
        public Modifier apply(Attribute.Enum input) {
          return Modifier.valueOf(input.getValue().name.toString());
        }
      };

  private static Set<Modifier> getIncompatibleModifiers(AnnotationTree tree, VisitorState state) {
    for (Attribute.Compound c : ASTHelpers.getSymbol(tree).getAnnotationMirrors()) {
      if (((TypeElement) c.getAnnotationType().asElement())
          .getQualifiedName()
          .contentEquals(GUAVA_ANNOTATION)) {
        @SuppressWarnings("unchecked")
        List<Attribute.Enum> modifiers =
            (List<Attribute.Enum>) c.member(state.getName("value")).getValue();
        return ImmutableSet.copyOf(Iterables.transform(modifiers, TO_MODIFIER));
      }
    }

    IncompatibleModifiers annotation = ASTHelpers.getAnnotation(tree, IncompatibleModifiers.class);
    if (annotation != null) {
      return ImmutableSet.copyOf(annotation.value());
    }

    return ImmutableSet.of();
  }

  @Override
  public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
    Set<Modifier> incompatibleModifiers = getIncompatibleModifiers(tree, state);
    if (incompatibleModifiers.isEmpty()) {
      return Description.NO_MATCH;
    }

    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof ModifiersTree)) {
      // e.g. An annotated package name
      return Description.NO_MATCH;
    }

    Set<Modifier> incompatible =
        Sets.intersection(incompatibleModifiers, ((ModifiersTree) parent).getFlags());

    if (incompatible.isEmpty()) {
      return Description.NO_MATCH;
    }

    String annotationName = ASTHelpers.getAnnotationName(tree);
    String nameString =
        annotationName != null
            ? String.format("The annotation '@%s'", annotationName)
            : "This annotation";
    String customMessage = String.format(MESSAGE_TEMPLATE, nameString, incompatible.toString());
    return buildDescription(tree).setMessage(customMessage).build();
  }
}
