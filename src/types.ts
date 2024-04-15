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
    PersAffix1MnGroup = "PersAffix1MnGroup",
    PersAffixUnvoicedGroup = "PersAffixUnvoicedGroup",
}

/* shak = tense. Communicates time of the action. */
enum VerbShak {
    PresentTransitive = "PresentTransitive",
    PresentContinuous = "PresentContinuous",
}

const GRAMMAR_PERSONS: GrammarPerson[] = [GrammarPerson.First, GrammarPerson.Second, GrammarPerson.SecondPolite, GrammarPerson.Third];
const GRAMMAR_NUMBERS: GrammarNumber[] = [GrammarNumber.Singular, GrammarNumber.Plural];
const GRAMMAR_SENTENCE_TYPES: SentenceType[] = [SentenceType.Statement, SentenceType.Negative, SentenceType.Question];