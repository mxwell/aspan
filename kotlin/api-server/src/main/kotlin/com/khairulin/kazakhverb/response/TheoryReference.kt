package com.khairulin.kazakhverb.response

import kotlinx.serialization.Serializable

@Serializable
data class TheoryReference(
    val referenceType: TheoryReferenceType,
    val title: String,
    val author: String,
    val url: String,
)
