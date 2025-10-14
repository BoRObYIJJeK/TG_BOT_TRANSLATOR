package ru.eptbw.bot;

import ru.eptbw.bot.commands.Command;
import ru.eptbw.bot.commands.AboutCommand;
import ru.eptbw.bot.commands.AuthorsCommand;
import ru.eptbw.bot.commands.HelpCommand;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private final Map<String, Command> commands;

    public CommandManager() {
        this.commands = new HashMap<>();
        initializeCommands();
    }

    private void initializeCommands() {
        // Сначала создаем основные команды
        Command aboutCommand = new AboutCommand();
        Command authorsCommand = new AuthorsCommand();

        // Добавляем их в карту
        registerCommand(aboutCommand);
        registerCommand(authorsCommand);

        // HelpCommand должен быть создан после остальных команд,
        // так как он получает ссылку на все команды
        Command helpCommand = new HelpCommand(commands);
        registerCommand(helpCommand);
    }

    public void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public Command getCommand(String commandName) {
        return commands.get(commandName.toLowerCase());
    }

    public boolean isCommand(String text) {
        return text != null && text.startsWith("/") && commands.containsKey(text.split(" ")[0].toLowerCase());
    }

    public String executeCommand(Update update, String text) {
        String[] parts = text.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];

        Command command = getCommand(commandName);
        if (command != null) {
            return command.execute(update, args);
        }

        return "❌ Неизвестная команда. Используйте /help для списка команд.";
    }
}