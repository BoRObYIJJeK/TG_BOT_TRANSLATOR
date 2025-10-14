package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.junit.jupiter.api.Assertions.*;

public class EchoCommandTest {
    private EchoCommand echoCommand;

    @BeforeEach
    void setUp() {
        echoCommand = new EchoCommand();
    }

    @Test
    void execute_WithArgs_ReturnsJoinedText() {
        Update update = createTestUpdate();
        String result = echoCommand.execute(update, new String[]{"Привет", "мир"});

        assertEquals("Привет мир", result);
    }

    @Test
    void execute_WithSingleArg_ReturnsText() {
        Update update = createTestUpdate();
        String result = echoCommand.execute(update, new String[]{"Тестовое сообщение"});

        assertEquals("Тестовое сообщение", result);
    }

    @Test
    void execute_WithoutArgs_ReturnsErrorMessage() {
        Update update = createTestUpdate();
        String result = echoCommand.execute(update, new String[]{});

        assertNotNull(result);
        assertTrue(result.contains("Пожалуйста, укажите текст для повторения"));
        assertTrue(result.contains("Пример: /echo Привет, мир!"));
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("/echo", echoCommand.getName());
    }

    @Test
    void getDescription_ReturnsCorrectDescription() {
        assertEquals("Повторяет введенный текст. Использование: /echo <текст>", echoCommand.getDescription());
    }

    private Update createTestUpdate() {
        Update update = new Update();
        Message message = new Message();
        message.setText("test");
        update.setMessage(message);
        return update;
    }
}