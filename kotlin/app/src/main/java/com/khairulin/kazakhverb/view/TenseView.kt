package com.khairulin.kazakhverb.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.khairulin.kazakhverb.vm.ConjugationVM
import com.khairulin.kazakhverb.vm.ContinuousAuxVerb
import com.khairulin.kazakhverb.vm.TenseId
import com.khairulin.kazakhverb.vm.TenseInfo

@Composable
fun TenseView(
    info: TenseInfo,
    conjugationVM: ConjugationVM
) {
    val tenseId = info.tenseId

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 10.dp, end = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            tenseId.title,
            modifier = Modifier,
            color = Color.Red,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Start,
        )
        Text(
            tenseId.description,
            modifier = Modifier,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
        )

        if (tenseId == TenseId.presentContinuous) {
            val options = ContinuousAuxVerb.entries
            val cur = options[conjugationVM.contAuxVerbIndex]

            Box (
                modifier = Modifier
                    .padding(6.dp)
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded }
                ) {
                    Text(cur.verb)
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "drop down",
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEachIndexed { index, option ->
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                conjugationVM.onContAuxVerbChange(index)
                            },
                            text = {
                                Text(option.verb)
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            info.forms.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                }

                SelectionContainer {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = row.pronoun,
                            modifier = Modifier
                                .weight(0.3f)
                                .wrapContentWidth(Alignment.End)
                                .padding(end = 8.dp)
                        )

                        HighlightedPhrasal(
                            modifier = Modifier
                                .weight(0.7f)
                                .wrapContentWidth(Alignment.Start),
                            prefix = "",
                            phrasal = row.form,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TenseViewPreview() {
    MaterialTheme {
        TenseView(
            info = TenseInfo.preview(),
            conjugationVM = viewModel()
        )
    }
}