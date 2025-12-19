package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.services.DatabaseManager;

import java.util.Map;

public class ProfileCommand extends AbstractCommand {
    private final DatabaseManager dbManager;

    public ProfileCommand() {
        super("/profile", "Показать профиль пользователя");
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public String execute(Update update, String[] args) {
        User user = update.getMessage().getFrom();
        Long userId = user.getId();

        Map<String, String> userInfo = dbManager.getUserInfo(userId);
        Map<String, String> stats = dbManager.getUserTranslationStats(userId);

        String userName = getUserDisplayName(user);
        String interfaceLang = userInfo.getOrDefault("language_code", "ru");
        String translationLang = userInfo.getOrDefault("translation_language", "en");

        String interfaceLangName = getLanguageDisplayName(interfaceLang);
        String translationLangName = getTranslationLanguageDisplayName(translationLang);

        return String.format("""
            👤 **Профиль пользователя**
            
            **Имя:** %s
            **ID:** %d
            
            🌍 **Настройки языка:**
            • Интерфейс: %s (%s)
            • Автоперевод: %s (%s)
            
            📊 **Статистика переводов:**
            • Всего переводов: %s
            • Уникальных языков: %s
            • Последний перевод: %s
            
            💡 Для изменения языка интерфейса: /setlanguage <язык>
            💡 Для изменения языка перевода: /settranslation <язык>
            💡 Для быстрого перевода: /translate <язык> <текст>
            """,
                userName,
                userId,
                interfaceLangName, interfaceLang,
                translationLangName, translationLang,
                stats.getOrDefault("total_translations", "0"),
                stats.getOrDefault("unique_languages", "0"),
                stats.getOrDefault("last_translation", "никогда")
        );
    }

    private String getUserDisplayName(User user) {
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            return "@" + user.getUserName();
        } else if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            return user.getFirstName() + " " + user.getLastName();
        } else {
            return user.getFirstName();
        }
    }

    private String getLanguageDisplayName(String languageCode) {
        java.util.Map<String, String> languages = java.util.Map.of(
                "ru", "Русский",
                "en", "English",
                "es", "Español",
                "fr", "Français",
                "de", "Deutsch"
        );
        return languages.getOrDefault(languageCode, "Русский");
    }

    private String getTranslationLanguageDisplayName(String languageCode) {
        java.util.Map<String, String> languages = java.util.Map.of(
                "en", "английский",
                "ru", "русский",
                "es", "испанский",
                "fr", "французский",
                "de", "немецкий"
        );
        return languages.getOrDefault(languageCode, "английский");
    }
}