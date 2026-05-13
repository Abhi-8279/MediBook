package com.medibook.schedule.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class CacheConfig {

    public static final String SCHEDULE_PUBLIC_PROVIDER_SLOTS_CACHE = "schedule-public-provider-slots";
    public static final String SCHEDULE_PUBLIC_SLOT_DETAIL_CACHE = "schedule-public-slot-detail";
    public static final String SCHEDULE_INTERNAL_AVAILABLE_SLOTS_CACHE = "schedule-internal-available-slots";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(2));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new LinkedHashMap<>();
        cacheConfigurations.put(SCHEDULE_PUBLIC_PROVIDER_SLOTS_CACHE, defaultConfiguration.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(SCHEDULE_PUBLIC_SLOT_DETAIL_CACHE, defaultConfiguration.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(SCHEDULE_INTERNAL_AVAILABLE_SLOTS_CACHE, defaultConfiguration.entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
                SCHEDULE_PUBLIC_PROVIDER_SLOTS_CACHE,
                SCHEDULE_PUBLIC_SLOT_DETAIL_CACHE,
                SCHEDULE_INTERNAL_AVAILABLE_SLOTS_CACHE);
    }
}
