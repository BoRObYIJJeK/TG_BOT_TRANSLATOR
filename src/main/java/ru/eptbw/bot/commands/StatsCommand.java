package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.services.DatabaseManager;

import java.util.Map;

public class StatsCommand extends AbstractCommand {
    private final DatabaseManager dbManager;

    public StatsCommand() {
        super("/stats", "Показать вашу статистику переводов");
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public String execute(Update update, String[] args) {
        User user = update.getMessage().getFrom();
        Long userId = user.getId();

        Map<String, String> stats = dbManager.getUserTranslationStats(userId);

        String lastTranslationLang = dbManager.getLastTranslationLanguage(userId);
        String lastLangDisplay = getTranslationLanguageDisplayName(lastTranslationLang);

        return String.format("""
            📊 **Ваша статистика переводов**
            
            • **Всего переводов:** %s
            • **Уникальных языков:** %s
            • **Последний перевод:** %s
            
            🌍 **Последний язык перевода:** %s (%s)
            
            💡 Просто отправьте текст без команды для автоматического перевода на этот язык!
            """,
                stats.getOrDefault("total_translations", "0"),
                stats.getOrDefault("unique_languages", "0"),
                stats.getOrDefault("last_translation", "никогда"),
                lastLangDisplay, lastTranslationLang
        );
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
