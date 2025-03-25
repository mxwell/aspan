#include "flat_node_trie.h"
#include "runes.h"
#include "trie.h"

#include "Poco/Clock.h"
#include "Poco/JSON/Object.h"
#include "Poco/JSON/Parser.h"
#include "Poco/Net/HTTPServer.h"
#include "Poco/Net/HTTPRequestHandler.h"
#include "Poco/Net/HTTPRequestHandlerFactory.h"
#include "Poco/Net/HTTPServerRequest.h"
#include "Poco/Net/HTTPServerResponse.h"
#include "Poco/Net/ServerSocket.h"
#include "Poco/Util/ServerApplication.h"
#include "Poco/URI.h"

#include <limits>
#include <optional>
#include <string>

using namespace Poco;
using namespace Poco::Net;
using namespace Poco::Util;

namespace {

static const std::string kAnalyzePath = "/analyze";
static const std::string kAnalyzeSubPath = "/analyze_sub";  // analyze subtitles where words have start/end timestamps that should be preserved in the response
static const std::string kDetectPath = "/detect";

static constexpr size_t kNoWordIndex = std::numeric_limits<size_t>::max();
static constexpr size_t kNoTimestamp = std::numeric_limits<uint32_t>::max();

bool StartsWith(const std::string& str, const std::string& prefix) {
    return str.compare(0, prefix.size(), prefix) == 0;
}

/* Response for /analyze:
 *
 * {
 *     "time": <DOUBLE>,
 *     "parts": [
 *         {
 *             "text": <STRING>,
 *             "forms": [
 *                 {
 *                     "initial": <STRING>,
 *                     "meta": <OBJECT>,
 *                     "transition": <STRING
 *                 },
 *                 {..}
 *             ]
 *         },
 *         {..}
 *     ]
 * }
 *
 */

std::optional<JSON::Object> buildAnalyzeResponse(const std::string& queryText, const NKiltMan::FlatNodeTrie& trie) {
    Clock clock{};

    JSON::Array textArray;

    NKiltMan::TRunes runes;
    auto conversionResult = NKiltMan::StringToRunesNoExcept(queryText, runes);
    if (conversionResult != NKiltMan::ConversionResult::SUCCESS) {
        return {};
    }

    NKiltMan::TRunes fragmentRunes;

    const JSON::Array kEmptyArray;

    auto flushFragment = [&textArray, &fragmentRunes, &kEmptyArray]() {
        if (!fragmentRunes.empty()) {
            std::string fragmentStr;
            NKiltMan::RunesToString(fragmentRunes, fragmentStr);

            JSON::Object partObject;
            partObject.set("text", fragmentStr);
            partObject.set("forms", kEmptyArray);

            textArray.add(partObject);

            fragmentRunes.clear();
        }
    };

    bool prevIsAlpha = false;
    for (auto runeIter = runes.begin(); runeIter != runes.end(); ) {
        if (!prevIsAlpha) {
            NKiltMan::TRunes::iterator next;
            auto node = trie.Traverse(runeIter, runes.end(), next);
            if (node && (next == runes.end() || !NKiltMan::IsAlpha(*next))) {
                const auto terminalId = node->terminalId;
                if (terminalId != NKiltMan::FlatNode::kNoTerminalId) {
                    flushFragment();

                    auto terminal = trie.terminals[terminalId];
                    JSON::Array wordArray;
                    for (const auto& terminalItem : terminal) {
                        auto keyIndex = terminalItem.keyIndex;
                        auto transitionId = terminalItem.transitionId;
                        auto keyItem = trie.keys[keyIndex];

                        JSON::Object word;
                        std::string wordStr;
                        NKiltMan::RunesToString(keyItem.runes, wordStr);
                        word.set("initial", wordStr);
                        word.set("meta", keyItem.metadata);
                        if (transitionId != NKiltMan::FlatNode::kNoTransitionId) {
                            auto transition = trie.transitions[transitionId];
                            word.set("transition", transition);
                        } else {
                            word.set("transition", "");
                        }
                        wordArray.add(word);
                    }

                    NKiltMan::TRunes recognizedRunes(runeIter, next);
                    // TODO can this be replaced with flushFragment() call?
                    std::string text;
                    NKiltMan::RunesToString(recognizedRunes, text);

                    JSON::Object partObject;
                    partObject.set("text", text);
                    partObject.set("forms", wordArray);
                    textArray.add(partObject);
                    prevIsAlpha = true;
                    runeIter = next;
                    continue;
                }
            }
        }
        prevIsAlpha = NKiltMan::IsAlpha(*runeIter);
        fragmentRunes.push_back(*runeIter);
        ++runeIter;
    }
    flushFragment();

    JSON::Object response;
    response.set("parts", textArray);
    response.set("time", clock.elapsed() / 1e6);
    return response;
}

/* Response for /analyze_sub:
 *
 * {
 *     "time": <DOUBLE>,
 *     "parts": [
 *         {
 *             "text": <STRING>,
 *             "startTime": <TIMESTAMP>,
 *             "endTime": <TIMESTAMP>,
 *             "forms": [
 *                 {
 *                     "initial": <STRING>,
 *                     "meta": <OBJECT>,
 *                     "transition": <STRING
 *                 },
 *                 {..}
 *             ]
 *         },
 *         {..}
 *     ]
 * }
 *
 */

using TTimeRange = std::pair<uint32_t, uint32_t>;
static constexpr TTimeRange kEmptyTimeRange{kNoTimestamp, kNoTimestamp};

bool TimeRangeIsEmpty(const TTimeRange& range) {
    return range.first == kNoTimestamp || range.second == kNoTimestamp;
}

void FlushRunesAsFragmentToJsonArray(const NKiltMan::TRunes& runes, const JSON::Array& forms, const TTimeRange& range, JSON::Array& array) {
    if (runes.empty()) {
        return;
    }

    std::string text;
    NKiltMan::RunesToString(runes, text);

    JSON::Object partObject;

    partObject.set("text", text);
    partObject.set("forms", forms);
    if (!TimeRangeIsEmpty(range)) {
        partObject.set("startTime", range.first);
        partObject.set("endTime", range.second);
    }

    array.add(partObject);
}

uint32_t ExtractTimestamp(JSON::Object::Ptr object, const std::string& key) {
    if (!object) {
        return std::numeric_limits<uint32_t>::max();
    }

    try {
        if (!object->has(key)) {
            return kNoTimestamp;
        }
        Poco::Dynamic::Var value = object->get(key);

        if (!value.isNumeric()) {
            return kNoTimestamp;
        }

        uint32_t timestamp = value.convert<uint32_t>();
        if (timestamp > 1'000'000'000) {
            return kNoTimestamp;
        }
        return timestamp;
    }
    catch (const Poco::Exception& /* e */) {
        // Catch any POCO conversion or other exceptions
        return kNoTimestamp;
    }
    catch (const std::exception& /* e */) {
        // Catch any standard library exceptions
        return kNoTimestamp;
    }
}

std::pair<uint32_t, uint32_t> GetTimeRange(const JSON::Array::Ptr words, const std::vector<size_t>& runeWordIndex, size_t runeStart, size_t runeEnd) {
    uint32_t rangeStart = kNoTimestamp;
    uint32_t rangeEnd = kNoTimestamp;
    for (size_t runePos = runeStart; runePos < runeEnd; ) {
        const size_t curWordIndex = runeWordIndex[runePos];
        bool wordStartCovered = (
            (curWordIndex != kNoWordIndex) &&
            (
                (runePos == 0) ||
                (runeWordIndex[runePos - 1] != curWordIndex)
            )
        );
        size_t wordEnd = runePos + 1;
        while (wordEnd < runeEnd && runeWordIndex[wordEnd] == curWordIndex) {
            ++wordEnd;
        }
        const bool wordEndCovered = (
            (wordEnd >= runeWordIndex.size()) ||
            (runeWordIndex[wordEnd] != curWordIndex)
        );
        if (wordStartCovered && wordEndCovered) {
            JSON::Object::Ptr wordObject = words->getObject(curWordIndex);
            auto startTime = ExtractTimestamp(wordObject, "startTime");
            auto endTime = ExtractTimestamp(wordObject, "endTime");
            if (startTime != kNoTimestamp && endTime != kNoTimestamp) {
                if (rangeStart == kNoTimestamp || rangeStart > startTime) {
                    rangeStart = startTime;
                }
                if (rangeEnd == kNoTimestamp || rangeEnd < endTime) {
                    rangeEnd = endTime;
                }
            }
        }
        runePos = wordEnd;
    }
    return {rangeStart, rangeEnd};
}

std::optional<JSON::Object> buildAnalyzeSubResponse(const JSON::Array::Ptr words, const NKiltMan::FlatNodeTrie& trie) {
    Clock clock{};

    /* Step 1. Convert all words to runes and collect in one place. Also, keep rune associations with the original words. */

    std::vector<size_t> runeWordIndex;
    NKiltMan::TRunes runes;
    for (size_t i = 0; i < words->size(); ++i) {
        JSON::Object::Ptr wordObject = words->getObject(i);
        std::string word = wordObject->getValue<std::string>("word");
        if (!runes.empty() && runes.back() != ' ') {
            runes.push_back(' ');
        }
        size_t startPos = runes.size();
        auto conversionResult = NKiltMan::StringToRunesNoExceptAppend(word, runes);
        if (conversionResult != NKiltMan::ConversionResult::SUCCESS) {
            return {};
        }
        size_t endPos = runes.size();
        while (runeWordIndex.size() < startPos) {
            runeWordIndex.push_back(kNoWordIndex);
        }
        for (size_t pos = startPos; pos < endPos; ++pos) {
            runeWordIndex.push_back(i);
        }
    }

    /* Step 2. Iterate over runes and look for matches with paths within the trie. */

    JSON::Array parts;
    size_t consumedPos = 0;
    NKiltMan::TRunes noMatchRunes;
    const JSON::Array kNoForms;
    bool prevIsAlpha = false;
    for (auto runeIter = runes.begin(); runeIter != runes.end(); ) {
        if (!prevIsAlpha) {
            NKiltMan::TRunes::iterator next;
            auto node = trie.Traverse(runeIter, runes.end(), next);
            if (node && (next == runes.end() || !NKiltMan::IsAlpha(*next))) {
                const auto terminalId = node->terminalId;
                if (terminalId != NKiltMan::FlatNode::kNoTerminalId) {
                    size_t runePos = static_cast<size_t>(runeIter - runes.begin());
                    auto noMatchRange = GetTimeRange(words, runeWordIndex, consumedPos, runePos);
                    FlushRunesAsFragmentToJsonArray(noMatchRunes, kNoForms, noMatchRange, parts);
                    consumedPos = runePos;
                    noMatchRunes.clear();

                    auto terminal = trie.terminals[terminalId];
                    JSON::Array wordArray;
                    for (const auto& terminalItem : terminal) {
                        auto keyIndex = terminalItem.keyIndex;
                        auto transitionId = terminalItem.transitionId;
                        auto keyItem = trie.keys[keyIndex];

                        JSON::Object word;
                        std::string wordStr;
                        NKiltMan::RunesToString(keyItem.runes, wordStr);
                        word.set("initial", wordStr);
                        word.set("meta", keyItem.metadata);
                        if (transitionId != NKiltMan::FlatNode::kNoTransitionId) {
                            auto transition = trie.transitions[transitionId];
                            word.set("transition", transition);
                        } else {
                            word.set("transition", "");
                        }
                        wordArray.add(word);
                    }

                    NKiltMan::TRunes recognizedRunes(runeIter, next);
                    size_t matchEndRunePos = static_cast<size_t>(next - runes.begin());
                    auto matchRange = GetTimeRange(words, runeWordIndex, consumedPos, matchEndRunePos);
                    FlushRunesAsFragmentToJsonArray(recognizedRunes, wordArray, matchRange, parts);
                    prevIsAlpha = true;
                    runeIter = next;
                    consumedPos = matchEndRunePos;
                    continue;
                }
            }
        }
        prevIsAlpha = NKiltMan::IsAlpha(*runeIter);
        noMatchRunes.push_back(*runeIter);
        ++runeIter;
    }
    auto noMatchRange = GetTimeRange(words, runeWordIndex, consumedPos, runes.size());
    FlushRunesAsFragmentToJsonArray(noMatchRunes, kNoForms, noMatchRange, parts);

    JSON::Object response;
    response.set("parts", parts);
    response.set("time", clock.elapsed() / 1e6);
    return response;
}

JSON::Object buildDetectResponse(const std::string& queryText, bool suggest, const NKiltMan::FlatNodeTrie& trie) {
    Clock clock{};

    JSON::Array array;
    JSON::Array suggestionsArray;

    NKiltMan::TRunes runes;
    NKiltMan::StringToRunes(queryText, runes);
    auto node = trie.Traverse(runes);
    if (node != nullptr) {
        const auto terminalId = node->terminalId;
        if (terminalId != NKiltMan::FlatNode::kNoTerminalId) {
            auto terminal = trie.terminals[terminalId];
            for (const auto& terminalItem : terminal) {
                auto keyIndex = terminalItem.keyIndex;
                auto transitionId = terminalItem.transitionId;
                auto keyItem = trie.keys[keyIndex];

                JSON::Object word;
                std::string wordStr;
                NKiltMan::RunesToString(keyItem.runes, wordStr);
                word.set("initial", wordStr);
                word.set("meta", keyItem.metadata);
                if (transitionId != NKiltMan::FlatNode::kNoTransitionId) {
                    auto transition = trie.transitions[transitionId];
                    word.set("transition", transition);
                }

                array.add(word);
            }
        }
        if (suggest) {
            auto suggestions = trie.GetSuggestions(node);
            for (const auto& suggestion: suggestions) {
                JSON::Array suggestionArray;
                if (StartsWith(suggestion.completion, queryText)) {
                    JSON::Object hl;
                    hl.set("hl", true);
                    hl.set("text", queryText);
                    suggestionArray.add(hl);

                    JSON::Object regular;
                    regular.set("hl", false);
                    regular.set("text", suggestion.completion.substr(queryText.size()));
                    suggestionArray.add(regular);
                } else {
                    JSON::Object regular;
                    regular.set("hl", false);
                    regular.set("text", suggestion.completion);
                    suggestionArray.add(regular);
                }

                JSON::Object suggestionObject;
                suggestionObject.set("completion", suggestionArray);
                suggestionsArray.add(suggestionObject);
            }
        }
    }

    JSON::Object response;
    response.set("form", queryText);
    response.set("words", array);
    response.set("time", clock.elapsed() / 1e6);
    if (suggest) {
        response.set("suggestions", suggestionsArray);
    }
    return response;
}

}  // namespace

