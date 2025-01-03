const fs = require('fs');
const aspan = require('../BUILD/aspan.js');

const kDetectSuggestFormsCommand = "detect_suggest_forms";

class PronounForm {
    constructor(form, weight, person, number, septik) {
        this.form = form;
        this.weight = weight;
        this.person = person;
        this.number = number;
        this.septik = septik;
    }
}

function createPronounForms(personIndex, numberIndex) {
    const grammarPerson = aspan.GRAMMAR_PERSONS[personIndex];
    const grammarNumber = aspan.GRAMMAR_NUMBERS[numberIndex];
    let builder = new aspan.PronounBuilder(grammarPerson, grammarNumber);

    let forms = [];
    for (let septikIndex = 0; septikIndex < aspan.SEPTIKS.length; ++septikIndex) {
        const septik = aspan.SEPTIKS[septikIndex];
        forms.push(
            new PronounForm(
                builder.septikForm(septik).raw,
                0.5,
                personIndex,
                numberIndex,
                septikIndex,
            )
        );
    }
    return forms;
}

function writeDetectSuggestFormsLine(forms, ruGlosses, enGlosses, outputStream) {
    let dataObject = {
        pos: "pronoun",
        base: forms[0].form,
        ruwkt: ruGlosses,
        enwkt: enGlosses,
        forms: forms,
    };
    let dataString = JSON.stringify(dataObject);
    outputStream.write(`${dataString}\n`);
}

function getGlosses(personIndex, numberIndex) {
    if (personIndex == 0) {
        if (numberIndex == 0) {
            return [["я"], ["I"]];
        } else {
            return [["мы"], ["we"]];
        }
    } else if (personIndex == 1) {
        if (numberIndex == 0) {
            return [["ты"], ["you"]];
        } else {
            return [["вы"], ["you"]];
        }
    } else if (personIndex == 2) {
        if (numberIndex == 0) {
            return [["Вы"], ["you"]];
        } else {
            return [["Вы"], ["you"]];
        }
    } else {
        if (numberIndex == 0) {
            return [["он", "она", "оно"], ["he", "she", "it"]];
        } else {
            return [["они"], ["they"]];
        }
    }
}

function generatePronounForms(args) {
    const outputStream = fs.createWriteStream(args.output);

    for (const personIndex in aspan.GRAMMAR_PERSONS) {
        for (const numberIndex in aspan.GRAMMAR_NUMBERS) {
            if (args.command == kDetectSuggestFormsCommand) {
                /**
                 * {
                 *   "base": <PRONOUN>,
                 *   "forms": [
                 *     {
                 *       "form": <FORM>,
                 *       "weight": <FORM_WEIGHT>,
                 *       "person": <PERSON>,
                 *       "number": <NUMBER>,
                 *       "septik": "<SEPTIK>",
                 *     },
                 *     ...
                 *   ]
                 * }
                 */
                let pronounForms = createPronounForms(personIndex, numberIndex);
                let glosses = getGlosses(personIndex, numberIndex);
                writeDetectSuggestFormsLine(pronounForms, glosses[0], glosses[1], outputStream);
            }
        }
    }
}

function checkCommandArgs(args, cmd, cmdArgsCount) {
    if (args.length != cmdArgsCount + 1) {
        throw new Error(`Command '${cmd}' requires exactly ${cmdArgsCount} arguments`);
    }
}

class Args {
    constructor(command, output) {
        this.command = command;
        this.output = output;
    }
}

function acceptCommandWithInputAndOutput(args, cmd) {
    checkCommandArgs(args, cmd, 1);
    return new Args(cmd, args[1]);
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
generatePronounForms(args);