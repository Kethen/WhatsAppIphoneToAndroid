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

package com.google.errorprone;

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.apply.ImportOrganizer;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Processes command-line options specific to error-prone.
 *
 * <p>Documentation for the available flags are available at http://errorprone.info/docs/flags
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ErrorProneOptions {

  private static final String SEVERITY_PREFIX = "-Xep:";
  private static final String PATCH_CHECKS_PREFIX = "-XepPatchChecks:";
  private static final String PATCH_OUTPUT_LOCATION = "-XepPatchLocation:";
  private static final String PATCH_IMPORT_ORDER_PREFIX = "-XepPatchImportOrder:";
  private static final String ERRORS_AS_WARNINGS_FLAG = "-XepAllErrorsAsWarnings";
  private static final String ENABLE_ALL_CHECKS = "-XepAllDisabledChecksAsWarnings";
  private static final String DISABLE_ALL_CHECKS = "-XepDisableAllChecks";
  private static final String IGNORE_UNKNOWN_CHECKS_FLAG = "-XepIgnoreUnknownCheckNames";
  private static final String DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG =
      "-XepDisableWarningsInGeneratedCode";

  /** see {@link javax.tools.OptionChecker#isSupportedOption(String)} */
  public static int isSupportedOption(String option) {
    boolean isSupported =
        option.startsWith(SEVERITY_PREFIX)
            || option.startsWith(ErrorProneFlags.PREFIX)
            || option.startsWith(PATCH_OUTPUT_LOCATION)
            || option.startsWith(PATCH_CHECKS_PREFIX)
            || option.equals(IGNORE_UNKNOWN_CHECKS_FLAG)
            || option.equals(DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG)
            || option.equals(ERRORS_AS_WARNINGS_FLAG)
            || option.equals(ENABLE_ALL_CHECKS)
            || option.equals(DISABLE_ALL_CHECKS);
    return isSupported ? 0 : -1;
  }

  public boolean isEnableAllChecksAsWarnings() {
    return enableAllChecksAsWarnings;
  }

  public boolean isDisableAllChecks() {
    return disableAllChecks;
  }

  /**
   * Severity levels for an error-prone check that define how the check results should be presented.
   */
  public enum Severity {
    DEFAULT, // whatever is specified in the @BugPattern annotation
    OFF,
    WARN,
    ERROR
  }

  @AutoValue
  abstract static class PatchingOptions {
    final boolean doRefactor() {
      return inPlace() || !baseDirectory().isEmpty();
    }

    abstract Set<String> namedCheckers();

    abstract boolean inPlace();

    abstract String baseDirectory();

    abstract Optional<Supplier<CodeTransformer>> customRefactorer();

    abstract ImportOrganizer importOrganizer();

    static Builder builder() {
      return new AutoValue_ErrorProneOptions_PatchingOptions.Builder()
          .baseDirectory("")
          .inPlace(false)
          .namedCheckers(Collections.emptySet())
          .importOrganizer(ImportOrganizer.STATIC_FIRST_ORGANIZER);
    }

    @AutoValue.Builder
    abstract static class Builder {

      abstract Builder namedCheckers(Set<String> checkers);

      abstract Builder inPlace(boolean inPlace);

      abstract Builder baseDirectory(String baseDirectory);

      abstract Builder customRefactorer(Supplier<CodeTransformer> refactorer);

      abstract Builder importOrganizer(ImportOrganizer importOrganizer);

      abstract PatchingOptions autoBuild();

      final PatchingOptions build() {

        PatchingOptions patchingOptions = autoBuild();

        // If anything is specified, then (checkers or refaster) and output must be set.
        if ((!patchingOptions.namedCheckers().isEmpty()
                || patchingOptions.customRefactorer().isPresent())
            ^ patchingOptions.doRefactor()) {
          throw new InvalidCommandLineOptionException(
              "-XepPatchChecks and -XepPatchLocation must be specified together");
        }
        return patchingOptions;
      }
    }
  }

  private final ImmutableList<String> remainingArgs;
  private final ImmutableMap<String, Severity> severityMap;
  private final boolean ignoreUnknownChecks;
  private final boolean disableWarningsInGeneratedCode;
  private final boolean dropErrorsToWarnings;
  private final boolean enableAllChecksAsWarnings;
  private final boolean disableAllChecks;
  private final ErrorProneFlags flags;
  private final PatchingOptions patchingOptions;

  private ErrorProneOptions(
      ImmutableMap<String, Severity> severityMap,
      ImmutableList<String> remainingArgs,
      boolean ignoreUnknownChecks,
      boolean disableWarningsInGeneratedCode,
      boolean dropErrorsToWarnings,
      boolean enableAllChecksAsWarnings,
      boolean disableAllChecks,
      ErrorProneFlags flags,
      PatchingOptions patchingOptions) {
    this.severityMap = severityMap;
    this.remainingArgs = remainingArgs;
    this.ignoreUnknownChecks = ignoreUnknownChecks;
    this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    this.dropErrorsToWarnings = dropErrorsToWarnings;
    this.enableAllChecksAsWarnings = enableAllChecksAsWarnings;
    this.disableAllChecks = disableAllChecks;
    this.flags = flags;
    this.patchingOptions = patchingOptions;
  }

  public String[] getRemainingArgs() {
    return remainingArgs.toArray(new String[remainingArgs.size()]);
  }

  public ImmutableMap<String, Severity> getSeverityMap() {
    return severityMap;
  }

  public boolean ignoreUnknownChecks() {
    return ignoreUnknownChecks;
  }

  public boolean disableWarningsInGeneratedCode() {
    return disableWarningsInGeneratedCode;
  }

  public boolean isDropErrorsToWarnings() {
    return dropErrorsToWarnings;
  }

  public ErrorProneFlags getFlags() {
    return flags;
  }

  public PatchingOptions patchingOptions() {
    return patchingOptions;
  }

  private static class Builder {
    private boolean ignoreUnknownChecks = false;
    private boolean disableWarningsInGeneratedCode = false;
    private boolean dropErrorsToWarnings = false;
    private boolean enableAllChecksAsWarnings = false;
    private boolean disableAllChecks = false;
    private Map<String, Severity> severityMap = new HashMap<>();
    private final ErrorProneFlags.Builder flagsBuilder = ErrorProneFlags.builder();
    private final PatchingOptions.Builder patchingOptionsBuilder = PatchingOptions.builder();

    private void parseSeverity(String arg) {
      // Strip prefix
      String remaining = arg.substring(SEVERITY_PREFIX.length());
      // Split on ':'
      String[] parts = remaining.split(":");
      if (parts.length > 2 || parts[0].isEmpty()) {
        throw new InvalidCommandLineOptionException("invalid flag: " + arg);
      }
      String checkName = parts[0];
      Severity severity;
      if (parts.length == 1) {
        severity = Severity.DEFAULT;
      } else { // parts.length == 2
        try {
          severity = Severity.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
          throw new InvalidCommandLineOptionException("invalid flag: " + arg);
        }
      }
      severityMap.put(checkName, severity);
    }

    public void parseFlag(String flag) {
      flagsBuilder.parseFlag(flag);
    }

    public void setIgnoreUnknownChecks(boolean ignoreUnknownChecks) {
      this.ignoreUnknownChecks = ignoreUnknownChecks;
    }

    public void setDisableWarningsInGeneratedCode(boolean disableWarningsInGeneratedCode) {
      this.disableWarningsInGeneratedCode = disableWarningsInGeneratedCode;
    }

    public void setDropErrorsToWarnings(boolean dropErrorsToWarnings) {
      severityMap
          .entrySet()
          .stream()
          .filter(e -> e.getValue() == Severity.ERROR)
          .forEach(e -> e.setValue(Severity.WARN));
      this.dropErrorsToWarnings = dropErrorsToWarnings;
    }

    public void setEnableAllChecksAsWarnings(boolean enableAllChecksAsWarnings) {
      // Checks manually disabled before this flag are reset to warning-level
      severityMap
          .entrySet()
          .stream()
          .filter(e -> e.getValue() == Severity.OFF)
          .forEach(e -> e.setValue(Severity.WARN));
      this.enableAllChecksAsWarnings = enableAllChecksAsWarnings;
    }

    public void setDisableAllChecks(boolean disableAllChecks) {
      // Discard previously set severities so that the DisableAllChecks flag is position sensitive.
      severityMap.clear();
      this.disableAllChecks = disableAllChecks;
    }

    public PatchingOptions.Builder patchingOptionsBuilder() {
      return patchingOptionsBuilder;
    }

    public ErrorProneOptions build(ImmutableList<String> outputArgs) {
      return new ErrorProneOptions(
          ImmutableMap.copyOf(severityMap),
          outputArgs,
          ignoreUnknownChecks,
          disableWarningsInGeneratedCode,
          dropErrorsToWarnings,
          enableAllChecksAsWarnings,
          disableAllChecks,
          flagsBuilder.build(),
          patchingOptionsBuilder.build());
    }
  }

  private static final ErrorProneOptions EMPTY = new Builder().build(ImmutableList.of());

  public static ErrorProneOptions empty() {
    return EMPTY;
  }

  /**
   * Given a list of command-line arguments, produce the corresponding {@link ErrorProneOptions}
   * instance.
   *
   * @param args command-line arguments
   * @return an {@link ErrorProneOptions} instance encapsulating the given arguments
   * @throws InvalidCommandLineOptionException if an error-prone option is invalid
   */
  public static ErrorProneOptions processArgs(Iterable<String> args) {
    Preconditions.checkNotNull(args);
    ImmutableList.Builder<String> outputArgs = ImmutableList.builder();

    /* By default, we throw an error when an unknown option is passed in, if for example you
     * try to disable a check that doesn't match any of the known checks.  This catches typos from
     * the command line.
     *
     * You can pass the IGNORE_UNKNOWN_CHECKS_FLAG to opt-out of that checking.  This allows you to
     * use command lines from different versions of error-prone interchangeably.
     */
    Builder builder = new Builder();
    for (String arg : args) {
      switch (arg) {
        case IGNORE_UNKNOWN_CHECKS_FLAG:
          builder.setIgnoreUnknownChecks(true);
          break;
        case DISABLE_WARNINGS_IN_GENERATED_CODE_FLAG:
          builder.setDisableWarningsInGeneratedCode(true);
          break;
        case ERRORS_AS_WARNINGS_FLAG:
          builder.setDropErrorsToWarnings(true);
          break;
        case ENABLE_ALL_CHECKS:
          builder.setEnableAllChecksAsWarnings(true);
          break;
        case DISABLE_ALL_CHECKS:
          builder.setDisableAllChecks(true);
          break;
        default:
          if (arg.startsWith(SEVERITY_PREFIX)) {
            builder.parseSeverity(arg);
          } else if (arg.startsWith(ErrorProneFlags.PREFIX)) {
            builder.parseFlag(arg);
          } else if (arg.startsWith(PATCH_OUTPUT_LOCATION)) {
            String remaining = arg.substring(PATCH_OUTPUT_LOCATION.length());
            if (remaining.equals("IN_PLACE")) {
              builder.patchingOptionsBuilder().inPlace(true);
            } else {
              if (remaining.isEmpty()) {
                throw new InvalidCommandLineOptionException("invalid flag: " + arg);
              }
              builder.patchingOptionsBuilder().baseDirectory(remaining);
            }
          } else if (arg.startsWith(PATCH_CHECKS_PREFIX)) {
            String remaining = arg.substring(PATCH_CHECKS_PREFIX.length());
            if (remaining.startsWith("refaster:")) {
              // Refaster rule, load from InputStream at file
              builder
                  .patchingOptionsBuilder()
                  .customRefactorer(
                      () -> {
                        String path = remaining.substring("refaster:".length());
                        try (InputStream in =
                                Files.newInputStream(FileSystems.getDefault().getPath(path));
                            ObjectInputStream ois = new ObjectInputStream(in)) {
                          return (CodeTransformer) ois.readObject();
                        } catch (IOException | ClassNotFoundException e) {
                          throw new RuntimeException("Can't load Refaster rule from " + path, e);
                        }
                      });
            } else {
              Iterable<String> checks = Splitter.on(',').trimResults().split(remaining);
              builder.patchingOptionsBuilder().namedCheckers(ImmutableSet.copyOf(checks));
            }
          } else if (arg.startsWith(PATCH_IMPORT_ORDER_PREFIX)) {
            String remaining = arg.substring(PATCH_IMPORT_ORDER_PREFIX.length());
            ImportOrganizer importOrganizer = ImportOrderParser.getImportOrganizer(remaining);
            builder.patchingOptionsBuilder().importOrganizer(importOrganizer);
          } else {
            outputArgs.add(arg);
          }
      }
    }

    return builder.build(outputArgs.build());
  }

  /**
   * Given a list of command-line arguments, produce the corresponding {@link ErrorProneOptions}
   * instance.
   *
   * @param args command-line arguments
   * @return an {@link ErrorProneOptions} instance encapsulating the given arguments
   * @throws InvalidCommandLineOptionException if an error-prone option is invalid
   */
  public static ErrorProneOptions processArgs(String[] args) {
    Preconditions.checkNotNull(args);
    return processArgs(Arrays.asList(args));
  }
}
