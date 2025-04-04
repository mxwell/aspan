package com.khairulin.kazakhverb.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.example.Phrasal
import org.example.PhrasalPart
import org.example.PhrasalPartType

enum class PartColor(val color: Color) {
    verbBase(Color(49, 151, 149)),
    verbTenseAffix(Color(221, 107, 32)),
    verbPersonalAffix(Color(90, 103, 216)),
    verbNegation(Color(229, 62, 62)),
    default(Color.Black),
    ;

    companion object {
        fun getPartColorByPartType(partType: PhrasalPartType): PartColor {
            return when(partType) {
                PhrasalPartType.VerbBase -> verbBase
                PhrasalPartType.VerbTenseAffix -> verbTenseAffix
                PhrasalPartType.VerbPersonalAffix -> verbPersonalAffix
                PhrasalPartType.VerbNegation -> verbNegation
                else -> default
            }
        }
    }
}

fun isMainPart(part: PhrasalPart): Boolean {
    return when(part.partType) {
        PhrasalPartType.VerbBase, PhrasalPartType.VerbTenseAffix -> true
        PhrasalPartType.VerbPersonalAffix, PhrasalPartType.VerbNegation -> true
        else -> false
    }
}

@Composable
fun HighlightedPart(part: PhrasalPart) {

}

@Composable
fun HighlightedPhrasal(
    modifier: Modifier = Modifier,
    prefix: String,
    phrasal: Phrasal
) {
    Text(
        text = buildAnnotatedString {
            append(prefix)
            phrasal.parts.forEach { part ->
                val partColor = PartColor.getPartColorByPartType(part.partType)
                val color = if (partColor != PartColor.default) {
                    partColor.color
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
                if (isMainPart(part)) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = color)) {
                        append(part.content)
                    }
                } else {
                    withStyle(SpanStyle(color = color)) {
                        append(part.content)
                    }
                }
            }
        },
        modifier = modifier,
    )
}