package com.khairulin.kazakhverb.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khairulin.kazakhverb.vm.ConjugationType
import com.khairulin.kazakhverb.vm.ConjugationVM
import com.khairulin.kazakhverb.vm.ViewModelState
import org.example.SentenceType

@Composable
fun ConjugationScreen(
    modifier: Modifier = Modifier,
    conjugationVM: ConjugationVM
) {
    val suggestions by conjugationVM.suggestionsFlow.collectAsState()
    val tenses by conjugationVM.tensesFlow.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    fun onSubmit() {
        keyboardController?.hide()
        conjugationVM.onSubmit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
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
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onSubmit()
                    }
                ),
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
                    onSubmit()
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

        if (suggestions.isNotEmpty()) {
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
                                .clickable {
                                    keyboardController?.hide()
                                    conjugationVM.applySuggestion(suggestion)
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        } else {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .padding(vertical = 10.dp)
            ) {
                val options = SentenceType.entries
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = {
                            conjugationVM.onSentenceTypeChange(index)
                        },
                        selected = index == conjugationVM.selectedSentenceTypeIndex,
                        label = {
                            Text(label.name)
                        }
                    )
                }
            }

            if (conjugationVM.state == ViewModelState.awaitingInput) {
                Icon(
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 10.dp),
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "submit"
                )
                Text(
                    "Enter a Kazakh verb into the field above to see its conjugation forms",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                )
                Row(
                    modifier = Modifier
                        .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Examples:")
                    Text(
                        "келу",
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(10.dp)
                            .clickable {
                                conjugationVM.applySuggestion("келу")
                            }
                    )
                    Text("or")
                    Text(
                        "алу",
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(10.dp)
                            .clickable {
                                conjugationVM.applySuggestion("алу")
                            }
                    )
                }
            } else if (conjugationVM.state == ViewModelState.loadingForms) {
                Spacer(
                    modifier = Modifier
                        .weight(2f)
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .weight(1f)
                        .width(64.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    "Generating conjugation for «${conjugationVM.loadedVerb}»…",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                )
                Spacer(
                    modifier = Modifier
                        .weight(2f)
                )
            } else if (conjugationVM.state == ViewModelState.loadedForms) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (conjugationVM.optionalExceptional) {
                        Row(
                            modifier = Modifier
                                .padding(start = 10.dp, top = 20.dp, end = 10.dp, bottom = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "info",
                                modifier = Modifier
                                    .size(32.dp)
                            )
                            Text(
                                "The entered verb can be conjugated in two different ways, as a regular verb and as an exceptional verb",
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                            )
                        }
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                        ) {
                            val options = ConjugationType.entries
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    ),
                                    onClick = {
                                        conjugationVM.onConjugationTypeChange(index)
                                    },
                                    selected = index == conjugationVM.conjugationTypeIndex,
                                    label = {
                                        Text(label.title)
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "Conjugation of the verb «${conjugationVM.loadedVerb}»",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    tenses.forEach {
                        TenseView(info = it, conjugationVM = conjugationVM)
                    }
                }
            } else if (conjugationVM.state == ViewModelState.notFound) {
                Text(
                    text = "The entered word doesn't seem to be a Kazakh verb. Please try again",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(20.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConjugationScreenPreview() {
    MaterialTheme {
        ConjugationScreen(conjugationVM = viewModel())
    }
}