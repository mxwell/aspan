package com.khairulin.kazakhverb.vm

import com.khairulin.kazakhverb.grammar.PhrasalBuilder

data class TenseInfo(
    val tenseId: TenseId,
    val forms: List<TenseFormRow>,
) {
    companion object {
        fun preview(): TenseInfo {
            return TenseInfo(
                TenseId.presentContinuous,
                listOf(
                    TenseFormRow(
                        "мен",
                        PhrasalBuilder()
                            .verbBase("жүрекі қобалж")
                            .tenseAffix("и")
                            .personalAffix("мын")
                            .build()
                    ),
                    TenseFormRow(
                        "сен",
                        PhrasalBuilder()
                            .verbBase("жүрекі қобалж")
                            .tenseAffix("и")
                            .personalAffix("сың")
                            .build()
                    )
                )
            )
        }
    }
}
