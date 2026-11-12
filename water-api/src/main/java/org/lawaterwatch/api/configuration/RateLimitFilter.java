package org.lawaterwatch.api.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;

//Token bucket per client IP: each IP gets N tokens a minute, every request spends one, an empty bucket is a 429
//Two separate buckets per IP, so using up the strict /api/address limit doesn't block the map or the leaderboard
public class RateLimitFilter extends OncePerRequestFilter {

    static final String ADDRESS_PATH = "/api/address";

    private final long addressPerMinute;
    private final long defaultPerMinute;
    //Filters run before Spring MVC, so @RestControllerAdvice can't see exceptions thrown here;
    //handing the 429 to the resolver sends it through GlobalExceptionHandler like every other error
    private final HandlerExceptionResolver exceptionResolver;

    //Bounded and expiring: an idle IP's bucket is dropped after 10 minutes (it would be full again anyway),
    //and the size cap stops someone cycling through IPs from growing memory without limit
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    public RateLimitFilter(long addressPerMinute, long defaultPerMinute, HandlerExceptionResolver exceptionResolver) {
        this.addressPerMinute = addressPerMinute;
        this.defaultPerMinute = defaultPerMinute;
        this.exceptionResolver = exceptionResolver;
    }

    //Only the API counts; Swagger UI and the docs are free. CORS preflights are answered earlier by the CorsFilter
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean address = request.getRequestURI().equals(ADDRESS_PATH);
        //The TCP peer address. Behind a proxy (nginx) this is the proxy, and X-Forwarded-For should only be
        //trusted from that known proxy, never taken as-is, since any client can fake the header
        String key = (address ? "address:" : "api:") + request.getRemoteAddr();
        Bucket bucket = buckets.get(key, k -> newBucket(address ? addressPerMinute : defaultPerMinute));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        //The reason only reaches the server log, the 429 body stays generic
        exceptionResolver.resolveException(request, response, null,
                new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "rate limit hit for " + key));
    }

    //Full bucket to start (allows a short burst), refilled evenly across the minute (5/min = one token every 12 s)
    private static Bucket newBucket(long perMinute) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)))
                .build();
    }
}
