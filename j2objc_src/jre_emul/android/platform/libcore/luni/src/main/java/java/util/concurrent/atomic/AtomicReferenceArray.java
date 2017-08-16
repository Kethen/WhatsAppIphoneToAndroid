/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/*-[
#include "java/lang/IndexOutOfBoundsException.h"
]-*/

/**
 * An array of object references in which elements may be updated
 * atomically.  See the {@link java.util.concurrent.atomic} package
 * specification for description of the properties of atomic
 * variables.
 * @since 1.5
 * @author Doug Lea
 * @param <E> The base class of elements held in this array
 */
public class AtomicReferenceArray<E> implements java.io.Serializable {
    private static final long serialVersionUID = -6209656149925076980L;

    private static final sun.misc.Unsafe U = sun.misc.Unsafe.getUnsafe();
    private static final long ARRAY;
    /* J2ObjC removed.
    private static final int ABASE;
    private static final int ASHIFT;
    */
    private final Object[] array; // must have exact type Object[]

    static {
        try {
            ARRAY = U.objectFieldOffset
                (AtomicReferenceArray.class.getDeclaredField("array"));
            /* J2ObjC unused.
            ABASE = U.arrayBaseOffset(Object[].class);
            int scale = U.arrayIndexScale(Object[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("array index scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
            */
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    /* J2ObjC unused.
    private long checkedByteOffset(int i) {
        if (i < 0 || i >= array.length)
            throw new IndexOutOfBoundsException("index " + i);

        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return ((long) i << ASHIFT) + ABASE;
    }
    */

    /**
     * Creates a new AtomicReferenceArray of the given length, with all
     * elements initially null.
     *
     * @param length the length of the array
     */
    public AtomicReferenceArray(int length) {
        array = new Object[length];
    }

    /**
     * Creates a new AtomicReferenceArray with the same length as, and
     * all elements copied from, the given array.
     *
     * @param array the array to copy elements from
     * @throws NullPointerException if array is null
     */
    public AtomicReferenceArray(E[] array) {
        // Visibility guaranteed by final field guarantees
        this.array = Arrays.copyOf(array, array.length, Object[].class);
    }

    /**
     * Returns the length of the array.
     *
     * @return the length of the array
     */
    public final int length() {
        return array.length;
    }

    /*-[
    static void CheckIdx(JavaUtilConcurrentAtomicAtomicReferenceArray *self, jint i) {
      if (i < 0 || i >= self->array_->size_) {
        @throw create_JavaLangIndexOutOfBoundsException_initWithNSString_(
            JreStrcat("$I", @"index ", i));
      }
    }

    static inline volatile_id *GetPtrUnchecked(
        JavaUtilConcurrentAtomicAtomicReferenceArray *self, jint i) {
      return (volatile_id *)&self->array_->buffer_[i];
    }

    static inline volatile_id *GetPtrChecked(
        JavaUtilConcurrentAtomicAtomicReferenceArray *self, jint i) {
      CheckIdx(self, i);
      return (volatile_id *)&self->array_->buffer_[i];
    }
    ]-*/

    /**
     * Gets the current value at position {@code i}.
     *
     * @param i the index
     * @return the current value
     */
    public final native E get(int i) /*-[
      return JreLoadVolatileId(GetPtrChecked(self, i));
    ]-*/;

    private final native E getUnchecked(int i) /*-[
      return JreLoadVolatileId(GetPtrUnchecked(self, i));
    ]-*/;

    /**
     * Sets the element at position {@code i} to the given value.
     *
     * @param i the index
     * @param newValue the new value
     */
    public final native void set(int i, E newValue) /*-[
      JreVolatileStrongAssign(GetPtrChecked(self, i), newValue);
    ]-*/;

    /**
     * Eventually sets the element at position {@code i} to the given value.
     *
     * @param i the index
     * @param newValue the new value
     * @since 1.6
     */
    public final native void lazySet(int i, E newValue) /*-[
      JreVolatileStrongAssign(GetPtrChecked(self, i), newValue);
    ]-*/;

    /**
     * Atomically sets the element at position {@code i} to the given
     * value and returns the old value.
     *
     * @param i the index
     * @param newValue the new value
     * @return the previous value
     */
    public final native E getAndSet(int i, E newValue) /*-[
      return JreExchangeVolatileStrongId(GetPtrChecked(self, i), newValue);
    ]-*/;

    /**
     * Atomically sets the element at position {@code i} to the given
     * updated value if the current value {@code ==} the expected value.
     *
     * @param i the index
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final native boolean compareAndSet(int i, E expect, E update) /*-[
      return JreCompareAndSwapVolatileStrongId(GetPtrChecked(self, i), expect, update);
    ]-*/;

    /**
     * Atomically sets the element at position {@code i} to the given
     * updated value if the current value {@code ==} the expected value.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param i the index
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     */
    public final native boolean weakCompareAndSet(int i, E expect, E update) /*-[
      return JreCompareAndSwapVolatileStrongId(GetPtrChecked(self, i), expect, update);
    ]-*/;

    /**
     * Atomically updates the element at index {@code i} with the results
     * of applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param i the index
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final E getAndUpdate(int i, UnaryOperator<E> updateFunction) {
        // long offset = checkedByteOffset(i);
        E prev, next;
        do {
            prev = get(i);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(i, prev, next));
        return prev;
    }

    /**
     * Atomically updates the element at index {@code i} with the results
     * of applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param i the index
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final E updateAndGet(int i, UnaryOperator<E> updateFunction) {
        // long offset = checkedByteOffset(i);
        E prev, next;
        do {
            prev = get(i);
            next = updateFunction.apply(prev);
        } while (!compareAndSet(i, prev, next));
        return next;
    }

    /**
     * Atomically updates the element at index {@code i} with the
     * results of applying the given function to the current and
     * given values, returning the previous value. The function should
     * be side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value at index {@code i} as its first
     * argument, and the given update as the second argument.
     *
     * @param i the index
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final E getAndAccumulate(int i, E x,
                                    BinaryOperator<E> accumulatorFunction) {
        // long offset = checkedByteOffset(i);
        E prev, next;
        do {
            prev = get(i);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(i, prev, next));
        return prev;
    }

    /**
     * Atomically updates the element at index {@code i} with the
     * results of applying the given function to the current and
     * given values, returning the updated value. The function should
     * be side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value at index {@code i} as its first
     * argument, and the given update as the second argument.
     *
     * @param i the index
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final E accumulateAndGet(int i, E x,
                                    BinaryOperator<E> accumulatorFunction) {
        // long offset = checkedByteOffset(i);
        E prev, next;
        do {
            prev = get(i);
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(i, prev, next));
        return next;
    }

    /**
     * Returns the String representation of the current values of array.
     * @return the String representation of the current values of array
     */
    public String toString() {
        int iMax = array.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(getUnchecked(i));
            if (i == iMax)
                return b.append(']').toString();
            b.append(',').append(' ');
        }
    }

    /**
     * Reconstitutes the instance from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Note: This must be changed if any additional fields are defined
        Object a = s.readFields().get("array", null);
        if (a == null || !a.getClass().isArray())
            throw new java.io.InvalidObjectException("Not array type");
        if (a.getClass() != Object[].class)
            a = Arrays.copyOf((Object[])a, Array.getLength(a), Object[].class);
        U.putObjectVolatile(this, ARRAY, a);
    }

}
