node_counter = 0
node_list = list()
char_list = list()
char_map = dict()
word_counter = 0
word_list = list()


def get_char_id(ch):
    if ch in char_map:
        return char_map[ch]
    char_id = len(char_list)
    char_map[ch] = char_id
    char_list.append(ch)
    return char_id


class Node(object):

    def __init__(self):
        global node_counter
        global node_list

        self.id = node_counter
        node_counter += 1
        self.children = dict()
        self.words = list()
        self.suggestions = list()

        node_list.append(self)

    def __str__(self):
        parts = []
        parts.append(str(len(self.words)))
        for w in self.words:
            parts.append(str(w))
        parts.append(str(len(self.suggestions)))
        for sugg in self.suggestions:
            parts.append(str(sugg[1]))
        parts.append(str(len(self.children)))
        for char_id, child in self.children.items():
            parts.append(str(char_id))
            parts.append(str(child.id))
        return "\t".join(parts)


def pull_suggestions(node):
    suggestions = list()
    for char_id, child in node.children.items():
        pull_suggestions(child)
        for w in child.words:
            suggestions.append((len(word_list[w]), w))
        for sugg in child.suggestions:
            word_id = sugg[1]
            suggestions.append((len(word_list[word_id]), word_id))
    suggestions.sort()
    node.suggestions = suggestions[:10]


def add_path(root, word):
    global word_counter
    global word_list

    node = root
    for ch in word:
        char_id = get_char_id(ch)
        child = node.children.get(char_id, None)
        if child is None:
            child = Node()
            node.children[char_id] = child
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


def word_to_char_ids(word):
    parts = []
    for ch in word:
        char_id = get_char_id(ch)
        parts.append(str(char_id))
    return " ".join(parts)


def dump_chars(path):
    with open(path, "wt") as chars_out:
        for char in char_list:
            chars_out.write(f"{char}\n")
        print(f"Dumped {len(char_list)} chars")


def dump_words(path):
    with open(path, "wt") as words_out:
        for word in word_list:
            s = word_to_char_ids(word)
            words_out.write(f"{s}\n")
        print(f"Dumped {len(word_list)} words")


def dump_trie(path):
    with open(path, "wt") as trie_out:
        for node in node_list:
            trie_out.write(f"{node}\n")
        print(f"Dumped {len(node_list)} nodes")


def main():
    root = load_verbs("data/verb_suggest/verbs_with_ru_en.v2.YYYYMMDD.csv")
    pull_suggestions(root)
    dump_chars("data/verb_suggest/chars.txt")
    dump_words("data/verb_suggest/suggestions.txt")
    dump_trie("data/verb_suggest/trie.txt")


main()
