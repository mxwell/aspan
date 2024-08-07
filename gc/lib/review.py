from enum import Enum


class ReviewStatus(Enum):
    UNKNOWN = 0
    NEW = 1
    PENDING = 2
    APPROVED = 3
    DECLINED = 4


class ReviewVote(Enum):
    DISAPPROVE = 0
    APPROVE = 1
