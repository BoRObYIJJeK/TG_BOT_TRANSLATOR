package ru.eptbw.bot;

import ru.eptbw.bot.commands.*;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.eptbw.bot.gemini.GeminiService;
import ru.eptbw.bot.services.DatabaseManager;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private final Map<String, Command> commands;
    private final DatabaseManager dbManager;

    public CommandManager(GeminiService geminiService) {
        this.commands = new HashMap<>();
        this.dbManager = DatabaseManager.getInstance();
        initializeCommands(geminiService);
    }

    private void initializeCommands(GeminiService geminiService) {
        // Создаем основные команды
        Command aboutCommand = new AboutCommand();
        Command authorsCommand = new AuthorsCommand();
        Command echoCommand = new EchoCommand();

        // Создаем SetLanguageCommand
        SetLanguageCommand setLanguageCommand = new SetLanguageCommand();

        // Создаем SetTranslationCommand
        SetTranslationCommand setTranslationCommand = new SetTranslationCommand();

        // Добавляем их в карту
        registerCommand(aboutCommand);
        registerCommand(authorsCommand);
        registerCommand(echoCommand);

        // Регистрируем основную команду и все ее вариации
        registerCommand(setLanguageCommand);
        registerCommandAliases(setLanguageCommand);

        // Регистрируем команду для настройки перевода
        registerCommand(setTranslationCommand);
        registerTranslationAliases(setTranslationCommand);

        // Создаем TranslateCommand
        Command translateCommand = new TranslateCommand(geminiService);
        registerCommand(translateCommand);

        // Создаем команду профиля
        Command profileCommand = new ProfileCommand();
        registerCommand(profileCommand);

        // Создаем команду статистики
        Command statsCommand = new StatsCommand();
        registerCommand(statsCommand);

        // HelpCommand должен быть создан после остальных команд
        Command helpCommand = new HelpCommand(commands);
        registerCommand(helpCommand);
    }

    public void registerTranslationAliases(SetTranslationCommand command) {
        String[] variations = command.getCommandVariations();
        for (String variation : variations) {
            commands.put(variation.toLowerCase(), command);
        }
    }
    public void registerCommand(Command command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    // Новый метод для регистрации алиасов команды
    public void registerCommandAliases(SetLanguageCommand command) {
        String[] variations = command.getCommandVariations();
        for (String variation : variations) {
            commands.put(variation.toLowerCase(), command);
        }
    }

    public Command getCommand(String commandName) {
        return commands.get(commandName.toLowerCase());
    }

    public boolean isCommand(String text) {
        if (text == null || !text.startsWith("/")) {
            return false;
        }

        String commandName = text.split(" ")[0].toLowerCase();

        // Проверяем основную команду и все алиасы
        return commands.containsKey(commandName);
    }

    public String executeCommand(Update update, String text) {
        String[] parts = text.split(" ", 2);
        String commandName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];

        Command command = getCommand(commandName);
        if (command != null) {
            Long userId = update.getMessage().getFrom().getId();
            org.telegram.telegrambots.meta.api.objects.User user = update.getMessage().getFrom();
            String userName = getUserDisplayName(user);
            long startTime = System.currentTimeMillis();

            try {
                String result = command.execute(update, args);
                long executionTime = System.currentTimeMillis() - startTime;

                // Сохраняем успешное выполнение с именем пользователя
                dbManager.saveSuccessfulCommand(userId, userName, commandName, executionTime);

                return result;

            } catch (Exception e) {
                long executionTime = System.currentTimeMillis() - startTime;

                // Сохраняем неудачное выполнение с именем пользователя
                dbManager.saveFailedCommand(userId, userName, commandName, executionTime, e.getMessage());

                return "❌ Произошла ошибка при выполнении команды: " + e.getMessage();
            }
        }

        return "❌ Неизвестная команда. Используйте /help для списка команд.";
    }

    private String getUserDisplayName(org.telegram.telegrambots.meta.api.objects.User user) {
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            return "@" + user.getUserName();
        } else if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            return user.getFirstName() + " " + user.getLastName();
        } else {
            return user.getFirstName();
        }
    }

    public Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }
}