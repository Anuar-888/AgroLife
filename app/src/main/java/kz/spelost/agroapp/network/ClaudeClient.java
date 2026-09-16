package kz.spelost.agroapp.network;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import kz.spelost.agroapp.model.ChatMessage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ВНИМАНИЕ (архитектурная заметка):
 * Сейчас этот клиент бьёт напрямую в api.anthropic.com с ключом, введённым локально в приложении.
 * Это подходит ТОЛЬКО для тестирования на своём устройстве — если собрать APK с зашитым ключом
 * и раздать его, ключ можно вытащить из приложения.
 *
 * Когда появится backend: замените BASE_URL на свой сервер (например
 * "https://api.spelost.kz/chat") и уберите передачу apiKey с телефона — сервер будет
 * сам обращаться к Anthropic со своим секретным ключом.
 */
public class ClaudeClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";

    public interface Callback2 {
        void onSuccess(String replyText);
        void onError(String message);
    }

    public static void sendMessage(String apiKey, String systemPrompt, List<ChatMessage> history, Callback2 callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("NO_KEY");
            return;
        }

        try {
            JSONArray messages = new JSONArray();
            for (ChatMessage m : history) {
                if (m.pending) continue;
                JSONObject obj = new JSONObject();
                obj.put("role", m.sender == ChatMessage.Sender.USER ? "user" : "assistant");
                obj.put("content", m.text);
                messages.put(obj);
            }

            JSONObject payload = new JSONObject();
            payload.put("model", "claude-sonnet-4-6");
            payload.put("max_tokens", 1000);
            payload.put("system", systemPrompt);
            payload.put("messages", messages);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(ANTHROPIC_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> callback.onError("Ошибка соединения: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (response.body() == null) {
                            mainHandler.post(() -> callback.onError("Пустой ответ сервера"));
                            return;
                        }
                        String respBody = response.body().string();
                        if (!response.isSuccessful()) {
                            mainHandler.post(() -> callback.onError("Ошибка API: " + response.code()));
                            return;
                        }
                        JSONObject json = new JSONObject(respBody);
                        JSONArray content = json.getJSONArray("content");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < content.length(); i++) {
                            JSONObject block = content.getJSONObject(i);
                            if (block.has("text")) sb.append(block.getString("text"));
                        }
                        String text = sb.length() > 0 ? sb.toString() : "Не удалось получить ответ.";
                        mainHandler.post(() -> callback.onSuccess(text));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Ошибка разбора ответа"));
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Ошибка запроса: " + e.getMessage());
        }
    }
}
