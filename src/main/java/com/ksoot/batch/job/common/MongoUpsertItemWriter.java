package com.ksoot.batch.job.common;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class MongoUpsertItemWriter<T> extends MongoItemWriter<T> {

  private static final String ID_KEY = "_id";

  private MongoOperations template;

  private String collection;

  private MongoIdGenerator<T, Object> idGenerator;

  public MongoUpsertItemWriter(
      final MongoOperations template,
      final String collection,
      final MongoIdGenerator<T, Object> idGenerator) {
    Assert.notNull(template, "'template' is required.");
    Assert.hasText(collection, "'collection' name is required.");
    Assert.notNull(idGenerator, "'idGenerator' is required.");
    this.template = template;
    this.collection = collection;
    this.idGenerator = idGenerator;
  }

  public MongoUpsertItemWriter(final MongoOperations template, final String collection) {
    this(template, collection, (T value) -> new ObjectId());
  }

  /**
   * Performs the actual write to the store via the template. This can be overridden by a subclass
   * if necessary.
   *
   * @param chunk the chunk of items to be persisted.
   */
  @Override
  protected void doWrite(Chunk<? extends T> chunk) {
    if (!CollectionUtils.isEmpty(chunk.getItems())) {
      saveOrUpdateRecord(chunk);
    }
  }

  private void saveOrUpdateRecord(Chunk<? extends T> chunk) {
    BulkOperations bulkOperations =
        initializeBulkOperations(BulkOperations.BulkMode.ORDERED, chunk.getItems().get(0));
    MongoConverter mongoConverter = this.template.getConverter();
    FindAndReplaceOptions upsert = new FindAndReplaceOptions().upsert();
    for (Object item : chunk) {
      Document document = new Document();
      mongoConverter.write(item, document);

      Object objectId =
          document.get(ID_KEY) != null ? document.get(ID_KEY) : this.idGenerator.generate((T) item);
      Query query = new Query().addCriteria(Criteria.where(ID_KEY).is(objectId));
      bulkOperations.replaceOne(query, document, upsert);
    }

    bulkOperations.execute();
  }

  private BulkOperations initializeBulkOperations(BulkOperations.BulkMode bulkMode, Object item) {
    BulkOperations bulkOperations;
    if (StringUtils.hasText(this.collection)) {
      bulkOperations = this.template.bulkOps(bulkMode, this.collection);
    } else {
      bulkOperations = this.template.bulkOps(bulkMode, ClassUtils.getUserClass(item));
    }
    return bulkOperations;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(this.template != null, "A MongoOperations implementation is required.");
  }
}
