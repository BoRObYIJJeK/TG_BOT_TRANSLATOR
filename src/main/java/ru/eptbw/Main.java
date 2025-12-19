package ru.eptbw;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.eptbw.bot.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс приложения с поддержкой многопоточности
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static TelegramBot bot;

    public static void main(String[] args) {
        // Добавляем обработчик завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdownHook));

        try {
            // Инициализация API Telegram ботов
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Создание экземпляра бота
            bot = new TelegramBot();

            // Регистрация бота в Telegram
            telegramBotsApi.registerBot(bot);

            logger.info("🤖 Бот успешно запущен с поддержкой многопоточности!");
            logger.info("🔗 Интегрирован с Google Gemini AI");
            logger.info("🧵 ThreadPool: core={}, max={}",
                    ru.eptbw.config.Config.getThreadPoolCoreSize(),
                    ru.eptbw.config.Config.getThreadPoolMaxSize());
            logger.info("📊 Статистика будет записываться в: {}",
                    ru.eptbw.config.Config.getStatsLogFile());

            // Бесконечный цикл для удержания программы
            keepAlive();

        } catch (TelegramApiException e) {
            logger.error("❌ Ошибка при запуске бота: ", e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("❌ Неожиданная ошибка: ", e);
            System.exit(1);
        }
    }

    private static void keepAlive() {
        try {
            // Бесконечный цикл с периодической проверкой
            while (true) {
                Thread.sleep(60000); // 1 минута
                logger.debug("Бот работает...");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Получен сигнал прерывания");
        }
    }

    private static void shutdownHook() {
        logger.info("Получен сигнал завершения работы...");
        if (bot != null) {
            bot.shutdown();
        }
        logger.info("Приложение завершено");
    }
}