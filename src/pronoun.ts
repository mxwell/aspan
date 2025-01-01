const PronounParts: Record<GrammarPerson, Record<GrammarNumber, Record<Septik, string[]>>> = {
    First: {
        Singular: {
            Atau: ["мен", "", ""],
            Ilik: ["мен", "", "ің"],
            Barys: ["маған", "", ""],
            Tabys: ["мен", "", "і"],
            Jatys: ["мен", "", "де"],
            Shygys: ["мен", "", "ен"],
            Komektes: ["мен", "", "імен"],
        },
        Plural: {
            Atau: ["біз", "", ""],
            Ilik: ["біз", "", "дің"],
            Barys: ["біз", "", "ге"],
            Tabys: ["біз", "", "ді"],
            Jatys: ["біз", "", "де"],
            Shygys: ["біз", "", "ден"],
            Komektes: ["біз", "", "бен"],
        },
    },
    Second: {
        Singular: {
            Atau: ["сен", "", ""],
            Ilik: ["сен", "", "ің"],
            Barys: ["саған", "", ""],
            Tabys: ["сен", "", "і"],
            Jatys: ["сен", "", "де"],
            Shygys: ["сен", "", "ен"],
            Komektes: ["сен", "", "імен"],
        },
        Plural: {
            Atau: ["сен", "дер", ""],
            Ilik: ["сен", "дер", "дің"],
            Barys: ["сен", "дер", "ге"],
            Tabys: ["сен", "дер", "ді"],
            Jatys: ["сен", "дер", "де"],
            Shygys: ["сен", "дер", "ден"],
            Komektes: ["сен", "дер", "мен"],
        },
    },
    SecondPolite: {
        Singular: {
            Atau: ["Сіз", "", ""],
            Ilik: ["Сіз", "", "дің"],
            Barys: ["Сіз", "", "ге"],
            Tabys: ["Сіз", "", "ді"],
            Jatys: ["Сіз", "", "де"],
            Shygys: ["Сіз", "", "ден"],
            Komektes: ["Сіз", "", "бен"],
        },
        Plural: {
            Atau: ["Сіз", "дер", ""],
            Ilik: ["Сіз", "дер", "дің"],
            Barys: ["Сіз", "дер", "ге"],
            Tabys: ["Сіз", "дер", "ді"],
            Jatys: ["Сіз", "дер", "де"],
            Shygys: ["Сіз", "дер", "ден"],
            Komektes: ["Сіз", "дер", "мен"],
        },
    },
    Third: {
        Singular: {
            Atau: ["ол", "", ""],
            Ilik: ["о", "", "ның"],
            Barys: ["оған", "", ""],
            Tabys: ["он", "", "ы"],
            Jatys: ["он", "", "да"],
            Shygys: ["о", "", "дан"],
            Komektes: ["оны", "", "мен"],
        },
        Plural: {
            Atau: ["олар", "", ""],
            Ilik: ["олар", "", "дың"],
            Barys: ["олар", "", "ға"],
            Tabys: ["олар", "", "ды"],
            Jatys: ["олар", "", "да"],
            Shygys: ["олар", "", "дан"],
            Komektes: ["олар", "", "мен"],
        },
    }
};

class PronounBuilder {
    private partsBySeptik: Record<Septik, string[]>;

    public constructor(person: GrammarPerson, number: GrammarNumber) {
        this.partsBySeptik = PronounParts[person][number];
    }

    septikForm(septik: Septik): Phrasal {
        const parts = this.partsBySeptik[septik];
        let builder = new PhrasalBuilder()
            .pronounBase(parts[0]);
        if (parts[1].length > 0) {
            builder.pluralAffix(parts[1]);
        }
        if (parts[2].length > 0) {
            builder.septikAffix(parts[2]);
        }
        return builder.build();
    }
}
