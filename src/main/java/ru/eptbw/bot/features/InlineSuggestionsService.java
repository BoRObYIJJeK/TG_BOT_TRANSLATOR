package ru.eptbw.bot.features;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import ru.eptbw.bot.TelegramBot;
import ru.eptbw.bot.commands.Command;
import ru.eptbw.config.Config;

import java.util.ArrayList;
import java.util.List;

public class InlineSuggestionsService {
    private static final Logger logger = LoggerFactory.getLogger(InlineSuggestionsService.class);
    private final TelegramBot bot;

    public InlineSuggestionsService(TelegramBot bot) {
        this.bot = bot;
        if (Config.isInlineSuggestionsEnabled()) {
            setupBotCommands();
        }
    }

    private void setupBotCommands() {
        try {
            List<BotCommand> commands = new ArrayList<>();

            // Основные команды для меню
            commands.add(new BotCommand("/start", "Запустить бота"));
            commands.add(new BotCommand("/help", "Помощь и список команд"));
            commands.add(new BotCommand("/translate", "Перевести текст"));
            commands.add(new BotCommand("/setlanguage", "Установить язык интерфейса"));
            commands.add(new BotCommand("/settranslation", "Установить язык перевода"));
            commands.add(new BotCommand("/profile", "Показать профиль"));
            commands.add(new BotCommand("/stats", "Статистика переводов"));
            commands.add(new BotCommand("/about", "О боте"));
            commands.add(new BotCommand("/authors", "Авторы"));

            SetMyCommands setCommands = new SetMyCommands();
            setCommands.setCommands(commands);
            setCommands.setScope(new BotCommandScopeDefault());

            bot.execute(setCommands);
            logger.info("Меню команд настроено успешно");

        } catch (Exception e) {
            logger.error("Ошибка настройки меню команд", e);
        }
    }

    public List<String> getCommandSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        // Получаем все команды из CommandManager
        var allCommands = bot.getCommandManager().getCommands();

        for (Command command : allCommands.values()) {
            String name = command.getName().toLowerCase();
            String description = command.getDescription().toLowerCase();

            // Проверяем совпадение с именем или описанием
            if (name.contains(lowerInput) || description.contains(lowerInput)) {
                suggestions.add(String.format("%s - %s", command.getName(), command.getDescription()));
            }
        }

        // Ограничиваем количество подсказок
        return suggestions.stream().limit(5).toList();
    }

    public String formatCommandWithTooltip(String commandName, String description) {
        return String.format("<code>%s</code> - <i>%s</i>", commandName, description);
    }
}