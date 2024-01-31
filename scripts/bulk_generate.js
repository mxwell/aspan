const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const FORMAT_ONE_LINE = "--one-line-format";
const FORMAT_SUGGEST = "--suggest-format";
const FORMAT_SUGGEST_INFINITIV = "--suggest-infinitiv-format";
const FORMAT_SUGGEST_INFINITIV_TRANSLATION = "--suggest-infinitiv-translation-format";

const KAZAKH_WEIGHT = 0.5;
const SHORT_VERB_WEIGHT = KAZAKH_WEIGHT / 2;
const EXACT_MATCH_WEIGHT = SHORT_VERB_WEIGHT / 2;
const FORM_WEIGHT_PORTION = EXACT_MATCH_WEIGHT / 10;
const SECOND_PLURAL_WEIGHT   = FORM_WEIGHT_PORTION * 1;
const FIRST_PLURAL_WEIGHT    = FORM_WEIGHT_PORTION * 2;
const SECOND_SINGULAR_WEIGHT = FORM_WEIGHT_PORTION * 3;
const FIRST_SINGULAR_WEIGHT  = FORM_WEIGHT_PORTION * 4;
const THIRD_PERSON_WEIGHT    = FORM_WEIGHT_PORTION * 5;
const INFINITIV_WEIGHT       = FORM_WEIGHT_PORTION * 6;

class WeightedForm {
    constructor(form, weight) {
        this.form = form;
        this.weight = weight;
    }
}

function createForms(caseFn, formsOut) {

    function addForm(person, number, weight) {
        let prev = null;
        if (formsOut.length > 0) {
            prev = formsOut[formsOut.length - 1].form;
        }
        let form = caseFn(person, number).raw;
        if (prev != form) {
            formsOut.push(new WeightedForm(
                form,
                weight
            ));
        }
    }

    const first = aspan.GrammarPerson.First;
    const second = aspan.GrammarPerson.Second;
    const secondP = aspan.GrammarPerson.SecondPolite;
    const third = aspan.GrammarPerson.Third;

    const sing = aspan.GrammarNumber.Singular;
    const plur = aspan.GrammarNumber.Plural;
    addForm(first, sing, FIRST_SINGULAR_WEIGHT);
    addForm(first, plur, FIRST_PLURAL_WEIGHT);
    addForm(second, sing, SECOND_SINGULAR_WEIGHT);
    addForm(second, plur, SECOND_PLURAL_WEIGHT);
    addForm(secondP, sing, SECOND_SINGULAR_WEIGHT);
    addForm(secondP, plur, SECOND_PLURAL_WEIGHT);
    addForm(third, sing, THIRD_PERSON_WEIGHT);
    addForm(third, plur, THIRD_PERSON_WEIGHT);
}

