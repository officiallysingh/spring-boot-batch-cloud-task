package com.ksoot.batch.job.common;

import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.batch.item.data.AbstractPaginatedDataItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class MongoAggregationPagingItemReader<T> extends AbstractPaginatedDataItemReader<T>
    implements InitializingBean {

  protected MongoOperations template;

  protected AggregationOperation[] aggregationOperations;

  protected Class<? extends T> type;

  protected String collection;

  public MongoAggregationPagingItemReader() {
    super();
    setName(ClassUtils.getShortName(MongoAggregationPagingItemReader.class));
  }

  /**
   * A Mongo Aggregation Operations to be used.
   *
   * @param aggregationOperations Mongo Aggregation Operations to be used.
   */
  public void setAggregationOperation(final AggregationOperation... aggregationOperations) {
    this.aggregationOperations = aggregationOperations;
  }

  /**
   * Used to perform operations against the MongoDB instance. Also handles the mapping of documents
   * to objects.
   *
   * @param template the MongoOperations instance to use
   * @see MongoOperations
   */
  public void setTemplate(final MongoOperations template) {
    this.template = template;
  }

  /**
   * The type of object to be returned for each {@link #read()} call.
   *
   * @param type the type of object to return
   */
  public void setTargetType(final Class<? extends T> type) {
    this.type = type;
  }

  /**
   * @param collection Mongo collection to be queried.
   */
  public void setCollection(String collection) {
    this.collection = collection;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Iterator<T> doPageRead() {
    return (Iterator<T>)
        this.template
            .aggregate(
                Aggregation.newAggregation(
                    ArrayUtils.addAll(
                        this.aggregationOperations,
                        Aggregation.limit(this.pageSize),
                        Aggregation.skip((long) (this.page) * this.pageSize))),
                this.collection,
                this.type)
            .getMappedResults()
            .iterator();
  }

  /**
   * Checks mandatory properties
   *
   * @see InitializingBean#afterPropertiesSet()
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.template, "An implementation of MongoOperations is required.");
    Assert.notNull(this.collection, "A Collection name is required.");
    Assert.notNull(this.type, "A type to convert the input into is required.");
    Assert.notNull(this.aggregationOperations, "AggregationOperation[] is required.");
    Assert.noNullElements(
        this.aggregationOperations, "AggregationOperation[] cannot have null elements.");
  }
}
