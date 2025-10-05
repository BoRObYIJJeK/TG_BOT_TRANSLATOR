package ru.eptbw.bot.commands;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AboutCommand extends AbstractCommand {
    public AboutCommand() {
        super("/about", "Выводит краткую информацию о боте");
    }

    @Override
    public String execute(Update update, String[] args) {
        return "🤖 Мой Telegram Бот\n\n" +
                "Это умный бот, созданный для демонстрации возможностей Telegram Bot API.\n" +
                "Версия: 1.0\n" +
                "Функционал будет расширяться!";
    }
}