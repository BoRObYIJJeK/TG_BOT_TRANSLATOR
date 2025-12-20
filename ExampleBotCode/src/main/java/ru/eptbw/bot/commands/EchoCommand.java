package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;

public class EchoCommand extends AbstractCommand {
    public EchoCommand() {
        super("/echo", "Повторяет введенный текст. Использование: /echo <текст>");
    }

    @Override
    public String execute(Update update, String[] args) {
        if (args.length == 0) {
            return "❌ Пожалуйста, укажите текст для повторения.\n" +
                    "Пример: /echo Привет, мир!";

        }

        return String.join(" ", args);
    }
}