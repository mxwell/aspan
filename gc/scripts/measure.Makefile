SCRIPT_PATH = scripts/measure_llm_vocab.py
WORD_SET = gen/word_set.csv
TEMP_BATCH_4O = gen/temp_batch_4o.jsonl
TEMP_BATCH_4O_MINI = gen/temp_batch_4o_mini.jsonl
BATCH_ID_4O = gen/batch_id_4o.txt
GPT4O_OUTPUT = gen/gpt4o_output.jsonl
BATCH_ID_4O_MINI = gen/batch_id_4o_mini.txt
GPT4O_MINI_OUTPUT = gen/gpt4o_mini_output.jsonl

HAIKU_TRANSLATION = gen/haiku_translation.jsonl
SONNET_TRANSLATION = gen/sonnet_translation.jsonl
GEMINI_FLASH_TRANSLATION = gen/gemini_flash_translation.jsonl
GEMINI_PRO_TRANSLATION = gen/gemini_pro_translation.jsonl

.PHONY: measure_4o measure_4o_mini measure_haiku measure_sonnet measure_gemini_flash measure_gemini_pro

$(BATCH_ID_4O): $(WORD_SET)
	python3 $(SCRIPT_PATH) launch-batch --input-words $(WORD_SET) --temp-batch $(TEMP_BATCH_4O) --model gpt-4o-2024-08-06 --batch-id-path $(BATCH_ID_4O)

$(BATCH_ID_4O_MINI): $(WORD_SET)
	python3 $(SCRIPT_PATH) launch-batch --input-words $(WORD_SET) --temp-batch $(TEMP_BATCH_4O_MINI) --model gpt-4o-mini-2024-07-18 --batch-id-path $(BATCH_ID_4O_MINI)

measure_4o:
	python3 $(SCRIPT_PATH) measure-batch-output --input-words $(WORD_SET) --batch-output $(GPT4O_OUTPUT) --batch-id-path $(BATCH_ID_4O)

measure_4o_mini:
	python3 $(SCRIPT_PATH) measure-batch-output --input-words $(WORD_SET) --batch-output $(GPT4O_MINI_OUTPUT) --batch-id-path $(BATCH_ID_4O_MINI)

$(HAIKU_TRANSLATION): $(WORD_SET)
	python3 $(SCRIPT_PATH) claude-translate --input-words $(WORD_SET) --batch-output $(HAIKU_TRANSLATION) --model claude-3-haiku-20240307

measure_haiku:
	python3 $(SCRIPT_PATH) claude-measure --input-words $(WORD_SET) --batch-output $(HAIKU_TRANSLATION)

$(SONNET_TRANSLATION): $(WORD_SET)
	python3 $(SCRIPT_PATH) claude-translate --input-words $(WORD_SET) --batch-output $(SONNET_TRANSLATION) --model claude-3-5-sonnet-20240620

measure_sonnet:
	python3 $(SCRIPT_PATH) claude-measure --input-words $(WORD_SET) --batch-output $(SONNET_TRANSLATION)

$(GEMINI_FLASH_TRANSLATION): $(WORD_SET)
	python3 $(SCRIPT_PATH) gemini-translate --input-words $(WORD_SET) --batch-output $(GEMINI_FLASH_TRANSLATION) --model gemini-1.5-flash

measure_gemini_flash:
	python3 $(SCRIPT_PATH) claude-measure --input-words $(WORD_SET) --batch-output $(GEMINI_FLASH_TRANSLATION)

$(GEMINI_PRO_TRANSLATION): $(WORD_SET)
	python3 $(SCRIPT_PATH) gemini-translate --input-words $(WORD_SET) --batch-output $(GEMINI_PRO_TRANSLATION) --model gemini-1.5-pro

measure_gemini_pro:
	python3 $(SCRIPT_PATH) claude-measure --input-words $(WORD_SET) --batch-output $(GEMINI_PRO_TRANSLATION)
