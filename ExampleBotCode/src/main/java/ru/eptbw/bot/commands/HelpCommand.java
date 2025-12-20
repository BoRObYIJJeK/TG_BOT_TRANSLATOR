package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.Map;

public class HelpCommand extends AbstractCommand {
    private final Map<String, Command> commands;

    public HelpCommand(Map<String, Command> commands) {
        super("/help", "Выводит информацию о командах. Использование: /help [команда]");
        this.commands = commands;
    }

    @Override
    public String execute(Update update, String[] args) {
        if (args.length > 0) {
            String commandName = args[0].toLowerCase();
            Command command = commands.get(commandName);
            if (command != null) {
                return "📖 Справка по команде " + command.getName() + ":\n\n" +
                        command.getDescription();
            } else {
                return "❌ Команда '" + args[0] + "' не найдена.\n" +
                        "Используйте /help для списка всех команд.";
            }
        }

        StringBuilder helpText = new StringBuilder("📋 Доступные команды:\n\n");
        commands.values().stream()
                .sorted((c1, c2) -> c1.getName().compareTo(c2.getName()))
                .forEach(command ->
                        helpText.append("• ").append(command.getName())
                                .append(" - ").append(command.getDescription())
                                .append("\n")
                );

        helpText.append("\nℹ️ Для подробной информации используйте: /help <команда>");

        return helpText.toString();
    }
}