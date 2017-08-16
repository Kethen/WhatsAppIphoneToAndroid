/*
 * Copyright (C) 2012 The Guava Authors
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

package com.google.common.math;

import static com.google.common.math.StatsTesting.ALLOWED_ERROR;
import static com.google.common.math.StatsTesting.ALL_MANY_VALUES;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_MAX;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_MEAN;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_MIN;
import static com.google.common.math.StatsTesting.INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_MAX;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_MEAN;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_MIN;
import static com.google.common.math.StatsTesting.LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.math.StatsTesting.MANY_VALUES;
import static com.google.common.math.StatsTesting.MANY_VALUES_COUNT;
import static com.google.common.math.StatsTesting.MANY_VALUES_MAX;
import static com.google.common.math.StatsTesting.MANY_VALUES_MEAN;
import static com.google.common.math.StatsTesting.MANY_VALUES_MIN;
import static com.google.common.math.StatsTesting.MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.math.StatsTesting.ONE_VALUE;
import static com.google.common.math.StatsTesting.OTHER_ONE_VALUE;
import static com.google.common.math.StatsTesting.TWO_VALUES;
import static com.google.common.math.StatsTesting.TWO_VALUES_MAX;
import static com.google.common.math.StatsTesting.TWO_VALUES_MEAN;
import static com.google.common.math.StatsTesting.TWO_VALUES_MIN;
import static com.google.common.math.StatsTesting.TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS;
import static com.google.common.truth.Truth.assertThat;
import static java.lang.Math.sqrt;

import com.google.common.collect.ImmutableList;
import com.google.common.math.StatsTesting.ManyValues;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import junit.framework.TestCase;

/**
 * Tests for {@link StatsAccumulator}. This tests the stats methods for instances built with
 * {@link StatsAccumulator#add} and {@link StatsAccumulator#addAll}, and various error cases of the
 * {@link StatsAccumulator#add} and {@link StatsAccumulator#addAll} methods. For tests of the
 * {@link StatsAccumulator#snapshot} method which returns {@link Stats} instances, see
 * {@link StatsTest}.
 *
 * @author Pete Gillin
 */
public class StatsAccumulatorTest extends TestCase {

  private StatsAccumulator emptyAccumulator;
  private StatsAccumulator emptyAccumulatorByAddAllEmptyIterable;
  private StatsAccumulator emptyAccumulatorByAddAllEmptyStats;
  private StatsAccumulator oneValueAccumulator;
  private StatsAccumulator oneValueAccumulatorByAddAllEmptyStats;
  private StatsAccumulator twoValuesAccumulator;
  private StatsAccumulator twoValuesAccumulatorByAddAllStats;
  private StatsAccumulator manyValuesAccumulatorByAddAllIterable;
  private StatsAccumulator manyValuesAccumulatorByAddAllIterator;
  private StatsAccumulator manyValuesAccumulatorByAddAllVarargs;
  private StatsAccumulator manyValuesAccumulatorByRepeatedAdd;
  private StatsAccumulator manyValuesAccumulatorByAddAndAddAll;
  private StatsAccumulator manyValuesAccumulatorByAddAllStats;
  private StatsAccumulator integerManyValuesAccumulatorByAddAllIterable;
  private StatsAccumulator longManyValuesAccumulatorByAddAllIterator;
  private StatsAccumulator longManyValuesAccumulatorByAddAllVarargs;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    emptyAccumulator = new StatsAccumulator();

    emptyAccumulatorByAddAllEmptyIterable = new StatsAccumulator();
    emptyAccumulatorByAddAllEmptyIterable.addAll(ImmutableList.<Double>of());

    emptyAccumulatorByAddAllEmptyStats = new StatsAccumulator();
    emptyAccumulatorByAddAllEmptyStats.addAll(Stats.of());

    oneValueAccumulator = new StatsAccumulator();
    oneValueAccumulator.add(ONE_VALUE);

    oneValueAccumulatorByAddAllEmptyStats = new StatsAccumulator();
    oneValueAccumulatorByAddAllEmptyStats.add(ONE_VALUE);
    oneValueAccumulatorByAddAllEmptyStats.addAll(Stats.of());

    twoValuesAccumulator = new StatsAccumulator();
    twoValuesAccumulator.addAll(TWO_VALUES);

    twoValuesAccumulatorByAddAllStats = new StatsAccumulator();
    twoValuesAccumulatorByAddAllStats.addAll(Stats.of(ONE_VALUE));
    twoValuesAccumulatorByAddAllStats.addAll(Stats.of(OTHER_ONE_VALUE));

