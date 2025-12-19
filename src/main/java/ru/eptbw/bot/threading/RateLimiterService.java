package ru.eptbw.bot.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eptbw.config.Config;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RateLimiterService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);
    private static RateLimiterService instance;

    private final Map<Long, UserRateLimit> userLimits = new ConcurrentHashMap<>();
    private final Semaphore globalLimiter;
    private final int perMinuteLimit;
    private final int perHourLimit;

    private RateLimiterService() {
        this.perMinuteLimit = Config.getRateLimitPerMinute();
        this.perHourLimit = Config.getRateLimitPerHour();
        this.globalLimiter = new Semaphore(perMinuteLimit);

        ThreadPoolManager.getInstance().scheduleAtFixedRate(
                this::cleanupExpiredUsers, 1, 1, TimeUnit.HOURS
        );

        logger.info("RateLimiterService инициализирован: {} в минуту, {} в час",
                perMinuteLimit, perHourLimit);
    }

    public static synchronized RateLimiterService getInstance() {
        if (instance == null) {
            instance = new RateLimiterService();
        }
        return instance;
    }

    public boolean allowRequest(Long userId, String endpoint) {
        try {
            // Глобальный лимит
            if (!globalLimiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                logger.warn("Глобальный лимит превышен для пользователя {}", userId);
                return false;
            }

            // Лимит по пользователю
            UserRateLimit userLimit = userLimits.computeIfAbsent(
                    userId, k -> new UserRateLimit(perMinuteLimit, perHourLimit)
            );

            boolean allowed = userLimit.tryAcquire();
            if (!allowed) {
                logger.warn("Лимит пользователя {} превышен: {}/min, {}/hour",
                        userId, userLimit.getMinuteCount(), userLimit.getHourCount());
            }

            return allowed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            // Возвращаем глобальный семафор через секунду
            ThreadPoolManager.getInstance().scheduleTask(() ->
                    globalLimiter.release(), 60, TimeUnit.SECONDS);
        }
    }

    public void recordSuccess(Long userId) {
        UserRateLimit userLimit = userLimits.get(userId);
        if (userLimit != null) {
            userLimit.recordSuccess();
        }
    }

    public void recordFailure(Long userId) {
        UserRateLimit userLimit = userLimits.get(userId);
        if (userLimit != null) {
            userLimit.recordFailure();
        }
    }

    public String getUserStats(Long userId) {
        UserRateLimit userLimit = userLimits.get(userId);
        if (userLimit == null) {
            return "Нет данных о лимитах";
        }

        return String.format("Лимиты пользователя %d: %d/%d в минуту, %d/%d в час, успешно: %d, ошибок: %d",
                userId,
                userLimit.getMinuteCount(), perMinuteLimit,
                userLimit.getHourCount(), perHourLimit,
                userLimit.getSuccessCount(),
                userLimit.getFailureCount());
    }

    private void cleanupExpiredUsers() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        userLimits.entrySet().removeIf(entry ->
                entry.getValue().getLastAccessTime() < cutoffTime);
        logger.info("Очистка устаревших пользователей. Осталось: {}", userLimits.size());
    }

    // Внутренний класс для хранения лимитов пользователя
    private static class UserRateLimit {
        private final Semaphore minuteLimiter;
        private final Semaphore hourLimiter;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile long lastAccessTime = System.currentTimeMillis();

        UserRateLimit(int perMinute, int perHour) {
            this.minuteLimiter = new Semaphore(perMinute);
            this.hourLimiter = new Semaphore(perHour);
        }

        boolean tryAcquire() {
            lastAccessTime = System.currentTimeMillis();
            return minuteLimiter.tryAcquire() && hourLimiter.tryAcquire();
        }

        void recordSuccess() {
            successCount.incrementAndGet();
            // Возвращаем семафоры через время
            ThreadPoolManager.getInstance().scheduleTask(() -> {
                minuteLimiter.release();
            }, 1, TimeUnit.MINUTES);

            ThreadPoolManager.getInstance().scheduleTask(() -> {
                hourLimiter.release();
            }, 1, TimeUnit.HOURS);
        }

        void recordFailure() {
            failureCount.incrementAndGet();
        }

        int getMinuteCount() {
            return minuteLimiter.availablePermits();
        }

        int getHourCount() {
            return hourLimiter.availablePermits();
        }

        int getSuccessCount() {
            return successCount.get();
        }

        int getFailureCount() {
            return failureCount.get();
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}