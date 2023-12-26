package com.ksoot.batch.config;

import com.ksoot.batch.utils.DateTimeUtils;
import com.mongodb.ConnectionString;
import com.mongodb.DBObjectCodecProvider;
import com.mongodb.DBRefCodecProvider;
import com.mongodb.DocumentToDBRefTransformer;
import com.mongodb.Jep395RecordCodecProvider;
//import com.mongodb.KotlinCodecProvider;
import com.mongodb.MongoClientSettings;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.codecs.GridFSFileCodecProvider;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
//import com.mongodb.client.model.mql.ExpressionCodecProvider;
import lombok.RequiredArgsConstructor;
import org.bson.UuidRepresentation;
import org.bson.codecs.BsonCodecProvider;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.CollectionCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.EnumCodecProvider;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.JsonObjectCodecProvider;
import org.bson.codecs.MapCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.SpringDataMongoDB;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@EnableConfigurationProperties(MongoProperties.class)
@Configuration
//@RequiredArgsConstructor
class MongoDBConfig extends AbstractMongoClientConfiguration {

  private static final CodecRegistry DEFAULT_CODEC_REGISTRY =
          fromProviders(asList(new ValueCodecProvider(),
                  new BsonValueCodecProvider(),
                  new DBRefCodecProvider(),
                  new DBObjectCodecProvider(),
                  new DocumentCodecProvider(new DocumentToDBRefTransformer()),
                  new CollectionCodecProvider(new DocumentToDBRefTransformer()),
                  new IterableCodecProvider(new DocumentToDBRefTransformer()),
                  new MapCodecProvider(new DocumentToDBRefTransformer()),
                  new GeoJsonCodecProvider(),
                  new GridFSFileCodecProvider(),
                  new Jsr310CodecProvider(),
                  new JsonObjectCodecProvider(),
                  new BsonCodecProvider(),
                  new EnumCodecProvider(),
//                  new ExpressionCodecProvider(),
                  new Jep395RecordCodecProvider()
//                  ,new KotlinCodecProvider()
          ));

  private final MongoProperties mongoProperties;

  private final CodecRegistry customCodecRegistry = CodecRegistries.fromRegistries(
          CodecRegistries.fromProviders(new OffsetDateTimeCodecProvider()),
          DEFAULT_CODEC_REGISTRY);

  MongoDBConfig(final MongoProperties mongoProperties) {
    this.mongoProperties = mongoProperties;
//    this.customCodecRegistry = CodecRegistries.fromRegistries(
//            CodecRegistries.fromProviders(new OffsetDateTimeCodecProvider()),
//            DEFAULT_CODEC_REGISTRY);
  }

  @Override
  protected void configureClientSettings(final MongoClientSettings.Builder builder) {
//    CodecRegistry customCodecRegistry = CodecRegistries.fromRegistries(
//        CodecRegistries.fromProviders(new OffsetDateTimeCodecProvider()),
//        DEFAULT_CODEC_REGISTRY);
    builder
        .applyConnectionString(new ConnectionString(this.mongoProperties.determineUri()))
        .uuidRepresentation(this.mongoProperties.getUuidRepresentation())
        .codecRegistry(this.customCodecRegistry)
    ;
  }

  @Override
  protected boolean autoIndexCreation() {
    return this.mongoProperties.isAutoIndexCreation();
  }

  @Override
  protected String getDatabaseName() {
    return this.mongoProperties.getDatabase();
  }

  @Bean
  MongoTemplate accountMongoTemplate(
          @Qualifier("accountMongoProperties") final MongoProperties accountMongoProperties,
          final MappingMongoConverter converter) {

      final MongoClientSettings settings = MongoClientSettings.builder()
              .applyConnectionString(new ConnectionString(accountMongoProperties.determineUri()))
              .uuidRepresentation(this.mongoProperties.getUuidRepresentation())
              .codecRegistry(this.customCodecRegistry).build();

      final MongoDatabaseFactory databaseFactory =
              new SimpleMongoClientDatabaseFactory(
                      MongoClients.create(settings, SpringDataMongoDB.driverInformation()),
                      accountMongoProperties.getDatabase());

      return new MongoTemplate(databaseFactory, converter);
    }

  @Bean
  MongoTemplate transactionMongoTemplate(
          @Qualifier("transactionMongoProperties") final MongoProperties transactionMongoProperties,
          final MappingMongoConverter converter) {

    final MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(transactionMongoProperties.determineUri()))
            .uuidRepresentation(this.mongoProperties.getUuidRepresentation())
            .codecRegistry(this.customCodecRegistry).build();

    final MongoDatabaseFactory databaseFactory =
            new SimpleMongoClientDatabaseFactory(
                    MongoClients.create(settings, SpringDataMongoDB.driverInformation()),
                    transactionMongoProperties.getDatabase());

    return new MongoTemplate(databaseFactory, converter);
  }

  @Bean
  ValidatingMongoEventListener validatingMongoEventListener(
      final LocalValidatorFactoryBean factory) {
    return new ValidatingMongoEventListener(factory);
  }

  @Bean
  LocalValidatorFactoryBean validatorFactory() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    return validator;
  }

  @Bean
  MongoTransactionManager transactionManager(final MongoDatabaseFactory mongoDatabaseFactory) {
    return new MongoTransactionManager(mongoDatabaseFactory);
  }

  @Override
  public MongoCustomConversions customConversions() {
    List<Object> converters = new ArrayList<>();
    converters.add(new ZonedDateTimeReadConverter());
    converters.add(new ZonedDateTimeWriteConverter());
    converters.add(new OffsetDateTimeReadConverter());
    converters.add(new OffsetDateTimeWriteConverter());
    return new MongoCustomConversions(converters);
  }

  @ReadingConverter
  class ZonedDateTimeReadConverter implements Converter<Date, ZonedDateTime> {

    @Override
    public ZonedDateTime convert(final Date source) {
      return source.toInstant().atZone(DateTimeUtils.SYSTEM_ZONE_ID);
    }
  }

  @WritingConverter
  class ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {

    @Override
    public Date convert(final ZonedDateTime source) {
      return Date.from(source.toInstant());
    }
  }

  @ReadingConverter
  class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(final Date source) {
      return source.toInstant().atZone(DateTimeUtils.SYSTEM_ZONE_ID).toOffsetDateTime();
    }
  }

  @WritingConverter
  class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {

    @Override
    public Date convert(final OffsetDateTime source) {
      return Date.from(source.toInstant());
    }
  }
}
