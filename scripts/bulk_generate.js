const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const kDetectorFormsCommand = "detector_forms";
const kDetectSuggestCommand = "detect_suggest_forms";
const kSuggestInfinitiveCommand = "suggest_infinitive";
const kSuggestInfinitiveTranslationCommand = "suggest_infinitive_translation";
const kPresentContinuousFormsCommand = "present_continuous_forms";
const kVerbsWithMetaCommand = "verbs_with_meta";
const kTestsetCommand = "testset";

const KAZAKH_WEIGHT = 0.5;
const SHORT_VERB_WEIGHT = KAZAKH_WEIGHT / 2;
const EXACT_MATCH_WEIGHT = SHORT_VERB_WEIGHT / 2;
const FREQ_WEIGHT_PORTION = EXACT_MATCH_WEIGHT / 8;
const STATEMENT_WEIGHT = FREQ_WEIGHT_PORTION / 2;
const FORM_WEIGHT_PORTION = STATEMENT_WEIGHT / 8;
const SECOND_PLURAL_WEIGHT   = FORM_WEIGHT_PORTION * 1;
const FIRST_PLURAL_WEIGHT    = FORM_WEIGHT_PORTION * 2;
const SECOND_SINGULAR_WEIGHT = FORM_WEIGHT_PORTION * 3;
const FIRST_SINGULAR_WEIGHT  = FORM_WEIGHT_PORTION * 4;
const THIRD_PERSON_WEIGHT    = FORM_WEIGHT_PORTION * 5;
const INFINITIVE_WEIGHT      = FORM_WEIGHT_PORTION * 6;

class WeightedForm {
    constructor(form, weight, sentenceTypeIndex, tenseName, person, number) {
        this.form = form;
        this.weight = weight;
        this.sentenceTypeIndex = sentenceTypeIndex;
        this.tenseName = tenseName;
        this.person = person;
        this.number = number;
    }
}

function countSpaces(s) {
    let spaces = 0;
    for (let i = 0; i < s.length; ++i) {
        if (s[i] == ' ') {
            ++spaces;
        }
    }
    return spaces;
}

const SENTENCE_TYPES = ["Statement", "Negative"];
const PERSONS = [aspan.GrammarPerson.First, aspan.GrammarPerson.Second, aspan.GrammarPerson.SecondPolite, aspan.GrammarPerson.Third];
const NUMBERS = [aspan.GrammarNumber.Singular, aspan.GrammarNumber.Plural];

