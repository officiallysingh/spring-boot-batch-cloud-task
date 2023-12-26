package com.ksoot.batch.job.common;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

@RequiredArgsConstructor
public class ListFlattenerKafkaWriter<K, T>
    implements ItemWriter<List<T>>, ItemStream, InitializingBean {

  private final KafkaItemWriter<K, T> delegate;

  @Override
  public void write(final Chunk<? extends List<T>> items) throws Exception {
    if (items == null) {
      return;
    }
    Chunk<T> chunk = new Chunk<>();
    items.getItems().stream().flatMap(List::stream).toList().forEach(chunk::add);
    if (!chunk.isEmpty()) {
      this.delegate.write(chunk);
    }
  }

  @Override
  public void open(final ExecutionContext executionContext) throws ItemStreamException {
    if (this.delegate instanceof ItemStream) {
      ((ItemStream) this.delegate).open(executionContext);
    }
  }

  @Override
  public void update(final ExecutionContext executionContext) throws ItemStreamException {
    if (this.delegate instanceof ItemStream) {
      ((ItemStream) this.delegate).update(executionContext);
    }
  }

  @Override
  public void close() throws ItemStreamException {
    if (this.delegate instanceof ItemStream) {
      ((ItemStream) this.delegate).close();
    }
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(this.delegate != null, "'KafkaItemWriter' is required.");
  }
}
