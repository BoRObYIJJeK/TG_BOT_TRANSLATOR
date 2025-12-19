package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.services.DatabaseManager;

import java.util.Map;

public class SetLanguageCommand extends AbstractCommand {
    private final DatabaseManager dbManager;

    // Поддерживаемые языки
    private final Map<String, String> supportedLanguages = Map.of(
            "ru", "Русский",
            "en", "English",
            "es", "Español",
            "fr", "Français",
            "de", "Deutsch"
    );

    // Языки для переводов
    private final Map<String, String> translationLanguages = Map.of(
            "en", "английский",
            "ru", "русский",
            "es", "испанский",
            "fr", "французский",
            "de", "немецкий"
    );

    public SetLanguageCommand() {
        super("/setlanguage", "Установить язык интерфейса и автоперевода (ru, en, es, fr, de)");
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public String execute(Update update, String[] args) {
        if (args == null || args.length == 0) {
            Long userId = update.getMessage().getFrom().getId();
            return showCurrentLanguages(userId) + "\n\n" + getSupportedLanguagesHelp();
        }

        if (args[0] == null) {
            Long userId = update.getMessage().getFrom().getId();
            return "❌ Неверный код языка.\n\n" + getSupportedLanguagesHelp();
        }

        Long userId = update.getMessage().getFrom().getId();
        String languageCode = args[0].toLowerCase();
        return setUserLanguage(userId, languageCode, update.getMessage().getFrom());
    }

    private String showCurrentLanguages(Long userId) {
        String currentLang = dbManager.getUserLanguage(userId);
        String translationLang = dbManager.getUserTranslationLanguage(userId);
        String langName = validateAndGetLanguageName(currentLang);
        String translationLangName = getTranslationLanguageDisplayName(translationLang);

        return """
            🌍 **Настройки языка:**
            
            • **Язык интерфейса:** %s (%s)
            • **Язык автоперевода:** %s (%s)
            
            💡 Теперь при смене языка интерфейса автоматически меняется и язык перевода!
            """.formatted(langName, currentLang, translationLangName, translationLang);
    }

    private String setUserLanguage(Long userId, String languageCode, User user) {
        if (!supportedLanguages.containsKey(languageCode)) {
            return "❌ Неподдерживаемый язык. Доступные языки:\n" + getSupportedLanguagesHelp();
        }

        // Сохраняем язык интерфейса в БД с информацией о пользователе
        dbManager.setUserLanguage(
                userId,
                languageCode,
                user.getUserName(),
                user.getFirstName(),
                user.getLastName()
        );

        // ВАЖНОЕ ИСПРАВЛЕНИЕ: Устанавливаем язык для автоперевода
        // По умолчанию: если русский интерфейс → английский перевод, и наоборот
        String translationLanguage = getDefaultTranslationLanguage(languageCode);
        dbManager.setUserTranslationLanguage(userId, translationLanguage);

        String langName = validateAndGetLanguageName(languageCode);
        String translationLangName = getTranslationLanguageDisplayName(translationLanguage);
        String userName = getUserDisplayName(user);

        String response = "👤 " + userName + "\n";
        response += switch (languageCode) {
            case "en" -> "✅ Interface language set to: " + langName;
            case "es" -> "✅ Idioma de interfaz establecido: " + langName;
            case "fr" -> "✅ Langue de l'interface définie: " + langName;
            case "de" -> "✅ Interface-Sprache eingestellt: " + langName;
            default -> "✅ Язык интерфейса установлен: " + langName;
        };

        response += "\n✅ Язык автоперевода установлен: " + translationLangName;
        response += "\n\nТеперь все тексты без команд будут автоматически переводиться на " + translationLangName + "!";

        return response;
    }

    private String getDefaultTranslationLanguage(String interfaceLanguage) {
        // По умолчанию: если русский интерфейс → английский перевод
        // Если английский интерфейс → русский перевод
        // Для других языков → английский перевод
        switch (interfaceLanguage) {
            case "ru": return "en";
            case "en": return "ru";
            default: return "en";
        }
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

    public String validateAndGetLanguageName(String languageCode) {
        if (languageCode == null) {
            return "Русский";
        }
        return supportedLanguages.getOrDefault(languageCode, "Русский");
    }

    private String getTranslationLanguageDisplayName(String languageCode) {
        return translationLanguages.getOrDefault(languageCode, "английский");
    }

    public String getSupportedLanguagesHelp() {
        StringBuilder sb = new StringBuilder("📚 Поддерживаемые языки интерфейса:\n");
        supportedLanguages.forEach((code, name) ->
                sb.append("• ").append(code).append(" - ").append(name).append("\n")
        );
        sb.append("\n💡 **При смене языка интерфейса автоматически меняется язык автоперевода:**");
        sb.append("\n• ru → en (русский интерфейс, перевод на английский)");
        sb.append("\n• en → ru (английский интерфейс, перевод на русский)");
        sb.append("\n• другие → en (другие языки, перевод на английский)");
        sb.append("\n\nИспользование: /setlanguage <код_языка>");
        return sb.toString();
    }

    public String getUserLanguage(Long userId) {
        return dbManager.getUserLanguage(userId);
    }

    // Метод для получения вариаций команды
    public String[] getCommandVariations() {
        return new String[] {
                "/setlanguage", "/setLanguage", "/setlang",
                "/language", "/lang", "/язык"
        };
    }
}