package com.ksoot.batch.config;

import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@EnableConfigurationProperties(MongoProperties.class)
@Configuration
public class MongoPropertiesConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.data.mongodb")
  MongoProperties mongoProperties() {
    return new MongoProperties();
  }

  @Bean
  @ConfigurationProperties("spring.data.mongodb.account")
  MongoProperties accountMongoProperties() {
    return new MongoProperties();
  }

  @Bean
  @ConfigurationProperties("spring.data.mongodb.transaction")
  MongoProperties transactionMongoProperties() {
    return new MongoProperties();
  }
}
