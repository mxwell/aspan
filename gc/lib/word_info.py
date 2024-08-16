from dataclasses import dataclass
from typing import List

@dataclass
class WordInfo:
    word_id: int
    word: str
    pos: str
    exc_verb: bool
    lang: str
    comment: str
    created_at: int
    translated_word_ids: List[int]
