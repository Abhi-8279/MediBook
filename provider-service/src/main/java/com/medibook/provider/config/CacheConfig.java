package com.medibook.provider.config;

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

    public static final String PROVIDER_PUBLIC_SEARCH_CACHE = "provider-public-search";
    public static final String PROVIDER_PUBLIC_DETAIL_CACHE = "provider-public-detail";
    public static final String PROVIDER_SPECIALIZATION_COUNT_CACHE = "provider-specialization-counts";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new LinkedHashMap<>();
        cacheConfigurations.put(PROVIDER_PUBLIC_SEARCH_CACHE, defaultConfiguration.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PROVIDER_PUBLIC_DETAIL_CACHE, defaultConfiguration.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(PROVIDER_SPECIALIZATION_COUNT_CACHE, defaultConfiguration.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(
                PROVIDER_PUBLIC_SEARCH_CACHE,
                PROVIDER_PUBLIC_DETAIL_CACHE,
                PROVIDER_SPECIALIZATION_COUNT_CACHE);
    }
}
