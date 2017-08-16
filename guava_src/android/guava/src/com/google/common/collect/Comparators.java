/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Provides static methods for working with {@link Comparator} instances. For many other helpful
 * comparator utilities, see either {@code Comparator} itself (for Java 8 or later), or
 * {@code com.google.common.collect.Ordering} (otherwise).
 *
 * <h3>Relationship to {@code Ordering}</h3>
 *
 * <p>In light of the significant enhancements to {@code Comparator} in Java 8, the overwhelming
 * majority of usages of {@code Ordering} can be written using only built-in JDK APIs. Because of
 * this, and because it's awkward to have to convert comparators into {@code Ordering} instances,
 * {@code Ordering} and its methods are planned for deletion. This class is intended to
 * "fill the gap" and provide those features of {@code Ordering} not already provided by the JDK.
 *
 * @since 21.0
 * @author Louis Wasserman
 */
@Beta
@GwtCompatible
public final class Comparators {
  private Comparators() {}

  /**
   * Returns a new comparator which sorts iterables by comparing corresponding elements pairwise
   * until a nonzero result is found; imposes "dictionary order." If the end of one iterable is
   * reached, but not the other, the shorter iterable is considered to be less than the longer one.
   * For example, a lexicographical natural ordering over integers considers {@code
   * [] < [1] < [1, 1] < [1, 2] < [2]}.
   *
   * <p>Note that {@code Collections.reverseOrder(lexicographical(comparator))} is not
   * equivalent to {@code lexicographical(Collections.reverseOrder(comparator))} (consider how each
   * would order {@code [1]} and {@code [1, 1]}).
   */
  // Note: 90% of the time we don't add type parameters or wildcards that serve only to "tweak" the
  // desired return type. However, *nested* generics introduce a special class of problems that we
  // think tip it over into being worthwhile.
  public static <T, S extends T> Comparator<Iterable<S>> lexicographical(Comparator<T> comparator) {
    return new LexicographicalOrdering<S>(checkNotNull(comparator));
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is greater than or
   * equal to the element that preceded it, according to the specified comparator. Note that this
   * is always true when the iterable has fewer than two elements.
   */
  public static <T> boolean isInOrder(Iterable<? extends T> iterable, Comparator<T> comparator) {
    checkNotNull(comparator);
    Iterator<? extends T> it = iterable.iterator();
    if (it.hasNext()) {
      T prev = it.next();
      while (it.hasNext()) {
        T next = it.next();
        if (comparator.compare(prev, next) > 0) {
          return false;
        }
        prev = next;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if each element in {@code iterable} after the first is <i>strictly</i>
   * greater than the element that preceded it, according to the specified comparator. Note that
   * this is always true when the iterable has fewer than two elements.
   */
  public static <T> boolean isInStrictOrder(
      Iterable<? extends T> iterable, Comparator<T> comparator) {
    checkNotNull(comparator);
    Iterator<? extends T> it = iterable.iterator();
    if (it.hasNext()) {
      T prev = it.next();
      while (it.hasNext()) {
        T next = it.next();
        if (comparator.compare(prev, next) >= 0) {
          return false;
        }
        prev = next;
      }
    }
    return true;
  }
}
