package ru.eptbw.bot.threading;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolManagerTest {

    @TempDir
    Path tempDir;

    private ThreadPoolManager threadPoolManager;
    private Path testStatsFile;

    @BeforeEach
    void setUp() throws Exception {
        // Создаем тестовый файл статистики
        testStatsFile = tempDir.resolve("test_stats.log");

        // Устанавливаем системное свойство для конфигурации
        System.setProperty("STATS_LOG_FILE", testStatsFile.toString());
        System.setProperty("THREAD_POOL_CORE_SIZE", "2");
        System.setProperty("THREAD_POOL_MAX_SIZE", "5");
        System.setProperty("ENABLE_STATS", "true");

        // Сбрасываем синглтон
        resetSingleton();

        // Создаем экземпляр
        threadPoolManager = ThreadPoolManager.getInstance();

        // Даем время на инициализацию
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }
    }

    private void resetSingleton() throws Exception {
        java.lang.reflect.Field instanceField = ThreadPoolManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSingletonPattern() {
        // Arrange & Act
        ThreadPoolManager instance1 = ThreadPoolManager.getInstance();
        ThreadPoolManager instance2 = ThreadPoolManager.getInstance();

        // Assert
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2);
    }

    @Test
    void testSubmitTaskSuccess() throws Exception {
        // Arrange
        AtomicInteger executionCounter = new AtomicInteger(0);
        String expectedResult = "Тестовый результат";

        // Act
        CompletableFuture<String> future = threadPoolManager.submitTask(() -> {
            executionCounter.incrementAndGet();
            return expectedResult;
        });

        String actualResult = future.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals(expectedResult, actualResult);
        assertEquals(1, executionCounter.get());
    }

    @Test
    void testSubmitTaskException() throws Exception {
        // Arrange
        String errorMessage = "Искусственная ошибка";

        // Act
        CompletableFuture<String> future = threadPoolManager.submitTask(() -> {
            throw new RuntimeException(errorMessage);
        });

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> future.get(5, TimeUnit.SECONDS));

        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals(errorMessage, exception.getCause().getMessage());
    }

    @Test
    void testMultipleTasksExecution() throws Exception {
        // Arrange
        int taskCount = 10;
        CountDownLatch allTasksStarted = new CountDownLatch(taskCount);
        CountDownLatch allTasksCompleted = new CountDownLatch(taskCount);
        AtomicInteger completedCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            threadPoolManager.submitTask(() -> {
                allTasksStarted.countDown();
                try {
                    // Ждем немного чтобы убедиться что задачи выполняются параллельно
                    Thread.sleep(50);
                    completedCount.incrementAndGet();
                    return "Задача " + taskId;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } finally {
                    allTasksCompleted.countDown();
                }
            });
        }

        // Assert
        assertTrue(allTasksStarted.await(2, TimeUnit.SECONDS),
                "Все задачи должны начать выполняться");
        assertTrue(allTasksCompleted.await(5, TimeUnit.SECONDS),
                "Все задачи должны завершиться");
        assertEquals(taskCount, completedCount.get());
    }

    @Test
    void testScheduleTask() throws Exception {
        // Arrange
        AtomicInteger executionCounter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        // Act
        threadPoolManager.scheduleTask(() -> {
            executionCounter.incrementAndGet();
            latch.countDown();
        }, 100, TimeUnit.MILLISECONDS);

        // Assert
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(1, executionCounter.get());
    }


    @Test
    void testGetDetailedStats() {
        // Act
        String stats = threadPoolManager.getDetailedStats();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.contains("Статистика потоков:"));
        assertTrue(stats.contains("Размер пула="));
        assertTrue(stats.contains("Активные="));
        assertTrue(stats.contains("В очереди="));
        assertTrue(stats.contains("Выполнено="));
    }

    @Test
    void testThreadPoolShutdown() throws Exception {
        // Arrange
        AtomicInteger taskCount = new AtomicInteger(0);

        // Добавляем несколько задач
        for (int i = 0; i < 3; i++) {
            threadPoolManager.submitTask(() -> {
                Thread.sleep(200);
                taskCount.incrementAndGet();
                return null;
            });
        }

        // Act
        threadPoolManager.shutdown();

        // Assert
        Thread.sleep(500); // Даем время на завершение
        assertTrue(threadPoolManager.getExecutor().isShutdown());
        assertTrue(taskCount.get() > 0);
    }

    @Test
    void testThreadPoolRejectionPolicy() throws Exception {
        // Arrange
        System.setProperty("THREAD_POOL_MAX_SIZE", "2");
        System.setProperty("THREAD_POOL_QUEUE_SIZE", "0");

        resetSingleton();
        ThreadPoolManager smallPool = ThreadPoolManager.getInstance();

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        // Act - пытаемся отправить больше задач чем может обработать пул
        for (int i = 0; i < 10; i++) {
            try {
                smallPool.submitTask(() -> {
                    try {
                        Thread.sleep(500); // Долгая задача
                        successCount.incrementAndGet();
                        return null;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                // Ожидаем что некоторые задачи будут отклонены
                latch.countDown();
            }
        }

        // Assert
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(successCount.get() > 0);

        smallPool.shutdown();
    }
}