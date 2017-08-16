/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers.method;

import com.google.common.base.Optional;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.ExpressionTree;
import java.util.Arrays;
import java.util.regex.Pattern;

/** Matchers that select on method name. */
public abstract class MethodNameMatcherImpl extends AbstractChainedMatcher<MatchState, MatchState>
    implements MethodNameMatcher {

  MethodNameMatcherImpl(AbstractSimpleMatcher<MatchState> baseMatcher) {
    super(baseMatcher);
  }

  @Override
  public ParameterMatcher withParameters(String... parameters) {
    return withParameters(Arrays.asList(parameters));
  }

  @Override
  public ParameterMatcher withParameters(Iterable<String> parameters) {
    return new ParameterMatcherImpl(this, Suppliers.fromStrings(parameters));
  }

  /** Matches on exact method name. */
  static class Exact extends MethodNameMatcherImpl {
    private final String name;

    Exact(AbstractSimpleMatcher<MatchState> baseMatcher, String name) {
      super(baseMatcher);
      this.name = name;
    }

    @Override
    protected Optional<MatchState> matchResult(
        ExpressionTree item, MatchState method, VisitorState state) {
      if (!method.sym().getSimpleName().contentEquals(name)) {
        return Optional.absent();
      }
      return Optional.of(method);
    }
  }

  /** Matches any method name. */
  static class Any extends MethodNameMatcherImpl {
    Any(AbstractSimpleMatcher<MatchState> baseMatcher) {
      super(baseMatcher);
    }

    @Override
    protected Optional<MatchState> matchResult(
        ExpressionTree item, MatchState method, VisitorState state) {
      return Optional.of(method);
    }
  }

  /** Matches methods with a name that matches the given regular expression. */
  static class Regex extends MethodNameMatcherImpl {

    private final Pattern regex;

    Regex(AbstractSimpleMatcher<MatchState> baseMatcher, Pattern regex) {
      super(baseMatcher);
      this.regex = regex;
    }

    @Override
    protected Optional<MatchState> matchResult(
        ExpressionTree item, MatchState method, VisitorState state) {
      if (!regex.matcher(method.sym().getSimpleName().toString()).matches()) {
        return Optional.absent();
      }
      return Optional.of(method);
    }
  }
}
