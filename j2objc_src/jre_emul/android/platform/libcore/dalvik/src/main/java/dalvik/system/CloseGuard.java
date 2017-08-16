/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dalvik.system;

/**
 * CloseGuard is a mechanism for flagging implicit finalizer cleanup of
 * resources that should have been cleaned up by explicit close
 * methods (aka "explicit termination methods" in Effective Java).
 * <p>
 * A simple example: <pre>   {@code
 *   class Foo {
 *
 *       private final CloseGuard guard = CloseGuard.get();
 *
 *       ...
 *
 *       public Foo() {
 *           ...;
 *           guard.open("cleanup");
 *       }
 *
 *       public void cleanup() {
 *          guard.close();
 *          ...;
 *       }
 *
 *       protected void finalize() throws Throwable {
 *           try {
 *               // Note that guard could be null if the constructor threw.
 *               if (guard != null) {
 *                   guard.warnIfOpen();
 *               }
 *               cleanup();
 *           } finally {
 *               super.finalize();
 *           }
 *       }
 *   }
 * }</pre>
 *
 * In usage where the resource to be explicitly cleaned up are
 * allocated after object construction, CloseGuard protection can
 * be deferred. For example: <pre>   {@code
 *   class Bar {
 *
 *       private final CloseGuard guard = CloseGuard.get();
 *
 *       ...
 *
 *       public Bar() {
 *           ...;
 *       }
 *
 *       public void connect() {
 *          ...;
 *          guard.open("cleanup");
 *       }
 *
 *       public void cleanup() {
 *          guard.close();
 *          ...;
 *       }
 *
 *       protected void finalize() throws Throwable {
 *           try {
 *               // Note that guard could be null if the constructor threw.
 *               if (guard != null) {
 *                   guard.warnIfOpen();
 *               }
 *               cleanup();
 *           } finally {
 *               super.finalize();
 *           }
 *       }
 *   }
 * }</pre>
 *
 * When used in a constructor calls to {@code open} should occur at
 * the end of the constructor since an exception that would cause
 * abrupt termination of the constructor will mean that the user will
 * not have a reference to the object to cleanup explicitly. When used
 * in a method, the call to {@code open} should occur just after
 * resource acquisition.
 *
 * @hide
 */
public final class CloseGuard {

    /**
     * Instance used when CloseGuard is disabled to avoid allocation.
     */
    private static final CloseGuard NOOP = new CloseGuard();

    /**
     * Enabled by default so we can catch issues early in VM startup.
     * Note, however, that Android disables this early in its startup,
     * but enables it with DropBoxing for system apps on debug builds.
     */
    // Disabled by default on iOS, since there's no VM startup to turn it off.
    private static volatile boolean ENABLED = false;

    /**
     * Hook for customizing how CloseGuard issues are reported.
     */
    private static volatile Reporter REPORTER = new DefaultReporter();

    /**
     * The default {@link Tracker}.
     */
    private static final DefaultTracker DEFAULT_TRACKER = new DefaultTracker();

    /**
     * Hook for customizing how CloseGuard issues are tracked.
     */
    private static volatile Tracker currentTracker = DEFAULT_TRACKER;

    /**
     * Returns a CloseGuard instance. If CloseGuard is enabled, {@code
     * #open(String)} can be used to set up the instance to warn on
     * failure to close. If CloseGuard is disabled, a non-null no-op
     * instance is returned.
     */
    public static CloseGuard get() {
        if (!ENABLED) {
            return NOOP;
        }
        return new CloseGuard();
    }

    /**
     * Used to enable or disable CloseGuard. Note that CloseGuard only
     * warns if it is enabled for both allocation and finalization.
     */
    public static void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    /**
     * True if CloseGuard mechanism is enabled.
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /**
     * Used to replace default Reporter used to warn of CloseGuard
     * violations. Must be non-null.
     */
    public static void setReporter(Reporter reporter) {
        if (reporter == null) {
            throw new NullPointerException("reporter == null");
        }
        REPORTER = reporter;
    }

    /**
     * Returns non-null CloseGuard.Reporter.
     */
    public static Reporter getReporter() {
        return REPORTER;
    }

    /**
     * Sets the {@link Tracker} that is notified when resources are allocated and released.
     *
     * <p>This is only intended for use by {@code dalvik.system.CloseGuardSupport} class and so
     * MUST NOT be used for any other purposes.
     *
     * @throws NullPointerException if tracker is null
     */
    public static void setTracker(Tracker tracker) {
        if (tracker == null) {
            throw new NullPointerException("tracker == null");
        }
        currentTracker = tracker;
    }

    /**
     * Returns {@link #setTracker(Tracker) last Tracker that was set}, or otherwise a default
     * Tracker that does nothing.
     *
     * <p>This is only intended for use by {@code dalvik.system.CloseGuardSupport} class and so
     * MUST NOT be used for any other purposes.
     */
    public static Tracker getTracker() {
        return currentTracker;
    }

    private CloseGuard() {}

    /**
     * If CloseGuard is enabled, {@code open} initializes the instance
     * with a warning that the caller should have explicitly called the
     * {@code closer} method instead of relying on finalization.
     *
     * @param closer non-null name of explicit termination method
     * @throws NullPointerException if closer is null, regardless of
     * whether or not CloseGuard is enabled
     */
    public void open(String closer) {
        // always perform the check for valid API usage...
        if (closer == null) {
            throw new NullPointerException("closer == null");
        }
        // ...but avoid allocating an allocationSite if disabled
        if (this == NOOP || !ENABLED) {
            return;
        }
        String message = "Explicit termination method '" + closer + "' not called";
        allocationSite = new Throwable(message);
        currentTracker.open(allocationSite);
    }

    private Throwable allocationSite;

    /**
     * Marks this CloseGuard instance as closed to avoid warnings on
     * finalization.
     */
    public void close() {
        currentTracker.close(allocationSite);
        allocationSite = null;
    }

    /**
     * If CloseGuard is enabled, logs a warning if the caller did not
     * properly cleanup by calling an explicit close method
     * before finalization. If CloseGuard is disabled, no action is
     * performed.
     */
    public void warnIfOpen() {
        if (allocationSite == null || !ENABLED) {
            return;
        }

        String message =
                ("A resource was acquired at attached stack trace but never released. "
                 + "See java.io.Closeable for information on avoiding resource leaks.");

        REPORTER.report(message, allocationSite);
    }

    /**
     * Interface to allow customization of tracking behaviour.
     *
     * <p>This is only intended for use by {@code dalvik.system.CloseGuardSupport} class and so
     * MUST NOT be used for any other purposes.
     */
    public interface Tracker {
        void open(Throwable allocationSite);
        void close(Throwable allocationSite);
    }

    /**
     * Default tracker which does nothing special and simply leaves it up to the GC to detect a
     * leak.
     */
    private static final class DefaultTracker implements Tracker {
        @Override
        public void open(Throwable allocationSite) {
        }

        @Override
        public void close(Throwable allocationSite) {
        }
    }

    /**
     * Interface to allow customization of reporting behavior.
     */
    public interface Reporter {
        void report (String message, Throwable allocationSite);
    }

    /**
     * Default Reporter which reports CloseGuard violations to the log.
     */
    private static final class DefaultReporter implements Reporter {
        @Override public void report (String message, Throwable allocationSite) {
            System.logW(message, allocationSite);
        }
    }
}
