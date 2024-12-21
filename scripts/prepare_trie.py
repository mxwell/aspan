node_counter = 0
node_list = list()
word_counter = 0
word_list = list()

class Node(object):

    def __init__(self):
        global node_counter
        global node_list

        self.id = node_counter
        node_counter += 1
        self.children = dict()
        self.words = list()

        node_list.append(self)

    def __str__(self):
        parts = []
        parts.append(str(len(self.words)))
        for w in self.words:
            parts.append(str(w))
        parts.append(str(len(self.children)))
        for ch, child in self.children.items():
            parts.append(str(ord(ch)))
            parts.append(str(child.id))
        return "\t".join(parts)


def add_path(root, word):
    global word_counter
    global word_list

    node = root
    for ch in word:
        child = node.children.get(ch, None)
        if child is None:
            child = Node()
            node.children[ch] = child
        node = child
    node.words.append(word_counter)
    word_counter += 1
    word_list.append(word)


def load_verbs(path):
    root = Node()
    counter = 0
    for line in open(path):
        verb = line.split("\t")[1]
        add_path(root, verb)
        counter += 1
    print(f"Loaded {counter} verbs")
    return root


def dump_words(path):
    with open(path, "wt") as words_out:
        for word in word_list:
            words_out.write(f"{word}\n")
        print(f"Dumped {len(word_list)} words")


def dump_trie(path):
    with open(path, "wt") as trie_out:
        for node in node_list:
            trie_out.write(f"{node}\n")
        print(f"Dumped {len(node_list)} nodes")


def main():
    root = load_verbs("data/verb_suggest/verbs_with_ru_en.v2.YYYYMMDD.csv")
    dump_words("data/verb_suggest/suggestions.YYYYMMDD.txt")
    dump_trie("data/verb_suggest/suggest_trie.YYYYMMDD.txt")


main()
