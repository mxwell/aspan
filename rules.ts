let VOWELS = "аәеиоөуүұыіэюя";
let SOFT_VOWELS = "әеөүі";

let PRESENT_TRANSITIVE_EXCEPT_VERBS1 = ["оқу", "аршу", "тану", "ренжу", "есту"];
let PRESENT_TRANSITIVE_EXCEPT_VERBS2 = ["қою", "құю"];
let PRESENT_TRANSITIVE_EXCEPT_VERBS3 = ["сүю"];
const PRESENT_TRANSITIVE_AFFIXES: Record<Face, Record<Plurality, String[]>> = {
    First: {
        Singular: ["мын", "мін"],
        Plural: ["мыз", "міз"],
    },
    Second: {
        Singular: ["сың", "сің"],
        Plural: ["сыңдар", "сіңдер"],
    },
    SecondPolite: {
        Singular: ["сыз", "сіз"],
        Plural: ["сыздар", "сіздер"],
    },
    Third: {
        Singular: ["ды", "ді"],
        Plural: ["ды", "ді"]
    }
}
