from dataclasses import dataclass


@dataclass
class WordInfo:
    word_id: int
    word: str
    pos: str
    exc_verb: bool
    lang: str
    created_at: int
