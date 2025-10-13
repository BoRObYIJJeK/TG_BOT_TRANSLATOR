package ru.eptbw.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {
    private final ru.eptbw.bot.CommandManager commandManager;

    public TelegramBot() {
        this.commandManager = new ru.eptbw.bot.CommandManager();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText().trim();
        String response;

        if (commandManager.isCommand(text)) {
            response = commandManager.executeCommand(update, text);
        } else {
            response = "👋 Привет! Я бот, который умеет выполнять команды.\n" +
                    "Введите /help для получения списка доступных команд.";
        }

        sendMessage(chatId, response);
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    // Метод для добавления новых команд (для расширения функциональности)
    public void registerCommand(ru.eptbw.bot.commands.Command command) {
        commandManager.registerCommand(command);
    }

    @Override
    public String getBotUsername() {
        return "eptbw_bot";
    }

    @Override
    public String getBotToken() {
        return ".....";
    }
}