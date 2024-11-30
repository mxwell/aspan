from dataclasses import dataclass
from enum import Enum


class ContribAction(Enum):
    ADD_TRANSLATION = 1
    APPROVE_CONFIRMED = 2
    DISAPPROVE_CONFIRMED = 3


@dataclass
class ContribEntry:
    action: ContribAction
    user_id: int
    created_at: int
