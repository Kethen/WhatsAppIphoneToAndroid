/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.scanner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.BugChecker;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckReturnValue;

/**
 * Supplies {@link Scanner}s and provides access to the backing sets of all {@link BugChecker}s and
 * enabled {@link BugChecker}s.
 */
public abstract class ScannerSupplier implements Supplier<Scanner> {

  /* Static factory methods and helpers */

  /** Returns a {@link ScannerSupplier} with a specific list of {@link BugChecker} classes. */
  @SafeVarargs
  public static ScannerSupplier fromBugCheckerClasses(
      Class<? extends BugChecker>... checkerClasses) {
    return fromBugCheckerClasses(Arrays.asList(checkerClasses));
  }

  private static ImmutableMap<String, SeverityLevel> defaultSeverities(
      Iterable<BugCheckerInfo> checkers) {
    ImmutableMap.Builder<String, SeverityLevel> severities = ImmutableMap.builder();
    for (BugCheckerInfo check : checkers) {
      severities.put(check.canonicalName(), check.defaultSeverity());
    }
    return severities.build();
  }

  /** Returns a {@link ScannerSupplier} with a specific list of {@link BugChecker} classes. */
  public static ScannerSupplier fromBugCheckerClasses(
      Iterable<Class<? extends BugChecker>> checkers) {
    ImmutableList.Builder<BugCheckerInfo> builder = ImmutableList.builder();
    for (Class<? extends BugChecker> checker : checkers) {
      builder.add(BugCheckerInfo.create(checker));
    }
    return fromBugCheckerInfos(builder.build());
  }

  /** Returns a {@link ScannerSupplier} built from a list of {@link BugCheckerInfo}s. */
  public static ScannerSupplier fromBugCheckerInfos(Iterable<BugCheckerInfo> checkers) {
    ImmutableBiMap.Builder<String, BugCheckerInfo> builder = ImmutableBiMap.builder();
    for (BugCheckerInfo checker : checkers) {
      builder.put(checker.canonicalName(), checker);
    }
    ImmutableBiMap<String, BugCheckerInfo> allChecks = builder.build();
    return new ScannerSupplierImpl(
        allChecks,
        defaultSeverities(allChecks.values()),
        ImmutableSet.<String>of(),
        ErrorProneFlags.empty());
  }

  /**
   * Returns a {@link ScannerSupplier} that just returns the {@link Scanner} that was passed in.
   * Used mostly for testing. Does not implement any method other than {@link
   * ScannerSupplier#get()}.
   */
  public static ScannerSupplier fromScanner(Scanner scanner) {
    return new InstanceReturningScannerSupplierImpl(scanner);
  }

  /* Instance methods */

  /**
   * Returns a map of check name to {@link BugCheckerInfo} for all {@link BugCheckerInfo}s in this
   * {@link ScannerSupplier}, including disabled ones.
   */
  @VisibleForTesting
  public abstract ImmutableBiMap<String, BugCheckerInfo> getAllChecks();

  /**
   * Returns the set of {@link BugCheckerInfo}s that are enabled in this {@link ScannerSupplier}.
   */
  public abstract ImmutableSet<BugCheckerInfo> getEnabledChecks();

  public abstract ImmutableMap<String, SeverityLevel> severities();

  protected abstract ImmutableSet<String> disabled();

  public abstract ErrorProneFlags getFlags();

