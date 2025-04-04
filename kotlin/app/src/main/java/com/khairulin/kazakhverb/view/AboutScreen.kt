package com.khairulin.kazakhverb.view

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
) {
    val version = extractVersion(LocalContext.current)
    val linkColor = MaterialTheme.colorScheme.tertiary
    val websiteText = "kazakhverb.khairulin.com"
    val websiteUrl = "https://kazakhverb.khairulin.com/"
    val websiteLink = buildAnnotatedString {
        append("⦁ ")
        withLink(LinkAnnotation.Url(url = websiteUrl)) {
            withStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline, color = linkColor)
            ) {
                append(websiteText)
            }
        }
    }
    val emailText = "kazakhverb@khairulin.com"
    val emailUrl = "mailto:kazakhverb@khairulin.com"
    val emailLink = buildAnnotatedString {
        append("⦁ ")
        withLink(LinkAnnotation.Url(url = emailUrl)) {
            withStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline, color = linkColor)
            ) {
                append(emailText)
            }
        }
    }
    val refText = "«Казахский язык. Просто о сложном»"
    val refUrl = "https://www.kaz-tili.kz/"
    val refLink = buildAnnotatedString {
        append("⦁ Валяева Т. В. ")
        withLink(LinkAnnotation.Url(url = refUrl)) {
            withStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline, color = linkColor)
            ) {
                append(refText)
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 30.dp, 20.dp, 30.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "We're here to help",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "If you have any questions or need help, please contact us at:",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = websiteLink,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = emailLink,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Reference",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "The most important source used while implementing the app’s grammar part:",
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = refLink,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "About app",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "App version: $version"
            )
        }
    }
}

private fun extractVersion(context: Context): String {
    return try {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "n/a"
    } catch (e: Exception) {
        "n/a"
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    MaterialTheme {
        AboutScreen()
    }
}