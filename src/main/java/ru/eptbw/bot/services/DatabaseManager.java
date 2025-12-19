package ru.eptbw.bot.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:bot_database.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Таблица для языковых настроек пользователей
            String createUserLanguagesTable = """
                CREATE TABLE IF NOT EXISTS user_languages (
                    user_id INTEGER PRIMARY KEY,
                    language_code TEXT NOT NULL DEFAULT 'ru',
                    translation_language TEXT DEFAULT 'en',  -- ← ДОБАВЛЯЕМ язык для переводов
                    user_name TEXT,
                    first_name TEXT,
                    last_name TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;

            // Таблица для истории переводов
            String createTranslationHistoryTable = """
                CREATE TABLE IF NOT EXISTS translation_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    user_name TEXT,
                    original_text TEXT NOT NULL,
                    translated_text TEXT NOT NULL,
                    source_language TEXT,
                    target_language TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES user_languages (user_id)
                )
            """;

            // Таблица для статистики использования
            String createUsageStatsTable = """
                CREATE TABLE IF NOT EXISTS usage_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    user_name TEXT,
                    command_name TEXT NOT NULL,
                    execution_time_ms INTEGER,
                    success BOOLEAN DEFAULT TRUE,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;

            stmt.execute(createUserLanguagesTable);
            stmt.execute(createTranslationHistoryTable);
            stmt.execute(createUsageStatsTable);

            System.out.println("✅ База данных инициализирована успешно");

        } catch (SQLException e) {
            System.err.println("❌ Ошибка инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== МЕТОДЫ ДЛЯ ЯЗЫКОВ ПОЛЬЗОВАТЕЛЕЙ ====================

    /**
     * Установить или обновить язык пользователя
     */
    public void setUserLanguage(Long userId, String languageCode, String userName,
                                String firstName, String lastName) {
        String sql = """
            INSERT OR REPLACE INTO user_languages 
            (user_id, language_code, user_name, first_name, last_name, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, languageCode);
            pstmt.setString(3, userName);
            pstmt.setString(4, firstName);
            pstmt.setString(5, lastName);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка установки языка пользователя: " + e.getMessage());
        }
    }

    /**
     * Установить язык для переводов пользователя
     */
    public void setUserTranslationLanguage(Long userId, String translationLanguage) {
        String sql = """
            UPDATE user_languages 
            SET translation_language = ?, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, translationLanguage);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка установки языка перевода: " + e.getMessage());
        }
    }

    /**
     * Получить язык интерфейса пользователя
     */
    public String getUserLanguage(Long userId) {
        String sql = "SELECT language_code FROM user_languages WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("language_code");
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка получения языка пользователя: " + e.getMessage());
        }

        return "ru"; // Язык по умолчанию
    }

    /**
     * Получить язык для переводов пользователя
     */
    public String getUserTranslationLanguage(Long userId) {
        String sql = "SELECT translation_language FROM user_languages WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String lang = rs.getString("translation_language");
                return lang != null ? lang : "en"; // По умолчанию английский
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка получения языка перевода: " + e.getMessage());
        }

        return "en"; // Язык перевода по умолчанию
    }

    /**
     * Получить полную информацию о пользователе
     */
    public Map<String, String> getUserInfo(Long userId) {
        Map<String, String> userInfo = new HashMap<>();
        String sql = """
            SELECT user_name, first_name, last_name, language_code, 
                   translation_language, created_at, updated_at 
            FROM user_languages WHERE user_id = ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                userInfo.put("user_name", rs.getString("user_name"));
                userInfo.put("first_name", rs.getString("first_name"));
                userInfo.put("last_name", rs.getString("last_name"));
                userInfo.put("language_code", rs.getString("language_code"));
                userInfo.put("translation_language", rs.getString("translation_language"));
                userInfo.put("created_at", rs.getString("created_at"));
                userInfo.put("updated_at", rs.getString("updated_at"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка получения информации о пользователе: " + e.getMessage());
        }

        return userInfo;
    }

    // ==================== МЕТОДЫ ДЛЯ ИСТОРИИ ПЕРЕВОДОВ ====================

    /**
     * Сохранить перевод в историю
     */
    public void saveTranslation(Long userId, String userName, String originalText,
                                String translatedText, String sourceLanguage, String targetLanguage) {
        String sql = """
            INSERT INTO translation_history 
            (user_id, user_name, original_text, translated_text, source_language, target_language)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, userName);
            pstmt.setString(3, originalText);
            pstmt.setString(4, translatedText);
            pstmt.setString(5, sourceLanguage);
            pstmt.setString(6, targetLanguage);
            pstmt.executeUpdate();

            // Обновляем последний язык перевода пользователя
            setUserTranslationLanguage(userId, targetLanguage);

        } catch (SQLException e) {
            System.err.println("❌ Ошибка сохранения перевода: " + e.getMessage());
        }
    }

    /**
     * Получить статистику переводов пользователя
     */
    public Map<String, String> getUserTranslationStats(Long userId) {
        Map<String, String> stats = new HashMap<>();
        String sql = """
            SELECT 
                COUNT(*) as total_translations,
                COUNT(DISTINCT target_language) as unique_languages,
                MAX(created_at) as last_translation
            FROM translation_history 
            WHERE user_id = ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.put("total_translations", String.valueOf(rs.getInt("total_translations")));
                stats.put("unique_languages", String.valueOf(rs.getInt("unique_languages")));
                stats.put("last_translation", rs.getString("last_translation"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка получения статистики: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Получить последний язык перевода пользователя
     */
    public String getLastTranslationLanguage(Long userId) {
        String sql = """
            SELECT target_language 
            FROM translation_history 
            WHERE user_id = ? 
            ORDER BY created_at DESC 
            LIMIT 1
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("target_language");
            }

        } catch (SQLException e) {
            System.err.println("❌ Ошибка получения последнего языка перевода: " + e.getMessage());
        }

        return "en"; // По умолчанию английский
    }

    // ==================== МЕТОДЫ ДЛЯ СТАТИСТИКИ ИСПОЛЬЗОВАНИЯ ====================

    /**
     * Сохранить статистику выполнения команды
     */
    public void saveCommandUsage(Long userId, String userName, String commandName,
                                 Long executionTimeMs, boolean success, String errorMessage) {
        String sql = """
            INSERT INTO usage_stats 
            (user_id, user_name, command_name, execution_time_ms, success, error_message)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, userName);
            pstmt.setString(3, commandName);
            pstmt.setLong(4, executionTimeMs);
            pstmt.setBoolean(5, success);
            pstmt.setString(6, errorMessage);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Ошибка сохранения статистики команды: " + e.getMessage());
        }
    }

    /**
     * Упрощенный метод для успешного выполнения
     */
    public void saveSuccessfulCommand(Long userId, String userName, String commandName, Long executionTimeMs) {
        saveCommandUsage(userId, userName, commandName, executionTimeMs, true, null);
    }

    /**
     * Метод для неудачного выполнения
     */
    public void saveFailedCommand(Long userId, String userName, String commandName,
                                  Long executionTimeMs, String error) {
        saveCommandUsage(userId, userName, commandName, executionTimeMs, false, error);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Проверить соединение с базой данных
     */
    public boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Ошибка соединения с БД: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получить размер базы данных
     */
    public long getDatabaseSize() {
        File dbFile = new File("bot_database.db");
        return dbFile.exists() ? dbFile.length() : 0;
    }

    /**
     * Сделать резервную копию базы данных
     */
    public boolean backupDatabase(String backupPath) {
        try {
            File original = new File("bot_database.db");
            File backup = new File(backupPath);

            if (!original.exists()) {
                System.err.println("❌ Файл базы данных не найден");
                return false;
            }

            Files.copy(
                    original.toPath(),
                    backup.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("✅ Резервная копия создана: " + backupPath);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Ошибка создания резервной копии: " + e.getMessage());
            return false;
        }
    }
}