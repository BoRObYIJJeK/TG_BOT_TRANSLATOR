package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.CommandManager;
import ru.eptbw.bot.gemini.GeminiService;

import static org.junit.jupiter.api.Assertions.*;

class CommandManagerTest {
    private CommandManager commandManager;

    @BeforeEach
    void setUp() {
        // Создаем тестовый GeminiService с заглушками
        GeminiService geminiService = new GeminiService("test-api-key", "test-model") {
            @Override
            public String translateText(String text, String targetLanguage) {
                return "Тестовый перевод: " + text + " на " + targetLanguage;
            }

            @Override
            public String generateResponse(String userMessage) {
                return "Тестовый ответ: " + userMessage;
            }

            @Override
            public String autoTranslateToRussian(String text) {
                return "Тестовый автоперевод: " + text;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }
        };

        commandManager = new CommandManager(geminiService);
    }

    @org.junit.jupiter.api.Test
    void registerCommand() {
        int initialSize = commandManager.getCommands().size();
        assertTrue(initialSize > 0, "Должны быть зарегистрированы базовые команды");

        // Проверяем что все ожидаемые команды зарегистрированы
        assertNotNull(commandManager.getCommand("/about"));
        assertNotNull(commandManager.getCommand("/authors"));
        assertNotNull(commandManager.getCommand("/echo"));
        assertNotNull(commandManager.getCommand("/setlanguage"));
        assertNotNull(commandManager.getCommand("/translate"));
        assertNotNull(commandManager.getCommand("/help"));
    }

    @org.junit.jupiter.api.Test
    void isCommand_WithInvalidName() {
        assertFalse(commandManager.isCommand("/shrek"));
    }

    @org.junit.jupiter.api.Test
    void isCommand_withValidName() {
        assertTrue(commandManager.isCommand("/help"));
        assertTrue(commandManager.isCommand("/about"));
        assertTrue(commandManager.isCommand("/authors"));
        assertTrue(commandManager.isCommand("/echo"));
        assertTrue(commandManager.isCommand("/setlanguage"));
        assertTrue(commandManager.isCommand("/translate"));
        assertFalse(commandManager.isCommand("привет как дела?"));
        assertFalse(commandManager.isCommand(null));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_UnknownCommand() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/unknowncommand");

        assertTrue(result.contains("❌ Неизвестная команда"));
        assertTrue(result.contains("/help"));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_WithOutArguments() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/about");

        assertTrue(result.contains("🤖 Мой Telegram Бот"));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_WithArguments() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/setlanguage");

        // Проверяем, что команда setlanguage работает без аргументов
        assertTrue(result.contains("🌍 Текущий язык") || result.contains("Поддерживаемые языки"));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_TranslateCommand() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/translate hello");

        // Проверяем, что команда translate работает
        assertFalse(result.contains("❌ Неизвестная команда"));
        assertNotNull(result);
        // Должен вернуть тестовый перевод
        assertTrue(result.contains("Тестовый перевод:"));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_EchoCommand() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/echo test message");

        assertEquals("test message", result);
    }

    @org.junit.jupiter.api.Test
    void executeCommand_EchoCommandWithoutArgs() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/echo");

        assertTrue(result.contains("❌ Пожалуйста, укажите текст для повторения"));
        assertTrue(result.contains("/echo Привет, мир!"));
    }

    @org.junit.jupiter.api.Test
    void getCommand_ExistingCommand() {
        assertNotNull(commandManager.getCommand("/about"));
        assertNotNull(commandManager.getCommand("/help"));
        assertNotNull(commandManager.getCommand("/translate"));
        assertNotNull(commandManager.getCommand("/setlanguage"));
        assertNotNull(commandManager.getCommand("/authors"));
        assertNotNull(commandManager.getCommand("/echo"));
    }

    @org.junit.jupiter.api.Test
    void getCommand_NonExistingCommand() {
        assertNull(commandManager.getCommand("/nonexistent"));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_HelpCommand() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/help");

        assertTrue(result.contains("📋 Доступные команды"));
        assertTrue(result.contains("/about"));
        assertTrue(result.contains("/help"));
        assertTrue(result.contains("/translate"));
        assertTrue(result.contains("/echo"));
    }

    @org.junit.jupiter.api.Test
    void executeCommand_AuthorsCommand() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/authors");

        assertTrue(result.contains("👨‍💻 Авторы проекта"));
        assertTrue(result.contains("Матвей Богданов") || result.contains("Вадим Дерябин"));
    }

    private Update createTestUpdate() {
        Update update = new Update();
        Message message = new Message();

        // Создаем пользователя
        User user = new User();
        user.setId(12345L);
        user.setFirstName("Test");
        user.setIsBot(false);

        message.setFrom(user);
        message.setText("test");
        update.setMessage(message);
        return update;
    }
}