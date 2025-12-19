package ru.eptbw.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.eptbw.bot.cache.CacheService;
import ru.eptbw.bot.features.InlineSuggestionsService;
import ru.eptbw.bot.gemini.GeminiService;
import ru.eptbw.bot.handlers.AsyncMessageHandler;
import ru.eptbw.bot.services.AutoTranslateService;
import ru.eptbw.bot.threading.RateLimiterService;
import ru.eptbw.bot.threading.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eptbw.config.Config;

public class TelegramBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private final CommandManager commandManager;
    private final GeminiService geminiService;
    private final AutoTranslateService autoTranslateService;
    private final AsyncMessageHandler asyncHandler;
    private final InlineSuggestionsService inlineSuggestions;

    public TelegramBot() {
        this.geminiService = new GeminiService();
        this.commandManager = new CommandManager(geminiService);
        this.autoTranslateService = new AutoTranslateService(geminiService);
        this.asyncHandler = new AsyncMessageHandler(this);
        this.inlineSuggestions = new InlineSuggestionsService(this);

        // Инициализация сервисов
        CacheService.getInstance();
        RateLimiterService.getInstance();

        logger.info("TelegramBot инициализирован с многопоточной обработкой");
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Асинхронная обработка сообщений
        asyncHandler.handleUpdateAsync(update);
    }

    // Геттеры для доступа к менеджерам
    public CommandManager getCommandManager() {
        return commandManager;
    }

    public AutoTranslateService getAutoTranslateService() {
        return autoTranslateService;
    }

    public void registerCommand(ru.eptbw.bot.commands.Command command) {
        commandManager.registerCommand(command);
        logger.info("Зарегистрирована команда: {}", command.getName());
    }

    @Override
    public String getBotUsername() {
        return Config.getBotName();
    }

    @Override
    public String getBotToken() {
        return Config.getTelegramBotToken();
    }

    // Метод для корректного завершения работы
    public void shutdown() {
        logger.info("Завершение работы бота...");

        asyncHandler.shutdown();
        ThreadPoolManager.getInstance().shutdown();

        try {
            Thread.sleep(2000); // Даем время на завершение операций
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Бот завершил работу");
    }
}