  /**
   * Applies options to this {@link ScannerSupplier}.
   *
   * <p>Command-line options to override check severities may do any of the following:
   *
   * <ul>
   *   <li>Enable a check that is currently off
   *   <li>Disable a check that is currently on
   *   <li>Change the severity of a check that is on, promoting a warning to an error or demoting an
   *       error to a warning
   * </ul>
   *
   * @param errorProneOptions an {@link ErrorProneOptions} object that encapsulates the overrides
   *     for this compilation
   * @throws InvalidCommandLineOptionException if the override map attempts to disable a check that
   *     may not be disabled
   */
  @CheckReturnValue
  public ScannerSupplier applyOverrides(ErrorProneOptions errorProneOptions)
      throws InvalidCommandLineOptionException {
    Map<String, Severity> severityOverrides = errorProneOptions.getSeverityMap();
    if (severityOverrides.isEmpty()
        && errorProneOptions.getFlags().isEmpty()
        && !errorProneOptions.isEnableAllChecksAsWarnings()
        && !errorProneOptions.isDropErrorsToWarnings()
        && !errorProneOptions.isDisableAllChecks()) {
      return this;
    }

    // Initialize result allChecks map and enabledChecks set with current state of this Supplier.
    ImmutableBiMap<String, BugCheckerInfo> checks = getAllChecks();
    Map<String, SeverityLevel> severities = new LinkedHashMap<>(severities());
    Set<String> disabled = new HashSet<>(disabled());

    if (errorProneOptions.isEnableAllChecksAsWarnings()) {
      disabled.forEach(c -> severities.put(c, SeverityLevel.WARNING));
      disabled.clear();
    }

    if (errorProneOptions.isDropErrorsToWarnings()) {
      getAllChecks()
          .values()
          .stream()
          .filter(
              c -> c.defaultSeverity() == SeverityLevel.ERROR && c.suppressibility().disableable())
          .forEach(c -> severities.put(c.canonicalName(), SeverityLevel.WARNING));
    }

    if (errorProneOptions.isDisableAllChecks()) {
      getAllChecks()
          .values()
          .stream()
          .filter(c -> c.suppressibility().disableable())
          .forEach(c -> disabled.add(c.canonicalName()));
    }

    // Process overrides
    severityOverrides.forEach(
        (checkName, newSeverity) -> {
          BugCheckerInfo check = getAllChecks().get(checkName);
          if (check == null) {
            if (errorProneOptions.ignoreUnknownChecks()) {
              return;
            }
            throw new InvalidCommandLineOptionException(checkName + " is not a valid checker name");
          }

          switch (newSeverity) {
            case OFF:
              if (!check.suppressibility().disableable()) {
                throw new InvalidCommandLineOptionException(
                    check.canonicalName() + " may not be disabled");
              }
              severities.remove(check.canonicalName());
              disabled.add(check.canonicalName());
              break;
            case DEFAULT:
              severities.put(check.canonicalName(), check.defaultSeverity());
              disabled.remove(check.canonicalName());
              break;
            case WARN:
              // Demoting an enabled check from an error to a warning is a form of disabling
              if (!disabled().contains(check.canonicalName())
                  && !check.suppressibility().disableable()
                  && check.defaultSeverity() == SeverityLevel.ERROR) {
                throw new InvalidCommandLineOptionException(
                    check.canonicalName()
                        + " is not disableable and may not be demoted to a warning");
              }
              severities.put(check.canonicalName(), SeverityLevel.WARNING);
              disabled.remove(check.canonicalName());
              break;
            case ERROR:
              severities.put(check.canonicalName(), SeverityLevel.ERROR);
              disabled.remove(check.canonicalName());
              break;
            default:
              throw new IllegalStateException("Unexpected severity level: " + newSeverity);
          }
        });

    return new ScannerSupplierImpl(
        checks,
        ImmutableMap.copyOf(severities),
        ImmutableSet.copyOf(disabled),
        errorProneOptions.getFlags());
  }

  /**
   * Composes this {@link ScannerSupplier} with the {@code other} {@link ScannerSupplier}. The set
   * of checks that are turned on is the intersection of the checks on in {@code this} and {@code
   * other}.
   */
  @CheckReturnValue
  public ScannerSupplier plus(ScannerSupplier other) {
    ImmutableBiMap<String, BugCheckerInfo> combinedAllChecks =
        ImmutableBiMap.<String, BugCheckerInfo>builder()
            .putAll(this.getAllChecks())
            .putAll(other.getAllChecks())
            .build();
    ImmutableMap<String, SeverityLevel> combinedSeverities =
        ImmutableMap.<String, SeverityLevel>builder()
            .putAll(severities())
            .putAll(other.severities())
            .build();
    ImmutableSet<String> disabled = ImmutableSet.copyOf(Sets.union(disabled(), other.disabled()));
    ErrorProneFlags combinedFlags = this.getFlags().plus(other.getFlags());
    return new ScannerSupplierImpl(combinedAllChecks, combinedSeverities, disabled, combinedFlags);
  }

  /**
   * Filters this {@link ScannerSupplier} based on the provided predicate. Returns a {@link
   * ScannerSupplier} with only the checks enabled that satisfy the predicate.
   */
  @CheckReturnValue
  public ScannerSupplier filter(Predicate<? super BugCheckerInfo> predicate) {
    ImmutableSet.Builder<String> disabled = ImmutableSet.builder();
    for (BugCheckerInfo check : getAllChecks().values()) {
      if (!predicate.apply(check)) {
        disabled.add(check.canonicalName());
      }
    }
    return new ScannerSupplierImpl(getAllChecks(), severities(), disabled.build(), getFlags());
  }
}
