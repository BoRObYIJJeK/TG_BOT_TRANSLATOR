package ru.eptbw.bot.gemini;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import ru.eptbw.config.Config;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
@EnabledIf("isApiKeyAvailable")
class GeminiServiceIntegrationTest {

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
            System.out.println("initialisated GeminiService with model: " + Config.getGeminiModel());
        }
    }

    @Test
    @DisplayName("Проверка доступности API")
    void isAvailable_ShouldReturnTrue() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");

        boolean available = geminiService.isAvailable();

        if (available) {
            System.out.println("✅ Gemini API доступен");
            assertTrue(available);
        } else {
            System.out.println("❌ Gemini API недоступен. Проверьте API ключ: " + Config.getGeminiApiKey());
            // Если API недоступен, все последующие тесты бессмысленны
            assumeTrue(false, "Gemini API недоступен");
        }
    }

    @Test
    @DisplayName("Перевод текста на русский язык")
    void translateText_ToRussian_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String result = geminiService.translateText("Hello world", "russian");

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Перевод не удался: " + result);
        }

        System.out.println("Перевод 'Hello world' на русский: " + result);
    }

    @Test
    @DisplayName("Перевод текста на английский язык")
    void translateText_ToEnglish_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String result = geminiService.translateText("Привет мир", "english");

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Перевод не удался: " + result);
        }

        System.out.println("Перевод 'Привет мир' на английский: " + result);
    }

    @Test
    @DisplayName("Генерация ответа на сообщение")
    void generateResponse_ShouldReturnValidAnswer() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String result = geminiService.generateResponse("Скажи привет");

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат генерации не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка") || result.contains("недоступна")) {
            fail("Генерация ответа не удалась: " + result);
        }

        System.out.println("Ответ на 'Скажи привет': " + result);
    }

    @Test
    @DisplayName("Автоматический перевод на русский")
    void autoTranslateToRussian_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String result = geminiService.autoTranslateToRussian("Good morning");

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат автоперевода не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Автоперевод не удался: " + result);
        }

        System.out.println("Автоперевод 'Good morning': " + result);
    }

    @Test
    @DisplayName("Обработка длинного текста")
    void translateText_LongText_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String longText = "This is a long text that needs to be translated. ".repeat(5);
        String result = geminiService.translateText(longText, "russian");

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода длинного текста не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Перевод длинного текста не удался: " + result);
        }

        System.out.println("Перевод длинного текста успешен");
    }

    @Test
    @DisplayName("Обработка специальных символов")
    void translateText_WithSpecialCharacters_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String text = "Hello @world! #test 123";
        String result = geminiService.translateText(text, "russian");

        assertNotNull(result);
        assertFalse(result.trim().isEmpty(), "Результат перевода текста с символами не должен быть пустым");

        if (result.contains("❌") || result.contains("Ошибка")) {
            fail("Перевод текста с символами не удался: " + result);
        }

        System.out.println("Перевод текста с символами: " + result);
    }

    @Test
    @DisplayName("Перевод на разные языки")
    void translateText_DifferentLanguages_ShouldWork() {
        assumeTrue(isApiKeyAvailable(), "API ключ не доступен");
        assumeTrue(geminiService.isAvailable(), "API недоступен");

        String[][] testCases = {
                {"spanish", "Hello world"},
                {"french", "Hello world"},
                {"german", "Hello world"},
                {"italian", "Hello world"}
        };

        for (String[] testCase : testCases) {
            String result = geminiService.translateText(testCase[1], testCase[0]);

            assertNotNull(result, "Результат для языка " + testCase[0] + " не должен быть null");
            assertFalse(result.trim().isEmpty(), "Результат для языка " + testCase[0] + " не должен быть пустым");

            if (result.contains("❌") || result.contains("Ошибка")) {
                fail("Перевод на " + testCase[0] + " не удался: " + result);
            }

            System.out.println("Перевод на " + testCase[0] + ": " + result);
        }
    }
}