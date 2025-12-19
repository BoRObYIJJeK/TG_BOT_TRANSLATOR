package ru.eptbw.bot.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.eptbw.bot.TelegramBot;
import ru.eptbw.bot.threading.ThreadPoolManager;
import ru.eptbw.config.Config;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageQueueService {
    private static final Logger logger = LoggerFactory.getLogger(MessageQueueService.class);
    private static MessageQueueService instance;

    private final BlockingQueue<MessageTask> messageQueue = new LinkedBlockingQueue<>(1000);
    private final TelegramBot bot;
    private final int maxRetries;
    private final long retryDelay;
    private final AtomicInteger pendingMessages = new AtomicInteger(0);
    private final AtomicInteger sentMessages = new AtomicInteger(0);
    private final AtomicInteger failedMessages = new AtomicInteger(0);

    private MessageQueueService(TelegramBot bot) {
        this.bot = bot;
        this.maxRetries = Config.getMaxRetryAttempts();
        this.retryDelay = Config.getRetryDelayMs();

        // Запускаем обработчики сообщений
        startMessageWorkers();

        logger.info("MessageQueueService инициализирован для бота {}", bot.getBotUsername());
    }

    public static synchronized void initialize(TelegramBot bot) {
        if (instance == null) {
            instance = new MessageQueueService(bot);
        }
    }

    public static MessageQueueService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MessageQueueService не инициализирован");
        }
        return instance;
    }

    public CompletableFuture<Boolean> sendMessageAsync(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        return sendMessageAsync(message);
    }

    public CompletableFuture<Boolean> sendMessageAsync(SendMessage message) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        MessageTask task = new MessageTask(message, future, maxRetries);

        if (messageQueue.offer(task)) {
            pendingMessages.incrementAndGet();
            future.whenComplete((result, error) -> {
                pendingMessages.decrementAndGet();
                if (Boolean.TRUE.equals(result)) {
                    sentMessages.incrementAndGet();
                } else {
                    failedMessages.incrementAndGet();
                }
            });
        } else {
            future.completeExceptionally(new RejectedExecutionException("Очередь сообщений переполнена"));
        }

        return future;
    }

    private void startMessageWorkers() {
        int workerCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        for (int i = 0; i < workerCount; i++) {
            ThreadPoolManager.getInstance().submitTask(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        MessageTask task = messageQueue.take();
                        processMessageTask(task);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.error("Ошибка в обработчике сообщений", e);
                    }
                }
                return null;
            });
        }

        logger.info("Запущено {} обработчиков сообщений", workerCount);
    }

    private void processMessageTask(MessageTask task) {
        for (int attempt = 0; attempt <= task.maxRetries; attempt++) {
            try {
                bot.execute(task.message);
                task.future.complete(true);
                logger.debug("Сообщение отправлено успешно (попытка {})", attempt + 1);
                return;
            } catch (TelegramApiException e) {
                logger.warn("Ошибка отправки сообщения (попытка {}): {}", attempt + 1, e.getMessage());

                if (attempt == task.maxRetries) {
                    task.future.completeExceptionally(e);
                    return;
                }

                try {
                    Thread.sleep(retryDelay * (attempt + 1)); // Экспоненциальная задержка
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    task.future.completeExceptionally(ie);
                    return;
                }
            }
        }
    }

    public String getQueueStats() {
        return String.format("Очередь сообщений: ожидает=%d, отправлено=%d, ошибок=%d, в очереди=%d",
                pendingMessages.get(), sentMessages.get(), failedMessages.get(), messageQueue.size());
    }

    public void shutdown() {
        logger.info("Завершение MessageQueueService...");
        // Очищаем очередь
        messageQueue.clear();
    }

    // Внутренний класс для задачи отправки сообщения
    private static class MessageTask {
        final SendMessage message;
        final CompletableFuture<Boolean> future;
        final int maxRetries;

        MessageTask(SendMessage message, CompletableFuture<Boolean> future, int maxRetries) {
            this.message = message;
            this.future = future;
            this.maxRetries = maxRetries;
        }
    }
}