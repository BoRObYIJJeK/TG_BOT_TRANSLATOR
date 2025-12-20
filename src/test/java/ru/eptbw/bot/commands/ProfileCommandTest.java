package ru.eptbw.bot.commands;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.junit.jupiter.api.Assertions.*;

class ProfileCommandSimpleTest{

    @Test
    void testProfileCommandShowsBasicInfo() {
        // 1. Создаем тестовые данные
        Long userId = 123456789L;
        String userName = "test_user";
        String firstName = "Иван";

        // 2. Создаем объекты
        User user = new User();
        user.setId(userId);
        user.setUserName(userName);
        user.setFirstName(firstName);

        Message message = new Message();
        message.setFrom(user);

        Update update = new Update();
        update.setMessage(message);

        // 3. Создаем команду
        ProfileCommand command = new ProfileCommand();

        // 4. Выполняем команду
        String result = command.execute(update, new String[]{});

        // 5. Проверяем результат
        assertNotNull(result, "Результат не должен быть null");
        assertTrue(result.contains("Профиль пользователя"), "Должно содержать заголовок профиля");
        assertTrue(result.contains(String.valueOf(userId)), "Должно содержать ID пользователя");
        assertTrue(result.contains("@" + userName), "Должно содержать username");
        assertTrue(result.contains(firstName), "Должно содержать имя");
    }
}