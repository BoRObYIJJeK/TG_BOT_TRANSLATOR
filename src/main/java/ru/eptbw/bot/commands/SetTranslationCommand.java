package ru.eptbw.bot.commands;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.eptbw.bot.services.DatabaseManager;

import java.util.Map;

public class SetTranslationCommand extends AbstractCommand {
    private final DatabaseManager dbManager;

    // Языки для переводов
    private final Map<String, String> translationLanguages = Map.of(
            "en", "английский",
            "ru", "русский",
            "es", "испанский",
            "fr", "французский",
            "de", "немецкий"
    );

    public SetTranslationCommand() {
        super("/settranslation", "Установить язык для автоперевода отдельно от интерфейса");
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public String execute(Update update, String[] args) {
        if (args == null || args.length == 0) {
            Long userId = update.getMessage().getFrom().getId();
            return showCurrentTranslationLanguage(userId) + "\n\n" + getSupportedLanguagesHelp();
        }

        String languageCode = args[0].toLowerCase();

        if (!translationLanguages.containsKey(languageCode)) {
            return "❌ Неподдерживаемый язык. Доступные языки:\n" + getSupportedLanguagesHelp();
        }

        Long userId = update.getMessage().getFrom().getId();
        User user = update.getMessage().getFrom();

        return setTranslationLanguage(userId, languageCode, user);
    }

    private String showCurrentTranslationLanguage(Long userId) {
        String currentLang = dbManager.getUserTranslationLanguage(userId);
        String interfaceLang = dbManager.getUserLanguage(userId);
        String langName = translationLanguages.getOrDefault(currentLang, "английский");
        String interfaceLangName = getInterfaceLanguageName(interfaceLang);

        return String.format("""
            🌍 **Текущие настройки перевода:**
            
            • **Язык интерфейса:** %s (%s)
            • **Язык автоперевода:** %s (%s)
            
            💡 Просто отправьте текст без команды для перевода на %s.
            """, interfaceLangName, interfaceLang, langName, currentLang, langName);
    }

    private String setTranslationLanguage(Long userId, String languageCode, User user) {
        // Устанавливаем язык перевода
        dbManager.setUserTranslationLanguage(userId, languageCode);

        String langName = translationLanguages.get(languageCode);
        String userName = getUserDisplayName(user);

        return String.format("""
            👤 %s
            ✅ Язык для автоперевода установлен: %s (%s)
            
            Теперь все тексты без команд будут автоматически переводиться на %s.
            
            ℹ️ Язык интерфейса остался без изменений.
            """, userName, langName, languageCode, langName);
    }

    private String getInterfaceLanguageName(String languageCode) {
        Map<String, String> interfaceLanguages = Map.of(
                "ru", "Русский",
                "en", "English",
                "es", "Español",
                "fr", "Français",
                "de", "Deutsch"
        );
        return interfaceLanguages.getOrDefault(languageCode, "Русский");
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

    private String getSupportedLanguagesHelp() {
        StringBuilder sb = new StringBuilder("📚 Поддерживаемые языки для переводов:\n");
        translationLanguages.forEach((code, name) ->
                sb.append("• ").append(code).append(" - ").append(name).append("\n")
        );
        sb.append("\nИспользование: /settranslation <код_языка>");
        sb.append("\n\n💡 Или используйте /translate <язык> <текст> для быстрого перевода");
        return sb.toString();
    }

    // Метод для получения вариаций команды
    public String[] getCommandVariations() {
        return new String[] {
                "/settranslation", "/settrans", "/translang",
                "/translation", "/autotranslate", "/автоперевод"
        };
    }
}