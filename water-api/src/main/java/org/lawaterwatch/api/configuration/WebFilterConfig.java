package org.lawaterwatch.api.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

//Plain servlet filters, no Spring Security (no login to protect). Order matters:
//1. CORS: answers browser preflights itself and adds CORS headers to every response, the 429s included,
//   so the frontend sees a real 429 instead of a misleading CORS error
//2. Rate limit: preflights never reach it, so they don't spend tokens
@Configuration
public class WebFilterConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST"));
        cors.setAllowedHeaders(List.of("Content-Type", "If-None-Match"));
        //Lets the frontend read the map's ETag
        cors.setExposedHeaders(List.of("ETag"));
        //Browsers may cache the preflight answer for an hour
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            @Value("${app.rate-limit.address-per-minute}") long addressPerMinute,
            @Value("${app.rate-limit.default-per-minute}") long defaultPerMinute,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(
                new RateLimitFilter(addressPerMinute, defaultPerMinute, exceptionResolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