class FormBuilder {
    constructor(verb, limitWords) {
        this.verb = verb;
        this.limitWords = limitWords;
        this.spaces = countSpaces(verb);
    }
    createForms(sentenceTypeIndex, tenseName, caseFn, formsOut) {
        let spaces = this.spaces;
        let limitWords = this.limitWords;
        function addForm(personIndex, numberIndex, weight) {
            let prev = null;
            if (formsOut.length > 0) {
                prev = formsOut[formsOut.length - 1].form;
            }
            const person = PERSONS[personIndex];
            const number = NUMBERS[numberIndex];
            let fullForm = caseFn(person, number).raw;
            let spacePos = -1;
            // If limitWords is set:
            // - if we have more words than in the infinitive, extra words are auxiliary and should be cut
            // Otherwise keep the full form.
            if (limitWords) {
                let offset = 0;
                for (let i = 0; i < spaces; ++i) {
                    let spacePos = fullForm.indexOf(" ", offset);
                    if (spacePos < 0) {
                        throw new Error(`Unexpected space count in form: ${fullForm}`);
                    }
                    offset = spacePos + 1;
                }
                spacePos = fullForm.indexOf(" ", offset);
            }
            let form = spacePos < 0 ? fullForm : fullForm.substring(0, spacePos);
            if (prev != form && form != aspan.NOT_SUPPORTED) {
                formsOut.push(new WeightedForm(
                    form,
                    weight,
                    sentenceTypeIndex,
                    tenseName,
                    personIndex,
                    numberIndex,
                ));
            }
        }

        const first = 0;
        const second = 1;
        const secondP = 2;
        const third = 3;

        const sing = 0;
        const plur = 1;
        addForm(first, sing, FIRST_SINGULAR_WEIGHT);
        addForm(first, plur, FIRST_PLURAL_WEIGHT);
        addForm(second, sing, SECOND_SINGULAR_WEIGHT);
        addForm(second, plur, SECOND_PLURAL_WEIGHT);
        addForm(secondP, sing, SECOND_SINGULAR_WEIGHT);
        addForm(secondP, plur, SECOND_PLURAL_WEIGHT);
        addForm(third, sing, THIRD_PERSON_WEIGHT);
        addForm(third, plur, THIRD_PERSON_WEIGHT);
    }
    createTenseForms(forceExceptional, sentenceTypeIndex, auxBuilder) {
        let verbBuilder = new aspan.VerbBuilder(this.verb, forceExceptional);
        let forms = [];
        if (sentenceTypeIndex == 0) {
            forms.push(new WeightedForm(this.verb, INFINITIVE_WEIGHT, "", "infinitive", "", ""));
        }
        const sentenceType = SENTENCE_TYPES[sentenceTypeIndex];
        this.createForms(
            sentenceTypeIndex,
            "presentTransitive",
            (person, number) => verbBuilder.presentTransitiveForm(person, number, sentenceType),
            forms
        );
        // Present continuous tense forms look the same for all sentence types after the auxiliary verb is stripped.
        if (sentenceTypeIndex == 0) {
            this.createForms(
                sentenceTypeIndex,
                "presentContinuous",
                (person, number) => verbBuilder.presentContinuousForm(person, number, sentenceType, auxBuilder),
                forms
            );
        }
        // Remote past tense forms look the same for all sentence types after the auxiliary verb is stripped.
        if (sentenceTypeIndex == 0) {
            this.createForms(
                sentenceTypeIndex,
                "remotePast",
                (person, number) => verbBuilder.remotePastTense(person, number, sentenceType),
                forms
            );
        }
        this.createForms(
            sentenceTypeIndex,
            "pastUncertain",
            (person, number) => verbBuilder.pastUncertainTense(person, number, sentenceType),
            forms
        );
        this.createForms(
            sentenceTypeIndex,
            "pastTransitive",
            (person, number) => verbBuilder.pastTransitiveTense(person, number, sentenceType),
            forms
        );
        this.createForms(
            sentenceTypeIndex,
            "past",
            (person, number) => verbBuilder.pastForm(person, number, sentenceType),
            forms
        );
        this.createForms(
            sentenceTypeIndex,
            "possibleFuture",
            (person, number) => verbBuilder.possibleFutureForm(person, number, sentenceType),
            forms
        );
        // Intention future tense forms look the same for all sentence types after the auxiliary verb is stripped.
        if (sentenceTypeIndex == 0) {
            this.createForms(
                sentenceTypeIndex,
                "intentionFuture",
                (person, number) => verbBuilder.intentionFutureForm(person, number, sentenceType),
                forms
            );
        }
        this.createForms(
            sentenceTypeIndex,
            "conditionalMood",
            (person, number) => verbBuilder.conditionalMood(person, number, sentenceType),
            forms
        );
        this.createForms(
            sentenceTypeIndex,
            "imperativeMood",
            (person, number) => verbBuilder.imperativeMood(person, number, sentenceType),
            forms
        );
        const pastParticiple = verbBuilder.pastParticiple(sentenceType).raw;
        forms.push(new WeightedForm(pastParticiple, THIRD_PERSON_WEIGHT, sentenceTypeIndex, "pastParticiple", "", ""));
        const presentParticiple = verbBuilder.presentParticiple(sentenceType).raw;
        forms.push(new WeightedForm(presentParticiple, THIRD_PERSON_WEIGHT, sentenceTypeIndex, "presentParticiple", "", ""));
        const futureParticiple = verbBuilder.futureParticiple(sentenceType).raw;
        forms.push(new WeightedForm(futureParticiple, THIRD_PERSON_WEIGHT, sentenceTypeIndex, "futureParticiple", "", ""));
        const perfectGerund = verbBuilder.perfectGerund(sentenceType).raw;
        forms.push(new WeightedForm(perfectGerund, THIRD_PERSON_WEIGHT, sentenceTypeIndex, "perfectGerund", "", ""));
        const continuousGerund = verbBuilder.continuousGerund(sentenceType).raw;
        forms.push(new WeightedForm(continuousGerund, THIRD_PERSON_WEIGHT, sentenceTypeIndex, "continuousGerund", "", ""));
        const intentionGerund = verbBuilder.intentionGerund(sentenceType).raw;
        forms.push(new WeightedForm(intentionGerund, THIRD_PERSON_WEIGHT, sentenceTypeIndex, "intentionGerund", "", ""));
        return forms;
    }
    createPresentContinuousForms(forceExceptional, sentenceTypeIndex, auxBuilder) {
        let verbBuilder = new aspan.VerbBuilder(this.verb, forceExceptional);
        let forms = [];
        const sentenceType = SENTENCE_TYPES[sentenceTypeIndex];
        this.createForms(
            sentenceTypeIndex,
            "presentContinuous",
            (person, number) => verbBuilder.presentContinuousForm(person, number, sentenceType, auxBuilder),
            forms
        );
        return forms;
    }
}

