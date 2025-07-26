package com.khairulin.kazakhverb.latex

import com.khairulin.kazakhverb.grammar.*

/**
 * Usage:
 *    ./gradlew :latex:run && tectonic --keep-intermediates latex/output.tex
 */
class LatexGenerator {
    private fun getPartColor(part: PhrasalPart): PartColor? {
        return when (part.partType) {
            PhrasalPartType.VerbBase -> PartColor.verbBase
            PhrasalPartType.VerbTenseAffix -> PartColor.tenseAffix
            PhrasalPartType.VerbPersonalAffix -> PartColor.personalAffix
            else -> null
        }
    }

    fun generateTable(sb: StringBuilder, caption: String, rows: List<Pair<String, Phrasal>>) {
        sb.apply {
            append("\\begin{table}[h]\n")
            append("\\centering\n")
            append("\\caption*{\\textcolor{sectiongray}{${caption}}}\n")

            append("\\bgroup\n")
            append("\\def\\arraystretch{1.03}%\n")
            append("\\begin{tabular}{rl}\n")  // No vertical lines
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    append("\\arrayrulecolor{horline}\\hline\n")
                }
                append("${row.first} & \\bf{")
                for (part in row.second.parts) {
                    val color = getPartColor(part)
                    if (color != null) {
                        append("\\textcolor{${color.name}}{${part.content}}")
                    } else {
                        append(part.content)
                    }
                }

                append("} \\\\\n")
            }
            append("\\end{tabular}\n")
            append("\\egroup\n")
            append("\\end{table}\n")
        }
    }

    fun generateImports(sb: StringBuilder) {
        val font = "DejaVu Serif"
        sb.apply {
            append("\\usepackage{booktabs}\n")
            append("\\usepackage{colortbl}\n")

            append("\\usepackage{fontspec}\n")
            append("\\usepackage{polyglossia}\n")
            append("\\setdefaultlanguage{russian}\n")
            append("\\setmainfont{${font}}\n")
            append("\\newfontfamily\\cyrillicfont{${font}}\n")

            append("\\usepackage{xcolor}\n")
            // Custom colors
            append("\\definecolor{${PartColor.verbBase.name}}{RGB}{49, 151, 149}\n")
            append("\\definecolor{${PartColor.tenseAffix.name}}{RGB}{221, 107, 32}\n")
            append("\\definecolor{${PartColor.personalAffix.name}}{RGB}{90, 103, 216}\n")
            append("\\definecolor{horline}{RGB}{226, 232, 240}\n")

            append("\\usepackage{caption}\n")

            append("\\usepackage[a6paper, top=16mm, left=6mm, right=6mm, bottom=6mm]{geometry}\n")
            append("\\usepackage{fancyhdr}\n")

            append("\\usepackage{titletoc}\n")
            append("\\contentsmargin{2em}\n")
            append("\\dottedcontents{chapter}[3em]{\\bfseries\\large}{3em}{1pc}\n")
            append("\\dottedcontents{section}[4em]{\\large}{3em}{1pc}\n")

            append("\\usepackage{hyperref}\n")
            append("\\hypersetup{colorlinks=true, linktoc=all, linkcolor=blue}\n")

            append("\\usepackage{titlesec}\n")
            append("\\definecolor{sectiongray}{RGB}{128,128,128}\n")
            // Customize section formatting
            append("\\titlespacing*{\\section}{0pt}{0.1em}{0.1em}\n")
            append("\\titleformat{\\section}\n")
            append("{\\centering\\color{sectiongray}\\normalfont\\Large\\bfseries}\n")  // Gray, large, bold
            append("{}\n")              // No section number
            append("{1em}\n")           // Space between number and title
            append("{}\n")              // Code before title (empty)
        }
    }

    private val kTenseDescriptions = listOf<Pair<String, String>>(
        Pair(
            "Настояще-будущее время",
            "Также называется переходным временем. Обозначает обычные действия или уверенное будущее. Время определяется контекстом."
        ),
        Pair(
            "Настоящее время",
            "Обозначает действие, происходящее в данный момент. Есть 4 возможных вспомогательных глагола. Для упрощения в справочнике все формы используют вспомогательный глагол «жату»."
        ),
        Pair(
            "Прошедшее время",
            "Обозначает действие в прошлом без уточнения времени."
        ),
        Pair(
            "Давнопрошедшее очевидное время",
            "Обозначает действие в далеком прошлом, очевидцем которого был говорящий."
        ),
        Pair(
            "Давнопрошедшее неочевидное время",
            "Обозначает действие в далеком прошлом, которому говорящий не был свидетелем."
        ),
        Pair(
            "Прошедшее переходное время",
            "Обозначает длительное, повторяющееся или привычное действие в прошлом."
        ),
        Pair(
            "Будущее предположительное время",
            "Обозначает вероятное будущее действие."
        ),
        Pair(
            "Будущее время намерения",
            "Обозначает действие в будущем, выражающее намерение что-либо сделать."
        ),
        Pair(
            "Условное наклонение",
            "Обозначает условие в сложных предложениях."
        ),
        Pair(
            "Повелительное наклонение",
            "Обозначает побуждение к действию в виде приказа, просьбы, совета."
        ),
        Pair(
            "Желательное наклонение",
            "Обозначает желание говорящего или другого лица."
        ),
    )

    private fun addLicensePage(sb: StringBuilder) {
        sb.apply {
            append("\\clearpage\n")
            append("\\thispagestyle{empty}\n")
            append("\\vspace*{\\fill}\n")
            append("\\begin{center}\n")
            append("Эта книга лицензирована под лицензией \\href{https://creativecommons.org/licenses/by-sa/4.0/}{Creative Commons BY-SA 4.0 International License}\n")
            append("\\end{center}\n")
            append("\\vspace*{\\fill}\n")
        }
    }

    fun generate(verbs: List<VerbEntry>): String {
        val sb = StringBuilder()
        sb.append("\\documentclass{book}\n")
        generateImports(sb)

        sb.apply {
            append("\\title{Справочник по спряжению. \\ ${verbs.size} глаголов}\n")
            append("\\author{Проект «Kazakh Verb»}\n")
            append("\\date{\\today}\n")

            append("\\begin{document}\n")
            append("\\pagestyle{plain}\n")

            append("\\maketitle\n")  // This will show title, author, and date

            append("\\tableofcontents\n")

            append("\\addcontentsline{toc}{chapter}{Введение}\n")
            append("\\chapter*{Введение}\n")

            append("Данный справочник содержит таблицы с формами спряжения глаголов казахского языка. Таблицы созданы с помощью программного генератора форм из проекта «Kazakh Verb». Проект «Kazakh Verb» включает в себя веб-сайт и мобильное приложение. Данный справочник призван заменить их в условиях технических ограничений.\n")
            append("\n\n")
            append("Справочник покрывает 100 глаголов. Список глаголов основан на списке популярных глаголов, опубликованном на веб-сайте \\href{https://www.kaz-tili.kz/}{www.kaz-tili.kz} Татьяны Валяевой.\n")
            append("\n\n")
            append("Каждый глагол представлен набором таблиц, содержащих основные времена и наклонения казахского языка.\n")
            append("\n\n")

            for ((title, description) in kTenseDescriptions) {
                append("\\textbf{${title}.} ")
                append(description)
                append("\n\n")
            }

            append("\\addcontentsline{toc}{chapter}{Спряжение}\n")
            append("\\chapter*{Спряжение}\n")
            append("\\newpage\n")
            append("\\pagestyle{fancy}\n")
            append("\\fancyhf{}\n")
            append("\\fancyhead[R]{\\thepage}\n")


        }

        val formGenerator = FormGenerator()
        val possFormGenerator = FormGenerator(true)
        val jatuBuilder = VerbBuilder("жату")

        for (verbEntry in verbs) {
            val builder = verbEntry.builder()
            sb.apply {
                append("\\newpage\n")

                val exception = if (verbEntry.forceExceptional) {
                    " (исключение)"
                } else {
                    ""
                }
                val sectionName = "${verbEntry.verbDictForm}${exception}"
                append("\\fancyhead[L]{${sectionName}}\n")
                append("\\addcontentsline{toc}{section}{${sectionName}}")
                append("\\section*{${sectionName}}\n")
            }
            generateTable(sb, "Настояще-будущее время", formGenerator.generate { grammarForm ->
                builder.presentTransitiveForm(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            generateTable(sb, "Настоящее время", formGenerator.generate { grammarForm ->
                builder.presentContinuousForm(grammarForm.person, grammarForm.number, SentenceType.Statement, jatuBuilder)
            })
            sb.append("\\clearpage\n")
            generateTable(sb, "Прошедшее время", formGenerator.generate { grammarForm ->
                builder.past(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            generateTable(sb, "Давнопрошедшее очевидное время", formGenerator.generate { grammarForm ->
                builder.remotePast(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            sb.append("\\clearpage\n")
            generateTable(sb, "Давнопрошедшее неочевидное время", formGenerator.generate { grammarForm ->
                builder.pastUncertainTense(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            generateTable(sb, "Прошедшее переходное время", formGenerator.generate { grammarForm ->
                builder.pastTransitiveTense(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            sb.append("\\clearpage\n")
            generateTable(sb, "Будущее предположительное время", formGenerator.generate { grammarForm ->
                builder.possibleFutureForm(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            generateTable(sb, "Будущее время намерения", formGenerator.generate { grammarForm ->
                builder.intentionFutureForm(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            sb.append("\\clearpage\n")
            generateTable(sb, "Условное наклонение", formGenerator.generate { grammarForm ->
                builder.conditionalMood(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            generateTable(sb, "Повелительное наклонение", formGenerator.generate { grammarForm ->
                builder.imperativeMood(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
            sb.append("\\clearpage\n")
            generateTable(sb, "Желательное наклонение", possFormGenerator.generate { grammarForm ->
                builder.optativeMood(grammarForm.person, grammarForm.number, SentenceType.Statement)
            })
        }

        addLicensePage(sb)

        sb.append("\\end{document}")

        return sb.toString()
    }
}