package kz.spelost.agroapp.ui.chat;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.data.CropRepository;
import kz.spelost.agroapp.model.ChatMessage;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.network.ClaudeClient;
import kz.spelost.agroapp.network.WeatherClient;
import kz.spelost.agroapp.util.DateUtils;

public class ChatFragment extends Fragment {

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView recyclerView;
    private EditText input, apiKeyInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = root.findViewById(R.id.chat_log);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(messages);
        recyclerView.setAdapter(adapter);

        input = root.findViewById(R.id.chat_input);
        apiKeyInput = root.findViewById(R.id.chat_api_key_input);
        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        if (AppState.getInstance().testAnthropicApiKey != null) {
            apiKeyInput.setText(AppState.getInstance().testAnthropicApiKey);
        }

        Button send = root.findViewById(R.id.chat_send);
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            input.setText("");
            AppState.getInstance().testAnthropicApiKey = apiKeyInput.getText().toString().trim();
            sendMessage(text);
        });

        if (messages.isEmpty()) {
            addMessage(new ChatMessage(ChatMessage.Sender.BOT,
                    "Здравствуйте! Спрашивайте что угодно про уход за культурой — например «что делать, если желтеют листья?» или «нужно ли поливать перед заморозком?».",
                    false));
        }

        return root;
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
    }

    private void sendMessage(String text) {
        addMessage(new ChatMessage(ChatMessage.Sender.USER, text, false));
        ChatMessage pending = new ChatMessage(ChatMessage.Sender.BOT, "печатает…", true);
        addMessage(pending);

        String apiKey = AppState.getInstance().testAnthropicApiKey;
        String systemPrompt = buildSystemPrompt();

        ClaudeClient.sendMessage(apiKey, systemPrompt, messages, new ClaudeClient.Callback2() {
            @Override
            public void onSuccess(String replyText) {
                if (!isAdded()) return;
                updatePending(replyText);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if ("NO_KEY".equals(message)) {
                    updatePending(getString(R.string.chat_no_key_message));
                } else {
                    updatePending("Не удалось получить ответ: " + message);
                }
            }
        });
    }

    private void updatePending(String text) {
        int lastIndex = messages.size() - 1;
        ChatMessage last = messages.get(lastIndex);
        last.text = text;
        last.pending = false;
        adapter.notifyItemChanged(lastIndex);
        recyclerView.scrollToPosition(lastIndex);
    }

    private String buildSystemPrompt() {
        AppState state = AppState.getInstance();
        String cropLabel = "не выбрана";
        String stageTitle = "неизвестен";

        if (state.selectedCropId != null) {
            Crop crop = CropRepository.getInstance().getById(state.selectedCropId);
            if (crop != null) {
                cropLabel = crop.label;
                Calendar plantDate = state.plantDateMillis > 0 ? state.getPlantDateCalendar() : DateUtils.nearestValidPlantingDate(crop);
                Calendar today = DateUtils.today();
                CropStage current = null;
                for (CropStage s : crop.stages) {
                    Calendar stageDate = DateUtils.addDays(plantDate, s.offsetDays);
                    if (!stageDate.after(today)) current = s;
                }
                stageTitle = current != null ? current.title : "вне сезона";
            }
        }

        String weatherSummary = state.weatherCache != null ? state.weatherCache.summaryForChat() : "нет данных";

        return "Ты — опытный агроном-консультант по имени Асыл, помогаешь фермеру в Казахстане (регион: " + state.locationName + ").\n" +
                "Текущая культура пользователя: " + cropLabel + ". Текущий этап ухода: " + stageTitle + ".\n" +
                "Погода: " + weatherSummary + ".\n" +
                "Отвечай кратко, конкретно, практическими шагами, на русском языке.";
    }
}
