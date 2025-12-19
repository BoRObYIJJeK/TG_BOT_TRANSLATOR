package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.junit.jupiter.api.Assertions.*;

class SetLanguageCommandTest {

    private SetLanguageCommand setLanguageCommand;

    @BeforeEach
    void setUp() {
        setLanguageCommand = new SetLanguageCommand();
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
    void getName_ShouldReturnSetLanguageCommand() {
        assertEquals("/setlanguage", setLanguageCommand.getName());
    }

    @Test
    void getDescription_ShouldReturnCorrectDescription() {
        String description = setLanguageCommand.getDescription();
        assertTrue(description.contains("Установить язык бота"));
        assertTrue(description.contains("ru, en, es, fr, de"));
    }

    @Test
    void execute_NoArguments_ShouldShowCurrentLanguageAndSupportedLanguages() {
        Update update = createTestUpdate(123L, "/setlanguage");
        String[] args = {};

        String result = setLanguageCommand.execute(update, args);

        assertTrue(result.contains("🌍 Текущий язык"));
        assertTrue(result.contains("📚 Поддерживаемые языки"));
        assertTrue(result.contains("ru - Русский"));
        assertTrue(result.contains("en - English"));
        assertTrue(result.contains("es - Español"));
        assertTrue(result.contains("fr - Français"));
        assertTrue(result.contains("de - Deutsch"));
        assertTrue(result.contains("/setlanguage <код_языка>"));
    }

    @Test
    void execute_WithNullArgsArray_ShouldShowCurrentLanguage() {
        Update update = createTestUpdate(123L, "/setlanguage");

        String result = setLanguageCommand.execute(update, null);

        assertTrue(result.contains("🌍 Текущий язык"));
        assertTrue(result.contains("📚 Поддерживаемые языки"));
    }

    @Test
    void execute_WithValidLanguageCode_ShouldSetLanguage() {
        Update update = createTestUpdate(123L, "/setlanguage en");
        String[] args = {"en"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Language set to: English", result);
        assertEquals("en", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithRussianLanguage_ShouldSetRussian() {
        Update update = createTestUpdate(123L, "/setlanguage ru");
        String[] args = {"ru"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Язык установлен: Русский", result);
        assertEquals("ru", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithNullLanguageCode_ShouldReturnError() {
        Update update = createTestUpdate(123L, "/setlanguage ");
        String[] args = {null};

        String result = setLanguageCommand.execute(update, args);

        assertTrue(result.contains("❌ Неверный код языка"));
        assertTrue(result.contains("Поддерживаемые языки"));

        // Язык не должен установиться
        assertEquals("ru", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithEmptyLanguageCode_ShouldReturnError() {
        Update update = createTestUpdate(123L, "/setlanguage ");
        String[] args = {""};

        String result = setLanguageCommand.execute(update, args);

        assertTrue(result.contains("❌ Неподдерживаемый язык"));
        assertTrue(result.contains("Доступные языки"));
        assertEquals("ru", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithInvalidLanguageCode_ShouldReturnError() {
        Update update = createTestUpdate(123L, "/setlanguage xx");
        String[] args = {"xx"};

        String result = setLanguageCommand.execute(update, args);

        assertTrue(result.contains("❌ Неподдерживаемый язык"));
        assertTrue(result.contains("Доступные языки"));
        assertEquals("ru", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithUpperCaseLanguageCode_ShouldWork() {
        Update update = createTestUpdate(123L, "/setlanguage EN");
        String[] args = {"EN"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Language set to: English", result);
        assertEquals("en", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithMixedCaseLanguageCode_ShouldWork() {
        Update update = createTestUpdate(123L, "/setlanguage En");
        String[] args = {"En"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Language set to: English", result);
        assertEquals("en", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithSpanishLanguage_ShouldSetSpanish() {
        Update update = createTestUpdate(123L, "/setlanguage es");
        String[] args = {"es"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Idioma establecido: Español", result);
        assertEquals("es", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithFrenchLanguage_ShouldSetFrench() {
        Update update = createTestUpdate(123L, "/setlanguage fr");
        String[] args = {"fr"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Langue définie: Français", result);
        assertEquals("fr", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithGermanLanguage_ShouldSetGerman() {
        Update update = createTestUpdate(123L, "/setlanguage de");
        String[] args = {"de"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Sprache eingestellt: Deutsch", result);
        assertEquals("de", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void getUserLanguage_ForNewUser_ShouldReturnDefaultRussian() {
        String userLanguage = setLanguageCommand.getUserLanguage(999L);
        assertEquals("ru", userLanguage);
    }

    @Test
    void getUserLanguage_AfterSettingLanguage_ShouldReturnSetLanguage() {
        Update update = createTestUpdate(123L, "/setlanguage en");
        setLanguageCommand.execute(update, new String[]{"en"});

        String userLanguage = setLanguageCommand.getUserLanguage(123L);
        assertEquals("en", userLanguage);
    }

    @Test
    void getUserLanguage_ForDifferentUsers_ShouldReturnCorrectLanguages() {
        Update update1 = createTestUpdate(111L, "/setlanguage en");
        Update update2 = createTestUpdate(222L, "/setlanguage es");
        Update update3 = createTestUpdate(333L, "/setlanguage fr");

        setLanguageCommand.execute(update1, new String[]{"en"});
        setLanguageCommand.execute(update2, new String[]{"es"});
        setLanguageCommand.execute(update3, new String[]{"fr"});

        assertEquals("en", setLanguageCommand.getUserLanguage(111L));
        assertEquals("es", setLanguageCommand.getUserLanguage(222L));
        assertEquals("fr", setLanguageCommand.getUserLanguage(333L));
    }

    @Test
    void execute_MultipleCallsForSameUser_ShouldUpdateLanguage() {
        Update update = createTestUpdate(123L, "/setlanguage");

        String result1 = setLanguageCommand.execute(update, new String[]{"en"});
        assertEquals("✅ Language set to: English", result1);
        assertEquals("en", setLanguageCommand.getUserLanguage(123L));

        String result2 = setLanguageCommand.execute(update, new String[]{"es"});
        assertEquals("✅ Idioma establecido: Español", result2);
        assertEquals("es", setLanguageCommand.getUserLanguage(123L));

        String result3 = setLanguageCommand.execute(update, new String[]{"de"});
        assertEquals("✅ Sprache eingestellt: Deutsch", result3);
        assertEquals("de", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_SwitchBackToDefaultLanguage_ShouldWork() {
        Update update = createTestUpdate(123L, "/setlanguage");

        // Устанавливаем другой язык
        String result1 = setLanguageCommand.execute(update, new String[]{"en"});
        assertEquals("✅ Language set to: English", result1);
        assertEquals("en", setLanguageCommand.getUserLanguage(123L));

        // Возвращаемся к русскому
        String result2 = setLanguageCommand.execute(update, new String[]{"ru"});
        assertEquals("✅ Язык установлен: Русский", result2);
        assertEquals("ru", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_WithMultipleSpaces_ShouldHandleCorrectly() {
        Update update = createTestUpdate(123L, "/setlanguage   en");
        String[] args = {"en"};

        String result = setLanguageCommand.execute(update, args);

        assertEquals("✅ Language set to: English", result);
        assertEquals("en", setLanguageCommand.getUserLanguage(123L));
    }

    @Test
    void execute_UserPersistsAfterMultipleCommands() {
        Long userId = 12345L;
        Update update = createTestUpdate(userId, "/setlanguage");

        // Устанавливаем язык
        setLanguageCommand.execute(update, new String[]{"fr"});
        assertEquals("fr", setLanguageCommand.getUserLanguage(userId));

        // Вызываем команду без аргументов - язык должен сохраниться
        String result = setLanguageCommand.execute(update, new String[]{});
        assertTrue(result.contains("🌍 Текущий язык"));
        assertTrue(result.contains("Français"));
        assertEquals("fr", setLanguageCommand.getUserLanguage(userId));
    }

    @Test
    void execute_ConcurrentUsers_ShouldNotInterfere() {
        Update update1 = createTestUpdate(100L, "/setlanguage en");
        Update update2 = createTestUpdate(200L, "/setlanguage es");
        Update update3 = createTestUpdate(300L, "/setlanguage fr");

        // Выполняем команды для разных пользователей
        setLanguageCommand.execute(update1, new String[]{"en"});
        setLanguageCommand.execute(update2, new String[]{"es"});
        setLanguageCommand.execute(update3, new String[]{"fr"});

        // Проверяем, что языки установлены правильно для каждого пользователя
        assertEquals("en", setLanguageCommand.getUserLanguage(100L));
        assertEquals("es", setLanguageCommand.getUserLanguage(200L));
        assertEquals("fr", setLanguageCommand.getUserLanguage(300L));

        // Проверяем, что пользователи не влияют друг на друга
        assertNotEquals(setLanguageCommand.getUserLanguage(100L), setLanguageCommand.getUserLanguage(200L));
        assertNotEquals(setLanguageCommand.getUserLanguage(200L), setLanguageCommand.getUserLanguage(300L));
    }

    @Test
    void execute_AfterInvalidCommand_LanguageShouldNotChange() {
        Long userId = 123L;
        Update update = createTestUpdate(userId, "/setlanguage");

        // Сначала устанавливаем валидный язык
        setLanguageCommand.execute(update, new String[]{"de"});
        assertEquals("de", setLanguageCommand.getUserLanguage(userId));

        // Пытаемся установить невалидный язык
        String result = setLanguageCommand.execute(update, new String[]{"invalid"});
        assertTrue(result.contains("❌ Неподдерживаемый язык"));

        // Язык должен остаться прежним
        assertEquals("de", setLanguageCommand.getUserLanguage(userId));
    }
}