package ru.eptbw.bot.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    private CacheService cacheService;

    @BeforeEach
    void setUp() throws Exception {
        // Сбрасываем синглтон
        resetSingleton();

        // Устанавливаем системные свойства
        System.setProperty("CACHE_MAX_SIZE", "100");
        System.setProperty("CACHE_EXPIRE_MINUTES", "1");

        // Получаем экземпляр
        cacheService = CacheService.getInstance();
        cacheService.clear(); // Очищаем кеш
    }

    private void resetSingleton() throws Exception {
        Field instanceField = CacheService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSingleton() {
        // Arrange & Act
        CacheService instance1 = CacheService.getInstance();
        CacheService instance2 = CacheService.getInstance();

        // Assert
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2);
    }

    @Test
    void testBasicPutAndGet() {
        // Arrange
        String key = "testKey";
        String value = "testValue";

        // Act
        cacheService.put(key, value);
        String retrieved = cacheService.get(key);

        // Assert
        assertEquals(value, retrieved);
    }

    @Test
    void testCacheContains() {
        // Arrange
        String key = "containsKey";
        String value = "containsValue";

        // Act
        cacheService.put(key, value);
        boolean contains = cacheService.contains(key);

        // Assert
        assertTrue(contains);
    }

    @Test
    void testCacheDoesNotContain() {
        // Arrange
        String nonExistentKey = "nonExistentKey";

        // Act
        boolean contains = cacheService.contains(nonExistentKey);

        // Assert
        assertFalse(contains);
    }

    @Test
    void testCacheRemove() {
        // Arrange
        String key = "toRemove";
        String value = "someValue";
        cacheService.put(key, value);

        // Act
        cacheService.remove(key);

        // Assert
        assertNull(cacheService.get(key));
        assertFalse(cacheService.contains(key));
    }

    @Test
    void testCacheClear() {
        // Arrange
        cacheService.put("key1", "value1");
        cacheService.put("key2", "value2");
        cacheService.put("key3", "value3");

        // Act
        cacheService.clear();

        // Assert
        assertEquals(0, cacheService.size());
        assertNull(cacheService.get("key1"));
        assertNull(cacheService.get("key2"));
        assertNull(cacheService.get("key3"));
    }

    @Test
    void testCacheSize() {
        // Arrange
        cacheService.clear();

        // Act
        cacheService.put("key1", "value1");
        cacheService.put("key2", "value2");
        cacheService.put("key3", "value3");

        // Assert
        assertEquals(3, cacheService.size());
    }

    @Test
    void testCacheExpiration() throws Exception {
        // Arrange
        String key = "expiringKey";
        String value = "expiringValue";

        // Act - кешируем с очень маленьким временем жизни
        cacheService.put(key, (Object) value, (long) 0.001); // ~0.06 секунды

        // Assert - сразу должно быть доступно
        assertNotNull(cacheService.get(key));
        assertEquals(value, cacheService.get(key));

        // Ждем истечения срока
        Thread.sleep(100);

        // Должно быть null после истечения
        assertNull(cacheService.get(key));
    }

    @Test
    void testCacheMaxSizeEviction() {
        // Arrange
        System.setProperty("CACHE_MAX_SIZE", "3");

        try {
            resetSingleton();
            CacheService smallCache = CacheService.getInstance();

            // Act - добавляем больше элементов чем размер кеша
            smallCache.put("key1", "value1");
            smallCache.put("key2", "value2");
            smallCache.put("key3", "value3");
            smallCache.put("key4", "value4"); // Должен вытеснить самый старый

            // Assert
            assertTrue(smallCache.size() <= 3);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.setProperty("CACHE_MAX_SIZE", "100");
        }
    }

    @Test
    void testConcurrentCacheAccess() throws Exception {
        // Arrange
        int threadCount = 10;
        int operationsPerThread = 50;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount * operationsPerThread);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        String key = "key-" + threadId + "-" + j;
                        String value = "value-" + threadId + "-" + j;

                        cacheService.put(key, value);
                        String retrieved = cacheService.get(key);

                        if (value.equals(retrieved)) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Игнорируем исключения для теста
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // Assert
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(successCount.get() > 0, "Должны быть успешные операции");
        assertTrue(cacheService.size() > 0, "Кеш не должен быть пустым");
    }

    @Test
    void testGetStats() {
        // Arrange
        cacheService.put("statKey1", "statValue1");
        cacheService.put("statKey2", "statValue2");

        // Act
        String stats = cacheService.getStats();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.contains("Кеш:"));
        assertTrue(stats.contains("размер="));
    }

    @Test
    void testTranslationCacheKeyGeneration() {
        // Arrange
        Long userId = 12345L;
        String text = "Hello world";
        String targetLang = "russian";

        // Act
        String cacheKey = cacheService.getTranslationCacheKey(userId, text, targetLang);

        // Assert
        assertNotNull(cacheKey);
        assertTrue(cacheKey.startsWith("translation:"));
        assertTrue(cacheKey.contains("12345"));
        assertTrue(cacheKey.contains("russian"));
    }

    @Test
    void testCommandCacheKeyGeneration() {
        // Arrange
        Long userId = 67890L;
        String command = "/translate";
        String args = "en Привет мир";

        // Act
        String cacheKey = cacheService.getCommandCacheKey(userId, command, args);

        // Assert
        assertNotNull(cacheKey);
        assertTrue(cacheKey.startsWith("command:"));
        assertTrue(cacheKey.contains("67890"));
        assertTrue(cacheKey.contains("/translate"));
    }

    @Test
    void testDifferentTypesInCache() {
        // Arrange
        String stringKey = "stringKey";
        String stringValue = "stringValue";

        String integerKey = "integerKey";
        Integer integerValue = 42;

        String booleanKey = "booleanKey";
        Boolean booleanValue = true;

        // Act
        cacheService.put(stringKey, stringValue);
        cacheService.put(integerKey, integerValue);
        cacheService.put(booleanKey, booleanValue);

        // Assert
        assertEquals(stringValue, cacheService.get(stringKey));
        assertEquals(integerValue, cacheService.get(integerKey));
        assertEquals(booleanValue, cacheService.get(booleanKey));
    }
}