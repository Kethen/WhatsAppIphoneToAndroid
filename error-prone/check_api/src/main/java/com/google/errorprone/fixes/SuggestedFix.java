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

package com.google.errorprone.fixes;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** @author alexeagle@google.com (Alex Eagle) */
public class SuggestedFix implements Fix {

  private final ImmutableList<FixOperation> fixes;
  private final ImmutableList<String> importsToAdd;
  private final ImmutableList<String> importsToRemove;

  private SuggestedFix(
      List<FixOperation> fixes, List<String> importsToAdd, List<String> importsToRemove) {
    this.fixes = ImmutableList.copyOf(fixes);
    this.importsToAdd = ImmutableList.copyOf(importsToAdd);
    this.importsToRemove = ImmutableList.copyOf(importsToRemove);
  }

  @Override
  public boolean isEmpty() {
    return fixes.isEmpty() && importsToAdd.isEmpty() && importsToRemove.isEmpty();
  }

  @Override
  public Collection<String> getImportsToAdd() {
    return importsToAdd;
  }

  @Override
  public Collection<String> getImportsToRemove() {
    return importsToRemove;
  }

  @Override
  public String toString(JCCompilationUnit compilationUnit) {
    StringBuilder result = new StringBuilder("replace ");
    for (Replacement replacement : getReplacements(compilationUnit.endPositions)) {
      result.append(
          String.format(
              "position %d:%d with \"%s\" ",
              replacement.startPosition(), replacement.endPosition(), replacement.replaceWith()));
    }
    return result.toString();
  }

  @Override
  public Set<Replacement> getReplacements(EndPosTable endPositions) {
    if (endPositions == null) {
      throw new IllegalArgumentException(
          "Cannot produce correct replacements without endPositions.");
    }
    Replacements replacements = new Replacements();
    for (FixOperation fix : fixes) {
      replacements.add(fix.getReplacement(endPositions));
    }
    return replacements.descending();
  }

  /** {@link Builder#replace(Tree, String)} */
  public static SuggestedFix replace(Tree tree, String replaceWith) {
    return builder().replace(tree, replaceWith).build();
  }

  /**
   * Replace the characters from startPos, inclusive, until endPos, exclusive, with the given
   * string.
   *
   * @param startPos The position from which to start replacing, inclusive
   * @param endPos The position at which to end replacing, exclusive
   * @param replaceWith The string to replace with
   */
  public static SuggestedFix replace(int startPos, int endPos, String replaceWith) {
    return builder().replace(startPos, endPos, replaceWith).build();
  }

  /**
   * Replace a tree node with a string, but adjust the start and end positions as well. For example,
   * if the tree node begins at index 10 and ends at index 30, this call will replace the characters
   * at index 15 through 25 with "replacement":
   *
   * <pre>
   * {@code fix.replace(node, "replacement", 5, -5)}
   * </pre>
   *
   * @param node The tree node to replace
   * @param replaceWith The string to replace with
   * @param startPosAdjustment The adjustment to add to the start position (negative is OK)
   * @param endPosAdjustment The adjustment to add to the end position (negative is OK)
   */
  public static SuggestedFix replace(
      Tree node, String replaceWith, int startPosAdjustment, int endPosAdjustment) {
    return builder().replace(node, replaceWith, startPosAdjustment, endPosAdjustment).build();
  }

  /** {@link Builder#prefixWith(Tree, String)} */
  public static SuggestedFix prefixWith(Tree node, String prefix) {
    return builder().prefixWith(node, prefix).build();
  }

  /** {@link Builder#postfixWith(Tree, String)} */
  public static SuggestedFix postfixWith(Tree node, String postfix) {
    return builder().postfixWith(node, postfix).build();
  }

  /** {@link Builder#delete(Tree)} */
  public static SuggestedFix delete(Tree node) {
    return builder().delete(node).build();
  }

