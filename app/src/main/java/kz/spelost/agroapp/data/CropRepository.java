package kz.spelost.agroapp.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kz.spelost.agroapp.data.local.AppDatabase;
import kz.spelost.agroapp.data.local.CropEntity;
import kz.spelost.agroapp.model.Crop;
import kz.spelost.agroapp.model.CropStage;
import kz.spelost.agroapp.model.PlantingWindow;

/**
 * Репозиторий для работы с культурами.
 * Использует Room для кэширования и хранения данных.
 */
public class CropRepository {

    private static CropRepository instance;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private final Map<String, Crop> memoryCache = new LinkedHashMap<>();

    private CropRepository(Context context) {
        db = AppDatabase.getInstance(context);
        initIfEmpty();
    }

    public static CropRepository getInstance(Context context) {
        if (instance == null) instance = new CropRepository(context);
        return instance;
    }

    // Обратная совместимость для старого кода (только если уже инициализирован)
    public static CropRepository getInstance() {
        return instance;
    }

    public interface Callback<T> {
        void onResult(T result);
    }

    public void getAll(Callback<List<Crop>> callback) {
        executor.execute(() -> {
            if (!memoryCache.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(new ArrayList<>(memoryCache.values())));
                return;
            }

            List<CropEntity> entities = db.cropDao().getAll();
            List<Crop> crops = new ArrayList<>();
            for (CropEntity e : entities) {
                Crop crop = convert(e);
                crops.add(crop);
                memoryCache.put(crop.id, crop);
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(crops));
        });
    }

    /** Синхронная версия для совместимости (использовать с осторожностью) */
    public List<Crop> getAll() {
        return new ArrayList<>(memoryCache.values());
    }

    public Crop getById(String id) {
        return memoryCache.get(id);
    }

    private void initIfEmpty() {
        executor.execute(() -> {
            if (db.cropDao().getAll().isEmpty()) {
                List<CropEntity> defaults = new ArrayList<>();
                defaults.add(convertToEntity(buildRaspberry()));
                defaults.add(convertToEntity(buildStrawberry()));
                defaults.add(convertToEntity(buildGarlic()));
                defaults.add(convertToEntity(buildTomato()));
                defaults.add(convertToEntity(buildCucumber()));
                defaults.add(convertToEntity(buildPotato()));
                defaults.add(convertToEntity(buildApple()));
                defaults.add(convertToEntity(buildWheat()));
                defaults.add(convertToEntity(buildOnion()));
                defaults.add(convertToEntity(buildCarrot()));
                defaults.add(convertToEntity(buildCabbage()));
                defaults.add(convertToEntity(buildPepper()));
                defaults.add(convertToEntity(buildCorn()));
                defaults.add(convertToEntity(buildSunflower()));
                defaults.add(convertToEntity(buildBeetroot()));
                defaults.add(convertToEntity(buildGrapes()));
                defaults.add(convertToEntity(buildApricot()));
                defaults.add(convertToEntity(buildCherry()));
                defaults.add(convertToEntity(buildPear()));
                defaults.add(convertToEntity(buildWatermelon()));
                defaults.add(convertToEntity(buildMelon()));
                defaults.add(convertToEntity(buildEggplant()));
                defaults.add(convertToEntity(buildZucchini()));
                defaults.add(convertToEntity(buildBarley()));
                defaults.add(convertToEntity(buildFlax()));
                defaults.add(convertToEntity(buildRadish()));
                db.cropDao().insertAll(defaults);
                
                // Предварительно заполняем кэш
                for (CropEntity e : defaults) {
                    memoryCache.put(e.id, convert(e));
                }
            } else {
                // Если не пусто, просто грузим в кэш
                List<CropEntity> entities = db.cropDao().getAll();
                for (CropEntity e : entities) {
                    memoryCache.put(e.id, convert(e));
                }
            }
        });
    }

    private Crop convert(CropEntity e) {
        Type windowListType = new TypeToken<ArrayList<PlantingWindow>>(){}.getType();
        Type stageListType = new TypeToken<ArrayList<CropStage>>(){}.getType();
        
        List<PlantingWindow> windows = gson.fromJson(e.plantingWindowsJson, windowListType);
        List<CropStage> stages = gson.fromJson(e.stagesJson, stageListType);
        
        return new Crop(e.id, e.label, e.icon, e.categoryId, windows, stages);
    }

    private CropEntity convertToEntity(Crop c) {
        return new CropEntity(
                c.id,
                c.label,
                c.icon,
                c.categoryId,
                gson.toJson(c.plantingWindows),
                gson.toJson(c.stages)
        );
    }

    private Crop buildRaspberry() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(3, 20, 4, 30, "весной: 20 марта – 30 апреля"));
        windows.add(new PlantingWindow(9, 1, 10, 15, "осенью: 1 сентября – 15 октября"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, -1, "🌱", "Посадка", "Высадка саженцев в траншеи.", "Компост или перегной (10 кг/м²)", "Обеспечьте хороший дренаж.", "Высокая"));
        stages.add(new CropStage(14, -1, "🌿", "Активный рост", "Первая подкормка после укоренения.", "Мочевина (азот) 20г/м²", "Важен регулярный полив.", "Средняя"));
        stages.add(new CropStage(45, -1, "🟢", "Бутонизация", "Установка опор, рыхление.", "Суперфосфат 40г/м²", "Подвяжите стебли к шпалере.", "Средняя"));
        stages.add(new CropStage(75, -1, "🌸", "Цветение", "Регулярный полив, удаление поросли.", "Зола древесная", "Удаляйте лишние побеги.", "Высокая"));
        stages.add(new CropStage(100, -1, "🔴", "Налив ягод", "Полив 1 раз в 3 дня.", "Гумат калия", "Не допускайте пересыхания.", "Высокая"));
        stages.add(new CropStage(115, 150, "🧺", "Сбор урожая", "Сбор ягод каждые 2 дня.", "—", "Собирайте ягоды в сухую погоду.", "Высокая"));
        stages.add(new CropStage(180, 200, "✂️", "Подготовка к зиме", "Обрезка отплодоносивших стеблей.", "Фосфор-Калий", "Пригните стебли к земле.", "Высокая"));
        stages.add(new CropStage(210, 365, "❄️", "Покой", "Зимовка под снегом.", "—", "Следите за целостностью укрытия.", "Низкая"));

        return new Crop("raspberry", "Малина", "🍓", "berries", windows, stages);
    }

    private Crop buildStrawberry() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 1, 5, 15, "весной: 1 апреля – 15 мая"));
        windows.add(new PlantingWindow(8, 1, 9, 20, "летом-осенью: 1 августа – 20 сентября"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, -1, "🌱", "Посадка", "Высадка рассады в лунки.", "Биогумус", "Не заглубляйте сердечко.", "Высокая"));
        stages.add(new CropStage(21, -1, "🌿", "Рост листьев", "Удаление первых усов.", "Нитроаммофоска 15г/м²", "Рыхлите почву после полива.", "Средняя"));
        stages.add(new CropStage(50, -1, "🌸", "Цветение", "Мульчирование соломой.", "Борная кислота", "Солома сохранит ягоды чистыми.", "Средняя"));
        stages.add(new CropStage(75, 100, "🧺", "Сбор урожая", "Собирать ягоды утром.", "—", "Регулярный сбор стимулирует рост.", "Высокая"));
        stages.add(new CropStage(120, -1, "✂️", "Уход после сбора", "Обрезка старых листьев.", "Комплексное удобрение", "Оставьте молодые листочки.", "Средняя"));
        stages.add(new CropStage(180, 200, "🍂", "Подготовка к зиме", "Укрытие агроволокном или хвоей.", "—", "Защита от бесснежных морозов.", "Высокая"));
        stages.add(new CropStage(210, 365, "❄️", "Покой", "Зимовка.", "—", "Растение находится под снегом.", "Низкая"));

        return new Crop("strawberry", "Клубника", "🍓", "berries", windows, stages);
    }

    private Crop buildGarlic() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(9, 20, 10, 25, "осенью: 20 сентября – 25 октября"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, -1, "🌱", "Посадка зубков", "Глубина 5–6 см.", "Фосфорно-калийное", "Используйте крупные зубки.", "Высокая"));
        stages.add(new CropStage(180, 200, "🌱", "Весенние всходы", "Рыхление междурядий.", "Аммиачная селитра", "Первая азотная подкормка.", "Средняя"));
        stages.add(new CropStage(230, -1, "✂️", "Удаление стрелок", "Обрезка цветочных стрелок.", "—", "Увеличивает размер головки.", "Средняя"));
        stages.add(new CropStage(260, 275, "🧺", "Сбор урожая", "Убирать при полегании листьев.", "—", "Не передержите в земле.", "Высокая"));
        stages.add(new CropStage(300, 365, "❄️", "Покой", "Хранение.", "—", "Хранить в сухом прохладном месте.", "Низкая"));

        return new Crop("garlic", "Чеснок", "🧄", "vegetables", windows, stages);
    }

    private Crop buildTomato() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 15, 6, 10, "в грунт: 15 мая – 10 июня"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, -1, "🌱", "Высадка рассады", "Высадка в грунт при стабильном тепле.", "Аммиачная селитра 20г/10л", "Закаляйте рассаду перед высадкой.", "Высокая"));
        stages.add(new CropStage(30, -1, "🌿", "Пасынкование", "Удаление боковых побегов.", "—", "Формируйте куст в 1-2 стебля.", "Средняя"));
        stages.add(new CropStage(60, -1, "🌸", "Цветение", "Формирование завязей.", "Борная кислота", "Слегка встряхивайте кусты.", "Средняя"));
        stages.add(new CropStage(90, 120, "🍅", "Сбор урожая", "Снимать плоды по мере покраснения.", "Сульфат калия 25г/10л", "Регулярный сбор ускоряет созревание.", "Высокая"));
        stages.add(new CropStage(150, 180, "🧹", "Уборка остатков", "Удаление ботвы с участка.", "—", "Профилактика фитофтороза.", "Средняя"));
        stages.add(new CropStage(200, 365, "❄️", "Покой", "—", "—", "Подготовка семян к новому сезону.", "Низкая"));

        return new Crop("tomato", "Помидор", "🍅", "vegetables", windows, stages);
    }

    private Crop buildCucumber() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 20, 6, 15, "в грунт: 20 мая – 15 июня"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, -1, "🌱", "Посев семян", "Посев в теплый грунт.", "Настой коровяка", "Грунт должен прогреться до 15°C.", "Высокая"));
        stages.add(new CropStage(20, -1, "🌿", "Рост плетей", "Подвязка к шпалере.", "Мочевина 15г/10л", "Обеспечьте вертикальную опору.", "Средняя"));
        stages.add(new CropStage(45, -1, "🌸", "Цветение", "Привлечение пчел, полив.", "—", "Поливайте только теплой водой.", "Средняя"));
        stages.add(new CropStage(60, 100, "🥒", "Сбор зеленцов", "Ежедневный сбор урожая.", "Древесная зола", "Не допускайте перерастания.", "Высокая"));
        stages.add(new CropStage(150, 180, "🧹", "Уборка плетей", "Очистка грядок от растительности.", "—", "Дезинфекция опор и теплицы.", "Средняя"));
        stages.add(new CropStage(200, 365, "❄️", "Покой", "—", "—", "Отдых почвы под снегом.", "Низкая"));

        return new Crop("cucumber", "Огурец", "🥒", "vegetables", windows, stages);
    }

    private Crop buildPotato() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 25, 5, 20, "посадка: 25 апреля – 20 мая"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, -1, "🥔", "Посадка клубней", "Глубина 8–10 см.", "Нитрофоска (в каждую лунку)", "Используйте пророщенные клубни.", "Высокая"));
        stages.add(new CropStage(25, -1, "🌿", "Всходы", "Первое окучивание.", "—", "Окучивание защищает от заморозков.", "Средняя"));
        stages.add(new CropStage(50, -1, "🌸", "Цветение", "Борьба с вредителями.", "Калимагнезия 20г/м²", "Важен полив в этот период.", "Высокая"));
        stages.add(new CropStage(90, 120, "🧺", "Уборка урожая", "Уборка после усыхания ботвы.", "—", "Просушите клубни перед хранением.", "Высокая"));
        stages.add(new CropStage(150, -1, "🧹", "Подготовка к зиме", "Уборка ботвы и перекопка участка.", "Органика", "Заделка удобрений под зиму.", "Средняя"));
        stages.add(new CropStage(180, 365, "❄️", "Покой", "Хранение в погребе.", "—", "Температура хранения +2..+4°C.", "Низкая"));

        return new Crop("potato", "Картофель", "🥔", "vegetables", windows, stages);
    }

    private Crop buildApple() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 1, 4, 30, "весной: апрель"));
        windows.add(new PlantingWindow(9, 20, 10, 20, "осенью: конец сентября – октябрь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 15, "✂️", "Обрезка", "Весенняя санитарная обрезка.", "Мочевина (до набухания почек)", "Удаление сухих и больных ветвей.", "Высокая"));
        stages.add(new CropStage(30, 45, "🌸", "Цветение", "Защита от заморозков (дымление).", "—", "Период активного опыления пчелами.", "Высокая"));
        stages.add(new CropStage(60, 90, "🍏", "Рост плодов", "Регулярный полив, подкормка.", "Комплексное NPK", "Следите за вредителями (плодожорка).", "Средняя"));
        stages.add(new CropStage(120, 160, "🧺", "Сбор урожая", "Сбор плодов по мере созревания.", "—", "Аккуратный съем без повреждений.", "Высокая"));
        stages.add(new CropStage(200, 220, "🍂", "Подготовка к зиме", "Побелка стволов, влагозарядный полив.", "Суперфосфат", "Укрытие молодых деревьев.", "Высокая"));
        stages.add(new CropStage(240, 365, "❄️", "Покой", "Зимовка.", "—", "Дерево находится в глубоком покое.", "Низкая"));

        return new Crop("apple", "Яблоня", "🍎", "fruits", windows, stages);
    }

    private Crop buildWheat() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 5, 5, 25, "посев: начало – середина мая"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🚜", "Посев", "Глубина заделки 4-6 см.", "Аммофос", "Важна влажность почвы при посеве.", "Высокая"));
        stages.add(new CropStage(20, 35, "🌱", "Кущение", "Обработка от сорняков.", "Азотная подкормка", "Формирование дополнительных стеблей.", "Средняя"));
        stages.add(new CropStage(60, 80, "🌾", "Колошение", "Борьба с болезнями (ржавчина).", "—", "Критическая стадия для урожайности.", "Высокая"));
        stages.add(new CropStage(100, 120, "🧺", "Уборка", "Прямое комбайнирование.", "—", "Влажность зерна должна быть 14-16%.", "Высокая"));
        stages.add(new CropStage(150, 180, "🚜", "Подготовка почвы", "Зяблевая вспашка.", "Минеральные удобрения", "Накопление влаги на следующий год.", "Средняя"));
        stages.add(new CropStage(200, 365, "❄️", "Покой", "Зимовка семян/поля под снегом.", "—", "Снегозадержание на полях.", "Низкая"));

        return new Crop("wheat", "Пшеница", "🌾", "grains", windows, stages);
    }

    private Crop buildOnion() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 20, 5, 10, "посадка севка: конец апреля – начало мая"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🧅", "Посадка севка", "Схема 10x20 см.", "Перегной", "Прогрейте севок перед посадкой.", "Средняя"));
        stages.add(new CropStage(30, 45, "🌿", "Рост пера", "Рыхление и прополка.", "Настой коровяка", "Не допускайте пересыхания почвы.", "Средняя"));
        stages.add(new CropStage(70, 90, "🧅", "Формирование луковицы", "Прекращение полива за 2 недели.", "—", "Шейка луковицы должна подсохнуть.", "Высокая"));
        stages.add(new CropStage(100, 115, "🧺", "Уборка и сушка", "Уборка при полегании пера.", "—", "Сушка на солнце или под навесом.", "Высокая"));
        stages.add(new CropStage(150, 365, "❄️", "Покой", "Хранение.", "—", "Оптимальная температура 0..+3°C.", "Низкая"));

        return new Crop("onion", "Лук", "🧅", "vegetables", windows, stages);
    }

    private Crop buildCarrot() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 15, 5, 15, "посев: апрель – май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🥕", "Посев", "Посев в бороздки.", "—", "Семена прорастают медленно.", "Средняя"));
        stages.add(new CropStage(30, 40, "🌱", "Прореживание", "Расстояние 4-5 см между всходами.", "—", "Защита от морковной мухи.", "Высокая"));
        stages.add(new CropStage(60, 90, "🥕", "Рост корнеплода", "Глубокий полив.", "Калийные удобрения", "Избегайте избытка азота.", "Средняя"));
        stages.add(new CropStage(110, 130, "🧺", "Уборка", "Выкапывание корнеплодов.", "—", "Не повреждайте кожицу при уборке.", "Высокая"));
        stages.add(new CropStage(150, 365, "❄️", "Покой", "Хранение в песке.", "—", "Хранение при высокой влажности.", "Низкая"));

        return new Crop("carrot", "Морковь", "🥕", "vegetables", windows, stages);
    }

    private Crop buildCabbage() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 5, 5, 25, "высадка рассады: май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🌱", "Высадка рассады", "Заглубление до первого листа.", "Биогумус", "Обильный полив при посадке.", "Высокая"));
        stages.add(new CropStage(30, 50, "🌿", "Розетка листьев", "Окучивание, борьба с гусеницами.", "Аммиачная селитра", "Важно для формирования кочана.", "Средняя"));
        stages.add(new CropStage(70, 100, "🥬", "Завязывание кочана", "Регулярный полив без застоев.", "Суперфосфат", "Кочаны должны стать плотными.", "Высокая"));
        stages.add(new CropStage(130, 150, "🧺", "Уборка", "Срезка кочанов.", "—", "Убирать до сильных заморозков.", "Высокая"));
        stages.add(new CropStage(180, 365, "❄️", "Покой", "Хранение.", "—", "Подвешивание или укладка в ящики.", "Низкая"));

        return new Crop("cabbage", "Капуста", "🥬", "vegetables", windows, stages);
    }

    private Crop buildPepper() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 25, 6, 10, "высадка в грунт: конец мая – июнь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🌱", "Посадка", "Температура почвы > 15°C.", "Комплексное для рассады", "Не переносит заморозков.", "Высокая"));
        stages.add(new CropStage(30, 45, "🌸", "Цветение", "Удаление первого цветка.", "Калийная селитра", "Стимулирует рост куста.", "Средняя"));
        stages.add(new CropStage(70, 100, "🫑", "Плодоношение", "Сбор в технической спелости.", "—", "Регулярный полив теплой водой.", "Высокая"));
        stages.add(new CropStage(130, 150, "🧹", "Уборка остатков", "Удаление кустов из грунта.", "—", "Подготовка почвы к следующему году.", "Низкая"));
        stages.add(new CropStage(180, 365, "❄️", "Покой", "—", "—", "Период отсутствия культуры в грунте.", "Низкая"));

        return new Crop("pepper", "Перец", "🫑", "vegetables", windows, stages);
    }

    private Crop buildCorn() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 10, 5, 25, "посев: май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🌽", "Посев", "Схема 30x70 см.", "Нитроаммофоска", "Глубина посева 5-7 см.", "Средняя"));
        stages.add(new CropStage(50, 70, "🌾", "Цветение (метелки)", "Дополнительный полив.", "—", "Важно для опыления початков.", "Высокая"));
        stages.add(new CropStage(90, 110, "🌽", "Молочная спелость", "Сбор початков для еды.", "—", "Зерна при нажатии выделяют сок.", "Средняя"));
        stages.add(new CropStage(120, 140, "🧺", "Уборка на зерно", "Полное высыхание оберток.", "—", "Уборка при влажности зерна < 25%.", "Высокая"));
        stages.add(new CropStage(180, 365, "❄️", "Покой", "—", "—", "Поле под паром или сидератами.", "Низкая"));

        return new Crop("corn", "Кукуруза", "🌽", "grains", windows, stages);
    }

    private Crop buildSunflower() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 1, 5, 20, "посев: май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🌻", "Посев", "Глубина 5-8 см.", "Борофоска", "Требует хорошо прогретой почвы.", "Средняя"));
        stages.add(new CropStage(60, 80, "🌼", "Цветение", "Период активного медосбора.", "—", "Критическая влагопотребность.", "Высокая"));
        stages.add(new CropStage(110, 130, "🌻", "Созревание", "Побурение корзинок.", "—", "Защита от птиц при необходимости.", "Средняя"));
        stages.add(new CropStage(140, 160, "🧺", "Уборка", "Обмолот корзинок.", "—", "Влажность семян 7-10%.", "Высокая"));
        stages.add(new CropStage(200, 365, "❄️", "Покой", "—", "—", "Севооборот: возврат через 7 лет.", "Низкая"));

        return new Crop("sunflower", "Подсолнечник", "🌻", "oilseeds", windows, stages);
    }

    private Crop buildBeetroot() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 1, 5, 20, "посев: май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🌱", "Посев", "Глубина 2-3 см.", "Зола", "Почва должна быть рыхлой.", "Средняя"));
        stages.add(new CropStage(30, 45, "🌱", "Прореживание", "Оставление самых сильных всходов.", "—", "Избегайте загущения.", "Средняя"));
        stages.add(new CropStage(80, 110, "🍠", "Налив корнеплода", "Полив соленой водой (1 ст.л/10л).", "Бор", "Повышает сахаристость свеклы.", "Средняя"));
        stages.add(new CropStage(120, 140, "🧺", "Уборка", "Выкопка до заморозков.", "—", "Берегите верхушечную почку.", "Высокая"));
        stages.add(new CropStage(180, 365, "❄️", "Покой", "Хранение.", "—", "Хранение поверх картофеля.", "Низкая"));

        return new Crop("beetroot", "Свекла", "🍠", "vegetables", windows, stages);
    }

    private Crop buildGrapes() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 15, 5, 15, "весной: апрель – май"));
        windows.add(new PlantingWindow(10, 1, 10, 30, "осенью: октябрь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 15, "🍇", "Открытие лозы", "Снятие зимнего укрытия.", "Нитроаммофоска", "Важно не передержать под укрытием.", "Высокая"));
        stages.add(new CropStage(40, 55, "🌸", "Цветение", "Нормировка соцветий.", "Борная кислота", "Избегайте полива во время цветения.", "Средняя"));
        stages.add(new CropStage(80, 120, "🍇", "Созревание ягод", "Чеканка побегов.", "Монофосфат калия", "Удаление лишних листьев у гроздей.", "Высокая"));
        stages.add(new CropStage(140, 160, "🧺", "Сбор урожая", "Срезка спелых гроздей.", "—", "Для вина собирают позже.", "Высокая"));
        stages.add(new CropStage(200, 220, "✂️", "Обрезка и укрытие", "Осенняя обрезка, укладка в траншеи.", "Перегной", "Обязательно для условий Казахстана.", "Высокая"));
        stages.add(new CropStage(240, 365, "❄️", "Покой", "Зимовка под укрытием.", "—", "Защита от промерзания корней.", "Высокая"));

        return new Crop("grapes", "Виноград", "🍇", "fruits", windows, stages);
    }

    private Crop buildApricot() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 1, 4, 20, "весной: начало апреля"));
        windows.add(new PlantingWindow(10, 1, 10, 20, "осенью: октябрь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 15, "✂️", "Обрезка", "Санитарная и формирующая обрезка.", "Мочевина 3% (до почек)", "Удаляйте загущающие ветви.", "Высокая"));
        stages.add(new CropStage(20, 35, "🌸", "Цветение", "Защита от возвратных заморозков.", "—", "Очень раннее цветение в Казахстане.", "Высокая"));
        stages.add(new CropStage(60, 90, "🍑", "Налив плодов", "Полив в период засухи.", "Сульфат калия", "Не допускайте пересыхания почвы.", "Средняя"));
        stages.add(new CropStage(100, 120, "🧺", "Сбор урожая", "Сбор плодов для переработки или хранения.", "—", "Собирайте аккуратно, плоды нежные.", "Высокая"));
        stages.add(new CropStage(150, 180, "🍂", "Подготовка к зиме", "Влагозарядный полив, побелка.", "Суперфосфат", "Побелка защищает от ожогов.", "Высокая"));
        stages.add(new CropStage(210, 365, "❄️", "Покой", "Зимовка.", "—", "Дерево спит.", "Низкая"));

        return new Crop("apricot", "Абрикос", "🍑", "fruits", windows, stages);
    }

    private Crop buildCherry() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 5, 4, 25, "весной: апрель"));
        windows.add(new PlantingWindow(9, 25, 10, 15, "осенью: конец сентября"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 15, "✂️", "Весенний уход", "Обрезка сухих ветвей.", "Азотные удобрения", "Рыхление приствольного круга.", "Средняя"));
        stages.add(new CropStage(25, 40, "🌸", "Цветение", "Опыление пчелами.", "Борная кислота (опрыскивание)", "Важна безветренная погода.", "Высокая"));
        stages.add(new CropStage(60, 80, "🍒", "Созревание", "Защита от птиц (сетки).", "—", "Полив 1 раз в неделю.", "Средняя"));
        stages.add(new CropStage(90, 105, "🧺", "Сбор урожая", "Сбор ягод с плодоножкой.", "—", "Быстрая переработка после сбора.", "Высокая"));
        stages.add(new CropStage(180, 200, "🍂", "Осенний уход", "Уборка опавшей листвы.", "Фосфор-Калий", "Профилактика коккомикоза.", "Средняя"));
        stages.add(new CropStage(240, 365, "❄️", "Покой", "Зимовка.", "—", "Растение в покое.", "Низкая"));

        return new Crop("cherry", "Вишня", "🍒", "fruits", windows, stages);
    }

    private Crop buildPear() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 1, 4, 30, "весной: апрель"));
        windows.add(new PlantingWindow(9, 20, 10, 20, "осенью: сентябрь-октябрь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 15, "✂️", "Обрезка", "Формирование кроны.", "Комплексное весеннее", "Удаление волчков.", "Высокая"));
        stages.add(new CropStage(35, 50, "🌸", "Цветение", "Период опыления.", "—", "Чувствительна к заморозкам.", "Высокая"));
        stages.add(new CropStage(70, 110, "🍐", "Рост плодов", "Нормировка урожая.", "Гумат калия", "Полив при нехватке дождей.", "Средняя"));
        stages.add(new CropStage(130, 160, "🧺", "Сбор урожая", "Съем в фазе съемной спелости.", "—", "Храните в прохладном месте.", "Высокая"));
        stages.add(new CropStage(200, 220, "🍂", "Закалка", "Осенний полив, мульчирование.", "Суперфосфат", "Защита корней от мороза.", "Средняя"));
        stages.add(new CropStage(240, 365, "❄️", "Покой", "Зимовка.", "—", "Глубокий покой.", "Низкая"));

        return new Crop("pear", "Груша", "🍐", "fruits", windows, stages);
    }

    private Crop buildWatermelon() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 10, 5, 30, "посев: май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🌱", "Посев", "Посев в прогретую до 15°C почву.", "Нитроаммофоска 20г/м²", "Глубина заделки 3-5 см.", "Высокая"));
        stages.add(new CropStage(30, 50, "🌿", "Плетение", "Направление плетей, рыхление.", "Настой навоза 1:10", "Важен глубокий редкий полив.", "Средняя"));
        stages.add(new CropStage(60, 75, "🌸", "Цветение", "Опыление насекомыми.", "—", "Ограничьте полив при цветении.", "Средняя"));
        stages.add(new CropStage(80, 110, "🍉", "Налив плодов", "Подкладывание дощечек под плоды.", "Зола древесная", "Полив 1 раз в 7-10 дней.", "Высокая"));
        stages.add(new CropStage(120, 140, "🧺", "Уборка", "Сбор при подсыхании усика.", "—", "Проверьте звонкость при постукивании.", "Высокая"));
        stages.add(new CropStage(160, 200, "🧹", "Уборка поля", "Удаление остатков плетей.", "—", "Очистка участка.", "Низкая"));

        return new Crop("watermelon", "Арбуз", "🍉", "fruits", windows, stages);
    }

    private Crop buildMelon() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 15, 6, 5, "посев: середина мая – июнь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🌱", "Посев", "Высадка в гребни.", "Органика", "Любит легкие песчаные почвы.", "Высокая"));
        stages.add(new CropStage(30, 50, "🌿", "Активный рост", "Прищипка главного стебля.", "Мочевина 10г/м²", "Стимуляция боковых побегов.", "Средняя"));
        stages.add(new CropStage(60, 80, "🌸", "Цветение и завязь", "Формирование 3-4 плодов.", "—", "Удаляйте лишние завязи.", "Средняя"));
        stages.add(new CropStage(90, 110, "🍈", "Созревание", "Появление характерного аромата.", "Сульфат калия", "Прекратите полив за 2 недели до сбора.", "Высокая"));
        stages.add(new CropStage(120, 140, "🧺", "Уборка", "Сбор плодов по мере спелости.", "—", "Не храните долго после сбора.", "Высокая"));
        stages.add(new CropStage(160, 200, "🧹", "Очистка", "Уборка растительных остатков.", "—", "Подготовка почвы.", "Низкая"));

        return new Crop("melon", "Дыня", "🍈", "fruits", windows, stages);
    }

    private Crop buildEggplant() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 20, 6, 10, "высадка рассады: конец мая – июнь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🌱", "Высадка рассады", "Посадка в теплую почву.", "Комплексное удобрение", "Очень чувствителен к холоду.", "Высокая"));
        stages.add(new CropStage(25, 45, "🌿", "Рост куста", "Рыхление, окучивание.", "Аммиачная селитра", "Полив только теплой водой.", "Средняя"));
        stages.add(new CropStage(50, 70, "🌸", "Цветение", "Борьба с колорадским жуком.", "—", "Важна высокая влажность почвы.", "Средняя"));
        stages.add(new CropStage(80, 110, "🍆", "Плодоношение", "Сбор плодов в фазе блеска.", "Древесная зола", "Не допускайте перезревания плодов.", "Высокая"));
        stages.add(new CropStage(120, 140, "🧺", "Массовый сбор", "Регулярный сбор урожая.", "—", "Срезайте секатором.", "Высокая"));
        stages.add(new CropStage(160, 180, "🧹", "Уборка", "Удаление растительности.", "—", "Очистка грядок.", "Низкая"));

        return new Crop("eggplant", "Баклажан", "🍆", "vegetables", windows, stages);
    }

    private Crop buildZucchini() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 10, 6, 15, "посев: май – июнь"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 7, "🌱", "Посев", "Посев в лунки по 2-3 семени.", "Компост", "Обеспечьте место для роста.", "Средняя"));
        stages.add(new CropStage(25, 40, "🌿", "Рост листьев", "Прополка и полив под корень.", "Нитрофоска", "Не мочите листья при поливе.", "Средняя"));
        stages.add(new CropStage(45, 60, "🌸", "Цветение", "Привлечение опылителей.", "—", "Обильный полив в жару.", "Средняя"));
        stages.add(new CropStage(70, 120, "🥒", "Плодоношение", "Сбор молодых плодов (15-20 см).", "Настой травы", "Частый сбор стимулирует новые завязи.", "Высокая"));
        stages.add(new CropStage(140, 160, "🧺", "Конец сезона", "Сбор плодов на хранение.", "—", "Кожица должна быть твердой.", "Средняя"));
        stages.add(new CropStage(180, 200, "🧹", "Уборка", "Удаление старых кустов.", "—", "Очистка территории.", "Низкая"));

        return new Crop("zucchini", "Кабачок", "🥒", "vegetables", windows, stages);
    }

    private Crop buildBarley() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 5, 5, 5, "посев: начало апреля – начало мая"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🚜", "Посев", "Глубина 3-5 см.", "Суперфосфат", "Ранний посев дает лучший урожай.", "Высокая"));
        stages.add(new CropStage(15, 30, "🌱", "Всходы и кущение", "Боронование.", "Азотная подкормка", "Критично наличие влаги.", "Средняя"));
        stages.add(new CropStage(50, 70, "🌾", "Выход в трубку", "Защита от вредителей.", "—", "Быстрый рост стебля.", "Средняя"));
        stages.add(new CropStage(80, 100, "🌾", "Колошение", "Налив зерна.", "—", "Чувствителен к засухе в этот период.", "Высокая"));
        stages.add(new CropStage(110, 130, "🧺", "Уборка", "Прямое комбайнирование.", "—", "Влажность зерна 14%.", "Высокая"));
        stages.add(new CropStage(160, 200, "🚜", "Послеуборочная обработка", "Лущение стерни.", "—", "Подготовка к следующему сезону.", "Средняя"));

        return new Crop("barley", "Ячмень", "🌾", "grains", windows, stages);
    }

    private Crop buildFlax() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(5, 1, 5, 20, "посев: май"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 5, "🌱", "Посев", "Узкорядный посев.", "Нитроаммофоска", "Нужна мелкокомковатая почва.", "Высокая"));
        stages.add(new CropStage(20, 40, "🌿", "Фаза елочки", "Обработка от льняной блошки.", "—", "Высота растений 5-10 см.", "Средняя"));
        stages.add(new CropStage(50, 65, "🌸", "Цветение", "Массовое голубое цветение.", "—", "Критическая стадия по влаге.", "Средняя"));
        stages.add(new CropStage(80, 100, "🌿", "Созревание", "Побурение коробочек.", "—", "Определение спелости волокна/семян.", "Средняя"));
        stages.add(new CropStage(110, 130, "🧺", "Уборка", "Теребление или комбайнирование.", "—", "Своевременность важна для качества.", "Высокая"));
        stages.add(new CropStage(160, 200, "🚜", "Подготовка почвы", "Осенняя вспашка.", "Калийные удобрения", "Заделка под зиму.", "Средняя"));

        return new Crop("flax", "Лен", "🌿", "industrial", windows, stages);
    }

    private Crop buildRadish() {
        List<PlantingWindow> windows = new ArrayList<>();
        windows.add(new PlantingWindow(4, 1, 5, 15, "весной: апрель – май"));
        windows.add(new PlantingWindow(8, 1, 8, 30, "летом: август"));

        List<CropStage> stages = new ArrayList<>();
        stages.add(new CropStage(0, 3, "🌱", "Посев", "Схема 5x15 см.", "—", "Очень короткий цикл роста.", "Средняя"));
        stages.add(new CropStage(7, 15, "🌿", "Рост листьев", "Регулярный обильный полив.", "Зола", "Недостаток влаги делает плод горьким.", "Высокая"));
        stages.add(new CropStage(18, 25, "🥗", "Формирование корнеплода", "Затенение при длинном дне.", "—", "Стрелкуется при жаре.", "Средняя"));
        stages.add(new CropStage(30, 45, "🧺", "Сбор урожая", "Выборочный сбор крупных корнеплодов.", "—", "Не передерживайте – станет дряблым.", "Высокая"));
        stages.add(new CropStage(60, 90, "🧹", "Уборка", "Очистка грядки.", "—", "Подготовка под следующую культуру.", "Низкая"));

        return new Crop("radish", "Редис", "🥗", "vegetables", windows, stages);
    }
}
