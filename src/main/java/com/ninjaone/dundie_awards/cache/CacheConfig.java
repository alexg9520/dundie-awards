package com.ninjaone.dundie_awards.cache;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.ninjaone.dundie_awards.model.OrganizationInfo;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ORGANIZATION_CACHE = "organization";
    public static final String TOTAL_AWARDS_CACHE = "totalAwards";
    public static final String TOTAL_AWARDS_BY_ORG_CACHE = "totalAwardsByOrganization";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper cacheObjectMapper) {
        // Serializer for OrganizationInfo objects
        JacksonJsonRedisSerializer<OrganizationInfo> orgSerializer = new JacksonJsonRedisSerializer<>(cacheObjectMapper, OrganizationInfo.class);
        
        // Serializer for Long values (used by totalAwards and totalAwardsByOrganization caches)
        JacksonJsonRedisSerializer<Long> longSerializer = new JacksonJsonRedisSerializer<>(cacheObjectMapper, Long.class);
        
        // Configuration for organization cache (stores OrganizationInfo)
        RedisCacheConfiguration orgCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("org:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(orgSerializer));
        
        // Configuration for numeric caches (stores Long values)
        RedisCacheConfiguration numericCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("award:")
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(longSerializer));
        
        // Map cache names to their specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(ORGANIZATION_CACHE, orgCacheConfig);
        cacheConfigurations.put(TOTAL_AWARDS_CACHE, numericCacheConfig);
        cacheConfigurations.put(TOTAL_AWARDS_BY_ORG_CACHE, numericCacheConfig);
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(numericCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(TOTAL_AWARDS_CACHE, TOTAL_AWARDS_BY_ORG_CACHE, ORGANIZATION_CACHE);
    }

}
