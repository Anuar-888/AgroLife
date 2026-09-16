package kz.spelost.agroapp.ui.profile;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import kz.spelost.agroapp.R;
import kz.spelost.agroapp.data.AppState;
import kz.spelost.agroapp.model.UserField;

public class ProfileFragment extends Fragment {

    private LinearLayout containerView;
    private AppState state;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        state = AppState.getInstance();
        containerView = root.findViewById(R.id.profile_fields_container);
        
        TextView nameText = root.findViewById(R.id.profile_user_name);
        nameText.setText(state.userName);

        root.findViewById(R.id.profile_card).setOnClickListener(v -> showEditNameDialog());
        root.findViewById(R.id.profile_btn_add_field).setOnClickListener(v -> showAddFieldDialog());

        refreshFieldsList();

        root.findViewById(R.id.profile_btn_logout).setOnClickListener(v -> {
            state.userName = "Guest";
            state.save(requireContext());
            requireActivity().finish();
        });

        return root;
    }

    private void refreshFieldsList() {
        if (containerView == null) return;
        containerView.removeAllViews();
        
        TextView countText = getView() != null ? getView().findViewById(R.id.profile_field_count) : null;
        if (countText != null) {
            int count = state.userFields.size();
            String text = count + (count == 1 ? " поле" : " поля");
            countText.setText(text);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        
        for (int i = 0; i < state.userFields.size(); i++) {
            UserField field = state.userFields.get(i);
            int index = i;
            
            View itemView = inflater.inflate(R.layout.item_profile_field, containerView, false);
            TextView nameTv = itemView.findViewById(R.id.profile_field_name);
            TextView infoTv = itemView.findViewById(R.id.profile_field_info);
            
            View editBtn = itemView.findViewById(R.id.profile_field_edit);
            View divider = itemView.findViewById(R.id.profile_field_divider);

            nameTv.setText("📍 " + field.name);
            String dateStr = sdf.format(new Date(field.plantDateMillis));
            infoTv.setText(field.areaHectares + " Га • Посажено: " + dateStr);

            if (editBtn != null) {
                editBtn.setOnClickListener(v -> {
                    android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), editBtn);
                    popup.getMenu().add("Редактировать");
                    popup.getMenu().add("Переместить вверх");
                    popup.getMenu().add("Переместить вниз");
                    popup.getMenu().add("Удалить");
                    
                    popup.setOnMenuItemClickListener(item -> {
                        String title = item.getTitle().toString();
                        if (title.equals("Редактировать")) {
                            showEditFieldDialog(field);
                        } else if (title.equals("Переместить вверх")) {
                            if (index > 0) {
                                Collections.swap(state.userFields, index, index - 1);
                                state.save(requireContext());
                                refreshFieldsList();
                            }
                        } else if (title.equals("Переместить вниз")) {
                            if (index < state.userFields.size() - 1) {
                                Collections.swap(state.userFields, index, index + 1);
                                state.save(requireContext());
                                refreshFieldsList();
                            }
                        } else if (title.equals("Удалить")) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Удалить поле?")
                                    .setMessage("Вы уверены, что хотите удалить поле " + field.name + "?")
                                    .setPositiveButton("Удалить", (d, w) -> {
                                        state.userFields.remove(field);
                                        state.save(requireContext());
                                        refreshFieldsList();
                                    })
                                    .setNegativeButton("Отмена", null)
                                    .show();
                        }
                        return true;
                    });
                    popup.show();
                });
            }

            itemView.setOnLongClickListener(v -> {
                if (editBtn != null) editBtn.performClick();
                return true;
            });
            
            if (divider != null) {
                divider.setVisibility(index == state.userFields.size() - 1 ? View.GONE : View.VISIBLE);
            }

            containerView.addView(itemView);
        }
    }

    private void showEditNameDialog() {
        EditText input = new EditText(getContext());
        input.setText(state.userName);
        input.setPadding(64, 32, 64, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Изменить имя")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        state.userName = newName;
                        state.save(requireContext());
                        TextView nameText = getView().findViewById(R.id.profile_user_name);
                        if (nameText != null) nameText.setText(newName);
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showEditFieldDialog(UserField field) {
        EditText input = new EditText(getContext());
        input.setText(field.name);
        input.setPadding(64, 32, 64, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("Переименовать поле")
                .setView(input)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        field.name = newName;
                        state.save(requireContext());
                        refreshFieldsList();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAddFieldDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(64, 32, 64, 32);

        final EditText nameInput = new EditText(getContext());
        nameInput.setHint("Название поля");
        layout.addView(nameInput);

        final EditText cropInput = new EditText(getContext());
        cropInput.setHint("Культура (raspberry, garlic, wheat)");
        layout.addView(cropInput);

        final EditText areaInput = new EditText(getContext());
        areaInput.setHint("Площадь (Га)");
        areaInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(areaInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Новое поле")
                .setView(layout)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String crop = cropInput.getText().toString().trim();
                    String areaStr = areaInput.getText().toString().trim();

                    if (name.isEmpty() || crop.isEmpty() || areaStr.isEmpty()) {
                        Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double area = Double.parseDouble(areaStr);
                        UserField newField = new UserField(
                                UUID.randomUUID().toString(),
                                name,
                                crop,
                                area,
                                System.currentTimeMillis()
                        );
                        state.userFields.add(newField);
                        state.save(requireContext());
                        refreshFieldsList();
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Неверный формат площади", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