function createTenseForms(verb, auxBuilder) {
    let forceExceptional = true;
    let verbBuilder = new aspan.VerbBuilder(verb, forceExceptional);
    let forms = [];
    let sentenceType = "Statement";
    createForms(
        (person, number) => verbBuilder.presentTransitiveForm(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.presentContinuousForm(person, number, sentenceType, auxBuilder),
        forms
    );
    createForms(
        (person, number) => verbBuilder.remotePastTense(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.pastUncertainTense(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.pastTransitiveTense(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.pastForm(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.possibleFutureForm(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.intentionFutureForm(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.conditionalMood(person, number, sentenceType),
        forms
    );
    createForms(
        (person, number) => verbBuilder.imperativeMood(person, number, sentenceType),
        forms
    );
    return forms;
}

class Args {
    constructor(input, output, outputFormat) {
        this.input = input;
        this.output = output;
        this.outputFormat = outputFormat;
        this.suggest = this.outputFormat != FORMAT_ONE_LINE;
        this.suggestForms = this.outputFormat == FORMAT_SUGGEST;
        this.translation = this.outputFormat == FORMAT_SUGGEST_INFINITIV_TRANSLATION;
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
    let ruGlosses = inputItem.ruGlosses.slice(0, 2);
    let enGlosses = inputItem.enGlosses.slice(0, 2);
    let partCountSuppression = estimateVerbPartCount(inputVerb);
    if (partCountSuppression > 2) continue;
    let forms = createTenseForms(inputVerb, auxBuilder);
    if (args.suggest) {
        let partCountWeight = (partCountSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
        writeSuggestLine(inputVerb, inputVerb, partCountWeight + EXACT_MATCH_WEIGHT + INFINITIV_WEIGHT, ruGlosses, enGlosses, "", outputStream);
        ++outputCounter;
        let simpleBaseForms = simplify(inputVerb);
        for (var j = 0; j < simpleBaseForms.length; ++j) {
            writeSuggestLine(inputVerb, simpleBaseForms[j], partCountWeight + INFINITIV_WEIGHT, ruGlosses, enGlosses, "", outputStream);
            ++outputCounter;
        }
        if (args.translation) {
            for (var j = 0; j < ruGlosses.length; ++j) {
                let translation = ruGlosses[j];
                let translationSuppression = estimateVerbPartCount(translation);
                let weight = (translationSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
                writeSuggestLine(inputVerb, translation, weight + INFINITIV_WEIGHT, [], [], "ru", outputStream);
            }
            for (var j = 0; j < enGlosses.length; ++j) {
                let translation = enGlosses[j];
                let translationSuppression = estimateVerbPartCount(translation);
                if (translationSuppression > 1) {
                    translationSuppression -= 1;
                }
                let weight = (translationSuppression < 2) ? SHORT_VERB_WEIGHT : 0.0;
                writeSuggestLine(inputVerb, translation, weight + INFINITIV_WEIGHT, [], [], "en", outputStream);
            }
        }
        if (args.suggestForms) {
            for (var i = 0; i < forms.length; ++i) {
                let weightedForm = forms[i];
                writeSuggestLine(inputVerb, weightedForm.form, partCountWeight + EXACT_MATCH_WEIGHT + weightedForm.weight, ruGlosses, enGlosses, "", outputStream);
                ++outputCounter;
                let simpleForms = simplify(weightedForm.form);
                for (var j = 0; j < simpleForms.length; ++j) {
                    writeSuggestLine(inputVerb, simpleForms[j], partCountWeight + weightedForm.weight, ruGlosses, enGlosses, "", outputStream);
                    ++outputCounter;
                }
            }
        }
    } else {
        outputStream.write(`${inputVerb}`);
        ++outputCounter;
        for (var i = 0; i < forms.length; ++i) {
            outputStream.write(`\t${forms[i].form}`);
            ++outputCounter;
        }
        outputStream.write(`\n`);
    }
    lineCounter += 1;
    if (lineCounter % 1000 == 0 && lineCounter > 0) {
        console.log(`Handled ${lineCounter} verb(s) so far.`)
    }
  }
  console.log(`Handled ${lineCounter} verb(s).`)
  console.log(`Produced ${outputCounter} form(s).`)
}

function isSupportedFormat(arg) {
    if (arg == FORMAT_ONE_LINE) {
        return arg;
    } else if (arg == FORMAT_SUGGEST) {
        return arg;
    } else if (arg == FORMAT_SUGGEST_INFINITIV) {
        return arg;
    } else if (arg == FORMAT_SUGGEST_INFINITIV_TRANSLATION) {
        return arg;
    } else {
        throw new Error(`Expected format argument but got: ${arg}.`)
    }
}

function parseArgs() {
    let args = process.argv.slice(2)
    if (args.length < 2 || args.length > 3) {
        throw new Error(`Unexpected number of arguments: ${args.length}.`)
    }
    var outputFormat = FORMAT_ONE_LINE;
    var argPos = 0;
    if (args.length == 3) {
        outputFormat = isSupportedFormat(args[0]);
        ++argPos;
    }
    let inputPath = args[argPos];
    let outputPath = args[argPos + 1];
    return new Args(inputPath, outputPath, outputFormat);
}

let args = parseArgs();
processLineByLine(args);