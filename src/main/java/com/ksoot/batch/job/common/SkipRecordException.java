package com.ksoot.batch.job.common;

import org.springframework.batch.core.step.skip.SkipException;
import org.springframework.lang.Nullable;

public class SkipRecordException extends SkipException {

  public SkipRecordException(String msg) {
    super(msg);
  }

  public SkipRecordException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }
}
