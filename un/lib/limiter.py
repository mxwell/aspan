import logging
import threading
import time


class Limiter(object):

    def __init__(self, slots_per_hour):
        assert slots_per_hour < 1000

        self.lock = threading.Lock()
        self.n = slots_per_hour
        self.slots = [None] * slots_per_hour

        self.size = 0
        self.head = 0
        self.tail = 0  # position for the next write

    def acquire(self):
        now = int(time.time())
        n = self.n
        with self.lock:
            if self.size < n:
                self.push(now)
                return True
            head_value = self.slots[self.head]
            if head_value < now - 3600:
                self.head = (self.head + 1) % n
                self.size -= 1
                self.push(now)
                return True
            logging.warn("Limiting: %d events in last hour, oldest %d, now %d",
                self.size, head_value, now)
            return False

    def push(self, now):
        self.slots[self.tail] = now
        self.tail = (self.tail + 1) % self.n
        self.size += 1
