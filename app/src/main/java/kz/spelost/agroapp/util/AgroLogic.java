package kz.spelost.agroapp.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import kz.spelost.agroapp.model.UserField;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.data.CropRepository;

public class AgroLogic {

    public enum TaskType {
        WATERING, FERTILIZER, HARVEST, OTHER
    }

    public static List<TaskType> getDayTasks(UserField field, Calendar date) {
        List<TaskType> tasks = new ArrayList<>();
        Crop crop = CropRepository.getInstance().getById(field.cropId);
        if (crop == null) return tasks;

        long diffMillis = date.getTimeInMillis() - field.plantDateMillis;
        int daysSincePlanting = (int) (diffMillis / (86400000L));

        // Ensure logic covers at least 270 days for all crops
        CropStage lastStage = crop.stages.get(crop.stages.size() - 1);
        int totalDays = lastStage.hasRange() ? lastStage.offsetEndDays : lastStage.offsetDays;
        int effectiveCycle = Math.max(totalDays, 270);

        if (daysSincePlanting < 0 || daysSincePlanting > effectiveCycle) return tasks;

        for (CropStage stage : crop.stages) {
            boolean active = false;
            if (stage.hasRange()) {
                if (daysSincePlanting >= stage.offsetDays && daysSincePlanting <= stage.offsetEndDays) {
                    active = true;
                }
            } else {
                if (daysSincePlanting == stage.offsetDays) {
                    active = true;
                }
            }

            if (active) {
                String combined = (stage.title + " " + stage.task + " " + stage.description).toLowerCase();
                if (combined.contains("полив") || combined.contains("вода")) {
                    if (!tasks.contains(TaskType.WATERING)) tasks.add(TaskType.WATERING);
                }
                if (combined.contains("удобрен") || combined.contains("подкормк") || combined.contains("нпк") || combined.contains("мпк") || stage.hasFertilizer()) {
                    if (!tasks.contains(TaskType.FERTILIZER)) tasks.add(TaskType.FERTILIZER);
                }
                if (combined.contains("сбор") || combined.contains("уборка") || combined.contains("урожай") || combined.contains("зрелость")) {
                    if (!tasks.contains(TaskType.HARVEST)) tasks.add(TaskType.HARVEST);
                }
            }
        }
        
        // Recurring watering and feeding logic
        if (daysSincePlanting >= 0 && daysSincePlanting <= effectiveCycle) {
            // Regular watering every 3-5 days
            int waterInterval = "raspberry".equalsIgnoreCase(crop.id) ? 3 : 5;
            if (daysSincePlanting % waterInterval == 0) {
                if (!tasks.contains(TaskType.WATERING)) tasks.add(TaskType.WATERING);
            }
            
            // Regular feeding every 15-20 days during active growth
            if (daysSincePlanting > 0 && daysSincePlanting < totalDays && daysSincePlanting % 20 == 0) {
                if (!tasks.contains(TaskType.FERTILIZER)) tasks.add(TaskType.FERTILIZER);
            }
        }
        
        return tasks;
    }

    public static class AgroAdvice {
        public String nextStep;
        public String recommendation;
        public String organicTip;
        public double waterAmountLiters;
        public int progressPercent;
        public String currentStageTitle;
        public boolean isWateringRequired;
        public boolean isFeedingRequired;
        public boolean isPruningRequired;
    }

    public static AgroAdvice getAdvice(UserField field) {
        Calendar cal = Calendar.getInstance();
        return getAdviceForDate(field, cal);
    }

