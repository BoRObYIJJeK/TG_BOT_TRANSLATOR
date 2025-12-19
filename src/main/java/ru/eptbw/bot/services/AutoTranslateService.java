package ru.eptbw.bot.services;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.gemini.GeminiService;
import ru.eptbw.bot.services.DatabaseManager;

public class AutoTranslateService {
    private final GeminiService geminiService;
    private final DatabaseManager dbManager;

    public AutoTranslateService(GeminiService geminiService) {
        this.geminiService = geminiService;
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Автоматический перевод текста на последний использованный язык пользователя
     */
    public String autoTranslate(Update update) {
        String text = update.getMessage().getText().trim();
        User user = update.getMessage().getFrom();
        Long userId = user.getId();

        if (text.length() > 1000) {
            return "❌ Текст слишком длинный. Максимальная длина - 1000 символов.";
        }

        // Получаем последний язык перевода пользователя
        String targetLanguage = dbManager.getUserTranslationLanguage(userId);
        String languageName = getLanguageDisplayName(targetLanguage);

        try {
            String translatedText = geminiService.translateText(text, getGeminiLanguageName(targetLanguage));

            // Сохраняем в историю
            String userName = getUserDisplayName(user);
            String sourceLang = getSourceLanguage(targetLanguage);

            dbManager.saveTranslation(userId, userName, text, translatedText, sourceLang, targetLanguage);

            return String.format("""
                🌍 **Автоматический перевод** (%s):
                
                %s
                
                💡 *Совет:* Используйте `/translate <язык> <текст>` для перевода на другой язык.
                """,
                    languageName, translatedText
            );

        } catch (Exception e) {
            return "❌ Ошибка при автоматическом переводе. Попробуйте использовать команду /translate.";
        }
    }

    private String getGeminiLanguageName(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "en": return "english";
            case "ru": return "russian";
            case "es": return "spanish";
            case "fr": return "french";
            case "de": return "german";
            default: return "english";
        }
    }

    private String getLanguageDisplayName(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "en": return "английский";
            case "ru": return "русский";
            case "es": return "испанский";
            case "fr": return "французский";
            case "de": return "немецкий";
            default: return "английский";
        }
    }

    private String getSourceLanguage(String targetLanguage) {
        return targetLanguage.equals("ru") ? "auto" : "russian";
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

    /**
     * Проверить, является ли текст командой
     */
    public boolean isCommand(String text) {
        return text != null && text.startsWith("/");
    }
}