class FlatNodeTrieRequestHandler: public HTTPRequestHandler
{
public:
    FlatNodeTrieRequestHandler(const NKiltMan::FlatNodeTrie& trie) :
        trie_(trie)
    {}
private:
    /**
     * Response for /detect?q=танымасын :
     *
     *   {
     *      form: "танымасын",
     *      words: [
     *        { "initial": "тану", "exceptional": true, "transition": "1:imperativeMood:3:0", "meta": <...> },
     *        { "initial": <...> }
     *      ]
     *   }
     *
    */
    void handleRequest(HTTPServerRequest& request, HTTPServerResponse& response) override
    {
        Application& app = Application::instance();
        // app.logger().information("Request from %s", request.clientAddress().toString());

        auto uriString = request.getURI();
        auto uri = Poco::URI(uriString);
        // app.logger().information("path: %s", uri.getPath());

        if (uri.getPath() == kAnalyzePath) {
            auto params = uri.getQueryParameters();
            std::string queryText;
            for (const auto& [key, value]: params) {
                if (key == "q") {
                    queryText = value;
                }
            }
            if (queryText.empty() || queryText.size() > 4096) {
                response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_BAD_REQUEST);
                response.setContentType("text/plain");
                if (queryText.empty()) {
                    response.send() << "Query parameter q is required";
                } else {
                    response.send() << "Query parameter q is too long";
                }
                return;
            }
            auto jsonObject = buildAnalyzeResponse(queryText, trie_);
            if (!jsonObject) {
                response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain");
                response.send() << "Internal error";
                return;
            }
            response.setContentType("application/json");
            jsonObject->stringify(response.send());
            return;
        } else if (uri.getPath() == kAnalyzeSubPath && request.getMethod() == HTTPRequest::HTTP_POST) {
            std::istream& requestStream = request.stream();
            JSON::Parser parser;
            auto result = parser.parse(requestStream);
            auto requestRoot = result.extract<JSON::Object::Ptr>();
            if (requestRoot->isNull("words")) {
                response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_BAD_REQUEST);
                response.setContentType("text/plain");
                response.send() << "words not found in request body";
                return;
            }
            JSON::Array::Ptr words = requestRoot->getArray("words");
            auto jsonResponse = buildAnalyzeSubResponse(words, trie_);
            if (!jsonResponse) {
                response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain");
                response.send() << "Internal error";
                return;
            }
            response.setContentType("application/json");
            jsonResponse->stringify(response.send());
            return;
        } else if (uri.getPath() != kDetectPath) {
            response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_NOT_FOUND);
            response.setContentType("text/plain");
            response.send() << "Not found";
            return;
        } else {
            auto params = uri.getQueryParameters();
            std::string queryText;
            bool suggest = false;
            for (const auto& [key, value]: params) {
                if (key == "q") {
                    queryText = value;
                } else if (key == "suggest") {
                    if (value == "1") {
                        suggest = true;
                    }
                }
            }
            if (queryText.empty()) {
                response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_BAD_REQUEST);
                response.setContentType("text/plain");
                response.send() << "Query parameter q is required";
                return;
            }
            auto jsonObject = buildDetectResponse(queryText, suggest, trie_);
            response.setContentType("application/json");
            jsonObject.stringify(response.send());
            return;
        }
    }
