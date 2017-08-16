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

package com.google.common.util.concurrent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase;

/**
 * Tests {@link SerializingExecutor}.
 *
 * @author JJ Furman
 */
public class SerializingExecutorTest extends TestCase {
  private static class FakeExecutor implements Executor {
    Queue<Runnable> tasks = Queues.newArrayDeque();
    @Override public void execute(Runnable command) {
      tasks.add(command);
    }

    boolean hasNext() {
      return !tasks.isEmpty();
    }

    void runNext() {
      assertTrue("expected at least one task to run", hasNext());
      tasks.remove().run();
    }

    void runAll() {
      while (hasNext()) {
        runNext();
      }
    }
  }
  private FakeExecutor fakePool;
  private SerializingExecutor e;

  @Override
  public void setUp() {
    fakePool = new FakeExecutor();
    e = new SerializingExecutor(fakePool);
  }

  public void testSerializingNullExecutor_fails() {
    try {
      new SerializingExecutor(null);
      fail("Should have failed with NullPointerException.");
    } catch (NullPointerException expected) {
    }
  }

  public void testBasics() {
    final AtomicInteger totalCalls = new AtomicInteger();
    Runnable intCounter = new Runnable() {
      @Override
      public void run() {
        totalCalls.incrementAndGet();
        // Make sure that no other tasks are scheduled to run while this is running.
        assertFalse(fakePool.hasNext());
      }
    };

    assertFalse(fakePool.hasNext());
    e.execute(intCounter);
    // A task should have been scheduled
    assertTrue(fakePool.hasNext());
    e.execute(intCounter);
    // Our executor hasn't run any tasks yet.
    assertEquals(0, totalCalls.get());
    fakePool.runAll();
    assertEquals(2, totalCalls.get());
    // Queue is empty so no runner should be scheduled.
    assertFalse(fakePool.hasNext());

    // Check that execute can be safely repeated
    e.execute(intCounter);
    e.execute(intCounter);
    e.execute(intCounter);
    // No change yet.
    assertEquals(2, totalCalls.get());
    fakePool.runAll();
    assertEquals(5, totalCalls.get());
    assertFalse(fakePool.hasNext());
  }

  public void testSuspend() {
    final AtomicInteger totalCalls = new AtomicInteger();
    Runnable suspender = new Runnable() {
      @Override
      public void run() {
        totalCalls.incrementAndGet();
        // Suspend the queue so no other calls run.
        e.suspend();
      }
    };

    assertFalse(fakePool.hasNext());
    e.execute(suspender);
    // A task should have been scheduled
    assertTrue(fakePool.hasNext());
    e.execute(suspender);
    fakePool.runAll();
    // Only the first call that was already scheduled before suspension should have run.
    assertEquals(1, totalCalls.get());
    // Queue is suspended so no runner should be scheduled.
    assertFalse(fakePool.hasNext());

    e.execute(suspender);
    // Queue is suspended so no runner should be scheduled.
    assertFalse(fakePool.hasNext());
    e.resume();
    // A task should have been scheduled
    assertTrue(fakePool.hasNext());
    fakePool.runAll();
    // Another call should have been run, but that suspended the queue again.
    assertEquals(2, totalCalls.get());
    assertFalse(fakePool.hasNext());

    // Suspend the queue from here as well, making the count two.
    e.suspend();
    e.resume();
    // Queue is still suspended, with count one, so no runner should be scheduled.
    assertFalse(fakePool.hasNext());
    e.resume();
    // A task should have been scheduled
    assertTrue(fakePool.hasNext());
    fakePool.runAll();
    // Another call should have been run, but that suspended the queue again.
    assertEquals(3, totalCalls.get());
  }

  public void testOrdering() {
    final List<Integer> callOrder = Lists.newArrayList();

    class FakeOp implements Runnable {
      final int op;

      FakeOp(int op) {
        this.op = op;
      }

      @Override
      public void run() {
        callOrder.add(op);
      }
    }

    e.execute(new FakeOp(0));
    e.execute(new FakeOp(1));
    e.execute(new FakeOp(2));
    fakePool.runAll();

    assertEquals(ImmutableList.of(0, 1, 2), callOrder);
  }

  public void testPrependContinuation() {
    final List<Integer> callOrder = Lists.newArrayList();

    class FakeOp implements Runnable {
      final int op;

      FakeOp(int op) {
        this.op = op;
      }

      @Override
      public void run() {
        callOrder.add(op);
      }
    }

    e.execute(new FakeOp(1));
    e.execute(new FakeOp(2));
    e.executeFirst(new FakeOp(0));
    fakePool.runAll();

    assertEquals(ImmutableList.of(0, 1, 2), callOrder);
  }

  public void testRuntimeException_doesNotStopExecution() {

    final AtomicInteger numCalls = new AtomicInteger();

    Runnable runMe = new Runnable() {
      @Override
      public void run() {
        numCalls.incrementAndGet();
        throw new RuntimeException("FAKE EXCEPTION!");
      }
    };

    e.execute(runMe);
    e.execute(runMe);
    fakePool.runAll();

    assertEquals(2, numCalls.get());
  }

  public void testInterrupt_doesNotStopExecution() {

    final AtomicInteger numCalls = new AtomicInteger();

    Runnable runMe = new Runnable() {
      @Override
      public void run() {
        numCalls.incrementAndGet();
      }
    };

    Thread.currentThread().interrupt();

    e.execute(runMe);
    e.execute(runMe);
    fakePool.runAll();

    assertEquals(2, numCalls.get());
    assertTrue(Thread.interrupted());
  }

  public void testDelegateRejection() {
    final AtomicInteger numCalls = new AtomicInteger();
    final AtomicBoolean reject = new AtomicBoolean(true);
    final SerializingExecutor executor = new SerializingExecutor(
        new Executor() {
          @Override public void execute(Runnable r) {
            if (reject.get()) {
              throw new RejectedExecutionException();
            }
            r.run();
          }
        });
    Runnable task = new Runnable() {
      @Override
      public void run() {
        numCalls.incrementAndGet();
      }
    };
    try {
      executor.execute(task);
      fail();
    } catch (RejectedExecutionException expected) {}
    assertEquals(0, numCalls.get());
    reject.set(false);
    executor.execute(task);
    assertEquals(2, numCalls.get());
  }

  public void testTaskThrowsError() throws Exception {
    class MyError extends Error {}
    final CyclicBarrier barrier = new CyclicBarrier(2);
    // we need to make sure the error gets thrown on a different thread.
    ExecutorService service = Executors.newSingleThreadExecutor();
    try {
      final SerializingExecutor executor = new SerializingExecutor(service);
      Runnable errorTask = new Runnable() {
        @Override
        public void run() {
          throw new MyError();
        }
      };
      Runnable barrierTask = new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      };
      executor.execute(errorTask);
      service.execute(barrierTask);  // submit directly to the service
      // the barrier task runs after the error task so we know that the error has been observed by
      // SerializingExecutor by the time the barrier is satified
      barrier.await(10, TimeUnit.SECONDS);
      executor.execute(barrierTask);
      // timeout means the second task wasn't even tried
      barrier.await(10, TimeUnit.SECONDS);
    } finally {
      service.shutdown();
    }
  }
}
