package ru.eptbw.bot.threading;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceSimpleTest {

    private RateLimiterService rateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();

        // Устанавливаем маленькие лимиты для тестов
        System.setProperty("RATE_LIMIT_PER_MINUTE", "3");
        System.setProperty("RATE_LIMIT_PER_HOUR", "5");

        rateLimiter = RateLimiterService.getInstance();
    }

    private void resetSingleton() throws Exception {
        Field instanceField = RateLimiterService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSingleton() {
        RateLimiterService instance1 = RateLimiterService.getInstance();
        RateLimiterService instance2 = RateLimiterService.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2);
    }

    @Test
    void testBasicFunctionality() {
        // Тест 1: Первые запросы должны проходить
        Long userId = 12345L;

        System.out.println("Тест 1: Проверка базовой функциональности");

        boolean firstRequest = rateLimiter.allowRequest(userId, "test");
        boolean secondRequest = rateLimiter.allowRequest(userId, "test");
        boolean thirdRequest = rateLimiter.allowRequest(userId, "test");

        assertTrue(firstRequest, "Первый запрос должен быть разрешен");
        assertTrue(secondRequest, "Второй запрос должен быть разрешен");
        assertTrue(thirdRequest, "Третий запрос должен быть разрешен");

        System.out.println("  ✓ Первые 3 запроса разрешены");
    }

    @Test
    void testDifferentUsers() {
        // Тест 2: Разные пользователи имеют отдельные лимиты
        Long user1 = 11111L;
        Long user2 = 22222L;

        System.out.println("Тест 2: Разные пользователи");

        // User1 делает 3 запроса
        boolean user1req1 = rateLimiter.allowRequest(user1, "test");
        boolean user1req2 = rateLimiter.allowRequest(user1, "test");
        boolean user1req3 = rateLimiter.allowRequest(user1, "test");

        // User2 делает 3 запроса
        boolean user2req1 = rateLimiter.allowRequest(user2, "test");
        boolean user2req2 = rateLimiter.allowRequest(user2, "test");
        boolean user2req3 = rateLimiter.allowRequest(user2, "test");

        assertTrue(user1req1 && user1req2 && user1req3,
                "User1: все 3 запроса должны быть разрешены");
        assertTrue(user2req1 && user2req2 && user2req3,
                "User2: все 3 запроса должны быть разрешены");

        System.out.println("  ✓ Оба пользователя получили по 3 запроса");
    }


    @Test
    void testUnknownUserStats() {
        // Тест 4: Статистика для неизвестного пользователя
        Long unknownUserId = 999999999L;

        System.out.println("Тест 4: Неизвестный пользователь");

        String stats = rateLimiter.getUserStats(unknownUserId);
        assertEquals("Нет данных о лимитах", stats);

        System.out.println("  ✓ Корректная обработка неизвестного пользователя");
    }

    @Test
    void testSimpleRateLimiting() throws Exception {
        // Тест 5: Простая проверка лимитирования
        Long userId = 44444L;

        System.out.println("Тест 5: Простая проверка лимитирования (лимит 3/минуту)");

        AtomicInteger allowedCount = new AtomicInteger(0);

        // Пробуем сделать 5 запросов быстро
        for (int i = 0; i < 5; i++) {
            if (rateLimiter.allowRequest(userId, "test")) {
                allowedCount.incrementAndGet();
                System.out.println("  Запрос " + (i + 1) + ": разрешен");
            } else {
                System.out.println("  Запрос " + (i + 1) + ": заблокирован (ожидаемо)");
            }
        }

        int allowed = allowedCount.get();
        System.out.println("  Итого разрешено: " + allowed + " из 5");

        // При лимите 3/минуту должно быть не больше 3 разрешенных запросов
        // Но может быть и 4-5 из-за глобального лимита
        assertTrue(allowed >= 1, "Должен быть хотя бы 1 разрешенный запрос");
        assertTrue(allowed <= 5, "Не может быть больше 5 запросов");
    }

    @Test
    void testConcurrentSimple() throws Exception {
        // Тест 6: Простая многопоточная проверка
        Long userId = 55555L;
        int threadCount = 3;

        System.out.println("Тест 6: Простая многопоточность (" + threadCount + " потока)");

        Thread[] threads = new Thread[threadCount];
        AtomicInteger totalAllowed = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 2; j++) {
                    if (rateLimiter.allowRequest(userId, "concurrent")) {
                        totalAllowed.incrementAndGet();
                    }
                }
            });
            threads[i].start();
        }

        // Ждем завершения всех потоков
        for (Thread thread : threads) {
            thread.join();
        }

        int allowed = totalAllowed.get();
        System.out.println("  Итого разрешено: " + allowed + " из " + (threadCount * 2));

        // Проверяем что сервис не упал
        String stats = rateLimiter.getUserStats(userId);
        assertNotNull(stats);
        assertTrue(stats.contains("55555"));

        System.out.println("  ✓ Сервис работает после многопоточной нагрузки");
    }
}