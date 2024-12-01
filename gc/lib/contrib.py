from dataclasses import dataclass
from enum import Enum


class ContribAction(Enum):
    ADD_TRANSLATION = 1
    APPROVE_CONFIRMED = 2
    DISAPPROVE_CONFIRMED = 3


@dataclass
class ContribEntry:
    translation_id: int
    review_id: int
    user_id: int
    action: ContribAction
    created_at: int # seconds since unix epoch
