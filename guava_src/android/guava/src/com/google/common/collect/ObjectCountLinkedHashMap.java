/*
 * Copyright (C) 2017 The Guava Authors
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

import static com.google.common.collect.CollectPreconditions.checkRemove;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multiset.Entry;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * ObjectCountLinkedHashMap is an implementation of {@code AbstractObjectCountMap} with insertion
 * iteration order, and uses arrays to store key objects and count values. Comparing to using a
 * traditional {@code LinkedHashMap} implementation which stores keys and count values as map
 * entries, {@code ObjectCountLinkedHashMap} minimizes object allocation and reduces memory
 * footprint.
 */
@GwtCompatible(serializable = true, emulated = true)
class ObjectCountLinkedHashMap<K> extends ObjectCountHashMap<K> {
  /** Creates an empty {@code ObjectCountLinkedHashMap} instance. */
  public static <K> ObjectCountLinkedHashMap<K> create() {
    return new ObjectCountLinkedHashMap<K>();
  }

  /**
   * Creates a {@code ObjectCountLinkedHashMap} instance, with a high enough "initial capacity" that
   * it <i>should</i> hold {@code expectedSize} elements without growth.
   *
   * @param expectedSize the number of elements you expect to add to the returned set
   * @return a new, empty {@code ObjectCountLinkedHashMap} with enough capacity to hold {@code
   *     expectedSize} elements without resizing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <K> ObjectCountLinkedHashMap<K> createWithExpectedSize(int expectedSize) {
    return new ObjectCountLinkedHashMap<K>(expectedSize);
  }

  private static final int ENDPOINT = -2;

  /**
   * Contains the link pointers corresponding with the entries, in the range of [0, size()). The
   * high 32 bits of each long is the "prev" pointer, whereas the low 32 bits is the "succ" pointer
   * (pointing to the nextEntry entry in the linked list). The pointers in [size(), entries.length)
   * are all "null" (UNSET).
   *
   * <p>A node with "prev" pointer equal to {@code ENDPOINT} is the first node in the linked list,
   * and a node with "nextEntry" pointer equal to {@code ENDPOINT} is the last node.
   */
  @VisibleForTesting transient long[] links;

  /** Pointer to the first node in the linked list, or {@code ENDPOINT} if there are no entries. */
  private transient int firstEntry;

  /** Pointer to the last node in the linked list, or {@code ENDPOINT} if there are no entries. */
  private transient int lastEntry;

  ObjectCountLinkedHashMap() {
    this(DEFAULT_SIZE);
  }

  ObjectCountLinkedHashMap(int expectedSize) {
    this(expectedSize, DEFAULT_LOAD_FACTOR);
  }

  ObjectCountLinkedHashMap(int expectedSize, float loadFactor) {
    super(expectedSize, loadFactor);
  }

  ObjectCountLinkedHashMap(AbstractObjectCountMap<K> map) {
    init(map.size(), DEFAULT_LOAD_FACTOR);
    for (int i = map.firstIndex(); i != -1; i = map.nextIndex(i)) {
      put(map.getKey(i), map.getValue(i));
    }
  }

  @Override
  void init(int expectedSize, float loadFactor) {
    super.init(expectedSize, loadFactor);
    firstEntry = ENDPOINT;
    lastEntry = ENDPOINT;
    links = new long[expectedSize];
    Arrays.fill(links, UNSET);
  }

  @Override
  int firstIndex() {
    return (firstEntry == ENDPOINT) ? -1 : firstEntry;
  }

  @Override
  int nextIndex(int index) {
    int result = getSuccessor(index);
    return (result == ENDPOINT) ? -1 : result;
  }

  private int getPredecessor(int entry) {
    return (int) (links[entry] >>> 32);
  }

  private int getSuccessor(int entry) {
    return (int) links[entry];
  }

  private void setSuccessor(int entry, int succ) {
    long succMask = (~0L) >>> 32;
    links[entry] = (links[entry] & ~succMask) | (succ & succMask);
  }

  private void setPredecessor(int entry, int pred) {
    long predMask = (~0L) << 32;
    links[entry] = (links[entry] & ~predMask) | ((long) pred << 32);
  }

  private void setSucceeds(int pred, int succ) {
    if (pred == ENDPOINT) {
      firstEntry = succ;
    } else {
      setSuccessor(pred, succ);
    }
    if (succ == ENDPOINT) {
      lastEntry = pred;
    } else {
      setPredecessor(succ, pred);
    }
  }

  @Override
  void insertEntry(int entryIndex, K key, int value, int hash) {
    super.insertEntry(entryIndex, key, value, hash);
    setSucceeds(lastEntry, entryIndex);
    setSucceeds(entryIndex, ENDPOINT);
  }

  @Override
  void moveLastEntry(int dstIndex) {
    int srcIndex = size() - 1;
    setSucceeds(getPredecessor(dstIndex), getSuccessor(dstIndex));
    if (dstIndex < srcIndex) {
      setSucceeds(getPredecessor(srcIndex), dstIndex);
      setSucceeds(dstIndex, getSuccessor(srcIndex));
    }
    super.moveLastEntry(dstIndex);
  }

  @Override
  void resizeEntries(int newCapacity) {
    super.resizeEntries(newCapacity);
    links = Arrays.copyOf(links, newCapacity);
  }

  private abstract class LinkedItr<T> implements Iterator<T> {
    private int nextEntry = firstEntry;
    private int toRemove = UNSET;
    private int expectedModCount = modCount;

    private void checkForConcurrentModification() {
      if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
      }
    }

    @Override
    public boolean hasNext() {
      return nextEntry != ENDPOINT;
    }

    abstract T getOutput(int entry);

    @Override
    public T next() {
      checkForConcurrentModification();
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T result = getOutput(nextEntry);
      toRemove = nextEntry;
      nextEntry = getSuccessor(nextEntry);
      return result;
    }

    @Override
    public void remove() {
      checkForConcurrentModification();
      checkRemove(toRemove != UNSET);
      ObjectCountLinkedHashMap.this.remove(keys[toRemove]);
      if (nextEntry >= size()) {
        nextEntry = toRemove;
      }
      expectedModCount = modCount;
      toRemove = UNSET;
    }
  }

  @Override
  Set<K> createKeySet() {
    return new KeySetView() {
      @Override
      public Object[] toArray() {
        return ObjectArrays.toArrayImpl(this);
      }

      @Override
      public <T> T[] toArray(T[] a) {
        return ObjectArrays.toArrayImpl(this, a);
      }

      @Override
      public Iterator<K> iterator() {
        return new LinkedItr<K>() {
          @SuppressWarnings("unchecked") // keys only contains Ks
          @Override
          K getOutput(int entry) {
            return (K) keys[entry];
          }
        };
      }
    };
  }

  @Override
  Set<Entry<K>> createEntrySet() {
    return new EntrySetView() {
      @Override
      public Iterator<Entry<K>> iterator() {
        return new LinkedItr<Entry<K>>() {
          @Override
          Entry<K> getOutput(int entry) {
            return new MapEntry(entry);
          }
        };
      }
    };
  }

  @Override
  public void clear() {
    super.clear();
    this.firstEntry = ENDPOINT;
    this.lastEntry = ENDPOINT;
  }
}
