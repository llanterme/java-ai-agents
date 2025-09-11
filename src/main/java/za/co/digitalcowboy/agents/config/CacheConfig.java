package za.co.digitalcowboy.agents.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Value("${serpapi.cache-ttl-seconds:3600}")
    private long serpApiCacheTtlSeconds;
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // OAuth state cache - expires after 10 minutes
        CaffeineCache oauthStateCache = new CaffeineCache("oauthState",
                Caffeine.newBuilder()
                        .maximumSize(10000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .build());
        
        // SERP API search results cache - expires after configured time (default 1 hour)
        CaffeineCache serpApiCache = new CaffeineCache("serpApiCache",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(Duration.ofSeconds(serpApiCacheTtlSeconds))
                        .recordStats()
                        .build());
        
        // Web search cache (alias for backwards compatibility)
        CaffeineCache webSearchCache = new CaffeineCache("webSearchCache",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(Duration.ofSeconds(serpApiCacheTtlSeconds))
                        .recordStats()
                        .build());
        
        cacheManager.setCaches(Arrays.asList(oauthStateCache, serpApiCache, webSearchCache));
        return cacheManager;
    }
}