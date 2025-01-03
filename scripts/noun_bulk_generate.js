const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const kDetectSuggestFormsCommand = "detect_suggest_forms";

class NounForm {
    constructor(form, weight, number, septik, possPerson, possNumber, wordgen) {
        this.form = form;
        this.weight = weight;
        if (number != null) {
            this.number = number;
        }
        if (septik != null) {
            this.septik = septik;
        }
        if (possPerson != null) {
            this.possPerson = possPerson;
        }
        if (possNumber != null) {
            this.possNumber = possNumber;
        }
        if (wordgen != null) {
            this.wordgen = wordgen;
        }
    }
}

function createMainNounForms(noun) {
    let nb = new aspan.NounBuilder(noun);

    const kSingularIndex = 0;
    const kPluralIndex = 1;

    let forms = [];
    /* TODO calculate weight */
    for (let septikIndex = 0; septikIndex < aspan.SEPTIKS.length; ++septikIndex) {
        const septik = aspan.SEPTIKS[septikIndex];
        forms.push(new NounForm(
            nb.septikForm(septik).raw,
            0.5,
            kSingularIndex,
            septikIndex,
            null,
            null,
            null,
        ));
        forms.push(new NounForm(
            nb.pluralSeptikForm(septik).raw,
            0.5,
            kPluralIndex,
            septikIndex,
            null,
            null,
            null,
        ));
        for (let possPersonIndex = 0; possPersonIndex < aspan.GRAMMAR_PERSONS.length; ++possPersonIndex) {
            const person = aspan.GRAMMAR_PERSONS[possPersonIndex];
            for (let possNumberIndex = 0; possNumberIndex < aspan.GRAMMAR_NUMBERS.length; ++possNumberIndex) {
                let number = aspan.GRAMMAR_NUMBERS[possNumberIndex];
                forms.push(new NounForm(
                    nb.possessiveSeptikForm(person, number, septik).raw,
                    0.5,
                    kSingularIndex,
                    septikIndex,
                    possPersonIndex,
                    possNumberIndex,
                    null,
                ));
                forms.push(new NounForm(
                    nb.pluralPossessiveSeptikForm(person, number, septik).raw,
                    0.5,
                    kPluralIndex,
                    septikIndex,
                    possPersonIndex,
                    possNumberIndex,
                    null,
                ));
            }
        }
    }
    forms.push(new NounForm(
        nb.relatedAdj().raw,
        0.5,
        null,
        null,
        null,
        null,
        "dagy",
    ));
    return forms;
}

function parseTranslations(s) {
    if (s == null || s.length == 0) {
        return [];
    }
    return s.split(",");
}

function writeDetectSuggestFormsLine(base, ruGlosses, enGlosses, forms, outputStream) {
    let dataObject = {
        pos: "noun",
        base: base,
        ruwkt: ruGlosses,
        enwkt: enGlosses,
        forms: forms,
    };
    let dataString = JSON.stringify(dataObject);
    outputStream.write(`${dataString}\n`);
}

class InputItem {
    constructor(line) {
        let parts = line.split("\t");
        this.valid = true;
        if (parts.length < 2) {
            this.valid = false;
        } else if (parts[0] == "v1") {
            this.noun = parts[1];
            this.freq = 0;
            this.ruGlosses = [];
            this.ruGlossesFe = [];
            this.enGlosses = [];
            this.enGlossesFe = [];
        } else if (parts[0] == "v2") {
            if (parts.length != 6) {
                console.log(`invalid number of parts in input line: ${line}`);
                this.valid = false;
                return;
            }
            this.noun = parts[1];
            this.freq = 0;
            this.ruGlosses = parseTranslations(parts[2]);
            this.ruGlossesFe = parseTranslations(parts[3]);
            this.enGlosses = parseTranslations(parts[4]);
            this.enGlossesFe = parseTranslations(parts[5]);
        } else {
            console.log(`failed to parse input line: ${line}`);
            this.valid = false;
        }
    }
}

async function processLineByLine(args) {
    const inputStream = fs.createReadStream(args.input);
    const outputStream = fs.createWriteStream(args.output);

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
        const inputNoun = inputItem.noun;

        if (args.command == kDetectSuggestFormsCommand) {
            /**
             * {
             *   "base": <NOUN>,
             *   "forms": [
             *     {
             *       "form": <FORM>,
             *       "weight": <FORM_WEIGHT>,
             *       "number": "<NUMBER>",
             *       "septik": "<SEPTIK>",
             *       "possPerson": "<PERSON>",
             *       "possNumber": "<NUMBER>",
             *       "wordgen": "<WORDGEN>",
             *     },
             *     ...
             *   ]
             * }
             */
            let nounForms = createMainNounForms(inputNoun.toLowerCase());
            writeDetectSuggestFormsLine(inputNoun, inputItem.ruGlosses, inputItem.enGlosses, nounForms, outputStream);
        }
    }
}

function checkCommandArgs(args, cmd, cmdArgsCount) {
    if (args.length != cmdArgsCount + 1) {
        throw new Error(`Command '${cmd}' requires exactly ${cmdArgsCount} arguments`);
    }
}

class Args {
    constructor(command, input, output) {
        this.command = command;
        this.input = input;
        this.output = output;
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
    if (command == kDetectSuggestFormsCommand) {
        return acceptCommandWithInputAndOutput(args, command);
    } else {
        throw new Error(`Unsupported command: ${command}`);
    }
}

let args = parseArgs();
processLineByLine(args);