class FormRow {
    constructor(verb, forceExceptional, sentenceType, forms) {
        this.verb = verb;
        this.forceExceptional = forceExceptional;
        this.sentenceType = sentenceType;
        this.forms = forms;
    }
}

function createTenseFormsForAllVariants(verb, auxBuilder) {
    const limitWords = true;
    let formBuilder = new FormBuilder(verb, limitWords);
    const optExceptMeaning = aspan.getOptExceptVerbMeanings(verb);
    const isOptionalException = optExceptMeaning != null;
    let rows = [];
    for (let i = 0; i < SENTENCE_TYPES.length; ++i) {
        rows.push(new FormRow(verb, 0, i,
            formBuilder.createTenseForms(false, i, auxBuilder)
        ))
        if (isOptionalException) {
            rows.push(new FormRow(verb, 1, i,
                formBuilder.createTenseForms(true, i, auxBuilder)
            ));
        }
    }
    return rows;
}

function createPresentContinuousForms(verb, auxBuilder) {
    const limitWords = false;
    let formBuilder = new FormBuilder(verb, limitWords);
    const optExceptMeaning = aspan.getOptExceptVerbMeanings(verb);
    const isOptionalException = optExceptMeaning != null;
    let rows = [];
    const sentenceTypeIndex = 0;

    rows.push(new FormRow(verb, 0, sentenceTypeIndex,
        formBuilder.createPresentContinuousForms(false, sentenceTypeIndex, auxBuilder)
    ));

    if (isOptionalException) {
        rows.push(new FormRow(verb, 1, sentenceTypeIndex,
            formBuilder.createPresentContinuousForms(true, sentenceTypeIndex, auxBuilder)
        ));
    }

    return rows;
}

class VerbWithMeta {
    constructor(verb, fe, softOffset) {
        this.verb = verb;
        this.fe = fe;
        this.softOffset = softOffset;
    }
}

function createVerbsWithMeta(verb) {
    const builder = new aspan.VerbBuilder(verb, false);

    let rows = [
        new VerbWithMeta(verb, false, builder.softOffset),
    ];
    const optExceptMeaning = aspan.getOptExceptVerbMeanings(verb);
    if (optExceptMeaning != null) {
        const feBuilder = new aspan.VerbBuilder(verb, true);
        rows.push(new VerbWithMeta(verb, true, feBuilder.softOffset));
    }
    return rows;
}

function serializePhrasal(phrasal) {
    let parts = [];
    for (const part of phrasal.parts) {
        if (part.content.length > 0) {
            parts.push(part.content)
        }
    }
    return parts.join("+");
}

function createForSentenceTypes(genFn) {
    const sentenceTypes = ["Statement", "Negative", "Question"];
    let result = {};
    for (const sentenceType of sentenceTypes) {
        let forms = [];
        for (const grammarPerson of aspan.GRAMMAR_PERSONS) {
            for (const grammarNumber of aspan.GRAMMAR_NUMBERS) {
                const phrasal = genFn(sentenceType, grammarPerson, grammarNumber);
                const s = serializePhrasal(phrasal);
                forms.push(s);
            }
        }
        result[sentenceType] = forms;
    }
    return result;
}

