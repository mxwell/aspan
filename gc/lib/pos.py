from enum import Enum


# Reference: https://universaldependencies.org/u/pos/all.html
class PosTag(Enum):
    ADJ = 1    # adjectives, including participles
    ADP = 2    # adpositions = prepositions + postpositions
    ADV = 3    # adverb, including adverbial participles
    AUX = 4    # auxiliary, used in verbal phrases
    CCONJ = 5  # coordinating conjunctions, such as "and", "or", "but"
    DET = 6    # determiners, e.g. articles, possessive/demonstrative/etc determiners
    INTJ = 7   # interjections, e.g. exclamations
    NOUN = 8   # common nouns
    NUM = 9    # numerals
    PART = 10  # particles, e.g. negation and question particles
    PRON = 11  # pronouns
    PROPN = 12 # proper noun
    PUNCT = 13 # punctuation, not really applicable for dictionary entries
    SCONJ = 14 # subordinating conjunctions, such as "that", "if", "while"
    SYM = 15   # symbols, e.g. $, %, unlikely to be used for dictionary entries
    VERB = 16  # verbs
    X = 17     # other, e.g. unintelligible material or foreign words


def parse_pos(pos):
    try:
        item = PosTag[pos]
        return item
    except KeyError as e:
        return None
