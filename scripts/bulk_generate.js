const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const SHORT_VERB_WEIGHT = 0.5;
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
    constructor(input, output, suggestFormat) {
        this.input = input;
        this.output = output;
        this.suggestFormat = suggestFormat;
    }
}

function writeSuggestLine(base, form, weight, outputStream) {
    outputStream.write(`${form}\t${weight}\t{\"base\": \"${base}\"}\n`)
}

function simplify(form) {
    let obvious = form
        .replace(/[ң]/gi, 'н')
        .replace(/[ғ]/gi, 'г')
        .replace(/[үұ]/gi, 'у')
        .replace(/[қ]/gi, 'к')
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
  for await (const line of rl) {
    let partCountSuppression = estimateVerbPartCount(line);
    if (partCountSuppression > 2) continue;
    let forms = createTenseForms(line, auxBuilder);
    if (args.suggestFormat) {
        let partCountWeight = (partCountSuppression < 2) ? 0.5 : 0.0;
        writeSuggestLine(line, line, partCountWeight + EXACT_MATCH_WEIGHT + INFINITIV_WEIGHT, outputStream);
        ++outputCounter;
        let simpleBaseForms = simplify(line);
        for (var j = 0; j < simpleBaseForms.length; ++j) {
            writeSuggestLine(line, simpleBaseForms[j], partCountWeight + INFINITIV_WEIGHT, outputStream);
            ++outputCounter;
        }
        for (var i = 0; i < forms.length; ++i) {
            let weightedForm = forms[i];
            writeSuggestLine(line, weightedForm.form, partCountWeight + EXACT_MATCH_WEIGHT + weightedForm.weight, outputStream);
            ++outputCounter;
            let simpleForms = simplify(weightedForm.form);
            for (var j = 0; j < simpleForms.length; ++j) {
                writeSuggestLine(line, simpleForms[j], partCountWeight + weightedForm.weight, outputStream);
                ++outputCounter;
            }
        }
    } else {
        outputStream.write(`${line}`);
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

function parseArgs() {
    let args = process.argv.slice(2)
    if (args.length < 2 || args.length > 3) {
        throw new Error(`Unexpected number of arguments: ${args.length}.`)
    }
    var suggestFormat = false;
    var argPos = 0;
    if (args.length == 3) {
        if (args[0] == "--suggest-format") {
            suggestFormat = true;
        } else {
            throw new Error(`Unexpected argument: ${args[0]}.`)
        }
        ++argPos;
    }
    let inputPath = args[argPos];
    let outputPath = args[argPos + 1];
    return new Args(inputPath, outputPath, suggestFormat);
}

let args = parseArgs();
processLineByLine(args);