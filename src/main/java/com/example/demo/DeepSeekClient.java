package com.example.demo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** DeepSeek Chat Completions 的最小客户端（OpenAI 兼容格式）。 */
public final class DeepSeekClient {

    public record Message(String role, String content) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private DeepSeekClient() {
    }

    public static String chat(String apiKey, String baseUrl, String model, List<Message> messages) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("没有填写 DeepSeek API Key");
        }
        String base = baseUrl == null || baseUrl.isBlank() ? "https://api.deepseek.com" : baseUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String endpoint = base.endsWith("/v1") ? base + "/chat/completions" : base + "/chat/completions";
        String modelName = model == null || model.isBlank() ? "deepseek-chat" : model.trim();

        JsonArray array = new JsonArray();
        for (Message message : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            item.addProperty("content", message.content());
            array.add(item);
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.add("messages", array);
        body.addProperty("stream", false);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 1200);

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject json;
        try {
            json = JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception parseError) {
            throw new RuntimeException("DeepSeek 返回无法解析（HTTP " + response.statusCode() + "）");
        }
        if (response.statusCode() != 200) {
            String message = json.has("error") && json.get("error").isJsonObject()
                    ? text(json.getAsJsonObject("error"), "message")
                    : "HTTP " + response.statusCode();
            throw new RuntimeException("DeepSeek 请求失败：" + message);
        }
        if (!json.has("choices") || json.getAsJsonArray("choices").isEmpty()) {
            throw new RuntimeException("DeepSeek 没有返回内容");
        }
        JsonObject choice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
        if (!choice.has("message")) throw new RuntimeException("DeepSeek 没有返回 message");
        String content = text(choice.getAsJsonObject("message"), "content");
        if (content == null || content.isBlank()) throw new RuntimeException("DeepSeek 返回内容为空");
        return content.trim();
    }

    private static String text(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
