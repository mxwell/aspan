package com.khairulin.kazakhverb.task

import com.khairulin.kazakhverb.grammar.Septik

object VerbList {
    data class Entry(
        val verb: VerbInfo,
        val supplements: List<SupplementNoun>
    )

    private fun verbTr(verb: String, translation: String) = VerbInfo(verb, translation = translation)

    val entries = listOf(
        Entry(verbTr("болу", "быть"), listOf(
            SupplementNoun("орталық", "центр", Septik.Jatys),
            SupplementNoun("мектеп", "школа", Septik.Jatys),
        )),
        Entry(verbTr("деу", "сказать"), listOf(
            SupplementNoun("дұрыс", "правильно", null),
            SupplementNoun("шындық", "правда", Septik.Tabys),
        )),
        Entry(verbTr("айту", "сказать"), listOf(
            SupplementNoun("ақиқат", "правда", Septik.Tabys),
            SupplementNoun("бала", "ребёнок", Septik.Barys),
            SupplementNoun("өтірік", "ложь", Septik.Atau),
        )),
        Entry(verbTr("ән айту", "петь"), listOf(
            SupplementNoun("қатты", "громко", null),
        )),
        Entry(verbTr("істеу", "делать"), listOf(
            SupplementNoun("тапсырма", "задание", Septik.Tabys),
            SupplementNoun("жұмыс", "работа", Septik.Tabys),
        )),
        Entry(verbTr("алу", "брать"), listOf(
            SupplementNoun("кітап", "книга", null),
            SupplementNoun("дүкен", "магазин", Septik.Shygys),
            SupplementNoun("сыйлық", "подарок", null),
            SupplementNoun("ақша", "деньги", null),
        )),
        Entry(verbTr("келу", "приезжать"), listOf(
            SupplementNoun("үй", "дом", Septik.Barys),
            SupplementNoun("жұмыс", "работа", Septik.Shygys),
            SupplementNoun("жұмыс", "работа", Septik.Barys),
        )),
        Entry(verbTr("көру", "смотреть"), listOf(
            SupplementNoun("фильм", "фильм", Septik.Tabys),
            SupplementNoun("теледидар", "телевизор", Septik.Shygys),
            SupplementNoun("сурет", "картина", Septik.Tabys),
        )),
        Entry(verbTr("жүру", "идти"), listOf(
            SupplementNoun("мектеп", "школа", Septik.Barys),
            SupplementNoun("саябақ", "парк", Septik.Jatys),
        )),
        Entry(verbTr("жұмыс істеу", "работать"), listOf(
            SupplementNoun("кеңсе", "офис", Septik.Jatys),
            SupplementNoun("маңызды", "важный", null),
        )),
        Entry(verbTr("беру", "давать"), listOf(
            SupplementNoun("тапсырма", "задание", null),
            SupplementNoun("кітап", "книга", null),
            SupplementNoun("рұқсат", "разрешение", null),
            SupplementNoun("дос", "друг", Septik.Barys, ownedBySubject = true),
        )),
        Entry(verbTr("жоспарлау", "планировать"), listOf(
            SupplementNoun("саяхат", "путешествие", Septik.Atau),
            SupplementNoun("демалыс", "отпуск", Septik.Tabys),
            SupplementNoun("құрылыс", "строительство", Septik.Tabys),
        )),
        Entry(verbTr("өту", "проходить"), listOf(
            SupplementNoun("емтихан", "экзамен", Septik.Shygys),
            SupplementNoun("көпір", "мост", Septik.Shygys),
            SupplementNoun("су", "вода", Septik.Shygys),
        )),
        Entry(verbTr("жасау", "делать"), listOf(
            SupplementNoun("жоспар", "план", Septik.Atau),
            SupplementNoun("тапсырма", "задание", Septik.Atau),
            SupplementNoun("кешкі ас", "ужин", Septik.Atau),
            SupplementNoun("шай", "чай", Septik.Atau),
        )),
        Entry(verbTr("бару", "идти"), listOf(
            SupplementNoun("дәрігер", "доктор", Septik.Barys),
            SupplementNoun("қала", "город", Septik.Barys),
            SupplementNoun("ауыл", "аул", Septik.Barys),
        )),
        Entry(verbTr("бару", "ехать"), listOf(
            SupplementNoun("ат", "лошадь", Septik.Komektes),
        )),
        Entry(verbTr("алып бару", "отнести"), listOf(
            SupplementNoun("дәрігер", "доктор", Septik.Barys),
            SupplementNoun("пошта", "почта", Septik.Barys),
        )),
        Entry(verbTr("ету", "делать"), listOf(
            SupplementNoun("дұрыс таңдау", "правильный выбор", Septik.Atau),
            SupplementNoun("рұқсат", "разрешение", Septik.Atau),
            SupplementNoun("хабар", "известие", Septik.Atau),
        )),
        Entry(verbTr("қызмет ету", "служить"), listOf(
            SupplementNoun("әскер", "армия", Septik.Jatys),
            SupplementNoun("кеме", "судно", Septik.Jatys),
        )),
        Entry(verbTr("көрсету", "показывать"), listOf(
            SupplementNoun("сурет", "картина", Septik.Atau),
            SupplementNoun("құжат", "документ", Septik.Tabys),
            SupplementNoun("жол", "дорога", Septik.Atau),
        )),
        Entry(verbTr("күту", "ждать"), listOf(
            SupplementNoun("жәрдем", "помощь", Septik.Atau),
            SupplementNoun("кезек", "очередь", Septik.Atau),
            SupplementNoun("автобус", "автобус", Septik.Atau),
            SupplementNoun("ұзақ", "долго", null),
        )),
        Entry(verbTr("жүргізу", "водить"), listOf(
            SupplementNoun("көлік", "транспорт", Septik.Atau),
        )),
        Entry(verbTr("қарау", "смотреть"), listOf(
            SupplementNoun("терезе", "окно", Septik.Shygys),
            SupplementNoun("экран", "экран", Septik.Barys),
            SupplementNoun("үміт", "надежда", Septik.Komektes),
        )),
        Entry(verbTr("шығу", "выходить"), listOf(
            SupplementNoun("алға", "вперёд", null),
            SupplementNoun("дала", "улица", Septik.Barys),
            SupplementNoun("қала", "город", Septik.Shygys),
        )),
        Entry(verbTr("жазу", "писать"), listOf(
            SupplementNoun("хат", "письмо", Septik.Tabys),
            SupplementNoun("тақта", "доска", Septik.Barys),
            SupplementNoun("күнделік", "дневник", Septik.Barys),
            SupplementNoun("мақала", "статья", Septik.Tabys),
        )),
        Entry(verbTr("жету", "доходить"), listOf(
            SupplementNoun("мектеп", "школа", Septik.Barys),
            SupplementNoun("тау", "гора", Septik.Barys),
            SupplementNoun("межелі жер", "место назначения", Septik.Barys),
        )),
        Entry(verbTr("сұрау", "спрашивать"), listOf(
            SupplementNoun("рұқсат", "разрешение", Septik.Atau),
            SupplementNoun("көмек", "помощь", Septik.Atau),
            SupplementNoun("пікір", "мнение", Septik.Tabys),
        )),
        Entry(verbTr("оқу", "читать"), listOf(
            SupplementNoun("дауыстап", "вслух", Septik.Atau),
            SupplementNoun("мақала", "статья", Septik.Tabys),
        )),
        Entry(verbTr("оқу", "учиться"), listOf(
            SupplementNoun("университет", "университет", Septik.Jatys),
            SupplementNoun("мектеп", "школа", Septik.Jatys),
        )),
        Entry(verbTr("қалу", "оставаться"), listOf(
            SupplementNoun("үй", "дом", Septik.Jatys),
            SupplementNoun("қала", "город", Septik.Jatys),
            SupplementNoun("қонақта", "в гостях", null),
            SupplementNoun("осында", "здесь", null),
        )),
        Entry(verbTr("көздеу", "стремиться"), listOf(
            SupplementNoun("нысана", "цель", Septik.Tabys),
            SupplementNoun("бостандық", "свобода", Septik.Tabys),
            SupplementNoun("мақсат", "цель", Septik.Tabys),
        )),
        Entry(verbTr("түсу", "спускаться"), listOf(
            SupplementNoun("автобус", "автобус", Septik.Shygys),
        )),
        Entry(verbTr("түсу", "падать"), listOf(
            SupplementNoun("жер", "земля", Septik.Barys),
            SupplementNoun("су", "вода", Septik.Barys),
        )),
        Entry(verbTr("ұсыну", "предлагать"), listOf(
            SupplementNoun("көмек", "помощь", Septik.Tabys),
            SupplementNoun("жоба", "проект", Septik.Atau),
            SupplementNoun("тағам", "пища", Septik.Tabys),
        )),
        Entry(verbTr("қатысу", "принимать участие"), listOf(
            SupplementNoun("жарыс", "состязание", Septik.Barys),
            SupplementNoun("жиналыс", "собрание", Septik.Barys),
            SupplementNoun("әлем чемпионатына", "в чемпионате мира", null),
        )),
        Entry(verbTr("тұру", "стоять"), listOf(
            SupplementNoun("орнында", "на месте", null),
            SupplementNoun("көше", "улица", Septik.Jatys),
            SupplementNoun("мектептің алдында", "перед школой", null),
        )),
        Entry(verbTr("қабылдау", ""), listOf(
            SupplementNoun("қонақ", "гость", Septik.Tabys),
            SupplementNoun("шешім", "решение", Septik.Atau),
            SupplementNoun("тапсырма", "задание", Septik.Tabys),
        )),
        Entry(verbTr("көріну", "виднеться"), listOf(
            SupplementNoun("алыстан", "издалека", null),
            SupplementNoun("терезе", "окно", Septik.Shygys),
        )),
        Entry(verbTr("қою", "ставить"), listOf(
            SupplementNoun("кітап", "книга", Septik.Tabys),
            SupplementNoun("сөре", "полка", Septik.Barys),
            SupplementNoun("сұрақ", "вопрос", Septik.Atau),
        )),
        Entry(verbTr("қою", "класть"), listOf(
            SupplementNoun("гүл", "цветок", Septik.Tabys),
            SupplementNoun("үстел", "стол", Septik.Barys),
        )),
        Entry(verbTr("бастау", "начинать"), listOf(
            SupplementNoun("жұмыс", "работа", Septik.Tabys),
            SupplementNoun("кешке", "вечером", null),
            SupplementNoun("саяхат", "путешествие", Septik.Tabys),
        )),
        Entry(verbTr("сурет салу", "рисовать"), listOf(
            SupplementNoun("дәптер", "тетрадь", Septik.Barys),
        )),
        Entry(verbTr("қармақ салу", "закидывать удочку"), listOf(
            SupplementNoun("өзен", "река", Septik.Barys),
        )),
        Entry(verbTr("шатыр салу", "разбивать палатку"), listOf(
            SupplementNoun("орман", "лес", Septik.Jatys),
        )),
        Entry(verbTr("қосу", "добавить"), listOf(
            SupplementNoun("шай", "чай", Septik.Barys),
            SupplementNoun("қант", "сахар", Septik.Atau),
            SupplementNoun("файл", "файд", Septik.Atau),
            SupplementNoun("дыбыс", "звук", Septik.Atau),
        )),
        Entry(verbTr("кету", "отправляться"), listOf(
            SupplementNoun("үй", "дом", Septik.Shygys),
            SupplementNoun("пойыз", "поезд", Septik.Komektes),
            SupplementNoun("жұмыс", "работа", Septik.Shygys),
        )),
        Entry(verbTr("алып кету", "забрать на вынос"), listOf(
            SupplementNoun("кофе", "кофе", Septik.Atau),
            SupplementNoun("самса", "самса", Septik.Atau),
        )),
        Entry(verbTr("есту", "слышать"), listOf(
            SupplementNoun("әңгіме", "рассказ", Septik.Atau),
            SupplementNoun("жаңалық", "новость", Septik.Tabys),
            SupplementNoun("дауыс", "голос", Septik.Tabys),
        )),
        Entry(verbTr("тыңдау", "слушать"), listOf(
            SupplementNoun("әдемі әуен", "красивая мелодия", Septik.Atau),
            SupplementNoun("дәріс", "лекция", Septik.Atau),
            SupplementNoun("кеңес", "совет", Septik.Tabys),
        )),
        Entry(verbTr("тарту", "тянуть"), listOf(
            SupplementNoun("арқан", "верёвка", Septik.Tabys),
            SupplementNoun("жіп", "верёвка", Septik.Tabys),
            SupplementNoun("қатты", "сильно", null),
            SupplementNoun("арқан", "верёвка", Septik.Komektes),
            SupplementNoun("жүк", "груз", Septik.Atau),
        )),
        Entry(verbTr("түрту", "толкнуть"), listOf(
            SupplementNoun("есік", "дверь", Septik.Tabys),
        )),
        Entry(verbTr("шығару", "издавать"), listOf(
            SupplementNoun("кітап", "книга", Septik.Atau),
            SupplementNoun("газет", "газета", Septik.Atau),
        )),
        Entry(verbTr("шығару", "выносить"), listOf(
            SupplementNoun("шешім", "решение", Septik.Atau),
            SupplementNoun("жүк", "груз", Septik.Tabys),
        )),
        Entry(verbTr("білу", "знать"), listOf(
            SupplementNoun("жауап", "ответ", Septik.Tabys),
            SupplementNoun("жаңалық", "новость", Septik.Tabys),
            SupplementNoun("ереже", "правило", Septik.Tabys),
        )),
        Entry(verbTr("ұстау", "держать"), listOf(
            SupplementNoun("доп", "мяч", Septik.Tabys),
            SupplementNoun("қалам", "ручка", Septik.Tabys),
            SupplementNoun("есік", "дверь", Septik.Tabys),
        )),
        Entry(verbTr("мықтап ұстау", "крепко держать"), listOf(
            SupplementNoun("есік", "дверь", Septik.Tabys),
        )),
        Entry(verbTr("ойлау", "думать"), listOf(
            SupplementNoun("жоспар", "план", Septik.Atau),
            SupplementNoun("болашақ туралы", "о будущем", null),
        )),
        Entry(verbTr("қарастыру", "рассматривать"), listOf(
            SupplementNoun("нұсқа", "вариант", Septik.Tabys),
            SupplementNoun("мәселе", "проблема", Septik.Tabys),
            SupplementNoun("мүмкіндік", "возможность", Septik.Tabys),
        )),
        Entry(verbTr("іздеу", "искать"), listOf(
            SupplementNoun("кітап", "книга", Septik.Atau),
            SupplementNoun("жұмыс", "работа", Septik.Atau),
            SupplementNoun("жоғалған кілт", "потерянный ключ", Septik.Tabys, ownedBySubject = true),
        )),
        Entry(verbTr("құрау", "составлять"), listOf(
            SupplementNoun("сөйлем", "предложение", Septik.Atau),
            SupplementNoun("жоспар", "план", Septik.Atau),
            SupplementNoun("команда", "команда", Septik.Atau),
        )),
        Entry(verbTr("өткізу", "проводить"), listOf(
            SupplementNoun("уақыт", "время", Septik.Tabys),
            SupplementNoun("жарыс", "состязание", Septik.Atau),
            SupplementNoun("демалыс", "отпуск", Septik.Tabys, ownedBySubject = true),
        )),
        Entry(verbTr("табу", "находить"), listOf(
            SupplementNoun("шешім", "решение", Septik.Atau),
            SupplementNoun("дос", "", Septik.Atau),
            SupplementNoun("уақыт", "", Septik.Atau),
        )),
        Entry(verbTr("табу", "зарабатывать"), listOf(
            SupplementNoun("ақша", "деньги", Septik.Atau),
        )),
        Entry(verbTr("дайындалу", "готовиться"), listOf(
            SupplementNoun("емтихан", "экзамен", Septik.Barys),
            SupplementNoun("сапар", "поездка", Septik.Barys),
            SupplementNoun("кездесу", "встреча", Septik.Barys),
        )),
        Entry(verbTr("қуану", "радоваться"), listOf(
            SupplementNoun("жеңіс", "", Septik.Barys),
            SupplementNoun("бұған", "этому", null),
            SupplementNoun("қатты", "сильно", null),
        )),
        Entry(verbTr("жеу", "кушать"), listOf(
            SupplementNoun("самса", "самса", Septik.Tabys),
        )),
        Entry(verbTr("кию", "надевать"), listOf(
            SupplementNoun("аяқ киім", "обувь", Septik.Tabys),
            SupplementNoun("көйлек", "платье", Septik.Atau),
        )),
        Entry(verbTr("ашу", "открывать"), listOf(
            SupplementNoun("есік", "дверь", Septik.Atau),
            SupplementNoun("есік", "дверь", Septik.Tabys),
        )),
        Entry(verbTr("жақындау", "приближаться"), listOf(
            SupplementNoun("қасқыр", "волк", Septik.Barys),
        )),
    )
}