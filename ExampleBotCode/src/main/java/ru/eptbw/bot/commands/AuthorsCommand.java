package ru.eptbw.bot.commands;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AuthorsCommand extends AbstractCommand{
    public AuthorsCommand() {
        super("/authors", "Выводит информацию об авторах");
    }

    @Override
    public String execute(Update update, String[] args) {
        return "👨‍💻 Авторы проекта:\n\n" +
                "• Разработчик: EPTBW Team\n" +
                "• Тестировщик: QA Department\n" +
                "• Идея: Innovation Lab\n\n" +
                "Связь: example@email.com";
    }
}