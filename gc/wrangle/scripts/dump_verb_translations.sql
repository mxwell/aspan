.mode json
SELECT
    wru_word, wru_exc_verb, wru_translations, wen_translations
FROM (
    SELECT
        w1.word AS wru_word,
        w1.exc_verb AS wru_exc_verb,
        GROUP_CONCAT(w2.word) AS wru_translations
    FROM
        words w1
    JOIN
        translations t ON w1.word_id = t.word_id
    JOIN
        words w2 ON t.translated_word_id = w2.word_id
    WHERE
        w1.pos = "VERB" AND
        w2.lang = "ru"
    GROUP BY
        w1.word_id
) wru
LEFT JOIN (
    SELECT
        w1.word AS wen_word,
        w1.exc_verb AS wen_exc_verb,
        GROUP_CONCAT(w2.word) AS wen_translations
    FROM
        words w1
    JOIN
        translations t ON w1.word_id = t.word_id
    JOIN
        words w2 ON t.translated_word_id = w2.word_id
    WHERE
        w1.pos = "VERB" AND
        w2.lang = "en"
    GROUP BY
        w1.word_id
) wen
ON wru.wru_word = wen.wen_word
;