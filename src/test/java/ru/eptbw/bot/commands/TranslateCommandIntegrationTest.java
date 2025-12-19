package ru.eptbw.bot.commands;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.gemini.GeminiService;
import ru.eptbw.config.Config;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@EnabledIf("isApiKeyAvailable")
class TranslateCommandIntegrationTest {

    private TranslateCommand translateCommand;
    private SetLanguageCommand setLanguageCommand;
    private GeminiService geminiService;

    // Проверяем доступность API ключа из .env
    static boolean isApiKeyAvailable() {
        String apiKey = Config.getGeminiApiKey();
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("AIzaSyAakI");
    }

    @BeforeAll
    void setUp() {
        if (isApiKeyAvailable()) {
            geminiService = new GeminiService();
            setLanguageCommand = new SetLanguageCommand();
            translateCommand = new TranslateCommand(geminiService);

            // Проверяем доступность API
            boolean available = geminiService.isAvailable();
            if (available) {
                System.out.println("✅ Gemini API доступен для тестов TranslateCommand");
            } else {
                System.out.println("❌ Gemini API недоступен для тестов TranslateCommand");
            }
        }
    }

    private Update createTestUpdate(Long userId, String text) {
        Update update = new Update();
        Message message = new Message();
        User user = new User();

        user.setId(userId);
        user.setFirstName("TestUser");
        user.setIsBot(false);

        message.setFrom(user);
        message.setText(text);
        update.setMessage(message);

        return update;
    }

    @Test
    @DisplayName("Перевод с английского на русский через команду")
    void execute_EnglishToRussian_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        Update update = createTestUpdate(123L, "/translate Hello world");
        String[] args = {"Hello", "world"};

        String result = translateCommand.execute(update, args);

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Команда перевода не удалась: " + result);
        }

        System.out.println("Результат команды перевода: " + result);
    }

    @Test
    @DisplayName("Перевод с русского на английский через команду")
    void execute_RussianToEnglish_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        Update update = createTestUpdate(123L, "/translate en Привет мир");
        String[] args = {"en", "Привет", "мир"};

        String result = translateCommand.execute(update, args);

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Команда перевода не удалась: " + result);
        }

        System.out.println("Результат команды перевода на английский: " + result);
    }

    @Test
    @DisplayName("Перевод на разные языки через команду")
    void execute_DifferentLanguages_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String[][] testCases = {
                {"es", "Hola mundo"},
                {"fr", "Bonjour le monde"},
                {"de", "Hallo Welt"}
        };

        for (String[] testCase : testCases) {
            Update update = createTestUpdate(123L, "/translate " + testCase[0] + " " + testCase[1]);
            String[] args = {testCase[0], testCase[1]};

            String result = translateCommand.execute(update, args);

            assertNotNull(result, "Пустой результат для языка: " + testCase[0]);
            assertFalse(result.trim().isEmpty(), "Результат для языка " + testCase[0] + " не должен быть пустым");

            if (result.contains("❌") || result.contains("Ошибка")) {
                fail("Перевод на " + testCase[0] + " не удался: " + result);
            }

            System.out.println("Перевод на " + testCase[0] + " через команду: " + result);
        }
    }

    @Test
    @DisplayName("Перевод с установленным языком пользователя")
    void execute_WithUserLanguage_ShouldUseUserLanguage() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        // Устанавливаем язык пользователя
        Long userId = 123L;
        Update setupUpdate = createTestUpdate(userId, "/setlanguage en");
        setLanguageCommand.execute(setupUpdate, new String[]{"en"});

        // Тестируем перевод
        Update update = createTestUpdate(userId, "/translate Привет мир");
        String[] args = {"Привет", "мир"};

        String result = translateCommand.execute(update, args);

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Перевод с установленным языком пользователя не удался: " + result);
        }

        System.out.println("Перевод с установленным языком пользователя: " + result);
    }

    @Test
    @DisplayName("Обработка слишком длинного текста")
    void execute_TextTooLong_ShouldReturnError() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");

        String longText = "a".repeat(1001);
        Update update = createTestUpdate(123L, "/translate " + longText);
        String[] args = {longText};

        String result = translateCommand.execute(update, args);

        assertNotNull(result);
        assertTrue(result.contains("❌") || result.contains("слишком длинный"),
                "Должна вернуться ошибка о слишком длинном тексте: " + result);

        System.out.println("Корректная обработка слишком длинного текста: " + result);
    }
}