function createTestsetRow(verb, auxBuilders, forceExceptional) {
    const builder = new aspan.VerbBuilder(verb, forceExceptional);
    let tenses = [];
    tenses.push(createForSentenceTypes(
        (sentenceType, grammarPerson, grammarNumber) => builder.presentTransitiveForm(grammarPerson, grammarNumber, sentenceType)
    ));
    for (const negateAux of [false, true]) {
        for (const auxBuilder of auxBuilders) {
            tenses.push(createForSentenceTypes(
                (sentenceType, grammarPerson, grammarNumber) => builder.presentContinuousForm(grammarPerson, grammarNumber, sentenceType, auxBuilder, negateAux)
            ));
        }
    }
    tenses.push(createForSentenceTypes(
        (sentenceType, grammarPerson, grammarNumber) => builder.pastForm(grammarPerson, grammarNumber, sentenceType)
    ));
    for (const negateAux of [false, true]) {
        tenses.push(createForSentenceTypes(
            (sentenceType, grammarPerson, grammarNumber) => builder.remotePastTense(grammarPerson, grammarNumber, sentenceType, negateAux)
        ));
    }
    tenses.push(createForSentenceTypes(
        (sentenceType, grammarPerson, grammarNumber) => builder.conditionalMood(grammarPerson, grammarNumber, sentenceType)
    ));
    tenses.push(createForSentenceTypes(
        (sentenceType, grammarPerson, grammarNumber) => builder.imperativeMood(grammarPerson, grammarNumber, sentenceType)
    ));
    const verbShak = "PresentTransitive";
    tenses.push(createForSentenceTypes(
        (sentenceType, grammarPerson, grammarNumber) => builder.wantClause(grammarPerson, grammarNumber, sentenceType, verbShak)
    ));
    const row = {
        "verb": verb,
        "forceExceptional": forceExceptional,
        "tenses": tenses
    };
    return JSON.stringify(row);
}

function createTestsetRows(verb, auxBuilders) {
    let rows = [];
    rows.push(createTestsetRow(verb, auxBuilders, false));
    const optExceptMeaning = aspan.getOptExceptVerbMeanings(verb);
    if (optExceptMeaning != null) {
        rows.push(createTestsetRow(verb, auxBuilders, true));
    }
    return rows;
}

class Args {
    constructor(command, input, output, outputFormat) {
        this.command = command;
        this.input = input;
        this.output = output;
    }
}

function printWeight(weight) {
    let s = weight.toFixed(5);
    let point = s.indexOf(".");
    if (point < 0) {
        return s;
    }
    let trunc = s.length;
    while (trunc - 1 > point && s[trunc - 1] == "0") {
        trunc -= 1;
    }
    if (trunc + 1 == point) {
        return s.substring(0, point);
    }
    if (trunc < s.length) {
        return s.substring(0, trunc);
    }
    return s;
}

function writeSuggestLine(base, form, weight, inputItem, translationLang, outputStream) {
    let dataObject = { base };
    if (inputItem != null) {
        if (inputItem.ruGlosses.length > 0) {
            dataObject.kvdru = inputItem.ruGlosses;
        }
        if (inputItem.ruGlossesFe.length > 0) {
            dataObject.kvdrufe = inputItem.ruGlossesFe;
        }
        if (inputItem.enGlosses.length > 0) {
            dataObject.kvden = inputItem.enGlosses;
        }
        if (inputItem.enGlossesFe.length > 0) {
            dataObject.kvdenfe = inputItem.enGlossesFe;
        }
    }
    let isTranslation = translationLang.length > 0;
    if (isTranslation && inputItem != null) {
        console.log(`unexpected non-null inputItem for translation suggest form`);
        process.exit(1);
    }
    let resultWeight = isTranslation ? weight : (weight + KAZAKH_WEIGHT);
    if (isTranslation) {
        dataObject.translation = translationLang;
    }
    let dataString = JSON.stringify(dataObject);
    outputStream.write(`${form}\t${printWeight(resultWeight)}\t${dataString}\n`);
}

