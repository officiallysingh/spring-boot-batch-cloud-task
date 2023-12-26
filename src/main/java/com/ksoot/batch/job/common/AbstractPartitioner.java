package com.ksoot.batch.job.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.batch.core.partition.support.PartitionNameProvider;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

@Slf4j
public abstract class AbstractPartitioner
    implements Partitioner, PartitionNameProvider, InitializingBean {

  public static final String PARTITION_NAME_PREFIX = "partition";
  public static final String PARTITION_DATA_VALUE_SEPARATOR = ",";
  public static final String PARTITION_DATA_EMBEDDED_VALUE_SEPARATOR = "|";

  protected final BatchProperties batchProperties;
  protected final String partitionDataKeyName;

  protected AbstractPartitioner(
      final BatchProperties batchProperties, final String partitionDataKeyName) {
    this.batchProperties = batchProperties;
    this.partitionDataKeyName = partitionDataKeyName;
  }

  protected abstract List<String> partitioningList();

  @Override
  public Map<String, ExecutionContext> partition(final int gridSize) {
    log.info("Grid size: " + gridSize);
    List<String> partitioningList = this.partitioningList();

    log.info(
        "Number of records in partitioning list: "
            + (CollectionUtils.isNotEmpty(partitioningList) ? partitioningList.size() : 0));

    if (CollectionUtils.isNotEmpty(partitioningList)) {
      Map<String, ExecutionContext> partitionContextMap = Maps.newHashMapWithExpectedSize(gridSize);

      int partitionSize = 0;
      int sourceLen = partitioningList.size();
      if (sourceLen > this.batchProperties.getTriggerPartitioningThreshold()) {
        if (sourceLen % gridSize == 0) {
          partitionSize = sourceLen / gridSize;
        } else {
          partitionSize = sourceLen / gridSize + 1;
        }
      }

      List<List<String>> partitions =
          partitionSize <= 1
              ? List.of(partitioningList)
              : Lists.partition(partitioningList, partitionSize);
      log.info(
          "Partition size: " + (CollectionUtils.isNotEmpty(partitions) ? partitions.size() : 0));

      int finalGridSize = partitions.size();
      List<String> partitionNames = this.getPartitionNames(finalGridSize);

      for (int i = 0; i < partitionNames.size(); i++) {
        List<String> partition = partitions.get(i);
        String partitionData = String.join(PARTITION_DATA_VALUE_SEPARATOR, partition);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.put(this.partitionDataKeyName, partitionData);
        partitionContextMap.put(partitionNames.get(i), executionContext);
      }

      return partitionContextMap;
    } else {
      return Collections.emptyMap();
    }
  }

  @Override
  public List<String> getPartitionNames(int gridSize) {
    return IntStream.range(0, gridSize)
        .mapToObj(i -> PARTITION_NAME_PREFIX + "-" + (i + 1))
        .toList();
  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(this.batchProperties != null, "'batchProperties' is required.");
    Assert.hasText(this.partitionDataKeyName, "'partitionDataKeyName' is required.");
  }
}
