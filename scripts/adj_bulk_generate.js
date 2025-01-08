const fs = require('fs');
const readline = require('readline');
const aspan = require('../BUILD/aspan.js');

const kDetectSuggestFormsCommand = "detect_suggest_forms";

const kDefaultWeight = 0.5;

class AdjForm {
    constructor(form, weight, wordgen) {
        this.form = form;
        this.weight = weight;
        if (wordgen != null) {
            this.wordgen = wordgen;
        }
    }
}

function createMainAdjForms(adj) {
    let ab = new aspan.AdjBuilder(adj);
    let forms = [];
    forms.push(new AdjForm(adj, kDefaultWeight, /* wordgen */ null));
    forms.push(
        new AdjForm(
            ab.rakForm().raw,
            kDefaultWeight,
            "rak",
        )
    );
    forms.push(
        new AdjForm(
            ab.dauForm().raw,
            kDefaultWeight,
            "dau",
        )
    );
    return forms;
}

function writeDetectSuggestFormsLine(base, ruGlosses, enGlosses, forms, outputStream) {
    let dataObject = {
        pos: "adjective",
        base: base,
        ruwkt: ruGlosses,
        enwkt: enGlosses,
        forms: forms,
    };
    let dataString = JSON.stringify(dataObject);
    outputStream.write(`${dataString}\n`);
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
        if (parts[0] == "v2") {
            if (parts.length != 6) {
                console.log(`invalid number of parts in input line: ${line}`);
                this.valid = false;
                return;
            }
            this.adj = parts[1];
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

    for await (const inputLine of rl) {
        let inputItem = new InputItem(inputLine);
        if (!inputItem.valid) {
            console.log(`Abort on invalid input line: ${inputLine}`);
            process.exit(1);
        }
        const inputAdj = inputItem.adj;

        if (args.command == kDetectSuggestFormsCommand) {
            /**
             * {
             *   "base": <ADJ>,
             *   "forms": [
             *     {
             *       "form": <FORM>,
             *       "weight": <FORM_WEIGHT>,
             *       "wordgen": "<WORDGEN>",
             *     },
             *     ...
             *   ]
             * }
             */
            let adjForms = createMainAdjForms(inputAdj.toLowerCase());
            writeDetectSuggestFormsLine(inputAdj, inputItem.ruGlosses, inputItem.enGlosses, adjForms, outputStream);
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