private:
    const NKiltMan::FlatNodeTrie& trie_;
};

class FlatNodeTrieRequestHandlerFactory: public HTTPRequestHandlerFactory
{
public:
    FlatNodeTrieRequestHandlerFactory(NKiltMan::FlatNodeTrie&& trie) :
        trie_(std::move(trie))
    {}

private:
    HTTPRequestHandler* createRequestHandler(const HTTPServerRequest&) override
    {
        return new FlatNodeTrieRequestHandler(trie_);
    }
private:
    NKiltMan::FlatNodeTrie trie_;
};

std::string DoDetect(const NKiltMan::FlatNodeTrie& trie, Poco::Logger& logger, const std::string& inputLine) {
    NKiltMan::TRunes runes;
    // TODO lowercase input
    auto conversionResult = NKiltMan::StringToRunesNoExcept(inputLine, runes);
    if (conversionResult != NKiltMan::ConversionResult::SUCCESS) {
        logger.information("Failed to convert input line: code %d, line %s", int(conversionResult), inputLine);
        return {};
    }
    auto inputRunesCount = runes.size();

    auto node = trie.Traverse(runes);
    if (node == nullptr) {
        return {};
    }

    const auto terminalId = node->terminalId;
    if (terminalId == NKiltMan::FlatNode::kNoTerminalId) {
        return {};
    }

    std::string result;
    std::size_t resultSize = 0;
    auto terminal = trie.terminals[terminalId];
    for (const auto& terminalItem : terminal) {
        auto keyIndex = terminalItem.keyIndex;
        auto transitionId = terminalItem.transitionId;
        auto keyItem = trie.keys[keyIndex];

        auto runesCount = keyItem.runes.size();
        if (runesCount == inputRunesCount) {
            NKiltMan::RunesToString(keyItem.runes, result);
            resultSize = runesCount;
            break;
        } else if (runesCount > resultSize) {
            NKiltMan::RunesToString(keyItem.runes, result);
            resultSize = runesCount;
        }
    }
    return result;
}

