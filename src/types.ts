enum GrammarPerson {
    First = "First",
    Second = "Second",
    SecondPolite = "SecondPolite",
    Third = "Third",
}

enum GrammarNumber {
    Singular = "Singular",
    Plural = "Plural",
}

enum SentenceType {
    Statement = "Statement",
    Negative = "Negative",
    Question = "Question",
}

enum PersAffix1LetterGroup {
    PersAffix1DefaultGroup = "PersAffix1DefaultGroup",
    PersAffix1GzGroup = "PersAffix1GzGroup",
    PersAffixUnvoicedGroup = "PersAffixUnvoicedGroup",
}

const GRAMMAR_PERSONS: GrammarPerson[] = [GrammarPerson.First, GrammarPerson.Second, GrammarPerson.SecondPolite, GrammarPerson.Third];
const GRAMMAR_NUMBERS: GrammarNumber[] = [GrammarNumber.Singular, GrammarNumber.Plural];