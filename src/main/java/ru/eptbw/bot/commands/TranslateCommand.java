package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.eptbw.bot.gemini.GeminiService;
import ru.eptbw.bot.services.DatabaseManager;

public class TranslateCommand extends AbstractCommand {
    private final GeminiService geminiService;
    private final DatabaseManager dbManager;

    public TranslateCommand(GeminiService geminiService) {
        super("/translate", "Перевести текст. Использование: /translate <текст> или /translate <язык> <текст>");
        this.geminiService = geminiService;
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public String execute(Update update, String[] args) {
        if (args.length == 0) {
            return getUsageHelp();
        }

        Long userId = update.getMessage().getFrom().getId();
        String targetLanguage = "english";
        String textToTranslate;

        if (args.length >= 2 && isValidLanguageCode(args[0])) {
            targetLanguage = getLanguageName(args[0]);
            textToTranslate = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        } else {
            // Используем последний язык перевода пользователя
            String userTranslationLang = dbManager.getUserTranslationLanguage(userId);
            targetLanguage = getLanguageName(userTranslationLang);
            textToTranslate = String.join(" ", args);
        }

        if (textToTranslate.length() > 1000) {
            return "❌ Текст слишком длинный. Максимальная длина - 1000 символов.";
        }

        try {
            String translatedText = geminiService.translateText(textToTranslate, targetLanguage);

            // Сохраняем в историю переводов с именем пользователя
            String userName = getUserDisplayName(update.getMessage().getFrom());
            String sourceLang = getSourceLanguage(targetLanguage);

            dbManager.saveTranslation(userId, userName, textToTranslate, translatedText, sourceLang, targetLanguage);

            return "🌍 Перевод на " + getLanguageDisplayName(targetLanguage) + ":\n\n" + translatedText;

        } catch (Exception e) {
            return "❌ Ошибка при переводе через Gemini. Пожалуйста, попробуйте позже.";
        }
    }

    private String getSourceLanguage(String targetLanguage) {
        return targetLanguage.equals("russian") ? "auto" : "russian";
    }

    private String getUsageHelp() {
        return """
            ❌ Неправильный формат команды.
            
            📝 **Использование:**
            • `/translate Привет мир` - перевести на последний использованный язык
            • `/translate en Привет мир` - перевести на английский
            
            🌍 **Поддерживаемые языки:**
            en - English, ru - Русский, es - Español, fr - Français, de - Deutsch
            
            💡 **Совет:** Просто отправьте текст без команды для автоматического перевода!
            """;
    }

    private boolean isValidLanguageCode(String code) {
        String[] supportedLanguages = {"en", "ru", "es", "fr", "de"};
        for (String lang : supportedLanguages) {
            if (lang.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    private String getLanguageName(String code) {
        switch (code.toLowerCase()) {
            case "en": return "english";
            case "ru": return "russian";
            case "es": return "spanish";
            case "fr": return "french";
            case "de": return "german";
            default: return "english";
        }
    }

    private String getLanguageDisplayName(String language) {
        switch (language.toLowerCase()) {
            case "english": return "английский";
            case "russian": return "русский";
            case "spanish": return "испанский";
            case "french": return "французский";
            case "german": return "немецкий";
            default: return language;
        }
    }

    private String getUserDisplayName(org.telegram.telegrambots.meta.api.objects.User user) {
        if (user.getUserName() != null && !user.getUserName().isEmpty()) {
            return "@" + user.getUserName();
        } else if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            return user.getFirstName() + " " + user.getLastName();
        } else {
            return user.getFirstName();
        }
    }
}