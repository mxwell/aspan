const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const kDetectSuggestFormsCommand = "detect_suggest_forms";

class NounForm {
    constructor(form, weight, number, septik) {
        this.form = form;
        this.weight = weight;
        this.number = number;
        this.septik = septik;
    }
}

function createMainNounForms(noun) {
    let nb = new aspan.NounBuilder(noun);

    let forms = [];
    /* TODO calculate weight */
    for (const septik of aspan.SEPTIKS) {
        forms.push(new NounForm(
            nb.septikForm(septik).raw,
            0.5,
            aspan.GrammarNumber.Singular,
            septik
        ));
    }
    for (const septik of aspan.SEPTIKS) {
        forms.push(new NounForm(
            nb.pluralSeptikForm(septik).raw,
            0.5,
            aspan.GrammarNumber.Plural,
            septik
        ));
    }
    return forms;
}

function writeDetectSuggestFormsLine(base, exceptional, forms, outputStream) {
    let dataObject = {
        base: base,
        exceptional: exceptional,
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
             *   "exceptional": <FORCE_EXCEPTIONAL>,
             *   "forms": [
             *     {
             *       "form": <FORM>,
             *       "weight": <FORM_WEIGHT>,
             *       "number": "<NUMBER>",
             *       "septik": "<SEPTIK>"
             *     },
             *     ...
             *   ]
             * }
             */
            let nounForms = createMainNounForms(inputNoun);
            writeDetectSuggestFormsLine(inputNoun, false, nounForms, outputStream);
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