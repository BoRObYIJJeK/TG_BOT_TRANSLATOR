package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.eptbw.bot.CommandManager;

import static org.junit.jupiter.api.Assertions.*;

class CommandManagerTest {
    private CommandManager commandManager;
    @BeforeEach
    void setUp() {
        commandManager = new CommandManager();
    }
    @org.junit.jupiter.api.Test
    void registerCommand() {
    }

    @org.junit.jupiter.api.Test
    void isCommand_WithInvalidName() {
        assertFalse(commandManager.isCommand("/pidor"));
    }
    void isCommand_withValidName() {
        assertTrue(commandManager.isCommand("/help"));
        assertFalse(commandManager.isCommand("привет как дела?"));
    }
    @org.junit.jupiter.api.Test
    void executeCommand_UnknownCommand() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "хуй");

        assertTrue(result.contains("❌ Неизвестная команда"));
    }
    @org.junit.jupiter.api.Test
    void executeCommand_WithOutArguments() {
        Update update = createTestUpdate();
        String result = commandManager.executeCommand(update, "/about");

        assertTrue(result.contains("🤖 Мой Telegram Бот"));
    }

    private Update createTestUpdate() {
        Update update = new Update();
        Message message = new Message();
        message.setText("test");
        update.setMessage(message);
        return update;
    }
}