package ru.eptbw.bot.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eptbw.config.Config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this(Config.getGeminiApiKey(), Config.getGeminiModel());
    }

    public GeminiService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = new ObjectMapper();
        // Игнорируем неизвестные поля в JSON
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        logger.info("Initialized GeminiService with model: {}", model);
    }

    public String translateText(String text, String targetLanguage) {
        try {
            String prompt = String.format(
                    "Переведи следующий текст на %s. Ответь ТОЛЬКО переводом без дополнительных комментариев:\n\n%s",
                    targetLanguage, text
            );

            return callGeminiAPI(prompt);

        } catch (Exception e) {
            logger.error("Ошибка при переводе текста: '{}'", text, e);
            return "⚠️ Ошибка перевода. Попробуйте позже.";
        }
    }

    public String autoTranslateToRussian(String text) {
        try {
            String prompt = "Определи язык следующего текста и переведи его на русский. " +
                    "Ответь ТОЛЬКО переводом без дополнительных комментариев:\n\n" + text;

            return callGeminiAPI(prompt);

        } catch (Exception e) {
            logger.error("Ошибка при автоматическом переводе", e);
            return "⚠️ Ошибка автоматического перевода.";
        }
    }

    public String generateResponse(String userMessage) {
        try {
            String prompt = String.format(
                    "Ты - полезный AI-ассистент в Telegram боте. Ответь на сообщение пользователя кратко и информативно.\n\n" +
                            "Сообщение пользователя: %s\n\n" +
                            "Ответь на русском языке, если пользователь не указал иное.",
                    userMessage
            );

            return callGeminiAPI(prompt);

        } catch (Exception e) {
            logger.error("Ошибка при генерации ответа для сообщения: '{}'", userMessage, e);
            return "🤖 В настоящее время AI-функция временно недоступна.\nИспользуйте команды: /help, /translate";
        }
    }

    private String callGeminiAPI(String prompt) throws IOException {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
        );

        String jsonBody = String.format(
                "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}],\"generationConfig\":{\"temperature\":0.3,\"maxOutputTokens\":1000}}",
                escapeJsonString(prompt)
        );

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        logger.debug("Отправка запроса к Gemini API (модель: {}): {}", model,
                prompt.substring(0, Math.min(prompt.length(), 100)) + "...");

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                logger.error("Ошибка Gemini API: {} - {}", response.code(), errorBody);

                if (response.code() == 404) {
                    throw new RuntimeException("Модель " + model + " не найдена. Проверьте название модели.");
                } else if (response.code() == 400) {
                    throw new RuntimeException("Неверный запрос к Gemini API.");
                } else if (response.code() == 403) {
                    throw new RuntimeException("Доступ запрещен. Проверьте API ключ и регион.");
                } else if (response.code() == 429) {
                    throw new RuntimeException("Превышена квота Gemini API.");
                } else if (response.code() == 401) {
                    throw new RuntimeException("Неверный API ключ Gemini.");
                }
                throw new RuntimeException("Gemini API error: " + response.code());
            }

            String responseBody = response.body().string();
            logger.debug("Получен ответ от Gemini API");

            return parseGeminiResponse(responseBody);
        }
    }

    private String parseGeminiResponse(String responseBody) throws IOException {
        GeminiResponse geminiResponse = objectMapper.readValue(responseBody, GeminiResponse.class);

        if (geminiResponse.candidates != null &&
                geminiResponse.candidates.length > 0 &&
                geminiResponse.candidates[0].content != null &&
                geminiResponse.candidates[0].content.parts != null &&
                geminiResponse.candidates[0].content.parts.length > 0) {

            String result = geminiResponse.candidates[0].content.parts[0].text.trim();
            logger.debug("Успешно распарсен ответ Gemini: {}", result);
            return result;
        } else {
            logger.error("Пустой ответ от Gemini API. Структура ответа: {}", responseBody);
            throw new RuntimeException("Пустой ответ от Gemini API");
        }
    }

    private String escapeJsonString(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    public boolean isAvailable() {
        try {
            String testResponse = callGeminiAPI("Ответь одним словом: OK");
            boolean available = testResponse != null && testResponse.contains("OK");
            if (available) {
                logger.info("✅ Gemini API доступен");
            } else {
                logger.warn("⚠️ Gemini API недоступен - пустой ответ");
            }
            return available;
        } catch (Exception e) {
            logger.warn("⚠️ Gemini API недоступен: {}", e.getMessage());
            return false;
        }
    }

    // Внутренние классы для парсинга JSON ответа Gemini
    public static class GeminiResponse {
        public Candidate[] candidates;
        public UsageMetadata usageMetadata;
    }

    public static class Candidate {
        public Content content;
        public String finishReason;
        public Double avgLogprobs;
    }

    public static class Content {
        public Part[] parts;
        public String role; // Добавляем поле role, которое есть в ответе
    }

    public static class Part {
        public String text;
    }

    public static class UsageMetadata {
        public int promptTokenCount;
        public int candidatesTokenCount;
        public int totalTokenCount;
    }
}