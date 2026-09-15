package com.jzo2o.aigc.guard;

import com.jzo2o.aigc.exception.AigcErrorCode;
import com.jzo2o.aigc.exception.AigcException;
import com.jzo2o.aigc.properties.AigcProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RMapCache;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AigcRequestGuardTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RRateLimiter rateLimiter = mock(RRateLimiter.class);
    @SuppressWarnings("unchecked")
    private final RMapCache<String, String> activeGenerations = mock(RMapCache.class);

    @Test
    void shouldRejectWhenRateLimiterHasNoPermit() {
        when(redissonClient.getRateLimiter("AIGC:RATE:7")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire()).thenReturn(false);

        assertThatThrownBy(() -> guard().acquire(7L, "s1"))
                .isInstanceOfSatisfying(AigcException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(AigcErrorCode.RATE_LIMITED));
    }

    @Test
    void shouldRejectConcurrentGeneration() {
        when(redissonClient.getRateLimiter("AIGC:RATE:7")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(redissonClient.<String, String>getMapCache("AIGC:GENERATION")).thenReturn(activeGenerations);
        when(activeGenerations.putIfAbsent(eq("s1"), anyString(), eq(100L), eq(TimeUnit.SECONDS)))
                .thenReturn("existing-token");

        assertThatThrownBy(() -> guard().acquire(7L, "s1"))
                .isInstanceOfSatisfying(AigcException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(AigcErrorCode.GENERATION_CONFLICT));
    }

    @Test
    void shouldConfigureConfiguredRateAndCreateLeaseWithExactTtl() {
        when(redissonClient.getRateLimiter("AIGC:RATE:7")).thenReturn(rateLimiter);
        when(rateLimiter.tryAcquire()).thenReturn(true);
        when(redissonClient.<String, String>getMapCache("AIGC:GENERATION")).thenReturn(activeGenerations);

        guard().acquire(7L, "s1");

        verify(rateLimiter).trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.MINUTES);
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(activeGenerations).putIfAbsent(eq("s1"), token.capture(), eq(100L), eq(TimeUnit.SECONDS));
        assertThat(token.getValue()).isNotBlank();
    }

    @Test
    void shouldReleaseOnlyItsTokenOnceWhenLeaseClosesRepeatedly() {
        GenerationLease lease = new GenerationLease(activeGenerations, "s1", "request-token");

        lease.close();
        lease.close();

        verify(activeGenerations, times(1)).remove("s1", "request-token");
        verify(activeGenerations, never()).remove("s1");
    }

    @Test
    void shouldRetryLeaseReleaseAfterRemovalFailure() {
        GenerationLease lease = new GenerationLease(activeGenerations, "s1", "request-token");
        when(activeGenerations.remove("s1", "request-token"))
                .thenThrow(new RuntimeException("Redis unavailable"))
                .thenReturn(true);

        assertThatThrownBy(lease::close)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis unavailable");

        lease.close();
        lease.close();

        verify(activeGenerations, times(2)).remove("s1", "request-token");
        verify(activeGenerations, never()).remove("s1");
    }

    private AigcRequestGuard guard() {
        return new AigcRequestGuard(redissonClient, new AigcProperties());
    }
}
