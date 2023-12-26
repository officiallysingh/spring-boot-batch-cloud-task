package com.ksoot.batch.job.common;

import java.util.Objects;
import org.bson.types.ObjectId;
import org.springframework.util.Assert;

@FunctionalInterface
public interface MongoIdGenerator<T, R> {

  static ObjectId compositeIdGenerator(final Object... values) {
    Assert.notEmpty(values, "'values' must not be null or empty");
    Assert.noNullElements(values, "'values' must not contain null elements");

    int compositeId = Objects.hash(values);
    String hexString = Integer.toHexString(compositeId);
    // Pad the hex string with leading zeros to ensure it has a length of 24 characters
    while (hexString.length() < 24) {
      hexString = "0" + hexString;
    }

    return new ObjectId(hexString);
  }

  R generate(final T input);
}
