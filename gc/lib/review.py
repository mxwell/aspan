from dataclasses import dataclass
from enum import Enum


class ReviewStatus(Enum):
    UNKNOWN = 0
    NEW = 1
    PENDING = 2
    APPROVED = 3
    DISAPPROVED = 4
    DISCARDED = 5


class ReviewVote(Enum):
    DISAPPROVE = -1
    APPROVE = 1
