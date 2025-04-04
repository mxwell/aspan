package com.khairulin.kazakhverb.vm

import android.util.Log
import com.khairulin.kazakhverb.config.ConfigSection
import org.example.GrammarNumber
import org.example.GrammarPerson
import org.example.Phrasal
import org.example.PhrasalBuilder
import org.example.Rules
import org.example.SentenceType
import org.example.StrManip
import org.example.VerbBuilder

class Generator {

    companion object {
        private val TAG = "Generator"

        val kNormPronouns = listOf(
            "мен",
            "біз",
            "сен",
            "сендер",
            "Сіз",
            "Сіздер",
            "ол",
            "олар",
        )

        val kGenPronouns = listOf(
            "менің",
            "біздің",
            "біздің",
            "сендердің",
            "Сіздің",
            "Сіздердің",
            "оның",
            "олардың",
        )

        val kFormParams = listOf(
            FormParams(GrammarPerson.First, GrammarNumber.Singular),
            FormParams(GrammarPerson.First, GrammarNumber.Plural),
            FormParams(GrammarPerson.Second, GrammarNumber.Singular),
            FormParams(GrammarPerson.Second, GrammarNumber.Plural),
            FormParams(GrammarPerson.SecondPolite, GrammarNumber.Singular),
            FormParams(GrammarPerson.SecondPolite, GrammarNumber.Plural),
            FormParams(GrammarPerson.Third, GrammarNumber.Singular),
            FormParams(GrammarPerson.Third, GrammarNumber.Plural),
        )
    }

    private val continuousAuxBuilders: Map<ContinuousAuxVerb, VerbBuilder> by lazy {
        val map = mutableMapOf<ContinuousAuxVerb, VerbBuilder>()
        for (auxVerb in ContinuousAuxVerb.entries) {
            val builder = VerbBuilder(auxVerb.verb)
            map[auxVerb] = builder
        }
        map
    }

    private fun buildForm(
        builder: VerbBuilder,
        tenseId: TenseId,
        person: GrammarPerson,
        number: GrammarNumber,
        sentenceType: SentenceType,
        contAux: ContinuousAuxVerb,
    ): Phrasal {
        return when (tenseId) {
            TenseId.presentTransitive -> builder.presentTransitiveForm(person, number, sentenceType)
            TenseId.presentContinuous -> {
                val auxBuilder = continuousAuxBuilders[contAux]
                if (auxBuilder == null) {
                    PhrasalBuilder.NOT_SUPPORTED_PHRASAL
                } else {
                    builder.presentContinuousForm(person, number, sentenceType, auxBuilder)
                }
            }
            TenseId.past -> builder.past(person, number, sentenceType)
            TenseId.remotePast -> builder.remotePast(person, number, sentenceType)
            TenseId.conditionalMood -> builder.conditionalMood(person, number, sentenceType)
            TenseId.imperativeMood -> builder.imperativeMood(person, number, sentenceType)
            TenseId.optativeMood -> builder.optativeMood(person, number, sentenceType)
        }
    }

    fun generateTense(
        tenseId: TenseId,
        formConfig: ConfigSection,
        verb: String,
        sentenceType: SentenceType,
        contAux: ContinuousAuxVerb,
        conjType: ConjugationType
    ): TenseInfo? {
        val rows = mutableListOf<TenseFormRow>()

        val pronouns = if (tenseId == TenseId.optativeMood) kGenPronouns else kNormPronouns

        val builder = try {
            VerbBuilder(verb, forceExceptional = conjType == ConjugationType.exceptionVerb)
        } catch (e: Exception) {
            Log.e(TAG, "generateTense: failed to create VerbBuilder")
            return null
        }

        for (i in pronouns.indices) {
            if (!formConfig.settings[i].on) {
                continue
            }

            val pronoun = pronouns[i]
            val params = kFormParams[i]

            val form = buildForm(
                builder,
                tenseId,
                params.grammarPerson,
                params.grammarNumber,
                sentenceType,
                contAux)

            rows.add(TenseFormRow(pronoun, form))
        }

        return TenseInfo(tenseId, rows)
    }

    fun isOptExcept(verb: String): Boolean {
        val lastWord = StrManip.getLastWord(verb)
        return Rules.OPT_EXCEPT_VERB_MEANINGS.containsKey(lastWord)
    }
}