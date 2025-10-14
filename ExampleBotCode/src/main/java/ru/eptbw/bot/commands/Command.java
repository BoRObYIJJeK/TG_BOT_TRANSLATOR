package ru.eptbw.bot.commands;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {
    String getName();
    String getDescription();
    String execute(Update update, String[] args);
}