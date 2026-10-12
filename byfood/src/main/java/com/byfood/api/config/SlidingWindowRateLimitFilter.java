package com.byfood.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita requisições por IP em uma janela deslizante para endpoints escolhidos por {@link #matches}.
 * O IP vem de {@code request.getRemoteAddr()}, que o Tomcat resolve a partir de X-Forwarded-For
 * apenas quando a requisição chega por um proxy interno confiável (server.forward-headers-strategy=native).
 */
public abstract class SlidingWindowRateLimitFilter extends OncePerRequestFilter {

    private static final int CLEANUP_EVERY = 500;

    private final int maxRequests;
    private final long windowMs;
    private final String message;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicInteger requestsSinceCleanup = new AtomicInteger();

    protected SlidingWindowRateLimitFilter(int maxRequests, long windowMs, String message) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
        this.message = message;
    }

    /** Atalho para o caso comum: método HTTP + caminho exato. */
    protected SlidingWindowRateLimitFilter(String method, String path, int maxRequests, long windowMs, String message) {
        this(maxRequests, windowMs, message);
        this.exact = request -> path.equals(request.getRequestURI()) && method.equalsIgnoreCase(request.getMethod());
    }

    private java.util.function.Predicate<HttpServletRequest> exact;

    /** Define quais requisições entram no limite. Subclasses podem sobrescrever para casar padrões. */
    protected boolean matches(HttpServletRequest request) {
        return exact != null && exact.test(request);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (requestsSinceCleanup.incrementAndGet() >= CLEANUP_EVERY) {
            requestsSinceCleanup.set(0);
            evictStale(now);
        }

        Deque<Long> times = hits.computeIfAbsent(request.getRemoteAddr(), k -> new ArrayDeque<>());
        synchronized (times) {
            dropExpired(times, now);
            if (times.size() >= maxRequests) {
                long retryAfterSeconds = Math.max(1, (windowMs - (now - times.peekFirst())) / 1000);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"" + message + "\"}");
                return;
            }
            times.addLast(now);
        }
        filterChain.doFilter(request, response);
    }

    private void dropExpired(Deque<Long> times, long now) {
        while (!times.isEmpty() && now - times.peekFirst() > windowMs) {
            times.removeFirst();
        }
    }

    /** Remove IPs sem requisições recentes para o mapa não crescer indefinidamente. */
    void evictStale(long now) {
        hits.entrySet().removeIf(entry -> {
            Deque<Long> times = entry.getValue();
            synchronized (times) {
                dropExpired(times, now);
                return times.isEmpty();
            }
        });
    }

    int trackedClients() {
        return hits.size();
    }
}
