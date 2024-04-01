const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const kDetectorFormsCommand = "detector_forms";
const kSuggestInfinitiveCommand = "suggest_infinitive";
const kSuggestInfinitiveTranslationCommand = "suggest_infinitive_translation";

const KAZAKH_WEIGHT = 0.5;
const SHORT_VERB_WEIGHT = KAZAKH_WEIGHT / 2;
const EXACT_MATCH_WEIGHT = SHORT_VERB_WEIGHT / 2;
const FORM_WEIGHT_PORTION = EXACT_MATCH_WEIGHT / 10;
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
    constructor(verb) {
        this.verb = verb;
        this.spaces = countSpaces(verb);
    }
    createForms(sentenceTypeIndex, tenseName, caseFn, formsOut) {
        let spaces = this.spaces;
        function addForm(personIndex, numberIndex, weight) {
            let prev = null;
            if (formsOut.length > 0) {
                prev = formsOut[formsOut.length - 1].form;
            }
            const person = PERSONS[personIndex];
            const number = NUMBERS[numberIndex];
            let fullForm = caseFn(person, number).raw;
            let offset = 0;
            for (let i = 0; i < spaces; ++i) {
                let spacePos = fullForm.indexOf(" ", offset);
                if (spacePos < 0) {
                    throw new Error(`Unexpected space count in form: ${fullForm}`);
                }
                offset = spacePos + 1;
            }
            let spacePos = fullForm.indexOf(" ", offset);
            let form = spacePos < 0 ? fullForm : fullForm.substring(0, spacePos);
            if (prev != form) {
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
        if (sentenceTypeIndex == 0 && !forceExceptional) {
            forms.push(new WeightedForm(this.verb, INFINITIVE_WEIGHT, "", "infinitive", "", ""));
        }
        const sentenceType = SENTENCE_TYPES[sentenceTypeIndex];
        this.createForms(
            sentenceTypeIndex,
            "presentTransitive",
            (person, number) => verbBuilder.presentTransitiveForm(person, number, sentenceType),
            forms
        );
        this.createForms(
            sentenceTypeIndex,
            "presentContinuous",
            (person, number) => verbBuilder.presentContinuousForm(person, number, sentenceType, auxBuilder),
            forms
        );
        this.createForms(
            sentenceTypeIndex,
            "remotePast",
            (person, number) => verbBuilder.remotePastTense(person, number, sentenceType),
            forms
        );
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
        this.createForms(
            sentenceTypeIndex,
            "intentionFuture",
            (person, number) => verbBuilder.intentionFutureForm(person, number, sentenceType),
            forms
        );
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
    let formBuilder = new FormBuilder(verb);
    const isOptionalException = aspan.isVerbOptionalException(verb);
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

function writeSuggestLine(base, form, weight, ruGlosses, enGlosses, translationLang, outputStream) {
    let dataObject = { base };
    if (ruGlosses.length > 0) {
        dataObject.ruwkt = ruGlosses;
    }
    if (enGlosses.length > 0) {
        dataObject.enwkt = enGlosses;
    }
    let isTranslation = translationLang.length > 0;
    if (isTranslation && (ruGlosses.length > 0 || enGlosses.length > 0)) {
        console.log(`unexpected glosses for translation suggest form`);
        process.exit(1);
    }
    let resultWeight = isTranslation ? weight : (weight + KAZAKH_WEIGHT);
    if (isTranslation) {
        dataObject.translation = translationLang;
    }
    let dataString = JSON.stringify(dataObject);
    outputStream.write(`${form}\t${printWeight(resultWeight)}\t${dataString}\n`);
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

class InputItem {
    constructor(line) {
        let parts = line.split("\t");
        this.valid = true;
        if (parts.length <= 0) {
            this.valid = false;
        } else {
            this.verb = parts[0];
            this.ruGlosses = [];
            this.enGlosses = [];
            for (let i = 1; i < parts.length; ++i) {
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

  let auxBuilder = new aspan.VerbBuilder("жату");

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
        let formRows = createTenseFormsForAllVariants(inputVerb, auxBuilder);
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
    } else if (args.command == kSuggestInfinitiveCommand || args.command == kSuggestInfinitiveTranslationCommand) {
        let partCountWeight = (partCountSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
        let ruGlosses = inputItem.ruGlosses.slice(0, 2);
        let enGlosses = inputItem.enGlosses.slice(0, 2);
        writeSuggestLine(inputVerb, inputVerb, partCountWeight + EXACT_MATCH_WEIGHT + INFINITIVE_WEIGHT, ruGlosses, enGlosses, "", outputStream);
        ++outputCounter;
        let simpleBaseForms = simplify(inputVerb);
        for (var j = 0; j < simpleBaseForms.length; ++j) {
            writeSuggestLine(inputVerb, simpleBaseForms[j], partCountWeight + INFINITIVE_WEIGHT, ruGlosses, enGlosses, "", outputStream);
            ++outputCounter;
        }
        if (args.command == kSuggestInfinitiveTranslationCommand) {
            for (var j = 0; j < ruGlosses.length; ++j) {
                let translation = ruGlosses[j];
                let translationSuppression = estimateVerbPartCount(translation);
                let weight = (translationSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
                writeSuggestLine(inputVerb, translation, weight + INFINITIVE_WEIGHT, [], [], "ru", outputStream);
            }
            for (var j = 0; j < enGlosses.length; ++j) {
                let translation = enGlosses[j];
                let translationSuppression = estimateVerbPartCount(translation);
                if (translationSuppression > 1) {
                    translationSuppression -= 1;
                }
                let weight = (translationSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
                writeSuggestLine(inputVerb, translation, weight + INFINITIVE_WEIGHT, [], [], "en", outputStream);
            }
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
    } else if (command == kSuggestInfinitiveCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else if (command == kSuggestInfinitiveTranslationCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else {
        throw new Error(`Unsupported command: ${command}`);
    }
}

let args = parseArgs();
processLineByLine(args);