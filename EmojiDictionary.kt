package com.example.data.emoji

data class EmojiEntry(
    val emoji: String,
    val primaryWordRu: String,
    val synonymsRu: List<String>,
    val primaryWordEn: String,
    val category: String
)

object EmojiDictionary {

    val entries: List<EmojiEntry> = listOf(
        // Эмоции и состояния
        EmojiEntry("😀", "радость", listOf("радостный", "веселый", "улыбка", "счастлив"), "happy", "Эмоции"),
        EmojiEntry("😂", "смех", listOf("смешно", "ржу", "хохот", "лол"), "laugh", "Эмоции"),
        EmojiEntry("🥰", "влюблен", listOf("любовь", "нежность", "обожание"), "in love", "Эмоции"),
        EmojiEntry("😎", "крутой", listOf("стильный", "молодец", "красавчик", "четкий"), "cool", "Эмоции"),
        EmojiEntry("🤔", "думать", listOf("размышление", "вопрос", "сомнение"), "thinking", "Эмоции"),
        EmojiEntry("😢", "грусть", listOf("плакать", "слезы", "печаль", "тоска"), "sad", "Эмоции"),
        EmojiEntry("😡", "злость", listOf("гнев", "ярость", "бесит", "злой"), "angry", "Эмоции"),
        EmojiEntry("😱", "страх", listOf("шок", "кошмар", "ужас", "испуг"), "scared", "Эмоции"),
        EmojiEntry("😴", "спать", listOf("сон", "устал", "дремать", "отдыхать"), "sleep", "Эмоции"),
        EmojiEntry("🥳", "праздник", listOf("тусовка", "юбилей", "вечеринка", "поздравляю"), "party", "Эмоции"),
        EmojiEntry("🤯", "взрыв мозга", listOf("офигеть", "шок", "поразительно"), "mind blown", "Эмоции"),
        EmojiEntry("💀", "смерть", listOf("мертвый", "капец", "погиб"), "dead", "Эмоции"),

        // Действия / Глаголы
        EmojiEntry("🏃", "бежать", listOf("бег", "спешить", "торопиться"), "run", "Действия"),
        EmojiEntry("🚶", "идти", listOf("ходить", "гулять", "пешком"), "walk", "Действия"),
        EmojiEntry("🍕", "есть", listOf("кушать", "пицца", "еда", "обедать", "перекус"), "eat", "Действия"),
        EmojiEntry("☕", "пить", listOf("кофе", "чай", "напиток", "глоток"), "drink", "Действия"),
        EmojiEntry("💻", "работать", listOf("программировать", "код", "компьютер", "ноутбук", "дело"), "work", "Действия"),
        EmojiEntry("🗣️", "говорить", listOf("сказать", "речь", "разговор", "болтать"), "speak", "Действия"),
        EmojiEntry("👀", "смотреть", listOf("видеть", "взгляд", "глаза", "глядеть"), "look", "Действия"),
        EmojiEntry("👂", "слышать", listOf("слушать", "ухо", "звук"), "hear", "Действия"),
        EmojiEntry("✍️", "писать", listOf("записать", "текст", "письмо", "ручка"), "write", "Действия"),
        EmojiEntry("🎮", "играть", listOf("игра", "гейминг", "джойстик", "развлекаться"), "play", "Действия"),
        EmojiEntry("🎵", "слушать музыку", listOf("песня", "мелодия", "трек"), "music", "Действия"),
        EmojiEntry("✈️", "лететь", listOf("самолет", "полет", "рейс"), "fly", "Действия"),
        EmojiEntry("🛍️", "покупать", listOf("магазин", "покупка", "шопинг"), "shop", "Действия"),
        EmojiEntry("🚗", "ехать", listOf("машина", "авто", "рулить"), "drive", "Действия"),
        EmojiEntry("📞", "звонить", listOf("телефон", "связь", "набрать"), "call", "Действия"),
        EmojiEntry("🎓", "учиться", listOf("учеба", "университет", "школа", "студент"), "study", "Действия"),

        // Люди и местоимения
        EmojiEntry("🙋", "я", listOf("меня", "мне", "мной", "сам"), "i", "Люди"),
        EmojiEntry("👉", "ты", listOf("тебя", "тебе", "тобой", "вы"), "you", "Люди"),
        EmojiEntry("👥", "мы", listOf("нас", "нам", "нами", "вместе"), "we", "Люди"),
        EmojiEntry("👨", "мужчина", listOf("парень", "он", "человек", "мужик"), "man", "Люди"),
        EmojiEntry("👩", "женщина", listOf("девушка", "она", "дама"), "woman", "Люди"),
        EmojiEntry("👶", "ребенок", listOf("малыш", "детство"), "baby", "Люди"),
        EmojiEntry("🤝", "друг", listOf("друзья", "дружба", "товарищ", "партнер"), "friend", "Люди"),

        // Предметы и технологии
        EmojiEntry("📱", "телефон", listOf("смартфон", "айфон", "мобильный"), "phone", "Предметы"),
        EmojiEntry("💵", "деньги", listOf("купюра", "богатство", "цена", "рубли", "доллары"), "money", "Предметы"),
        EmojiEntry("🏠", "дом", listOf("квартира", "жилище", "здание", "домой"), "home", "Предметы"),
        EmojiEntry("📖", "книга", listOf("читать", "учебник", "страница"), "book", "Предметы"),
        EmojiEntry("🔑", "ключ", listOf("пароль", "доступ", "код"), "key", "Предметы"),
        EmojiEntry("🔒", "замок", listOf("закрыто", "безопасность", "блок"), "lock", "Предметы"),
        EmojiEntry("💡", "идея", listOf("мысль", "лампа", "озарение"), "idea", "Предметы"),
        EmojiEntry("🚀", "ракета", listOf("быстро", "старт", "космос", "взлет"), "rocket", "Предметы"),
        EmojiEntry("🤖", "робот", listOf("бот", "алгоритм", "ии"), "robot", "Предметы"),
        EmojiEntry("🔋", "батарея", listOf("зарядка", "энергия", "сила"), "battery", "Предметы"),

        // Природа и мир
        EmojiEntry("☀️", "солнце", listOf("день", "жара", "свет", "ясно"), "sun", "Природа"),
        EmojiEntry("🌙", "луна", listOf("ночь", "темнота", "месяц"), "moon", "Природа"),
        EmojiEntry("🌧️", "дождь", listOf("ливень", "пасмурно", "сыро"), "rain", "Природа"),
        EmojiEntry("🔥", "огонь", listOf("пламя", "горячо", "пожар", "топ"), "fire", "Природа"),
        EmojiEntry("🌊", "вода", listOf("море", "океан", "волна", "река"), "water", "Природа"),
        EmojiEntry("🐱", "кот", listOf("кошка", "котик", "котенок"), "cat", "Природа"),
        EmojiEntry("🐶", "собака", listOf("пес", "щенок"), "dog", "Природа"),
        EmojiEntry("🌳", "дерево", listOf("лес", "парк"), "tree", "Природа"),
        EmojiEntry("🌸", "цветок", listOf("весна", "красота"), "flower", "Природа"),

        // Логика и связки
        EmojiEntry("👋", "привет", listOf("здравствуй", "хей", "салют"), "hello", "Связки"),
        EmojiEntry("✌️", "пока", listOf("до свидания", "прощай"), "bye", "Связки"),
        EmojiEntry("🙏", "спасибо", listOf("благодарю", "пожалуйста"), "thanks", "Связки"),
        EmojiEntry("✅", "да", listOf("верно", "согласен", "точно", "ок"), "yes", "Связки"),
        EmojiEntry("❌", "нет", listOf("нельзя", "отмена", "неверно", "ложь"), "no", "Связки"),
        EmojiEntry("👍", "хорошо", listOf("отлично", "класс", "супер", "лайк"), "good", "Связки"),
        EmojiEntry("👎", "плохо", listOf("ужасно", "дизлайк"), "bad", "Связки"),
        EmojiEntry("❓", "вопрос", listOf("почему", "зачем", "что", "как"), "why", "Связки"),
        EmojiEntry("❗", "внимание", listOf("важно", "срочно"), "warning", "Связки"),
        EmojiEntry("⏳", "время", listOf("минута", "час", "секунда", "ждать"), "time", "Связки"),
        EmojiEntry("📍", "где", listOf("место", "точка", "адрес", "здесь"), "where", "Связки"),
        EmojiEntry("❤️", "любить", listOf("обожать", "сердце"), "love", "Связки")
    )

