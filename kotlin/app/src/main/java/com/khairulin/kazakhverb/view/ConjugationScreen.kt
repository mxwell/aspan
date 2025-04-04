package com.khairulin.kazakhverb.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khairulin.kazakhverb.vm.ConjugationVM

@Composable
fun ConjugationScreen(
    modifier: Modifier = Modifier,
    conjugationVM: ConjugationVM
) {
    val suggestions by conjugationVM.suggestionsFlow.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Conjugation Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                conjugationVM.lastEntered,
                onValueChange = { it: String ->
                    conjugationVM.onVerbChange(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                label = {
                    Text("verb")
                }
            )
            OutlinedButton(
                onClick = {
                    conjugationVM.onSubmit()
                },
                modifier = Modifier
                    .size(50.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = "submit"
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, start = 10.dp, end = 10.dp, bottom = 4.dp),
            shape = RectangleShape,
        ) {
            LazyColumn {
                items(suggestions) { suggestion: String ->
                    Text(
                        text = suggestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { conjugationVM.onSuggestionClick(suggestion) }
                            .padding(16.dp)
                    )
                }
            }
        }

        // TODO
    }
}

@Preview(showBackground = true)
@Composable
fun ConjugationScreenPreview() {
    MaterialTheme {
        ConjugationScreen(conjugationVM = viewModel())
    }
}