package com.ksoot.batch.config;

import com.mongodb.MongoClientSettings;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import java.time.OffsetDateTime;

public class OffsetDateTimeCodecProvider implements CodecProvider {

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == OffsetDateTime.class) {
            return (Codec<T>) new OffsetDateTimeCodec();
        }
        return null;
    }

    public static CodecRegistry getCodecRegistryWithOffsetDateTime() {
        return CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(new OffsetDateTimeCodec()),
                MongoClientSettings.getDefaultCodecRegistry()
        );
    }
}
