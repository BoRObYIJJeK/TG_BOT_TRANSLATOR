package ru.eptbw;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.eptbw.bot.TelegramBot;
import ru.eptbw.bot.commands.EchoCommand;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

        TelegramBot bot = new TelegramBot();
        // Демонстрация добавления новой команды
        bot.registerCommand(new EchoCommand());

        telegramBotsApi.registerBot(bot);
        System.out.println("Бот успешно запущен!");
    }
}