    manyValuesAccumulatorByAddAllIterable = new StatsAccumulator();
    manyValuesAccumulatorByAddAllIterable.addAll(MANY_VALUES);

    manyValuesAccumulatorByAddAllIterator = new StatsAccumulator();
    manyValuesAccumulatorByAddAllIterator.addAll(MANY_VALUES.iterator());

    manyValuesAccumulatorByAddAllVarargs = new StatsAccumulator();
    manyValuesAccumulatorByAddAllVarargs.addAll(Doubles.toArray(MANY_VALUES));

    manyValuesAccumulatorByRepeatedAdd = new StatsAccumulator();
    for (double value : MANY_VALUES) {
      manyValuesAccumulatorByRepeatedAdd.add(value);
    }

    manyValuesAccumulatorByAddAndAddAll = new StatsAccumulator();
    manyValuesAccumulatorByAddAndAddAll.add(MANY_VALUES.get(0));
    manyValuesAccumulatorByAddAndAddAll.addAll(MANY_VALUES.subList(1, MANY_VALUES.size()));

    manyValuesAccumulatorByAddAllStats = new StatsAccumulator();
    manyValuesAccumulatorByAddAllStats.addAll(
        Stats.of(MANY_VALUES.subList(0, MANY_VALUES.size() / 2)));
    manyValuesAccumulatorByAddAllStats.addAll(
        Stats.of(MANY_VALUES.subList(MANY_VALUES.size() / 2, MANY_VALUES.size())));

    integerManyValuesAccumulatorByAddAllIterable = new StatsAccumulator();
    integerManyValuesAccumulatorByAddAllIterable.addAll(INTEGER_MANY_VALUES);

    longManyValuesAccumulatorByAddAllIterator = new StatsAccumulator();
    longManyValuesAccumulatorByAddAllIterator.addAll(LONG_MANY_VALUES.iterator());

