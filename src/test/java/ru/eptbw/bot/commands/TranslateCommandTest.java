package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.gemini.GeminiService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TranslateCommandTest {

    // Простая реализация GeminiService для тестов
    private static class TestGeminiService extends GeminiService {
        private final Map<String, String> translations = new HashMap<>();

        public TestGeminiService() {
            // Предопределенные переводы для тестов
            translations.put("Hello world->russian", "Привет мир");
            translations.put("Привет мир->english", "Hello world");
            translations.put("Bonjour->russian", "Добрый день");
            translations.put("Hola->russian", "Привет");
        }

        @Override
        public String translateText(String text, String targetLanguage) {
            String key = text + "->" + targetLanguage;
            if (translations.containsKey(key)) {
                return translations.get(key);
            }
            return "[Тестовый перевод: " + text + " на " + targetLanguage + "]";
        }
    }

    // Простая реализация SetLanguageCommand для тестов
    private static class TestSetLanguageCommand extends SetLanguageCommand {
        private final Map<Long, String> userLanguages = new HashMap<>();

        @Override
        public String getUserLanguage(Long userId) {
            return userLanguages.getOrDefault(userId, "ru"); // По умолчанию русский
        }

        public void setUserLanguage(Long userId, String language) {
            userLanguages.put(userId, language);
        }
    }

    private TranslateCommand translateCommand;
    private TestGeminiService testGeminiService;
    private TestSetLanguageCommand testSetLanguageCommand;

    @BeforeEach
    void setUp() {
        testGeminiService = new TestGeminiService();
        testSetLanguageCommand = new TestSetLanguageCommand();
        translateCommand = new TranslateCommand(testGeminiService);
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
    void getName_ShouldReturnTranslateCommand() {
        assertEquals("/translate", translateCommand.getName());
    }

    @Test
    void getDescription_ShouldContainCorrectInformation() {
        String description = translateCommand.getDescription();
        assertTrue(description.contains("Перевести текст"));
        assertTrue(description.contains("/translate <текст>"));
        assertTrue(description.contains("/translate <язык> <текст>"));
    }

    @Test
    void execute_NoArguments_ShouldReturnUsageHelp() {
        Update update = createTestUpdate(123L, "/translate");
        String[] args = {};

        String result = translateCommand.execute(update, args);

        assertTrue(result.contains("❌ Неправильный формат команды"));
        assertTrue(result.contains("/translate Привет мир"));
        assertTrue(result.contains("/translate en Привет мир"));
        assertTrue(result.contains("Поддерживаемые языки"));
    }

    @Test
    void execute_WithTextOnly_ShouldUseDefaultLanguage() {
        Update update = createTestUpdate(123L, "/translate Hello world");
        String[] args = {"Hello", "world"};

        String result = translateCommand.execute(update, args);

        assertEquals("Привет мир", result);
    }

    @Test
    void execute_WithLanguageCodeAndText_ShouldUseSpecifiedLanguage() {
        Update update = createTestUpdate(123L, "/translate en Привет мир");
        String[] args = {"en", "Привет", "мир"};

        String result = translateCommand.execute(update, args);

        assertEquals("Hello world", result);
    }

    @Test
    void execute_WithDifferentLanguage_ShouldWork() {
        Update update = createTestUpdate(123L, "/translate es Hola");
        String[] args = {"es", "Hola"};

        String result = translateCommand.execute(update, args);

        // Должен перевести на испанский (в нашем тесте это будет тестовый перевод)
        assertTrue(result.contains("[Тестовый перевод: Hola на spanish]"));
    }

    @Test
    void execute_WithUserLanguageSet_ShouldUseUserLanguage() {
        // Устанавливаем язык пользователя
        testSetLanguageCommand.setUserLanguage(123L, "en");

        Update update = createTestUpdate(123L, "/translate Привет мир");
        String[] args = {"Привет", "мир"};

        String result = translateCommand.execute(update, args);

        assertEquals("Hello world", result);
    }

    @Test
    void execute_WithInvalidLanguageCode_ShouldUseDefaultLanguage() {
        Update update = createTestUpdate(123L, "/translate xx Hello world");
        String[] args = {"xx", "Hello", "world"};

        String result = translateCommand.execute(update, args);

        // Должен проигнорировать невалидный код и использовать язык по умолчанию
        assertEquals("Привет мир", result);
    }

    @Test
    void execute_TextTooLong_ShouldReturnError() {
        String longText = "a".repeat(1001);
        Update update = createTestUpdate(123L, "/translate " + longText);
        String[] args = {longText};

        String result = translateCommand.execute(update, args);

        assertTrue(result.contains("❌ Текст слишком длинный"));
        assertTrue(result.contains("1000 символов"));
    }

    @Test
    void execute_WithSingleWord_ShouldWork() {
        Update update = createTestUpdate(123L, "/translate Bonjour");
        String[] args = {"Bonjour"};

        String result = translateCommand.execute(update, args);

        assertEquals("Добрый день", result);
    }

    @Test
    void execute_WithUpperCaseLanguageCode_ShouldWork() {
        Update update = createTestUpdate(123L, "/translate EN Привет");
        String[] args = {"EN", "Привет"};

        String result = translateCommand.execute(update, args);

        // Должен распознать код языка независимо от регистра
        assertTrue(result.contains("[Тестовый перевод: Привет на english]"));
    }

    @Test
    void execute_WithMixedCaseLanguageCode_ShouldWork() {
        Update update = createTestUpdate(123L, "/translate Es Hola");
        String[] args = {"Es", "Hola"};

        String result = translateCommand.execute(update, args);

        assertTrue(result.contains("[Тестовый перевод: Hola на spanish]"));
    }

    @Test
    void execute_WithSpecialCharacters_ShouldHandleCorrectly() {
        Update update = createTestUpdate(123L, "/translate Hello @world! #test");
        String[] args = {"Hello", "@world!", "#test"};

        String result = translateCommand.execute(update, args);

        assertTrue(result.contains("[Тестовый перевод: Hello @world! #test на russian]"));
    }

    @Test
    void execute_WithMultipleUsersDifferentLanguages_ShouldWorkCorrectly() {
        // Первый пользователь с английским
        testSetLanguageCommand.setUserLanguage(111L, "en");
        Update update1 = createTestUpdate(111L, "/translate Текст");
        String result1 = translateCommand.execute(update1, new String[]{"Текст"});

        // Второй пользователь с французским
        testSetLanguageCommand.setUserLanguage(222L, "fr");
        Update update2 = createTestUpdate(222L, "/translate Текст");
        String result2 = translateCommand.execute(update2, new String[]{"Текст"});

        // Третий пользователь без установленного языка (по умолчанию русский)
        Update update3 = createTestUpdate(333L, "/translate Текст");
        String result3 = translateCommand.execute(update3, new String[]{"Текст"});

        // Все должны работать без ошибок
        assertFalse(result1.contains("❌"));
        assertFalse(result2.contains("❌"));
        assertFalse(result3.contains("❌"));

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
    }

    @Test
    void execute_WhenGeminiServiceThrowsException_ShouldReturnError() {
        // Создаем GeminiService, который всегда бросает исключение
        GeminiService failingService = new GeminiService() {
            @Override
            public String translateText(String text, String targetLanguage) {
                throw new RuntimeException("API недоступен");
            }
        };

        TranslateCommand failingTranslateCommand = new TranslateCommand(failingService);

        Update update = createTestUpdate(123L, "/translate Hello");
        String[] args = {"Hello"};

        String result = failingTranslateCommand.execute(update, args);

        assertTrue(result.contains("❌ Ошибка при переводе через Gemini"));
        assertTrue(result.contains("попробуйте позже"));
    }

    @Test
    void execute_WithAllSupportedLanguages_ShouldWork() {
        String[] supportedLanguages = {"en", "ru", "es", "fr", "de", "it", "ja", "ko", "zh", "ar"};

        for (String lang : supportedLanguages) {
            Update update = createTestUpdate(123L, "/translate " + lang + " тест");
            String[] args = {lang, "тест"};

            String result = translateCommand.execute(update, args);

            // Проверяем что нет ошибки и есть какой-то результат
            assertFalse(result.contains("❌"), "Ошибка для языка: " + lang);
            assertFalse(result.contains("Ошибка"), "Ошибка для языка: " + lang);
            assertNotNull(result, "Пустой результат для языка: " + lang);
            assertFalse(result.isEmpty(), "Пустой результат для языка: " + lang);
        }
    }

    @Test
    void execute_WithEmptyTextAfterLanguageCode_ShouldTreatAsText() {
        Update update = createTestUpdate(123L, "/translate en");
        String[] args = {"en"}; // Только код языка без текста

        String result = translateCommand.execute(update, args);

        // Должен обработать "en" как текст для перевода
        assertFalse(result.contains("❌ Неправильный формат команды"));
        assertTrue(result.contains("[Тестовый перевод: en на russian]"));
    }
}