    public static AgroAdvice getAdviceForDate(UserField field, Calendar date) {
        AgroAdvice advice = new AgroAdvice();
        long targetMillis = date.getTimeInMillis();
        long diffMillis = targetMillis - field.plantDateMillis;
        long daysSincePlanting = diffMillis / (86400000L);
        if (daysSincePlanting < 0) {
            advice.progressPercent = 0;
            advice.currentStageTitle = "Ожидание";
            advice.nextStep = "Посадка запланирована на " + new java.text.SimpleDateFormat("dd.MM", java.util.Locale.getDefault()).format(new java.util.Date(field.plantDateMillis));
            return advice;
        }

        Crop crop = CropRepository.getInstance().getById(field.cropId);
        if (crop == null) {
            advice.nextStep = "Данные культуры недоступны.";
            return advice;
        }

        CropStage currentStage = null;
        int maxCycleDays = 0;
        
        for (CropStage stage : crop.stages) {
            int stageEnd = stage.hasRange() ? stage.offsetEndDays : stage.offsetDays;
            if (stageEnd > maxCycleDays) maxCycleDays = stageEnd;

            if (daysSincePlanting >= stage.offsetDays) {
                if (!stage.hasRange() || daysSincePlanting <= stage.offsetEndDays) {
                    currentStage = stage;
                }
            }
        }

        if (daysSincePlanting > maxCycleDays) {
            advice.progressPercent = 100;
            advice.currentStageTitle = "Завершение цикла";
            advice.recommendation = "Цикл завершен. Соберите остатки урожая.";
            advice.organicTip = "Совет: Обогатите почву органикой для следующего сезона.";
            advice.nextStep = "Урожай готов!";
        } else {
            advice.progressPercent = (int) Math.min(100, (daysSincePlanting * 100) / (maxCycleDays > 0 ? maxCycleDays : 1));
            if (currentStage != null) {
                advice.currentStageTitle = currentStage.title;
                advice.recommendation = currentStage.task;
                advice.organicTip = "Совет: " + currentStage.description;
                
                List<TaskType> dayTasks = getDayTasks(field, date);
                advice.isWateringRequired = dayTasks.contains(TaskType.WATERING);
                advice.isFeedingRequired = dayTasks.contains(TaskType.FERTILIZER);
                advice.isPruningRequired = advice.recommendation.toLowerCase().contains("обрезк");

                double rate = 20000;
                if ("raspberry".equalsIgnoreCase(crop.id)) rate = 35000;
                else if ("apple".equalsIgnoreCase(crop.id)) rate = 45000;
                else if ("wheat".equalsIgnoreCase(crop.id)) rate = 15000;
                else if ("tomato".equalsIgnoreCase(crop.id)) rate = 25000;
                else if ("watermelon".equalsIgnoreCase(crop.id)) rate = 30000;
                else if ("corn".equalsIgnoreCase(crop.id)) rate = 22000;
                
                advice.waterAmountLiters = field.areaHectares * rate;
                if (advice.isWateringRequired) {
                    advice.nextStep = "Полив: необходимо " + Math.round(advice.waterAmountLiters / 7) + " л воды.";
                } else {
                    advice.nextStep = "Следите за влажностью почвы.";
                }
            } else {
                advice.currentStageTitle = "Начальная стадия";
                advice.nextStep = "Первые всходы появятся скоро.";
            }
        }

        return advice;
    }

    public static long calculateSeeds(String cropId, double areaM2) {
        double factor = 0.5; // Default seeds per m2
        if ("raspberry".equalsIgnoreCase(cropId)) factor = 0.2; // Саженцы
        if ("apple".equalsIgnoreCase(cropId)) factor = 0.05;
        if ("wheat".equalsIgnoreCase(cropId)) factor = 400;
        if ("corn".equalsIgnoreCase(cropId)) factor = 7;
        return Math.round(areaM2 * factor);
    }

    public static List<String> getProTips(String cropId) {
        List<String> tips = new ArrayList<>();
        tips.add("Используйте мульчирование для сохранения влаги.");
        tips.add("Проверяйте листья на наличие вредителей каждое утро.");
        if ("raspberry".equalsIgnoreCase(cropId)) {
            tips.add("Обрезайте старые побеги сразу после сбора урожая.");
            tips.add("Обеспечьте хорошую вентиляцию между рядами.");
        }
        if ("apple".equalsIgnoreCase(cropId)) {
            tips.add("Прореживайте плоды для получения более крупных яблок.");
            tips.add("Белите стволы весной для защиты от ожогов.");
        }
        return tips;
    }

}