  /** {@link Builder#swap(Tree, Tree)} */
  public static SuggestedFix swap(Tree node1, Tree node2) {
    return builder().swap(node1, node2).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builds {@link SuggestedFix}s. */
  public static class Builder {

    private final List<FixOperation> fixes = new ArrayList<>();
    private final List<String> importsToAdd = new ArrayList<>();
    private final List<String> importsToRemove = new ArrayList<>();

    protected Builder() {}

    public boolean isEmpty() {
      return fixes.isEmpty() && importsToAdd.isEmpty() && importsToRemove.isEmpty();
    }

    public SuggestedFix build() {
      return new SuggestedFix(fixes, importsToAdd, importsToRemove);
    }

    private Builder with(FixOperation fix) {
      fixes.add(fix);
      return this;
    }

    public Builder replace(Tree node, String replaceWith) {
      checkNotSyntheticConstructor(node);
      return with(new ReplacementFix((DiagnosticPosition) node, replaceWith));
    }

    /**
     * Replace the characters from startPos, inclusive, until endPos, exclusive, with the given
     * string.
     *
     * @param startPos The position from which to start replacing, inclusive
     * @param endPos The position at which to end replacing, exclusive
     * @param replaceWith The string to replace with
     */
    public Builder replace(int startPos, int endPos, String replaceWith) {
      DiagnosticPosition pos = new IndexedPosition(startPos, endPos);
      return with(new ReplacementFix(pos, replaceWith));
    }

    /**
     * Replace a tree node with a string, but adjust the start and end positions as well. For
     * example, if the tree node begins at index 10 and ends at index 30, this call will replace the
     * characters at index 15 through 25 with "replacement":
     *
     * <pre>
     * {@code fix.replace(node, "replacement", 5, -5)}
     * </pre>
     *
     * @param node The tree node to replace
     * @param replaceWith The string to replace with
     * @param startPosAdjustment The adjustment to add to the start position (negative is OK)
     * @param endPosAdjustment The adjustment to add to the end position (negative is OK)
     */
    public Builder replace(
        Tree node, String replaceWith, int startPosAdjustment, int endPosAdjustment) {
      checkNotSyntheticConstructor(node);
      return with(
          new ReplacementFix(
              new AdjustedPosition((JCTree) node, startPosAdjustment, endPosAdjustment),
              replaceWith));
    }

    public Builder prefixWith(Tree node, String prefix) {
      checkNotSyntheticConstructor(node);
      return with(new PrefixInsertion((DiagnosticPosition) node, prefix));
    }

    public Builder postfixWith(Tree node, String postfix) {
      checkNotSyntheticConstructor(node);
      return with(new PostfixInsertion((DiagnosticPosition) node, postfix));
    }

    public Builder delete(Tree node) {
      checkNotSyntheticConstructor(node);
      return replace(node, "");
    }

    public Builder swap(Tree node1, Tree node2) {
      checkNotSyntheticConstructor(node1);
      checkNotSyntheticConstructor(node2);
      // calling Tree.toString() is kind of cheesy, but we don't currently have a better option
      // TODO(cushon): consider an approach that doesn't rewrite the original tokens
      fixes.add(new ReplacementFix((DiagnosticPosition) node1, node2.toString()));
      fixes.add(new ReplacementFix((DiagnosticPosition) node2, node1.toString()));
      return this;
    }

    /**
     * Add an import statement as part of this SuggestedFix. Import string should be of the form
     * "foo.bar.baz".
     */
    public Builder addImport(String importString) {
      importsToAdd.add("import " + importString);
      return this;
    }

    /**
     * Add a static import statement as part of this SuggestedFix. Import string should be of the
     * form "foo.bar.baz".
     */
    public Builder addStaticImport(String importString) {
      importsToAdd.add("import static " + importString);
      return this;
    }

    /**
     * Remove an import statement as part of this SuggestedFix. Import string should be of the form
     * "foo.bar.baz".
     */
    public Builder removeImport(String importString) {
      importsToRemove.add("import " + importString);
      return this;
    }

    /**
     * Remove a static import statement as part of this SuggestedFix. Import string should be of the
     * form "foo.bar.baz".
     */
    public Builder removeStaticImport(String importString) {
      importsToRemove.add("import static " + importString);
      return this;
    }

    /**
     * Merges all edits from {@code other} into {@code this}. If {@code other} is null, do nothing.
     */
    public Builder merge(@Nullable Builder other) {
      if (other == null) {
        return this;
      }
      fixes.addAll(other.fixes);
      importsToAdd.addAll(other.importsToAdd);
      importsToRemove.addAll(other.importsToRemove);
      return this;
    }

    /**
     * Merges all edits from {@code other} into {@code this}. If {@code other} is null, do nothing.
     */
    public Builder merge(@Nullable SuggestedFix other) {
      if (other == null) {
        return this;
      }
      fixes.addAll(other.fixes);
      importsToAdd.addAll(other.importsToAdd);
      importsToRemove.addAll(other.importsToRemove);
      return this;
    }

    /**
     * Implicit default constructors are one of the few synthetic constructs added to the AST early
     * enough to be visible from Error Prone, so we do a sanity-check here to prevent attempts to
     * edit them.
     */
    private static void checkNotSyntheticConstructor(Tree tree) {
      if (tree instanceof MethodTree && ASTHelpers.isGeneratedConstructor((MethodTree) tree)) {
        throw new AssertionError("Cannot edit synthetic AST nodes");
      }
    }
  }

  /** Models a single fix operation. */
  private static interface FixOperation {
    /** Calculate the replacement operation once end positions are available. */
    Replacement getReplacement(EndPosTable endPositions);
  }

  /** Inserts new text at a specific insertion point (e.g. prefix or postfix). */
  private abstract static class InsertionFix implements FixOperation {
    protected abstract int getInsertionIndex(EndPosTable endPositions);

    protected final DiagnosticPosition position;
    protected final String insertion;

    protected InsertionFix(DiagnosticPosition position, String insertion) {
      checkArgument(position.getStartPosition() >= 0, "invalid start position");
      this.position = position;
      this.insertion = insertion;
    }

    @Override
    public Replacement getReplacement(EndPosTable endPositions) {
      int insertionIndex = getInsertionIndex(endPositions);
      return Replacement.create(insertionIndex, insertionIndex, insertion);
    }
  }

  private static class PostfixInsertion extends InsertionFix {
    public PostfixInsertion(DiagnosticPosition tree, String insertion) {
      super(tree, insertion);
    }

    @Override
    protected int getInsertionIndex(EndPosTable endPositions) {
      return position.getEndPosition(endPositions);
    }
  }

  private static class PrefixInsertion extends InsertionFix {
    public PrefixInsertion(DiagnosticPosition tree, String insertion) {
      super(tree, insertion);
    }

    @Override
    protected int getInsertionIndex(EndPosTable endPositions) {
      return position.getStartPosition();
    }
  }

  /** Replaces an entire diagnostic position (from start to end) with the given string. */
  private static class ReplacementFix implements FixOperation {
    private final DiagnosticPosition original;
    private final String replacement;

    public ReplacementFix(DiagnosticPosition original, String replacement) {
      checkArgument(original.getStartPosition() >= 0, "invalid start position");
      this.original = original;
      this.replacement = replacement;
    }

    @Override
    public Replacement getReplacement(EndPosTable endPositions) {
      return Replacement.create(
          original.getStartPosition(), original.getEndPosition(endPositions), replacement);
    }
  }

  /** @deprecated prefer {@link SuggestedFixes#addModifiers} */
  @Deprecated
  public static Fix addModifier(Tree tree, Modifier modifier, VisitorState state) {
    return SuggestedFixes.addModifiers(tree, state, modifier);
  }

  /** @deprecated prefer {@link SuggestedFixes#removeModifiers} */
  @Deprecated
  public static Fix removeModifier(Tree tree, Modifier modifier, VisitorState state) {
    return SuggestedFixes.removeModifiers(tree, state, modifier);
  }
}
