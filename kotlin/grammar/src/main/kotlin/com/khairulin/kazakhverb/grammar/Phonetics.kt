package com.khairulin.kazakhverb.grammar

object Phonetics {
    fun genuineVowel(char: Char): Boolean {
        return Rules.VOWELS_EXCEPT_U_I.contains(char)
    }

    enum class SoftHardType {
        SOFT_STRONG,
        SOFT_WEAK,
        NEUTRAL,
        HARD_WEAK,
        HARD_STRONG
    }

    val VOWELS_BY_SOFT_HARD: Map<Char, SoftHardType> = mapOf(
        'ә' to SoftHardType.SOFT_STRONG,
        'е' to SoftHardType.SOFT_STRONG,
        'ө' to SoftHardType.SOFT_STRONG,
        'ү' to SoftHardType.SOFT_STRONG,
        'і' to SoftHardType.SOFT_STRONG,
        'и' to SoftHardType.SOFT_WEAK,
        'ю' to SoftHardType.NEUTRAL,
        'у' to SoftHardType.NEUTRAL,
        'а' to SoftHardType.HARD_STRONG,
        'о' to SoftHardType.HARD_STRONG,
        'ұ' to SoftHardType.HARD_STRONG,
        'ы' to SoftHardType.HARD_STRONG,
        'я' to SoftHardType.HARD_STRONG
    )

    fun wordIsSoft(lowercase: String): Boolean {
        val exceptional = Rules.HARD_SOFT_EXCEPTIONS[lowercase]
        if (exceptional != null) {
            return exceptional
        }

        for (char in lowercase.reversed()) {
            val vtype = VOWELS_BY_SOFT_HARD[char]
            if (vtype == null) {
                continue
            }
            if (vtype == SoftHardType.SOFT_STRONG || vtype == SoftHardType.SOFT_WEAK) {
                return true
            }
            if (vtype == SoftHardType.HARD_STRONG) {
                return false
            }
        }

        return false
    }
}