    longManyValuesAccumulatorByAddAllVarargs = new StatsAccumulator();
    longManyValuesAccumulatorByAddAllVarargs.addAll(Longs.toArray(LONG_MANY_VALUES));
  }

  public void testCount() {
    assertThat(emptyAccumulator.count()).isEqualTo(0);
    assertThat(emptyAccumulatorByAddAllEmptyIterable.count()).isEqualTo(0);
    assertThat(emptyAccumulatorByAddAllEmptyStats.count()).isEqualTo(0);
    assertThat(oneValueAccumulator.count()).isEqualTo(1);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.count()).isEqualTo(1);
    assertThat(twoValuesAccumulator.count()).isEqualTo(2);
    assertThat(twoValuesAccumulatorByAddAllStats.count()).isEqualTo(2);
    assertThat(manyValuesAccumulatorByAddAllIterable.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllIterator.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllVarargs.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByRepeatedAdd.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAndAddAll.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllStats.count()).isEqualTo(MANY_VALUES_COUNT);
    assertThat(integerManyValuesAccumulatorByAddAllIterable.count())
        .isEqualTo(StatsTesting.INTEGER_MANY_VALUES_COUNT);
    assertThat(longManyValuesAccumulatorByAddAllIterator.count())
        .isEqualTo(StatsTesting.LONG_MANY_VALUES_COUNT);
    assertThat(longManyValuesAccumulatorByAddAllVarargs.count())
        .isEqualTo(StatsTesting.LONG_MANY_VALUES_COUNT);
  }

  public void testCountOverflow_doesNotThrow() {
    StatsAccumulator accumulator = new StatsAccumulator();
    accumulator.add(ONE_VALUE);
    for (int power = 1; power < Long.SIZE - 1; power++) {
      accumulator.addAll(accumulator.snapshot());
    }
    // Should overflow without throwing.
    accumulator.addAll(accumulator.snapshot());
    assertThat(accumulator.count()).isLessThan(0L);
  }

  public void testMean() {
    try {
      emptyAccumulator.mean();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.mean();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.mean();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(oneValueAccumulator.mean()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.mean()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(twoValuesAccumulator.mean()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MEAN);
    assertThat(twoValuesAccumulatorByAddAllStats.mean())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_MEAN);
    assertThat(manyValuesAccumulatorByAddAllIterable.mean())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN);
    assertThat(manyValuesAccumulatorByAddAllIterator.mean())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN);
    assertThat(manyValuesAccumulatorByAddAllVarargs.mean())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN);
    assertThat(manyValuesAccumulatorByRepeatedAdd.mean())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN);
    assertThat(manyValuesAccumulatorByAddAndAddAll.mean())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN);
    assertThat(manyValuesAccumulatorByAddAllStats.mean())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN);
    // For datasets of many double values created from an iterable, we test many combinations of
    // finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      StatsAccumulator accumulator = new StatsAccumulator();
      StatsAccumulator accumulatorByAddAllStats = new StatsAccumulator();
      accumulator.addAll(values.asIterable());
      for (double value : values.asIterable()) {
        accumulatorByAddAllStats.addAll(Stats.of(value));
      }
      double mean = accumulator.mean();
      double meanByAddAllStats = accumulatorByAddAllStats.mean();
      if (values.hasAnyNaN()) {
        assertThat(mean).named("mean of " + values).isNaN();
        assertThat(meanByAddAllStats).named("mean by addAll(Stats) of " + values).isNaN();
      } else if (values.hasAnyPositiveInfinity() && values.hasAnyNegativeInfinity()) {
        assertThat(mean).named("mean of " + values).isNaN();
        assertThat(meanByAddAllStats).named("mean by addAll(Stats) of " + values).isNaN();
      } else if (values.hasAnyPositiveInfinity()) {
        assertThat(mean).named("mean of " + values).isPositiveInfinity();
        assertThat(meanByAddAllStats)
            .named("mean by addAll(Stats) of " + values)
            .isPositiveInfinity();
      } else if (values.hasAnyNegativeInfinity()) {
        assertThat(mean).named("mean of " + values).isNegativeInfinity();
        assertThat(meanByAddAllStats)
            .named("mean by addAll(Stats) of " + values)
            .isNegativeInfinity();
      } else {
        assertThat(mean).named("mean of " + values).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MEAN);
        assertThat(meanByAddAllStats)
            .named("mean by addAll(Stats) of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_MEAN);
      }
    }
    assertThat(integerManyValuesAccumulatorByAddAllIterable.mean())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN);
    assertThat(longManyValuesAccumulatorByAddAllIterator.mean())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN);
    assertThat(longManyValuesAccumulatorByAddAllVarargs.mean())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN);
  }

  public void testSum() {
    assertThat(emptyAccumulator.sum()).isWithin(0.0).of(0.0);
    assertThat(emptyAccumulatorByAddAllEmptyIterable.sum()).isWithin(0.0).of(0.0);
    assertThat(emptyAccumulatorByAddAllEmptyStats.sum()).isWithin(0.0).of(0.0);
    assertThat(oneValueAccumulator.sum()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.sum()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(twoValuesAccumulator.sum()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MEAN * 2);
    assertThat(twoValuesAccumulatorByAddAllStats.sum())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_MEAN * 2);
    assertThat(manyValuesAccumulatorByAddAllIterable.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllIterator.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllVarargs.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByRepeatedAdd.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAndAddAll.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllStats.sum())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MEAN * MANY_VALUES_COUNT);
    assertThat(integerManyValuesAccumulatorByAddAllIterable.sum())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MEAN * INTEGER_MANY_VALUES_COUNT);
    assertThat(longManyValuesAccumulatorByAddAllIterator.sum())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN * LONG_MANY_VALUES_COUNT);
    assertThat(longManyValuesAccumulatorByAddAllVarargs.sum())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MEAN * LONG_MANY_VALUES_COUNT);
  }

  public void testPopulationVariance() {
    try {
      emptyAccumulator.populationVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.populationVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.populationVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(oneValueAccumulator.populationVariance()).isWithin(0.0).of(0.0);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.populationVariance()).isWithin(0.0).of(0.0);
    assertThat(twoValuesAccumulator.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS / 2);
    assertThat(twoValuesAccumulatorByAddAllStats.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS / 2);
    assertThat(manyValuesAccumulatorByAddAllIterable.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllIterator.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllVarargs.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByRepeatedAdd.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAndAddAll.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    assertThat(manyValuesAccumulatorByAddAllStats.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
    // For datasets of many double values created from an iterator, we test many combinations of
    // finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      StatsAccumulator accumulator = new StatsAccumulator();
      StatsAccumulator accumulatorByAddAllStats = new StatsAccumulator();
      accumulator.addAll(values.asIterable().iterator());
      for (double value : values.asIterable()) {
        accumulatorByAddAllStats.addAll(Stats.of(value));
      }
      double populationVariance = accumulator.populationVariance();
      double populationVarianceByAddAllStats = accumulatorByAddAllStats.populationVariance();
      if (values.hasAnyNonFinite()) {
        assertThat(populationVariance).named("population variance of " + values).isNaN();
        assertThat(populationVarianceByAddAllStats)
            .named("population variance by addAll(Stats) of " + values)
            .isNaN();
      } else {
        assertThat(populationVariance)
            .named("population variance of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
        assertThat(populationVarianceByAddAllStats)
            .named("population variance by addAll(Stats) of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT);
      }
    }
    assertThat(integerManyValuesAccumulatorByAddAllIterable.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / INTEGER_MANY_VALUES_COUNT);
    assertThat(longManyValuesAccumulatorByAddAllIterator.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT);
    assertThat(longManyValuesAccumulatorByAddAllVarargs.populationVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT);
  }

  public void testPopulationStandardDeviation() {
    try {
      emptyAccumulator.populationStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.populationStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.populationStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(oneValueAccumulator.populationStandardDeviation()).isWithin(0.0).of(0.0);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.populationStandardDeviation())
        .isWithin(0.0)
        .of(0.0);
    assertThat(twoValuesAccumulator.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS / 2));
    assertThat(twoValuesAccumulatorByAddAllStats.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS / 2));
    assertThat(manyValuesAccumulatorByAddAllIterable.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(manyValuesAccumulatorByAddAllIterator.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(manyValuesAccumulatorByAddAllVarargs.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(manyValuesAccumulatorByRepeatedAdd.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(manyValuesAccumulatorByAddAndAddAll.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(manyValuesAccumulatorByAddAllStats.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / MANY_VALUES_COUNT));
    assertThat(integerManyValuesAccumulatorByAddAllIterable.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / INTEGER_MANY_VALUES_COUNT));
    assertThat(longManyValuesAccumulatorByAddAllIterator.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT));
    assertThat(longManyValuesAccumulatorByAddAllVarargs.populationStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / LONG_MANY_VALUES_COUNT));
  }

  public void testSampleVariance() {
    try {
      emptyAccumulator.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulator.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulatorByAddAllEmptyStats.sampleVariance();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(twoValuesAccumulator.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS);
    assertThat(twoValuesAccumulatorByAddAllStats.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS);
    assertThat(manyValuesAccumulatorByAddAllIterable.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(manyValuesAccumulatorByAddAllIterator.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(manyValuesAccumulatorByAddAllVarargs.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(manyValuesAccumulatorByRepeatedAdd.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(manyValuesAccumulatorByAddAndAddAll.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(manyValuesAccumulatorByAddAllStats.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1));
    assertThat(integerManyValuesAccumulatorByAddAllIterable.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (INTEGER_MANY_VALUES_COUNT - 1));
    assertThat(longManyValuesAccumulatorByAddAllIterator.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1));
    assertThat(longManyValuesAccumulatorByAddAllVarargs.sampleVariance())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1));
  }

  public void testSampleStandardDeviation() {
    try {
      emptyAccumulator.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulator.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      oneValueAccumulatorByAddAllEmptyStats.sampleStandardDeviation();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(twoValuesAccumulator.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS));
    assertThat(twoValuesAccumulatorByAddAllStats.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(TWO_VALUES_SUM_OF_SQUARES_OF_DELTAS));
    assertThat(manyValuesAccumulatorByAddAllIterable.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(manyValuesAccumulatorByAddAllIterator.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(manyValuesAccumulatorByAddAllVarargs.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(manyValuesAccumulatorByRepeatedAdd.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(manyValuesAccumulatorByAddAndAddAll.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(manyValuesAccumulatorByAddAllStats.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (MANY_VALUES_COUNT - 1)));
    assertThat(integerManyValuesAccumulatorByAddAllIterable.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(INTEGER_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (INTEGER_MANY_VALUES_COUNT - 1)));
    assertThat(longManyValuesAccumulatorByAddAllIterator.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1)));
    assertThat(longManyValuesAccumulatorByAddAllVarargs.sampleStandardDeviation())
        .isWithin(ALLOWED_ERROR)
        .of(sqrt(LONG_MANY_VALUES_SUM_OF_SQUARES_OF_DELTAS / (LONG_MANY_VALUES_COUNT - 1)));
  }

  public void testMax() {
    try {
      emptyAccumulator.max();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.max();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.max();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(oneValueAccumulator.max()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.max()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(twoValuesAccumulator.max()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MAX);
    assertThat(twoValuesAccumulatorByAddAllStats.max()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MAX);
    assertThat(manyValuesAccumulatorByAddAllIterable.max())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MAX);
    assertThat(manyValuesAccumulatorByAddAllIterator.max())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MAX);
    assertThat(manyValuesAccumulatorByAddAllVarargs.max())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MAX);
    assertThat(manyValuesAccumulatorByRepeatedAdd.max())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MAX);
    assertThat(manyValuesAccumulatorByAddAndAddAll.max())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MAX);
    assertThat(manyValuesAccumulatorByAddAllStats.max())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MAX);
    // For datasets of many double values created from an array, we test many combinations of
    // finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      StatsAccumulator accumulator = new StatsAccumulator();
      StatsAccumulator accumulatorByAddAllStats = new StatsAccumulator();
      accumulator.addAll(values.asArray());
      for (double value : values.asIterable()) {
        accumulatorByAddAllStats.addAll(Stats.of(value));
      }
      double max = accumulator.max();
      double maxByAddAllStats = accumulatorByAddAllStats.max();
      if (values.hasAnyNaN()) {
        assertThat(max).named("max of " + values).isNaN();
        assertThat(maxByAddAllStats).named("max by addAll(Stats) of " + values).isNaN();
      } else if (values.hasAnyPositiveInfinity()) {
        assertThat(max).named("max of " + values).isPositiveInfinity();
        assertThat(maxByAddAllStats)
            .named("max by addAll(Stats) of " + values)
            .isPositiveInfinity();
      } else {
        assertThat(max).named("max of " + values).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MAX);
        assertThat(maxByAddAllStats)
            .named("max by addAll(Stats) of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_MAX);
      }
    }
    assertThat(integerManyValuesAccumulatorByAddAllIterable.max())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MAX);
    assertThat(longManyValuesAccumulatorByAddAllIterator.max())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MAX);
    assertThat(longManyValuesAccumulatorByAddAllVarargs.max())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MAX);
  }

  public void testMin() {
    try {
      emptyAccumulator.min();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyIterable.min();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    try {
      emptyAccumulatorByAddAllEmptyStats.min();
      fail("Expected IllegalStateException");
    } catch (IllegalStateException expected) {
    }
    assertThat(oneValueAccumulator.min()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(oneValueAccumulatorByAddAllEmptyStats.min()).isWithin(ALLOWED_ERROR).of(ONE_VALUE);
    assertThat(twoValuesAccumulator.min()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MIN);
    assertThat(twoValuesAccumulatorByAddAllStats.min()).isWithin(ALLOWED_ERROR).of(TWO_VALUES_MIN);
    assertThat(manyValuesAccumulatorByAddAllIterable.min())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MIN);
    assertThat(manyValuesAccumulatorByAddAllIterator.min())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MIN);
    assertThat(manyValuesAccumulatorByAddAllVarargs.min())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MIN);
    assertThat(manyValuesAccumulatorByRepeatedAdd.min())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MIN);
    assertThat(manyValuesAccumulatorByAddAndAddAll.min())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MIN);
    assertThat(manyValuesAccumulatorByAddAllStats.min())
        .isWithin(ALLOWED_ERROR)
        .of(MANY_VALUES_MIN);
    // For datasets of many double values created by adding elements individually, we test many
    // combinations of finite and non-finite values:
    for (ManyValues values : ALL_MANY_VALUES) {
      StatsAccumulator accumulator = new StatsAccumulator();
      StatsAccumulator accumulatorByAddAllStats = new StatsAccumulator();
      for (double value : values.asIterable()) {
        accumulator.add(value);
        accumulatorByAddAllStats.addAll(Stats.of(value));
      }
      double min = accumulator.min();
      double minByAddAllStats = accumulatorByAddAllStats.min();
      if (values.hasAnyNaN()) {
        assertThat(min).named("min of " + values).isNaN();
        assertThat(minByAddAllStats).named("min by addAll(Stats) of " + values).isNaN();
      } else if (values.hasAnyNegativeInfinity()) {
        assertThat(min).named("min of " + values).isNegativeInfinity();
        assertThat(minByAddAllStats)
            .named("min by addAll(Stats) of " + values)
            .isNegativeInfinity();
      } else {
        assertThat(min).named("min of " + values).isWithin(ALLOWED_ERROR).of(MANY_VALUES_MIN);
        assertThat(minByAddAllStats)
            .named("min by addAll(Stats) of " + values)
            .isWithin(ALLOWED_ERROR)
            .of(MANY_VALUES_MIN);
      }
    }
    assertThat(integerManyValuesAccumulatorByAddAllIterable.min())
        .isWithin(ALLOWED_ERROR)
        .of(INTEGER_MANY_VALUES_MIN);
    assertThat(longManyValuesAccumulatorByAddAllIterator.min())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MIN);
    assertThat(longManyValuesAccumulatorByAddAllVarargs.min())
        .isWithin(ALLOWED_ERROR)
        .of(LONG_MANY_VALUES_MIN);
  }
}
