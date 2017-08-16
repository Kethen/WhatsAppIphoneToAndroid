/*
 * Copyright (C) 2008 The Guava Authors
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

import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

/**
 * A {@link BiMap} whose contents will never change, with many other important properties detailed
 * at {@link ImmutableCollection}.
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
public abstract class ImmutableBiMap<K, V> extends ImmutableMap<K, V> implements BiMap<K, V> {

  /**
   * Returns the empty bimap.
   */
  // Casting to any type is safe because the set will never hold any elements.
  @SuppressWarnings("unchecked")
  public static <K, V> ImmutableBiMap<K, V> of() {
    return (ImmutableBiMap<K, V>) RegularImmutableBiMap.EMPTY;
  }

  /**
   * Returns an immutable bimap containing a single entry.
   */
  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
    checkEntryNotNull(k1, v1);
    return new RegularImmutableBiMap<K, V>(new Object[] {k1, v1}, 1);
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys or values are added
   */
  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    return new RegularImmutableBiMap<K, V>(new Object[] {k1, v1, k2, v2}, 2);
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys or values are added
   */
  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    checkEntryNotNull(k3, v3);
    return new RegularImmutableBiMap<K, V>(
        new Object[] {k1, v1, k2, v2, k3, v3}, 3);
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys or values are added
   */
  public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    checkEntryNotNull(k3, v3);
    checkEntryNotNull(k4, v4);
    return new RegularImmutableBiMap<K, V>(
        new Object[] {k1, v1, k2, v2, k3, v3, k4, v4}, 4);
  }

  /**
   * Returns an immutable map containing the given entries, in order.
   *
   * @throws IllegalArgumentException if duplicate keys or values are added
   */
  public static <K, V> ImmutableBiMap<K, V> of(
      K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
    checkEntryNotNull(k1, v1);
    checkEntryNotNull(k2, v2);
    checkEntryNotNull(k3, v3);
    checkEntryNotNull(k4, v4);
    checkEntryNotNull(k5, v5);
    return new RegularImmutableBiMap<K, V>(
        new Object[] {k1, v1, k2, v2, k3, v3, k4, v4, k5, v5}, 5);
  }

  // looking for of() with > 5 entries? Use the builder instead.

  /**
   * Returns a new builder. The generated builder is equivalent to the builder
   * created by the {@link Builder} constructor.
   */
  public static <K, V> Builder<K, V> builder() {
    return new Builder<K, V>();
  }

  /**
   * Returns a new builder, expecting the specified number of entries to be added.
   *
   * <p>If {@code expectedSize} is exactly the number of entries added to the builder before {@link
   * Builder#build} is called, the builder is likely to perform better than an unsized {@link
   * #builder()} would have.
   *
   * <p>It is not specified if any performance benefits apply if {@code expectedSize} is close to,
   * but not exactly, the number of entries added to the builder.
   * 
   * @since 24.0
   */
  @Beta
  public static <K, V> Builder<K, V> builderWithExpectedSize(int expectedSize) {
    checkNonnegative(expectedSize, "expectedSize");
    return new Builder<K, V>(expectedSize);
  }

  /**
   * A builder for creating immutable bimap instances, especially {@code public
   * static final} bimaps ("constant bimaps"). Example: <pre>   {@code
   *
   *   static final ImmutableBiMap<String, Integer> WORD_TO_INT =
   *       new ImmutableBiMap.Builder<String, Integer>()
   *           .put("one", 1)
   *           .put("two", 2)
   *           .put("three", 3)
   *           .build();}</pre>
   *
   * <p>For <i>small</i> immutable bimaps, the {@code ImmutableBiMap.of()} methods
   * are even more convenient.
   *
   * <p>By default, a {@code Builder} will generate bimaps that iterate over entries in the order
   * they were inserted into the builder.  For example, in the above example,
   * {@code WORD_TO_INT.entrySet()} is guaranteed to iterate over the entries in the order
   * {@code "one"=1, "two"=2, "three"=3}, and {@code keySet()} and {@code values()} respect the same
   * order. If you want a different order, consider using
   * {@link #orderEntriesByValue(Comparator)}, which changes this builder to sort
   * entries by value.
   *
   * <p>Builder instances can be reused - it is safe to call {@link #build}
   * multiple times to build multiple bimaps in series. Each bimap is a superset
   * of the bimaps created before it.
   *
   * @since 2.0
   */
  public static final class Builder<K, V> extends ImmutableMap.Builder<K, V> {
    /**
     * Creates a new builder. The returned builder is equivalent to the builder
     * generated by {@link ImmutableBiMap#builder}.
     */
    public Builder() {
      super();
    }

    Builder(int size) {
      super(size);
    }
    
    /**
     * Associates {@code key} with {@code value} in the built bimap. Duplicate
     * keys or values are not allowed, and will cause {@link #build} to fail.
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> put(K key, V value) {
      super.put(key, value);
      return this;
    }

    /**
     * Adds the given {@code entry} to the bimap.  Duplicate keys or values
     * are not allowed, and will cause {@link #build} to fail.
     *
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
      super.put(entry);
      return this;
    }

    /**
     * Associates all of the given map's keys and values in the built bimap.
     * Duplicate keys or values are not allowed, and will cause {@link #build}
     * to fail.
     *
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    @CanIgnoreReturnValue
    @Override
    public Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
      super.putAll(map);
      return this;
    }

    /**
     * Adds all of the given entries to the built bimap.  Duplicate keys or
     * values are not allowed, and will cause {@link #build} to fail.
     *
     * @throws NullPointerException if any key, value, or entry is null
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Beta
    @Override
    public Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      super.putAll(entries);
      return this;
    }

    /**
     * Configures this {@code Builder} to order entries by value according to the specified
     * comparator.
     *
     * <p>The sort order is stable, that is, if two entries have values that compare
     * as equivalent, the entry that was inserted first will be first in the built map's
     * iteration order.
     *
     * @throws IllegalStateException if this method was already called
     * @since 19.0
     */
    @CanIgnoreReturnValue
    @Beta
    @Override
    public Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
      super.orderEntriesByValue(valueComparator);
      return this;
    }

    /**
     * Returns a newly-created immutable bimap.  The iteration order of the returned bimap is
     * the order in which entries were inserted into the builder, unless
     * {@link #orderEntriesByValue} was called, in which case entries are sorted by value.
     *
     * @throws IllegalArgumentException if duplicate keys or values were added
     */
    @Override
    public ImmutableBiMap<K, V> build() {
      if (size == 0) {
        return of();
      }
      sortEntries();
      entriesUsed = true;
      return new RegularImmutableBiMap<K, V>(alternatingKeysAndValues, size);
    }
  }

  /**
   * Returns an immutable bimap containing the same entries as {@code map}. If
   * {@code map} somehow contains entries with duplicate keys (for example, if
   * it is a {@code SortedMap} whose comparator is not <i>consistent with
   * equals</i>), the results of this method are undefined.
   *
   * <p>The returned {@code BiMap} iterates over entries in the same order as the
   * {@code entrySet} of the original map.
   *
   * <p>Despite the method name, this method attempts to avoid actually copying
   * the data when it is safe to do so. The exact circumstances under which a
   * copy will or will not be performed are undocumented and subject to change.
   *
   * @throws IllegalArgumentException if two keys have the same value or two values have the same
   *     key
   * @throws NullPointerException if any key or value in {@code map} is null
   */
  public static <K, V> ImmutableBiMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
    if (map instanceof ImmutableBiMap) {
      @SuppressWarnings("unchecked") // safe since map is not writable
      ImmutableBiMap<K, V> bimap = (ImmutableBiMap<K, V>) map;
      // TODO(lowasser): if we need to make a copy of a BiMap because the
      // forward map is a view, don't make a copy of the non-view delegate map
      if (!bimap.isPartialView()) {
        return bimap;
      }
    }
    return copyOf(map.entrySet());
  }

  /**
   * Returns an immutable bimap containing the given entries.  The returned bimap iterates over
   * entries in the same order as the original iterable.
   *
   * @throws IllegalArgumentException if two keys have the same value or two
   *         values have the same key
   * @throws NullPointerException if any key, value, or entry is null
   * @since 19.0
   */
  @Beta
  public static <K, V> ImmutableBiMap<K, V> copyOf(
      Iterable<? extends Entry<? extends K, ? extends V>> entries) {
    int estimatedSize =
        (entries instanceof Collection)
            ? ((Collection<?>) entries).size()
            : ImmutableCollection.Builder.DEFAULT_INITIAL_CAPACITY;
    return new Builder<K, V>(estimatedSize).putAll(entries).build();
  }

  ImmutableBiMap() {}

  /**
   * {@inheritDoc}
   *
   * <p>The inverse of an {@code ImmutableBiMap} is another
   * {@code ImmutableBiMap}.
   */
  @Override
  public abstract ImmutableBiMap<V, K> inverse();

  /**
   * Returns an immutable set of the values in this map, in the same order they appear in {@link
   * #entrySet}.
   */
  @Override
  public ImmutableSet<V> values() {
    return inverse().keySet();
  }

  @Override
  final ImmutableSet<V> createValues() {
    throw new AssertionError("should never be called");
  }

  /**
   * Guaranteed to throw an exception and leave the bimap unmodified.
   *
   * @throws UnsupportedOperationException always
   * @deprecated Unsupported operation.
   */
  @CanIgnoreReturnValue
  @Deprecated
  @Override
  public V forcePut(K key, V value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Serialized type for all ImmutableBiMap instances. It captures the logical
   * contents and they are reconstructed using public factory methods. This
   * ensures that the implementation types remain as implementation details.
   *
   * Since the bimap is immutable, ImmutableBiMap doesn't require special logic
   * for keeping the bimap and its inverse in sync during serialization, the way
   * AbstractBiMap does.
   */
  private static class SerializedForm extends ImmutableMap.SerializedForm {
    SerializedForm(ImmutableBiMap<?, ?> bimap) {
      super(bimap);
    }

    @Override
    Object readResolve() {
      Builder<Object, Object> builder = new Builder<>();
      return createMap(builder);
    }

    private static final long serialVersionUID = 0;
  }

  @Override
  Object writeReplace() {
    return new SerializedForm(this);
  }
}
