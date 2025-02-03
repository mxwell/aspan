import logging

CYR_TO_ASCII = {
  "а": "a",
  "ә": "Ae",
  "б": "b",
  "в": "v",
  "г": "g",
  "ғ": "Gh",
  "д": "d",
  "е": "Ye",
  "ё": "Yo",
  "ж": "Zh",
  "з": "z",
  "и": "Yi",
  "й": "j",
  "к": "k",
  "қ": "q",
  "л": "l",
  "м": "m",
  "н": "n",
  "ң": "Ng",
  "о": "o",
  "ө": "Oe",
  "п": "p",
  "р": "r",
  "с": "s",
  "т": "t",
  "у": "u",
  "ұ": "Uu",
  "ү": "Ue",
  "ф": "f",
  "х": "x",
  "һ": "h",
  "ц": "c",
  "ч": "Ch",
  "ш": "Sh",
  "щ": "Tsch",
  "ъ": "Hard",
  "ы": "y",
  "і": "i",
  "ь": "Soft",
  "э": "e",
  "ю": "Yu",
  "я": "Ya",
  " ": "_",
  "-": "-",
  "?": "Qestion",
  "!": "Bang",
  ",": "Comma",
  ".": "Dot",
  ":": "Colon",
  ";": "Semicol",
  "(": "BraceOp",
  ")": "BraceCl",
  "\"": "DQuot",
  "'": "SQuot",
}


def transliterate(cyr):
    result = []
    prev_space = False
    for c in cyr:
        if c == " ":
            if prev_space:
                logging.error("transliterate error: repeated space")
                return None
            prev_space = True
        else:
            prev_space = False

        dst = CYR_TO_ASCII.get(c)
        if dst:
            result.append(dst)
        else:
            logging.error("transliterate error: unsupported char %s", c)
            return None

    return "".join(result)
