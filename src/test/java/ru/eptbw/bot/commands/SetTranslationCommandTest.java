package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetTranslationCommandTest {

    private SetTranslationCommand command;
    private Update update;
    private Message message;
    private User user;

    @BeforeEach
    void setUp() {
        // Создаем реальный объект команды
        command = new SetTranslationCommand();

        // Создаем тестовые объекты Telegram API
        update = new Update();
        message = new Message();
        user = new User();

        user.setId(12345L);
        user.setUserName("testuser");
        user.setFirstName("Test");
        user.setLastName("User");

        message.setFrom(user);
        update.setMessage(message);
    }

    @Test
    void testGetName() {
        assertEquals("/settranslation", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Установить язык для автоперевода отдельно от интерфейса",
                command.getDescription());
    }

    @Test
    void testExecute_NoArguments_ReturnsHelpWithCurrentSettings() {
        // Выполнение
        String result = command.execute(update, new String[]{});

        // Проверки структуры ответа
        assertNotNull(result);
        assertTrue(result.contains("Текущие настройки перевода"));
        assertTrue(result.contains("Язык интерфейса"));
        assertTrue(result.contains("Язык автоперевода"));
        assertTrue(result.contains("Поддерживаемые языки для переводов"));
        assertTrue(result.contains("en - английский"));
        assertTrue(result.contains("ru - русский"));
        assertTrue(result.contains("es - испанский"));
        assertTrue(result.contains("fr - французский"));
        assertTrue(result.contains("de - немецкий"));
    }

    @Test
    void testExecute_ValidEnglishLanguage_ReturnsSuccessMessage() {
        // Выполнение
        String result = command.execute(update, new String[]{"en"});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("✅ Язык для автоперевода установлен"));
        assertTrue(result.contains("английский"));
        assertTrue(result.contains("en"));
        assertTrue(result.contains("@testuser"));
        assertTrue(result.contains("автоматически переводиться"));
    }

    @Test
    void testExecute_ValidRussianLanguage_ReturnsSuccessMessage() {
        // Выполнение
        String result = command.execute(update, new String[]{"ru"});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("✅ Язык для автоперевода установлен"));
        assertTrue(result.contains("русский"));
        assertTrue(result.contains("ru"));
    }

    @Test
    void testExecute_ValidSpanishLanguage_ReturnsSuccessMessage() {
        // Выполнение
        String result = command.execute(update, new String[]{"es"});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("✅ Язык для автоперевода установлен"));
        assertTrue(result.contains("испанский"));
        assertTrue(result.contains("es"));
    }

    @Test
    void testExecute_ValidFrenchLanguage_ReturnsSuccessMessage() {
        // Выполнение
        String result = command.execute(update, new String[]{"fr"});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("✅ Язык для автоперевода установлен"));
        assertTrue(result.contains("французский"));
        assertTrue(result.contains("fr"));
    }

    @Test
    void testExecute_ValidGermanLanguage_ReturnsSuccessMessage() {
        // Выполнение
        String result = command.execute(update, new String[]{"de"});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("✅ Язык для автоперевода установлен"));
        assertTrue(result.contains("немецкий"));
        assertTrue(result.contains("de"));
    }

    @Test
    void testExecute_InvalidLanguageCode_ReturnsErrorMessage() {
        // Выполнение
        String result = command.execute(update, new String[]{"xx"});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("❌ Неподдерживаемый язык"));
        assertTrue(result.contains("Поддерживаемые языки для переводов"));
        assertTrue(result.contains("en - английский"));
        assertTrue(result.contains("ru - русский"));
    }

    @Test
    void testExecute_LowercaseLanguageCode_Works() {
        // Выполнение - передаем код в верхнем регистре
        String result = command.execute(update, new String[]{"ES"});

        // Проверки - должен корректно обработать
        assertNotNull(result);
        assertTrue(result.contains("испанский"));
        assertTrue(result.contains("es"));
    }

    @Test
    void testExecute_EmptyStringArgument_ReturnsError() {
        // Выполнение
        String result = command.execute(update, new String[]{""});

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("❌ Неподдерживаемый язык"));
    }

    @Test
    void testExecute_NullArguments_ReturnsHelp() {
        // Выполнение
        String result = command.execute(update, null);

        // Проверки
        assertNotNull(result);
        assertTrue(result.contains("Текущие настройки перевода"));
    }

    @Test
    void testGetCommandVariations() {
        // Выполнение
        String[] variations = command.getCommandVariations();

        // Проверки
        assertNotNull(variations);
        assertEquals(6, variations.length);

        // Проверяем основные вариации
        assertEquals("/settranslation", variations[0]);
        assertEquals("/settrans", variations[1]);
        assertEquals("/translang", variations[2]);
        assertEquals("/translation", variations[3]);
        assertEquals("/autotranslate", variations[4]);
        assertEquals("/автоперевод", variations[5]);

        // Проверяем, что все вариации уникальны
        long distinctCount = java.util.Arrays.stream(variations).distinct().count();
        assertEquals(variations.length, distinctCount);
    }

    @Test
    void testCommandInheritance() {
        // Проверяем, что команда наследуется от правильных классов
        assertTrue(command instanceof AbstractCommand);
        assertTrue(command instanceof Command);
    }

    @Test
    void testUserDisplayNameFormatting_WithUsername() {
        user.setUserName("username123");
        user.setFirstName("First");
        user.setLastName("Last");

        String result = command.execute(update, new String[]{"en"});
        assertTrue(result.contains("@username123"));
    }

    @Test
    void testUserDisplayNameFormatting_WithoutUsername_WithLastName() {
        user.setUserName(null);
        user.setFirstName("First");
        user.setLastName("Last");

        String result = command.execute(update, new String[]{"en"});
        assertTrue(result.contains("First Last"));
    }

    @Test
    void testUserDisplayNameFormatting_WithoutUsername_WithoutLastName() {
        user.setUserName(null);
        user.setFirstName("FirstOnly");
        user.setLastName(null);

        String result = command.execute(update, new String[]{"en"});
        assertTrue(result.contains("FirstOnly"));
    }

    @Test
    void testSupportedLanguagesThroughReflection() throws Exception {
        // Используем рефлексию для проверки приватного поля (без изменения)
        java.lang.reflect.Field languagesField = SetTranslationCommand.class.getDeclaredField("translationLanguages");
        languagesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> languages = (Map<String, String>) languagesField.get(command);

        // Проверяем, что карта содержит все ожидаемые языки
        assertNotNull(languages);
        assertEquals(5, languages.size());
        assertEquals("английский", languages.get("en"));
        assertEquals("русский", languages.get("ru"));
        assertEquals("испанский", languages.get("es"));
        assertEquals("французский", languages.get("fr"));
        assertEquals("немецкий", languages.get("de"));
    }

    @Test
    void testPrivateMethodsThroughReflection() throws Exception {
        // Тестируем приватные методы через рефлексию

        // Метод getInterfaceLanguageName
        Method getInterfaceLanguageNameMethod = SetTranslationCommand.class.getDeclaredMethod(
                "getInterfaceLanguageName", String.class);
        getInterfaceLanguageNameMethod.setAccessible(true);

        String result = (String) getInterfaceLanguageNameMethod.invoke(command, "ru");
        assertEquals("Русский", result);

        result = (String) getInterfaceLanguageNameMethod.invoke(command, "en");
        assertEquals("English", result);

        result = (String) getInterfaceLanguageNameMethod.invoke(command, "es");
        assertEquals("Español", result);

        // Метод getSupportedLanguagesHelp
        Method getSupportedLanguagesHelpMethod = SetTranslationCommand.class.getDeclaredMethod(
                "getSupportedLanguagesHelp");
        getSupportedLanguagesHelpMethod.setAccessible(true);

        String helpText = (String) getSupportedLanguagesHelpMethod.invoke(command);
        assertNotNull(helpText);
        assertTrue(helpText.contains("Поддерживаемые языки для переводов"));
        assertTrue(helpText.contains("en - английский"));
    }
}

