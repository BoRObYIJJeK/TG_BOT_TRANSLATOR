package ru.eptbw.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    public static String getGeminiApiKey() {
        return dotenv.get("GEMINI_API_KEY");
    }

    public static String getGeminiModel() {
        return dotenv.get("GEMINI_MODEL", "gemini-2.0-flash");
    }

    public static String getTelegramBotToken() {
        return dotenv.get("TELEGRAM_BOT_TOKEN");
    }

    public static String getBotName() {
        return dotenv.get("BOT_NAME", "GeminiTranslationBot");
    }

    // Метод для проверки загрузки конфигурации
    public static void printConfig() {
        System.out.println("Загружена конфигурация. Параметры:");
        System.out.println("GEMINI_API_KEY: " + (getGeminiApiKey() != null ? "***" + getGeminiApiKey().substring(Math.max(0, getGeminiApiKey().length() - 4)) : "null"));
        System.out.println("GEMINI_MODEL: " + getGeminiModel());
        System.out.println("TELEGRAM_BOT_TOKEN: " + (getTelegramBotToken() != null ? "***" + getTelegramBotToken().substring(Math.max(0, getTelegramBotToken().length() - 4)) : "null"));
        System.out.println("BOT_NAME: " + getBotName());
    }

    public static int getThreadPoolCoreSize() {
        return Integer.parseInt(dotenv.get("THREAD_POOL_CORE_SIZE", "10"));
    }

    public static int getThreadPoolMaxSize() {
        return Integer.parseInt(dotenv.get("THREAD_POOL_MAX_SIZE", "50"));
    }

    public static int getThreadPoolQueueSize() {
        return Integer.parseInt(dotenv.get("THREAD_POOL_QUEUE_SIZE", "1000"));
    }

    public static long getThreadKeepAliveTime() {
        return Long.parseLong(dotenv.get("THREAD_KEEP_ALIVE_TIME", "60"));
    }

    // Rate limiting
    public static int getRateLimitPerMinute() {
        return Integer.parseInt(dotenv.get("RATE_LIMIT_PER_MINUTE", "60"));
    }

    public static int getRateLimitPerHour() {
        return Integer.parseInt(dotenv.get("RATE_LIMIT_PER_HOUR", "1000"));
    }

    // Кеширование
    public static int getCacheMaxSize() {
        return Integer.parseInt(dotenv.get("CACHE_MAX_SIZE", "1000"));
    }

    public static long getCacheExpireMinutes() {
        return Long.parseLong(dotenv.get("CACHE_EXPIRE_MINUTES", "60"));
    }

    // Очередь сообщений
    public static int getMaxRetryAttempts() {
        return Integer.parseInt(dotenv.get("MAX_RETRY_ATTEMPTS", "3"));
    }

    public static long getRetryDelayMs() {
        return Long.parseLong(dotenv.get("RETRY_DELAY_MS", "1000"));
    }

    // Статистика
    public static String getStatsLogFile() {
        return dotenv.get("STATS_LOG_FILE", "bot_stats.log");
    }

    public static boolean isStatsEnabled() {
        return Boolean.parseBoolean(dotenv.get("ENABLE_STATS", "true"));
    }

    // Всплывающие подсказки
    public static boolean isInlineSuggestionsEnabled() {
        return Boolean.parseBoolean(dotenv.get("ENABLE_INLINE_SUGGESTIONS", "true"));
    }
}