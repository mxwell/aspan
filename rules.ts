const VOWELS = "аәеиоөуүұыіэюя";
const SOFT_VOWELS = "әеөүі";
const CONS_GROUP1 = "руйл";
const CONS_GROUP2 = "жзмнң";

const PRESENT_TRANSITIVE_EXCEPT_VERBS1 = ["оқу", "аршу", "тану", "ренжу", "есту"];
const PRESENT_TRANSITIVE_EXCEPT_VERBS2 = ["қою", "құю", "қию"];
const PRESENT_TRANSITIVE_EXCEPT_VERBS3 = ["сүю"];
const PRESENT_TRANSITIVE_EXCEPT_VERBS4 = ["кию"];
const PRESENT_TRANSITIVE_AFFIXES: Record<GrammarPerson, Record<GrammarNumber, String[]>> = {
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
};

const MAME = ["ма", "ме"];
const BABE = ["ба", "бе"];
const PAPE = ["па", "пе"];

const PRONOUN_BY_PERSON_NUMBER: Record<GrammarPerson, Record<GrammarNumber, String>> = {
    First: {
        Singular: "мен",
        Plural: "біз"
    },
    Second: {
        Singular: "сен",
        Plural: "сендер"
    },
    SecondPolite: {
        Singular: "Сіз",
        Plural: "Сіздер"
    },
    Third: {
        Singular: "ол",
        Plural: "олар"
    }
};