void BatchDetect(const NKiltMan::FlatNodeTrie& trie, Poco::Logger& logger) {
    std::string inputLine;
    std::size_t inputCounter = 0;
    std::size_t outputCounter = 0;
    while (std::getline(std::cin, inputLine)) {
        auto detected = DoDetect(trie, logger, inputLine);
        std::cout << inputLine << '\t' << detected << '\n';
        ++inputCounter;
        if (detected.size()) {
            ++outputCounter;
        }
    }
    logger.information("Detected %z forms out of %z", outputCounter, inputCounter);
}

class WebServerApp: public ServerApplication
{
    void initialize(Application& self)
    {
        loadConfiguration();
        ServerApplication::initialize(self);
    }

    int main(const std::vector<std::string>& args)
    {
        if (args.size() == 2 && args[0] == "convert") { // kiltman convert trie.txt
            auto trieBuilder = NKiltMan::BuildTrie(&logger());
            trieBuilder.PrintTrie(args[1]);
            logger().information("Trie printed to %s", args[1]);
            return Application::EXIT_OK;
        } else if (args.size() > 0 && args[0] == "prepare_detect_suggest") { // kiltman prepare_detect_suggest detect_suggest_forms.jsonl detect_suggest_trie.txt
            if (args.size() != 3) {
                logger().error("Invalid arguments: expected input and output file paths");
                return Application::EXIT_CONFIG;
            }
            logger().information("Loading detect+suggest data from %s", args[1]);
            auto trieBuilder = NKiltMan::BuildDetectSuggestTrie(args[1], &logger());
            logger().information("Dumping trie data to %s", args[2]);
            trieBuilder.PrintTrie(args[2]);
            logger().information("Trie is dumped");
            return Application::EXIT_OK;
        } else if (2 <= args.size() && args.size() <= 3 && args[0] == "load") { // kiltman load trie.txt [port]
            logger().information("Loading trie from %s", args[1]);
            Clock clock{};
            auto trie = NKiltMan::LoadTrie(args[1], &logger());
            logger().information("Loading time: %.3f seconds", clock.elapsed() / 1e6);
            logger().information(
                "Loaded trie with %z runes, %z transitions, %z keys, %z values, %z children, %z nodes",
                trie.runes.size(), trie.transitions.size(), trie.keys.size(), trie.values.size(), trie.childData.size(), trie.nodes.size()
            );
            auto runesSpace = trie.GetRunesSpace();
            auto transitionsSpace = trie.GetTransitionsSpace();
            auto terminalsSpace = trie.GetTerminalsSpace();
            auto keysSpace = trie.GetKeysSpace();
            auto valuesSpace = trie.GetValuesSpace();
            auto childDataSpace = trie.GetChildDataSpace();
            auto nodesSpace = trie.GetNodesSpace();
            auto suggestionsSpace = trie.GetSuggestionsSpace();
            auto totalSpace = (
                runesSpace +
                transitionsSpace +
                terminalsSpace +
                keysSpace +
                valuesSpace +
                childDataSpace +
                nodesSpace +
                suggestionsSpace
            );
            logger().information("RAM usage estimation: %z bytes total", totalSpace);
            logger().information("  runes:       %z\t%z bytes", trie.runes.size(), runesSpace);
            logger().information("  transitions: %z\t%z bytes", trie.transitions.size(), transitionsSpace);
            logger().information("  terminals:   %z\t%z bytes", trie.terminals.size(), terminalsSpace);
            logger().information("  keys:        %z\t%z bytes", trie.keys.size(), keysSpace);
            logger().information("  values:      %z\t%z bytes", trie.values.size(), valuesSpace);
            logger().information("  childData:   %z\t%z bytes", trie.childData.size(), childDataSpace);
            logger().information("  nodes:       %z\t%z bytes", trie.nodes.size(), nodesSpace);
            logger().information("  suggestions: %z\t%z bytes", trie.suggestions.size(), suggestionsSpace);
            logger().information("  sizeof(FlatNode): %z", sizeof(NKiltMan::FlatNode));

            UInt16 port = 8080;
            if (args.size() > 2) {
                int intPort = std::stoi(args[2]);
                if (intPort < 0 || intPort > 65535) {
                    logger().error("Invalid port: %s", args[2]);
                    return Application::EXIT_CONFIG;
                }
                port = static_cast<UInt16>(intPort);
                logger().information("Using port %hu", port);
            }

            HTTPServer srv(new FlatNodeTrieRequestHandlerFactory(std::move(trie)), port);
            srv.start();
            logger().information("HTTP Server started on port %hu.", port);
            waitForTerminationRequest();
            logger().information("Stopping HTTP Server...");
            srv.stop();
            return Application::EXIT_OK;
        } else if (args.size() == 2 && args[0] == "batch_detect") { // kiltman batch_detect trie.txt
            // stdin accepts word forms, stdout prints detected base forms

            logger().information("Loading trie from %s", args[1]);
            Clock clock{};
            auto trie = NKiltMan::LoadTrie(args[1], &logger());
            logger().information("Loading time: %.3f seconds", clock.elapsed() / 1e6);

            BatchDetect(trie, logger());
            return Application::EXIT_OK;
        } else {
            logger().error("Invalid arguments");
            return Application::EXIT_CONFIG;
        }
    }
};

POCO_SERVER_MAIN(WebServerApp)