function writeDetectSuggestLine(base, exceptional, ruGlosses, enGlosses, forms, outputStream) {
    let dataObject = {
        pos: "verb",
        base: base,
        exceptional: exceptional,
        ruwkt: ruGlosses,
        enwkt: enGlosses,
        forms: forms,
    };
    let dataString = JSON.stringify(dataObject);
    outputStream.write(`${dataString}\n`);
}

function simplify(form) {
    let obvious = form
        .replace(/[ң]/gi, 'н')
        .replace(/[ғ]/gi, 'г')
        .replace(/[үұ]/gi, 'у')
        .replace(/[қ]/gi, 'к')
        .replace(/[һ]/gi, 'х')
        .replace(/[ө]/gi, 'о');

    let result = [];
    if (obvious.indexOf('ә') >= 0) {
        if (obvious.indexOf('і') >= 0) {
            result.push(
                obvious
                    .replace(/[ә]/gi, 'я')
                    .replace(/[і]/gi, 'и')
            );
            result.push(
                obvious
                    .replace(/[ә]/gi, 'э')
                    .replace(/[і]/gi, 'и')
            );
            /*
            result.push(
                obvious
                    .replace(/[ә]/gi, 'я')
                    .replace(/[і]/gi, 'ы')
            );
            result.push(
                obvious
                    .replace(/[ә]/gi, 'э')
                    .replace(/[і]/gi, 'ы')
            );
            */
        } else {
            result.push(
                obvious
                    .replace(/[ә]/gi, 'я')
            );
            result.push(
                obvious
                    .replace(/[ә]/gi, 'э')
            );
        }
    } else if (obvious.indexOf('і') >= 0) {
        result.push(
            obvious
                .replace(/[і]/gi, 'и')
        );
        /*
        result.push(
            obvious
                .replace(/[і]/gi, 'ы')
        );
        */
    } else if (obvious != form) {
        result.push(obvious);
    }
    return result;
}

/**
 * The function serves to suppress verbs on their word count and especially on comma [','] presence.
 */
function estimateVerbPartCount(verb) {
    let commas = 0;
    let separators = 0;
    for (var i = 0; i < verb.length; ++i) {
        let ch = verb[i];
        if (ch == ',') {
            ++commas;
        } else if (ch == ' ' || ch == '-') {
            ++separators;
        }
    }
    if (commas > 0) {
        return 10;
    }
    return separators + 1;
}

function parseTranslations(s) {
    if (s == null || s.length == 0) {
        return [];
    }
    return s.split(",");
}

class InputItem {
    constructor(line) {
        let parts = line.split("\t");
        this.valid = true;
        if (parts.length <= 0) {
            this.valid = false;
        } else if (parts[0] == "v2") {
            if (parts.length != 6) {
                console.log(`invalid number of parts in input line: ${line}`);
                this.valid = false;
                return;
            }
            this.verb = parts[1];
            this.freq = 0;
            this.ruGlosses = parseTranslations(parts[2]);
            this.ruGlossesFe = parseTranslations(parts[3]);
            this.enGlosses = parseTranslations(parts[4]);
            this.enGlossesFe = parseTranslations(parts[5]);
        } else {
            this.verb = parts[0];
            this.freq = parseInt(parts[1]);
            this.ruGlosses = [];
            this.ruGlossesFe = [];  // not supported in the input format
            this.enGlosses = [];
            this.enGlossesFe = [];  // not supported in the input format
            for (let i = 2; i < parts.length; ++i) {
                let part = parts[i];
                if (part.startsWith("ruwkt:")) {
                    this.ruGlosses.push(part.substring(6));
                } else if (part.startsWith("enwkt:")) {
                    this.enGlosses.push(part.substring(6));
                } else {
                    this.valid = false;
                    break;
                }
            }
        }
    }
}

