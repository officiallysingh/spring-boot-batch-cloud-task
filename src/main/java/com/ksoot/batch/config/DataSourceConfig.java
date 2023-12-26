package com.ksoot.batch.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@EnableConfigurationProperties(MongoProperties.class)
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.data.mongodb")
    MongoProperties mongoProperties() {
        return new MongoProperties();
    }

    // Custom converters would not be available in this MongoTemplate
//    @Bean
//    MongoTemplate accountMongoTemplate(
//            @Qualifier("accountMongoProperties") final MongoProperties accountMongoProperties) {
//        return new MongoTemplate(
//                MongoClients.create(accountMongoProperties.determineUri()), accountMongoProperties.getDatabase());
//    }

    // Custom converters would be available in this MongoTemplate
//      @Bean
//      MongoTemplate accountMongoTemplate(
//          @Qualifier("accountMongoProperties") final MongoProperties accountMongoProperties,
//          final MappingMongoConverter converter,
//          final MongoClientSettings mongoClientSettings) {
//          CodecRegistry codecRegistry = mongoClientSettings.getCodecRegistry(); //.register(new OffsetDateTimeCodec());
//
//          MongoClientSettings mongoClientSettings() {
//
//              MongoClientSettings.Builder builder = MongoClientSettings.builder();
//              builder.uuidRepresentation(UuidRepresentation.JAVA_LEGACY);
////              configureClientSettings(builder);
////              return builder.build();
//
//              builder
//                      .applyConnectionString(new ConnectionString(accountMongoProperties.determineUri()))
////                      .uuidRepresentation(this.accountMongoProperties.getUuidRepresentation())
//                      .codecRegistry(customCodecRegistry)
//              ;
//        final MongoDatabaseFactory databaseFactory =
//            new SimpleMongoClientDatabaseFactory(
//                MongoClients.create(accountMongoProperties.determineUri()),
//                accountMongoProperties.getDatabase());
////          return MongoClients.create(mongoClientSettings);
//        return new MongoTemplate(databaseFactory, converter);
//      }

    @Bean
    @ConfigurationProperties("spring.data.mongodb.account")
    MongoProperties accountMongoProperties() {
        return new MongoProperties();
    }

    // Custom converters would not be available in this MongoTemplate
//    @Bean
//    MongoTemplate transactionMongoTemplate(
//            @Qualifier("transactionMongoProperties") final MongoProperties transactionMongoProperties) {
//        return new MongoTemplate(
//                MongoClients.create(transactionMongoProperties.determineUri()), transactionMongoProperties.getDatabase());
//    }

    // Custom converters would be available in this MongoTemplate
//        @Bean
//        MongoTemplate transactionMongoTemplate(
//            @Qualifier("transactionMongoProperties") final MongoProperties transactionMongoProperties,
//            final MappingMongoConverter converter) {
//          final MongoDatabaseFactory databaseFactory =
//              new SimpleMongoClientDatabaseFactory(
//                  MongoClients.create(transactionMongoProperties.determineUri()),
//                  transactionMongoProperties.getDatabase());
//          return new MongoTemplate(databaseFactory, converter);
//        }

    @Bean
    @ConfigurationProperties("spring.data.mongodb.transaction")
    MongoProperties transactionMongoProperties() {
        return new MongoProperties();
    }

//  @Bean
//  public MongoClientSettingsBuilderCustomizer mongoClientSettingsCustomizer() {
//    CodecRegistry customCodecRegistry = OffsetDateTimeCodecProvider.getCodecRegistryWithOffsetDateTime();
//    return builder -> {
//      builder.codecRegistry(customCodecRegistry);
//      // Apply other settings to the builder.
//    };
//  }
}
