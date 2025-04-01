package org.example

import kotlinx.serialization.Serializable

@Serializable
data class TestsetRow(
    val verb: String,
    val forceExceptional: Boolean,
    val tenses: List<PlusFormSet>,
)