async function processLineByLine(args) {
  const inputStream = fs.createReadStream(args.input);
  const outputStream = fs.createWriteStream(args.output);

  const auxBuilders = [
    new aspan.VerbBuilder("жату"),
    new aspan.VerbBuilder("жүру"),
    new aspan.VerbBuilder("отыру"),
    new aspan.VerbBuilder("тұру"),
  ];

  const rl = readline.createInterface({
    input: inputStream,
    crlfDelay: Infinity,
  });

  let lineCounter = 0;
  let outputCounter = 0;
  for await (const inputLine of rl) {
    let inputItem = new InputItem(inputLine);
    if (!inputItem.valid) {
        console.log(`Abort on invalid input line: ${inputLine}`);
        process.exit(1);
    }
    let inputVerb = inputItem.verb;
    let partCountSuppression = estimateVerbPartCount(inputVerb);
    if (partCountSuppression > 2) continue;

    if (args.command == kDetectorFormsCommand) {
        let formRows = createTenseFormsForAllVariants(inputVerb, auxBuilders[0]);
        for (let rowIndex = 0; rowIndex < formRows.length; ++rowIndex) {
            const formRow = formRows[rowIndex];
            outputStream.write(`${formRow.verb}:${formRow.forceExceptional}`)
            ++outputCounter;
            let row = formRow.forms;
            for (let i = 0; i < row.length; ++i) {
                const wf = row[i];
                outputStream.write(`\t${wf.form}:${wf.sentenceTypeIndex}:${wf.tenseName}:${wf.person}:${wf.number}`);
                ++outputCounter;
            }
            outputStream.write(`\n`);
        }
    } else if (args.command == kDetectSuggestCommand) {
        /**
         * {
         *   "base": <INFINITIVE>,
         *   "exceptional": <FORCE_EXCEPTIONAL>,
         *   "ruwkt": [<RU_GLOSSES>],
         *   "enwkt": [<EN_GLOSSES>],
         *   "forms": [
         *     {
         *       "form": <FORM>,
         *       "weight": <FORM_WEIGHT>,
         *       "sent": "<SENTENCE_TYPE>",
         *       "tense": "<TENSE>",
         *       "person": "<PERSON>",
         *       "number": "<NUMBER>"
         *     },
         *     ...
         *   ]
         * }
         */

        let partCountWeight = (partCountSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
        let freqWeight = Math.log(inputItem.freq + 1.0) * FREQ_WEIGHT_PORTION;

        let formRows = createTenseFormsForAllVariants(inputVerb, auxBuilders[0]);
        let regularForms = [];
        let exceptionalForms = [];
        for (let rowIndex = 0; rowIndex < formRows.length; ++rowIndex) {
            const formRow = formRows[rowIndex];
            let row = formRow.forms;
            for (let i = 0; i < row.length; ++i) {
                const wf = row[i];
                let weight = partCountWeight + wf.weight;
                if (wf.tenseName == "infinitive") {
                    weight += freqWeight;
                }
                if (wf.sentenceTypeIndex == 0) {
                    weight += STATEMENT_WEIGHT;
                }
                const verbForm = {
                    form: wf.form,
                    weight: printWeight(weight),
                    sent: wf.sentenceTypeIndex,
                    tense: wf.tenseName,
                    person: wf.person,
                    number: wf.number,
                };
                if (formRow.forceExceptional == 1) {
                    exceptionalForms.push(verbForm);
                } else {
                    regularForms.push(verbForm);
                }
                ++outputCounter;
            }
        }

        writeDetectSuggestLine(inputVerb, 0, inputItem.ruGlosses, inputItem.enGlosses, regularForms, outputStream);
        if (exceptionalForms.length > 0) {
            writeDetectSuggestLine(inputVerb, 1, inputItem.ruGlossesFe, inputItem.enGlossesFe, exceptionalForms, outputStream);
        }

    } else if (args.command == kSuggestInfinitiveCommand || args.command == kSuggestInfinitiveTranslationCommand) {
        let partCountWeight = (partCountSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
        let freqWeight = Math.log(inputItem.freq + 1.0) * FREQ_WEIGHT_PORTION;
        let baseWeight = partCountWeight + freqWeight;
        let ruGlosses = inputItem.ruGlosses.slice(0, 2);
        let enGlosses = inputItem.enGlosses.slice(0, 2);
        writeSuggestLine(inputVerb, inputVerb, baseWeight + EXACT_MATCH_WEIGHT + INFINITIVE_WEIGHT, inputItem, "", outputStream);
        ++outputCounter;
        let simpleBaseForms = simplify(inputVerb);
        for (var j = 0; j < simpleBaseForms.length; ++j) {
            writeSuggestLine(inputVerb, simpleBaseForms[j], baseWeight + INFINITIVE_WEIGHT, inputItem, "", outputStream);
            ++outputCounter;
        }
        if (args.command == kSuggestInfinitiveTranslationCommand) {
            for (var j = 0; j < ruGlosses.length; ++j) {
                let translation = ruGlosses[j];
                let translationSuppression = estimateVerbPartCount(translation);
                let weight = (translationSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
                writeSuggestLine(inputVerb, translation, weight + INFINITIVE_WEIGHT, null, "ru", outputStream);
            }
            for (var j = 0; j < enGlosses.length; ++j) {
                let translation = enGlosses[j];
                let translationSuppression = estimateVerbPartCount(translation);
                if (translationSuppression > 1) {
                    translationSuppression -= 1;
                }
                let weight = (translationSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
                writeSuggestLine(inputVerb, translation, weight + INFINITIVE_WEIGHT, null, "en", outputStream);
            }
        }
    } else if (args.command == kPresentContinuousFormsCommand) {
        for (let i = 0; i < auxBuilders.length; ++i) {
            let auxBuilder = auxBuilders[i];
            let formRows = createPresentContinuousForms(inputVerb, auxBuilder);
            for (let rowIndex = 0; rowIndex < formRows.length; ++rowIndex) {
                const formRow = formRows[rowIndex];
                let row = formRow.forms;
                for (let i = 0; i < row.length; ++i) {
                    outputStream.write(`${formRow.verb}:${formRow.forceExceptional}`)
                    outputStream.write(`\t${auxBuilder.verbDictForm}`);
                    const wf = row[i];
                    outputStream.write(`\t${wf.form}`);
                    outputCounter += 2;
                    outputStream.write(`\n`);
                }
            }
        }
    } else if (args.command == kVerbsWithMetaCommand) {
        const rows = createVerbsWithMeta(inputVerb);
        for (let i = 0; i < rows.length; ++i) {
            const row = rows[i];
            const fe = row.fe ? 1 : 0;
            outputStream.write(`${row.verb}\t${fe}\t${row.softOffset}\n`);
            outputCounter += 1;
        }
    } else if (args.command == kTestsetCommand) {
        const rows = createTestsetRows(inputVerb, auxBuilders);
        for (let i = 0; i < rows.length; ++i) {
            const row = rows[i];
            outputStream.write(`${row}\n`);
            outputCounter += 1;
        }
    }
    lineCounter += 1;
    if (lineCounter % 1000 == 0 && lineCounter > 0) {
        console.log(`Handled ${lineCounter} verb(s) so far.`)
    }
  }
  console.log(`Handled ${lineCounter} verb(s).`)
  console.log(`Produced ${outputCounter} form(s).`)
}

function checkCommandArgs(args, cmd, cmdArgsCount) {
    if (args.length != cmdArgsCount + 1) {
        throw new Error(`Command '${cmd}' requires exactly ${cmdArgsCount} arguments`);
    }
}

function acceptCommandWithInputAndOutput(args, cmd) {
    checkCommandArgs(args, cmd, 2);
    return new Args(cmd, args[1], args[2]);
}

function parseArgs() {
    let args = process.argv.slice(2)
    if (args.length < 1) {
        throw new Error(`Command argument is required`);
    }
    const command = args[0];
    if (command == kDetectorFormsCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kDetectSuggestCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kSuggestInfinitiveCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kSuggestInfinitiveTranslationCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kPresentContinuousFormsCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kVerbsWithMetaCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kTestsetCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else {
        throw new Error(`Unsupported command: ${command}`);
    }
}

let args = parseArgs();
processLineByLine(args);