package ru.eptbw.bot.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.eptbw.bot.TelegramBot;
import ru.eptbw.bot.cache.CacheService;
import ru.eptbw.bot.messaging.MessageQueueService;
import ru.eptbw.bot.threading.RateLimiterService;
import ru.eptbw.bot.threading.ThreadPoolManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AsyncMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageHandler.class);
    private final TelegramBot bot;
    private final CacheService cacheService;
    private final RateLimiterService rateLimiter;
    private final ConcurrentHashMap<Long, Long> lastUserMessageTime = new ConcurrentHashMap<>();
    private static final long USER_COOLDOWN_MS = 1000;
    private boolean initialized = false;

    public AsyncMessageHandler(TelegramBot bot) {
        this.bot = bot;
        this.cacheService = CacheService.getInstance();
        this.rateLimiter = RateLimiterService.getInstance();

        // Инициализация очереди сообщений
        try {
            MessageQueueService.initialize(bot);
            initialized = true;
            logger.info("AsyncMessageHandler успешно инициализирован");
        } catch (Exception e) {
            logger.error("Ошибка инициализации AsyncMessageHandler", e);
            initialized = false;
        }
    }

    public void handleUpdateAsync(Update update) {
        if (!initialized) {
            logger.error("AsyncMessageHandler не инициализирован, обработка невозможна");
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long userId = update.getMessage().getFrom().getId();
        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText().trim();

        logger.info("Получено сообщение от {} ({}): {}", userId, chatId, text);

        // Проверка кулдауна пользователя
        if (isUserInCooldown(userId)) {
            logger.debug("Пользователь {} в кулдауне, пропускаем сообщение", userId);
            return;
        }

        // Проверка rate limiting
        if (!rateLimiter.allowRequest(userId, "message")) {
            sendRateLimitMessage(chatId, userId);
            return;
        }

        // Асинхронная обработка
        ThreadPoolManager.getInstance().submitTask(() -> {
            try {
                logger.debug("Начата обработка сообщения от пользователя {}", userId);
                processMessage(update, userId, chatId, text);
                rateLimiter.recordSuccess(userId);
                logger.debug("Завершена обработка сообщения от пользователя {}", userId);
            } catch (Exception e) {
                logger.error("Ошибка обработки сообщения от пользователя {}", userId, e);
                rateLimiter.recordFailure(userId);
                sendErrorMessage(chatId);
            }

            return null;
        });
    }

    private void processMessage(Update update, Long userId, String chatId, String text) {
        logger.debug("Обработка сообщения: userId={}, text={}", userId, text);

        // Проверка кеша команд
        String cacheKey = cacheService.getCommandCacheKey(userId, text, null);
        String cachedResponse = cacheService.get(cacheKey);

        if (cachedResponse != null) {
            logger.debug("Используем кешированный ответ для пользователя {}", userId);
            sendMessageAsync(chatId, cachedResponse);
            return;
        }

        // Обработка команды или текста
        String response;
        if (text.startsWith("/")) {
            logger.debug("Обработка команды: {}", text);
            response = bot.getCommandManager().executeCommand(update, text);
        } else {
            logger.debug("Обработка текста для автоперевода");
            response = bot.getAutoTranslateService().autoTranslate(update);
        }

        logger.debug("Сгенерирован ответ длиной {} символов",
                response != null ? response.length() : 0);

        // Кеширование ответа
        if (response != null && !response.contains("❌") && !response.contains("⚠️")) {
            cacheService.put(cacheKey, response, 5); // Кешируем на 5 минут
        }

        // Асинхронная отправка ответа
        sendMessageAsync(chatId, response);

        // Обновление времени последнего сообщения
        lastUserMessageTime.put(userId, System.currentTimeMillis());
    }

    private boolean isUserInCooldown(Long userId) {
        Long lastTime = lastUserMessageTime.get(userId);
        if (lastTime == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - lastTime;
        return elapsed < USER_COOLDOWN_MS;
    }

    private void sendRateLimitMessage(String chatId, Long userId) {
        String message = "⏳ Вы отправляете сообщения слишком быстро. Пожалуйста, подождите немного.\n\n" +
                rateLimiter.getUserStats(userId);

        sendMessageSync(chatId, message); // Используем синхронную отправку для важных сообщений
    }

    private void sendErrorMessage(String chatId) {
        String message = "❌ Произошла ошибка при обработке вашего сообщения. Пожалуйста, попробуйте позже.";
        sendMessageSync(chatId, message);
    }

    private void sendMessageAsync(String chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Попытка отправки пустого сообщения в чат {}", chatId);
            return;
        }

        logger.debug("Отправка асинхронного сообщения в чат {} (длина: {})",
                chatId, text.length());

        MessageQueueService.getInstance().sendMessageAsync(chatId, text)
                .thenAccept(success -> {
                    if (success) {
                        logger.debug("Сообщение успешно отправлено в чат {}", chatId);
                    } else {
                        logger.error("Не удалось отправить сообщение в чат {}", chatId);
                    }
                })
                .exceptionally(e -> {
                    logger.error("Ошибка отправки сообщения в чат {}", chatId, e);
                    // Пробуем отправить синхронно как запасной вариант
                    sendMessageSync(chatId, text);
                    return null;
                });
    }

    private void sendMessageSync(String chatId, String text) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendMessage message =
                    new org.telegram.telegrambots.meta.api.methods.send.SendMessage();
            message.setChatId(chatId);
            message.setText(text);

            bot.execute(message);
            logger.debug("Синхронное сообщение отправлено в чат {}", chatId);
        } catch (Exception e) {
            logger.error("Ошибка синхронной отправки сообщения в чат {}", chatId, e);
        }
    }

    public void shutdown() {
        logger.info("Завершение AsyncMessageHandler...");
        if (initialized) {
            MessageQueueService.getInstance().shutdown();
        }
    }
}