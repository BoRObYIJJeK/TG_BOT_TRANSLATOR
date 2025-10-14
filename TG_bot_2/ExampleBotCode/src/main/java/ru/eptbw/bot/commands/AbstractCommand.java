package ru.eptbw.bot.commands;
import org.telegram.telegrambots.meta.api.objects.Update;

public abstract class AbstractCommand  implements Command {
    private final String name;
    private final String description;

    protected AbstractCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}