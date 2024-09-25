.mode tabs
SELECT
    "v2",
    wru.word AS word,
    wru.translations AS kvd_ru,
    wru_fe.translations AS kvd_ru_fe,
    wen.translations AS kvd_en,
    wen_fe.translations AS kvd_en_fe
FROM (
    SELECT
        w1.word AS word,
        GROUP_CONCAT(w2.word) AS translations
    FROM
        words w1
    JOIN
        translations t ON w1.word_id = t.word_id
    JOIN
        words w2 ON t.translated_word_id = w2.word_id
    WHERE
        w1.pos = "VERB" AND
        w1.exc_verb = 0 AND
        w2.lang = "ru"
    GROUP BY
        w1.word
) wru
LEFT JOIN (
    SELECT
        w1.word AS word,
        GROUP_CONCAT(w2.word) AS translations
    FROM
        words w1
    JOIN
        translations t ON w1.word_id = t.word_id
    JOIN
        words w2 ON t.translated_word_id = w2.word_id
    WHERE
        w1.pos = "VERB" AND
        w1.exc_verb = 1 AND
        w2.lang = "ru"
    GROUP BY
        w1.word
) wru_fe
ON wru.word = wru_fe.word
LEFT JOIN (
    SELECT
        w1.word AS word,
        GROUP_CONCAT(w2.word) AS translations
    FROM
        words w1
    JOIN
        translations t ON w1.word_id = t.word_id
    JOIN
        words w2 ON t.translated_word_id = w2.word_id
    WHERE
        w1.pos = "VERB" AND
        w1.exc_verb = 0 AND
        w2.lang = "en"
    GROUP BY
        w1.word
) wen
ON wru.word = wen.word
LEFT JOIN (
    SELECT
        w1.word AS word,
        GROUP_CONCAT(w2.word) AS translations
    FROM
        words w1
    JOIN
        translations t ON w1.word_id = t.word_id
    JOIN
        words w2 ON t.translated_word_id = w2.word_id
    WHERE
        w1.pos = "VERB" AND
        w1.exc_verb = 1 AND
        w2.lang = "en"
    GROUP BY
        w1.word
) wen_fe
ON wru.word = wen_fe.word
ORDER BY wru.word
;