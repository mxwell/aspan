package com.khairulin.kazakhverb.grammar

enum class Septik {
    Atau,
    Ilik,
    Barys,
    Tabys,
    Jatys,
    Shygys,
    Komektes,
    ;

    companion object {
        fun ofIndex(index: Int): Septik {
            return entries[index]
        }
    }
}