    // Phonetic Alphabet Cipher mapping for exact symbol-level reversible encoding
    val ruCharToEmoji: Map<Char, String> = mapOf(
        'а' to "🍎", 'б' to "🍌", 'в' to "🍇", 'г' to "🍄", 'д' to "🍈",
        'е' to "🍉", 'ё' to "🦔", 'ж' to "🦒", 'з' to "🦓", 'и' to "🥑",
        'й' to "🫐", 'к' to "🥝", 'л' to "🍋", 'м' to "🥭", 'н' to "🥥",
        'о' to "🍊", 'п' to "🍐", 'р' to "🚀", 'с' to "🍓", 'т' to "🍅",
        'у' to "🦆", 'ф' to "🦩", 'х' to "🐹", 'ц' to "🌸", 'ч' to "🐢",
        'ш' to "🍫", 'щ' to "🦭", 'ъ' to "🧱", 'ы' to "🧀", 'ь' to "🪶",
        'э' to "⚡", 'ю' to "🪐", 'я' to "☀️", ' ' to "▫️"
    )

    val emojiToRuChar: Map<String, Char> by lazy {
        ruCharToEmoji.entries.associate { (k, v) -> v to k }
    }

    // Bidirectional Fast Lookup Maps
    val wordToEmoji: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        for (entry in entries) {
            map[entry.primaryWordRu.lowercase()] = entry.emoji
            map[entry.primaryWordEn.lowercase()] = entry.emoji
            for (syn in entry.synonymsRu) {
                map[syn.lowercase()] = entry.emoji
            }
        }
        map
    }

    val emojiToWordRu: Map<String, String> by lazy {
        entries.associate { it.emoji to it.primaryWordRu }
    }

    val emojiToWordEn: Map<String, String> by lazy {
        entries.associate { it.emoji to it.primaryWordEn }
    }
}
