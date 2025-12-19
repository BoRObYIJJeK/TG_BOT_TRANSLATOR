package ru.eptbw.bot.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eptbw.config.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


//1. Задача поступает в submitTask()
//2. Если есть свободные потоки → выполняется сразу
//3. Если нет свободных потоков → попадает в очередь
//4. Если очередь заполнена (1000 задач) → создается новый поток (до 50)
//5. Если достигнут максимум потоков (50) → выполняется в вызывающем потоке
//6. После выполнения → поток ждет 60 секунд новых задач
//7. Если за 60 секунд нет задач → поток завершается (но не меньше 10)
public class ThreadPoolManager {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);
    private static ThreadPoolManager instance;

    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private String statsFilePath;

    private ThreadPoolManager() {
        int corePoolSize = Config.getThreadPoolCoreSize();
        int maxPoolSize = Config.getThreadPoolMaxSize();
        long keepAliveTime = Config.getThreadKeepAliveTime();

        // Используем SynchronousQueue
        BlockingQueue<Runnable> workQueue = new SynchronousQueue<>();

        executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                new BotThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        scheduledExecutor = Executors.newScheduledThreadPool(2);

        // Получаем путь к файлу статистики
        statsFilePath = getStatsFilePath();
        logger.info("Файл статистики: {}", statsFilePath);

        // Предварительное создание всех ядерных потоков
        executor.prestartAllCoreThreads();

        logger.info("ThreadPoolManager инициализирован: core={}, max={}",
                corePoolSize, maxPoolSize);

        // Запускаем сбор статистики если включен
        if (Config.isStatsEnabled()) {
            // Создаем файл сразу при инициализации
            createStatsFile();

            scheduledExecutor.scheduleAtFixedRate(this::logStats, 1, 1, TimeUnit.MINUTES);
            scheduledExecutor.scheduleAtFixedRate(this::dumpStatsToFile, 5, 5, TimeUnit.MINUTES);
        }
    }

    private String getStatsFilePath() {
        String statsFile = Config.getStatsLogFile();

        // Если путь относительный, делаем его абсолютным
        File file = new File(statsFile);
        if (!file.isAbsolute()) {
            // Используем текущую рабочую директорию
            String currentDir = System.getProperty("user.dir");
            file = new File(currentDir, statsFile);
        }

        return file.getAbsolutePath();
    }

    private void createStatsFile() {
        try {
            File file = new File(statsFilePath);

            // Создаем родительские директории если нужно
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirsCreated = parentDir.mkdirs();
                logger.debug("Созданы директории {}: {}", parentDir.getAbsolutePath(), dirsCreated);
            }

            // Создаем файл если его нет
            if (!file.exists()) {
                boolean created = file.createNewFile();
                if (created) {
                    logger.info("Файл статистики создан: {}", statsFilePath);

                    // Записываем заголовок
                    try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                        writer.println("=".repeat(80));
                        writer.println("Статистика работы ThreadPoolManager");
                        writer.println("Время запуска: " + java.time.LocalDateTime.now());
                        writer.println("=".repeat(80));
                        writer.println();
                    }
                } else {
                    logger.warn("Файл статистики уже существует: {}", statsFilePath);
                }
            } else {
                logger.info("Файл статистики уже существует: {}", statsFilePath);
            }

            // Проверяем права на запись
            if (!file.canWrite()) {
                logger.error("Нет прав на запись в файл: {}", statsFilePath);
                // Пробуем альтернативный путь
                tryAlternativeStatsPath();
            }

        } catch (Exception e) {
            logger.error("Ошибка создания файла статистики: {}", e.getMessage(), e);
            // Пробуем альтернативный путь
            tryAlternativeStatsPath();
        }
    }

    private void tryAlternativeStatsPath() {
        try {
            // Пробуем создать в домашней директории
            String homeDir = System.getProperty("user.home");
            String altPath = homeDir + File.separator + "gemini_bot_stats.log";

            File altFile = new File(altPath);

            // Создаем родительские директории если нужно
            File parentDir = altFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            boolean created = altFile.createNewFile();

            if (created) {
                logger.info("Файл статистики создан в альтернативном месте: {}", altPath);
                // Обновляем путь
                statsFilePath = altPath;
            } else {
                logger.info("Альтернативный файл уже существует: {}", altPath);
                statsFilePath = altPath;
            }
        } catch (Exception e) {
            logger.error("Не удалось создать файл статистики ни в одном месте", e);
        }
    }

    private void dumpStatsToFile() {
        if (statsFilePath == null || statsFilePath.isEmpty()) {
            logger.warn("Путь к файлу статистики не указан");
            return;
        }

        try {
            String stats = getDetailedStats();

            // Используем простой FileWriter для надежности
            try (FileWriter writer = new FileWriter(statsFilePath, true);
                 PrintWriter pw = new PrintWriter(writer)) {
                pw.print(stats);
                pw.flush();
            }

            logger.debug("Статистика записана в файл: {} байт", stats.length());

        } catch (Exception e) {
            logger.error("Ошибка записи статистики в файл {}: {}",
                    statsFilePath, e.getMessage());

            // Вывод в консоль как запасной вариант
            System.out.println("[BACKUP STATS] " + getDetailedStats());
        }
    }

    public String getDetailedStats() {
        int poolSize = executor.getPoolSize();
        int activeCount = executor.getActiveCount();
        long completed = executor.getCompletedTaskCount();
        long queueSize = executor.getQueue().size();

        long avgTime = totalTasks.get() > 0 ?
                totalProcessingTime.get() / totalTasks.get() : 0;

        return String.format("[%s] Статистика потоков: " +
                        "Размер пула=%d, Активные=%d, В очереди=%d, " +
                        "Выполнено=%d, Всего задач=%d, Ошибок=%d, " +
                        "Среднее время=%dмс\n",
                java.time.LocalDateTime.now(),
                poolSize, activeCount, queueSize,
                completed, totalTasks.get(), failedTasks.get(),
                avgTime);
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public <T> CompletableFuture<T> submitTask(Callable<T> task) {
        totalTasks.incrementAndGet();
        activeThreads.incrementAndGet();

        long startTime = System.currentTimeMillis();

        CompletableFuture<T> future = new CompletableFuture<>();

        try {
            executor.execute(() -> {
                try {
                    T result = task.call();
                    completedTasks.incrementAndGet();
                    future.complete(result);
                } catch (Exception e) {
                    failedTasks.incrementAndGet();
                    logger.error("Ошибка выполнения задачи", e);
                    future.completeExceptionally(e);
                } finally {
                    activeThreads.decrementAndGet();
                    totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);
                }
            });
        } catch (RejectedExecutionException e) {
            logger.warn("Пул потоков переполнен, выполнение в вызывающем потоке");
            activeThreads.decrementAndGet();

            // Выполняем задачу синхронно в текущем потоке
            try {
                T result = task.call();
                completedTasks.incrementAndGet();
                future.complete(result);
            } catch (Exception ex) {
                failedTasks.incrementAndGet();
                logger.error("Ошибка выполнения задачи в вызывающем потоке", ex);
                future.completeExceptionally(ex);
            } finally {
                totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);
            }
        }

        return future;
    }

    public void scheduleTask(Runnable task, long delay, TimeUnit unit) {
        scheduledExecutor.schedule(() -> submitTask(() -> {
            task.run();
            return null;
        }), delay, unit);
    }

    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Ошибка выполнения периодической задачи", e);
            }
        }, initialDelay, period, unit);
    }

    private void logStats() {
        String stats = getDetailedStats();
        logger.info(stats.trim()); // Убираем перенос строки для красивого вывода в лог
    }

    public void shutdown() {
        logger.info("Завершение работы ThreadPoolManager...");

        // Записываем финальную статистику
        if (Config.isStatsEnabled() && statsFilePath != null) {
            dumpStatsToFile();

            try (FileWriter writer = new FileWriter(statsFilePath, true);
                 PrintWriter pw = new PrintWriter(writer)) {
                pw.println("\n" + "=".repeat(80));
                pw.println("ФИНАЛЬНАЯ СТАТИСТИКА");
                pw.println("Время завершения: " + java.time.LocalDateTime.now());
                pw.println("Всего задач выполнено: " + completedTasks.get());
                pw.println("Задач с ошибками: " + failedTasks.get());
                pw.println("Среднее время обработки: " +
                        (totalTasks.get() > 0 ?
                                totalProcessingTime.get() / totalTasks.get() : 0) + "мс");
                pw.println("=".repeat(80));
            } catch (Exception e) {
                logger.error("Ошибка записи финальной статистики", e);
            }
        }

        executor.shutdown();
        scheduledExecutor.shutdown();

        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ThreadPoolManager завершил работу");
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    // Кастомная фабрика потоков - ДОБАВЬТЕ ЭТОТ КЛАСС
    private static class BotThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "bot-thread-";

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setUncaughtExceptionHandler((t, e) -> {
                logger.error("Необработанное исключение в потоке {}", t.getName(), e);
            });
            return thread;
        }
    }
}