// Дополнительные тесты для граничных случаев
class SetTranslationCommandEdgeCasesTest {

    @Test
    void testConstructorInitialization() {
        SetTranslationCommand command = new SetTranslationCommand();

        assertNotNull(command);
        assertEquals("/settranslation", command.getName());
        assertEquals("Установить язык для автоперевода отдельно от интерфейса",
                command.getDescription());
    }

    @Test
    void testExecute_WithMultipleSpaces() {
        SetTranslationCommand command = new SetTranslationCommand();

        Update update = new Update();
        Message message = new Message();
        User user = new User();

        user.setId(999L);
        user.setUserName("test");
        user.setFirstName("Multi");
        user.setLastName("Space");

        message.setFrom(user);
        update.setMessage(message);

        // Тестируем с лишними пробелами
        String result = command.execute(update, new String[]{"  en  "});

        assertNotNull(result);
        assertTrue(result.contains("английский"));
    }

    @Test
    void testExecute_MultipleArguments_IgnoresExtras() {
        SetTranslationCommand command = new SetTranslationCommand();

        Update update = new Update();
        Message message = new Message();
        User user = new User();

        user.setId(888L);
        user.setUserName("multiargs");
        user.setFirstName("Multi");

        message.setFrom(user);
        update.setMessage(message);

        // Тестируем с несколькими аргументами
        String result = command.execute(update, new String[]{"fr", "extra", "arguments"});

        assertNotNull(result);
        // Должен взять только первый аргумент
        assertTrue(result.contains("французский"));
        assertFalse(result.contains("extra")); // Лишние аргументы не должны отображаться
    }

    @Test
    void testResponseStructureForAllLanguages() {
        SetTranslationCommand command = new SetTranslationCommand();

        Update update = new Update();
        Message message = new Message();
        User user = new User();

        user.setId(777L);
        user.setUserName("alllangs");
        user.setFirstName("All");
        user.setLastName("Languages");

        message.setFrom(user);
        update.setMessage(message);

        // Тестируем все поддерживаемые языки
        String[] languages = {"en", "ru", "es", "fr", "de"};
        String[] expectedNames = {"английский", "русский", "испанский", "французский", "немецкий"};

        for (int i = 0; i < languages.length; i++) {
            String result = command.execute(update, new String[]{languages[i]});

            assertNotNull(result);
            assertTrue(result.contains("✅ Язык для автоперевода установлен"));
            assertTrue(result.contains(expectedNames[i]));
            assertTrue(result.contains(languages[i]));
            assertTrue(result.contains("@alllangs"));
        }
    }

    @Test
    void testCommandVariationsAreValid() {
        SetTranslationCommand command = new SetTranslationCommand();
        String[] variations = command.getCommandVariations();

        // Проверяем, что все вариации начинаются с /
        for (String variation : variations) {
            assertTrue(variation.startsWith("/"), "Вариация должна начинаться с /: " + variation);
            assertTrue(variation.length() > 1, "Вариация должна быть длиннее 1 символа: " + variation);
        }
    }
}