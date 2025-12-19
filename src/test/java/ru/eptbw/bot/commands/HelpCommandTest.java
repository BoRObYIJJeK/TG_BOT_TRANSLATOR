package ru.eptbw.bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class HelpCommandTest {
    private Map<String, Command> commands;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        commands = new HashMap<>();
        commands.put("/about", new AboutCommand());
        commands.put("/echo", new EchoCommand());
        commands.put("/authors", new AuthorsCommand());
        helpCommand = new HelpCommand(commands);
    }
    @org.junit.jupiter.api.Test
    void execute_ShowAllCommands() {
        Update update = createTestUpdate();
        String result = helpCommand.execute(update, new String[]{});

        assertTrue(result.contains("\uD83D\uDCCB Доступные команды"));
        assertTrue(result.contains("/echo"));
        assertTrue(result.contains("/about"));
        assertTrue(result.contains("/help"));

    }

    @org.junit.jupiter.api.Test
    void execute_checkCommand() {
        Update update = createTestUpdate();
        String result = helpCommand.execute(update, new String[]{"/about"});

        assertTrue(result.contains  ("/about"));
    }
    @org.junit.jupiter.api.Test
    void execute_NotACommand() {
        Update update = createTestUpdate();
        String result = helpCommand.execute(update, new String[] {"asdasd"});

        assertTrue(result.contains(" Команда 'asdasd' не найдена"));
    }

    @org.junit.jupiter.api.Test
    void testIsSorted() {
        Update update = createTestUpdate();
        String result = helpCommand.execute(update, new String[] {});

        int aboutIdx = result.indexOf("/about");
        int authorsIdx = result.indexOf("/authors");
        int echoIdx = result.indexOf("/echo");
        int helpIdx = result.indexOf("/help");

        assertTrue(aboutIdx < authorsIdx);
        assertTrue(authorsIdx < echoIdx);
        assertTrue(helpIdx > echoIdx );
    }

    private Update createTestUpdate() {
        Update update = new Update();
        Message message = new Message();
        message.setText("test");
        update.setMessage(message);
        return update;
    }
}