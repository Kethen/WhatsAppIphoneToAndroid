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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.base.Objects;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;

@BugPattern(
  name = "TypeParameterShadowing",
  summary = "Type parameter declaration overrides another type parameter already declared",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.STYLE
)
public class TypeParameterShadowing extends BugChecker
    implements MethodTreeMatcher, ClassTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getTypeParameters().isEmpty()) {
      return Description.NO_MATCH;
    }
    return findDuplicatesOf(tree, tree.getTypeParameters(), state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getTypeParameters().isEmpty()) {
      return Description.NO_MATCH;
    }
    return findDuplicatesOf(tree, tree.getTypeParameters(), state);
  }

  private Description findDuplicatesOf(
      Tree tree, List<? extends TypeParameterTree> typeParameters, VisitorState state) {

    Symbol symbol = ASTHelpers.getSymbol(tree);
    if (symbol == null) {
      return Description.NO_MATCH;
    }
    List<TypeVariableSymbol> enclosingTypeSymbols = typeVariablesEnclosing(symbol);
    if (enclosingTypeSymbols.isEmpty()) {
      return Description.NO_MATCH;
    }

    List<TypeVariableSymbol> conflictingTypeSymbols = new ArrayList<>();
    typeParameters.forEach(
        param ->
            enclosingTypeSymbols
                .stream()
                .filter(tvs -> tvs.name.contentEquals(param.getName()))
                .findFirst()
                .ifPresent(conflictingTypeSymbols::add));

    if (conflictingTypeSymbols.isEmpty()) {
      return Description.NO_MATCH;
    }

    Description.Builder descriptionBuilder = buildDescription(tree);
    String message =
        "Found aliased type parameters: "
            + conflictingTypeSymbols
                .stream()
                .map(tvs -> tvs.name + " declared in " + tvs.owner.getSimpleName())
                .collect(Collectors.joining("\n"));

    descriptionBuilder.setMessage(message);

    Set<String> typeVarsInScope =
        Streams.concat(enclosingTypeSymbols.stream(), symbol.getTypeParameters().stream())
            .map(v -> v.name.toString())
            .collect(toImmutableSet());
    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
    conflictingTypeSymbols
        .stream()
        .map(
            v ->
                renameTypeVariable(
                    tree,
                    typeParameters,
                    v.name,
                    replacementTypeVarName(v.name, typeVarsInScope),
                    state))
        .forEach(fixBuilder::merge);

    descriptionBuilder.addFix(fixBuilder.build());
    return descriptionBuilder.build();
  }

  private static final Pattern TRAILING_DIGIT_EXTRACTOR = Pattern.compile("^(.*?)(\\d+)$");
  // T -> T2
  // T2 -> T3
  // T -> T4 (if T2 and T3 already exist)
  private String replacementTypeVarName(Name name, Set<String> superTypeVars) {
    String baseName = name.toString();
    int typeVarNum = 2;

    Matcher matcher = TRAILING_DIGIT_EXTRACTOR.matcher(name);
    if (matcher.matches()) {
      baseName = matcher.group(1);
      typeVarNum = Integer.parseInt(matcher.group(2)) + 1;
    }

    String replacementName;
    while (superTypeVars.contains(replacementName = baseName + typeVarNum)) {
      typeVarNum++;
    }

    return replacementName;
  }

  private static SuggestedFix renameTypeVariable(
      Tree sourceTree,
      List<? extends TypeParameterTree> typeParameters,
      Name typeVariable,
      String typeVarReplacement,
      VisitorState state) {

    TypeParameterTree matchingTypeParam =
        typeParameters
            .stream()
            .filter(t -> t.getName().contentEquals(typeVariable))
            .collect(MoreCollectors.onlyElement());
    Symbol typeVariableSymbol = ASTHelpers.getSymbol(matchingTypeParam);

    // replace only the type parameter name (and not any upper bounds)
    String name = matchingTypeParam.getName().toString();
    int pos = ((JCTree) matchingTypeParam).getStartPosition();
    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder().replace(pos, pos + name.length(), typeVarReplacement);

    ((JCTree) sourceTree)
        .accept(
            new TreeScanner() {
              @Override
              public void visitIdent(JCTree.JCIdent tree) {
                Symbol identSym = ASTHelpers.getSymbol(tree);
                if (Objects.equal(identSym, typeVariableSymbol)) {
                  // Lambda parameters can be desugared early, so we need to make sure the source
                  // is there. In the example below, we would try to suggest replacing the node 't'
                  // with T2, since the compiler desugars to g((T t) -> false). The extra condition
                  // prevents us from doing that.

                  // Foo<T> {
                  //   <G> void g(Predicate<G> p) {},
                  //   <T> void blah() {
                  //     g(t -> false);
                  //   }
                  // }
                  if (Objects.equal(state.getSourceForNode(tree), name)) {
                    fixBuilder.replace(tree, typeVarReplacement);
                  }
                }
              }
            });
    return fixBuilder.build();
  }

  private static List<TypeVariableSymbol> typeVariablesEnclosing(Symbol sym) {
    List<TypeVariableSymbol> typeVarScopes = new ArrayList<>();
    outer:
    while (!sym.isStatic()) {
      sym = sym.owner;
      switch (sym.getKind()) {
        case PACKAGE:
          break outer;
        case METHOD:
        case CLASS:
          typeVarScopes.addAll(sym.getTypeParameters());
          break;
        default: // fall out
      }
    }
    return typeVarScopes;
  }
}
