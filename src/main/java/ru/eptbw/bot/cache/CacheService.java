package ru.eptbw.bot.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eptbw.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private static CacheService instance;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    private final long expireMinutes;

    private CacheService() {
        this.maxSize = Config.getCacheMaxSize();
        this.expireMinutes = Config.getCacheExpireMinutes();

        // Очистка устаревших записей каждые 5 минут
        ru.eptbw.bot.threading.ThreadPoolManager.getInstance().scheduleAtFixedRate(
                this::cleanupExpired, 5, 5, TimeUnit.MINUTES
        );

        logger.info("CacheService инициализирован: maxSize={}, expire={}min",
                maxSize, expireMinutes);
    }

    public static synchronized CacheService getInstance() {
        if (instance == null) {
            instance = new CacheService();
        }
        return instance;
    }

    public void put(String key, Object value) {
        put(key, value, expireMinutes);
    }

    public void put(String key, Object value, long customExpireMinutes) {
        if (cache.size() >= maxSize) {
            // Удаляем самую старую запись при переполнении
            String oldestKey = findOldestKey();
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }

        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis(),
                TimeUnit.MINUTES.toMillis(customExpireMinutes));
        cache.put(key, entry);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }

        entry.updateAccessTime();
        return (T) entry.getValue();
    }

    public boolean contains(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return false;

        if (entry.isExpired()) {
            cache.remove(key);
            return false;
        }

        return true;
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public String getStats() {
        int expired = 0;
        long totalSize = 0;

        for (CacheEntry entry : cache.values()) {
            if (entry.isExpired()) {
                expired++;
            }
            totalSize += entry.getSize();
        }

        return String.format("Кеш: размер=%d, устарело=%d, память=~%dKB",
                cache.size(), expired, totalSize / 1024);
    }

    private void cleanupExpired() {
        int removed = 0;
        long now = System.currentTimeMillis();

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                cache.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            logger.debug("Очищено устаревших записей из кеша: {}", removed);
        }
    }

    private String findOldestKey() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().getLastAccessTime() < oldestTime) {
                oldestTime = entry.getValue().getLastAccessTime();
                oldestKey = entry.getKey();
            }
        }

        return oldestKey;
    }

    // Вспомогательные методы для конкретных типов кеширования
    public String getTranslationCacheKey(Long userId, String text, String targetLang) {
        return String.format("translation:%d:%s:%s",
                userId, text.hashCode(), targetLang);
    }

    public String getCommandCacheKey(Long userId, String command, String args) {
        return String.format("command:%d:%s:%s",
                userId, command, args != null ? args.hashCode() : 0);
    }

    // Внутренний класс для записи кеша
    private static class CacheEntry {
        private final Object value;
        private final long creationTime;
        private final long expireMillis;
        private volatile long lastAccessTime;

        CacheEntry(Object value, long creationTime, long expireMillis) {
            this.value = value;
            this.creationTime = creationTime;
            this.expireMillis = expireMillis;
            this.lastAccessTime = creationTime;
        }

        Object getValue() {
            return value;
        }

        long getLastAccessTime() {
            return lastAccessTime;
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long currentTime) {
            return currentTime > (creationTime + expireMillis);
        }

        long getSize() {
            if (value instanceof String) {
                return ((String) value).length() * 2L; // UTF-16
            } else if (value instanceof byte[]) {
                return ((byte[]) value).length;
            }
            return 0;
        }
    }
}