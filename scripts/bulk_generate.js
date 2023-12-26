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

async function processLineByLine(inputFilePath, outputFilePath) {
  const inputStream = fs.createReadStream(inputFilePath);
  const outputStream = fs.createWriteStream(outputFilePath);

  let auxBuilder = new aspan.VerbBuilder("жату");

  const rl = readline.createInterface({
    input: inputStream,
    crlfDelay: Infinity,
  });

  let lineCounter = 0;
  for await (const line of rl) {
    let forms = createTenseForms(line, auxBuilder);
    outputStream.write(`${line}\t${forms.join('\t')}\n`);
    lineCounter += 1;
    if (lineCounter % 1000 == 0 && lineCounter > 0) {
        console.log(`Handled ${lineCounter} verb(s) so far.`)
    }
  }
  console.log(`Handled ${lineCounter} verb(s).`)
}

let args = process.argv.slice(2)
let EXPECTED_ARGS = 2;
if (args.length != EXPECTED_ARGS) {
    throw new Error(`Expected ${EXPECTED_ARGS} arguments but got ${args.length}.`)
}
processLineByLine(args[0], args[1]);