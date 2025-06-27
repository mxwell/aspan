package com.khairulin.kazakhverb.grammar

enum class Septik(val index: Int, val ruShort: String) {
    Atau(0, "именит."),
    Ilik(1, "родит."),
    Barys(2, "дат."),
    Tabys(3, "винит."),
    Jatys(4, "мест."),
    Shygys(5, "исход."),
    Komektes(6, "творит."),
    ;

    companion object {
        fun ofIndex(index: Int): Septik {
            return entries[index]
        }
    }
}