package com.ksoot.batch.job.common;

import org.apache.commons.lang3.builder.Builder;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.util.Assert;

public class MongoItemWriters {

  public static <T> CollectionBuilder<T> template(final MongoOperations template) {
    return new ItemWriterBuilder<>(template);
  }

  public interface CollectionBuilder<T> {
    public IdGeneratorBuilder<T> collection(final String collection);
  }

  public interface IdGeneratorBuilder<T> extends Builder<MongoItemWriter<T>> {
    public Builder<MongoItemWriter<T>> idGenerator(final MongoIdGenerator<T, Object> idGenerator);
  }

  public static class ItemWriterBuilder<T> implements CollectionBuilder<T>, IdGeneratorBuilder<T> {

    private MongoOperations template;

    private MongoIdGenerator<T, Object> idGenerator;

    private String collection;

    ItemWriterBuilder(MongoOperations template) {
      Assert.notNull(template, "'template' is required.");
      this.template = template;
    }

    @Override
    public IdGeneratorBuilder<T> collection(String collection) {
      Assert.hasText(collection, "'collection' name is required.");
      this.collection = collection;
      return this;
    }

    @Override
    public Builder<MongoItemWriter<T>> idGenerator(MongoIdGenerator<T, Object> idGenerator) {
      Assert.notNull(idGenerator, "'duplicateCriteria' is required.");
      this.idGenerator = idGenerator;
      return this;
    }

    @Override
    public MongoItemWriter<T> build() {
      if (this.idGenerator == null) {
        return new MongoItemWriterBuilder<T>()
            .template(this.template)
            .collection(this.collection)
            .build();
      } else {
        return new MongoUpsertItemWriter<>(this.template, this.collection, this.idGenerator);
      }
    }
  }
}
