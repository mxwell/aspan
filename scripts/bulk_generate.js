const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

function createForms(caseFn, formsOut) {
    for (const person of aspan.GRAMMAR_PERSONS) {
        for (const number of aspan.GRAMMAR_NUMBERS) {
            const verbPhrase = caseFn(person, number).raw;
            formsOut.push(verbPhrase);
        }
    }
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
    let forms = createTenseForms(line, auxBuilder);
    if (args.suggestFormat) {
        writeSuggestLine(line, line, 1.0, outputStream);
        ++outputCounter;
        let simpleBaseForms = simplify(line);
        for (var j = 0; j < simpleBaseForms.length; ++j) {
            writeSuggestLine(line, simpleBaseForms[j], 0.6, outputStream);
            ++outputCounter;
        }
        for (var i = 0; i < forms.length; ++i) {
            let form = forms[i];
            writeSuggestLine(line, form, 0.8, outputStream);
            ++outputCounter;
            let simpleForms = simplify(form);
            for (var j = 0; j < simpleForms.length; ++j) {
                writeSuggestLine(line, simpleForms[j], 0.6, outputStream);
                ++outputCounter;
            }
        }
    } else {
        outputStream.write(`${line}\t${forms.join('\t')}\n`);
        outputCounter += forms.length;
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