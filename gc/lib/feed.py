from dataclasses import dataclass
from typing import List


@dataclass
class VoteInfo:
    vote: int  # ReviewVote.value
    name: str


@dataclass
class FeedItem:
    name: str
    src_word: str
    src_lang: str
    dst_word: str
    dst_lang: str
    translation_id: int
    votes: List[VoteInfo]
    created_at: int
