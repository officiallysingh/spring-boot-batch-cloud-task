package com.ksoot.batch.job.common;

import org.springframework.batch.core.ExitStatus;
import org.springframework.util.Assert;

public class StepStatus {

  private StepStatus() {
    throw new AssertionError("Static helper class. Not supposed to be instantiated");
  }

  public static ExitStatus unknown() {
    return ExitStatus.UNKNOWN;
  }

  public static ExitStatus executing() {
    return ExitStatus.EXECUTING;
  }

  public static ExitStatus completed() {
    return ExitStatus.COMPLETED;
  }

  public static ExitStatus noProcessing() {
    return ExitStatus.NOOP;
  }

  public static ExitStatus failed() {
    return ExitStatus.FAILED;
  }

  public static ExitStatus stopped() {
    return ExitStatus.STOPPED;
  }

  public static final ExitStatus COMPLETED_WITH_SKIPS =
      new ExitStatus("COMPLETED_WITH_SKIPS", "Completed with skips");

  public static ExitStatus of(final String code, final String description) {
    Assert.hasText("code", "ExitStatus code required");
    return new ExitStatus(code, description);
  }

  public static ExitStatus of(final String code) {
    Assert.hasText("code", "ExitStatus code required");
    return new ExitStatus(code);
  }

  public static boolean isCompletedWithSkips(ExitStatus exitStatus) {
    return exitStatus.getExitCode().equals(COMPLETED_WITH_SKIPS.getExitCode());
  }

  public static String[] retryableStatusCodes() {
    return new String[] {ExitStatus.FAILED.getExitCode(), COMPLETED_WITH_SKIPS.getExitCode()};
  }
}
