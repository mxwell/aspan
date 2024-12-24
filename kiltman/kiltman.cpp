#include "flat_node_trie.h"
#include "runes.h"
#include "trie.h"

#include "Poco/Clock.h"
#include "Poco/JSON/Object.h"
#include "Poco/Net/HTTPServer.h"
#include "Poco/Net/HTTPRequestHandler.h"
#include "Poco/Net/HTTPRequestHandlerFactory.h"
#include "Poco/Net/HTTPServerRequest.h"
#include "Poco/Net/HTTPServerResponse.h"
#include "Poco/Net/ServerSocket.h"
#include "Poco/Util/ServerApplication.h"
#include "Poco/URI.h"

#include <optional>
#include <string>

using namespace Poco;
using namespace Poco::Net;
using namespace Poco::Util;

namespace {

static const std::string kAnalyzePath = "/analyze";
static const std::string kDetectPath = "/detect";

bool StartsWith(const std::string& str, const std::string& prefix) {
    return str.compare(0, prefix.size(), prefix) == 0;
}

std::optional<JSON::Object> buildAnalyzeResponse(const std::string& queryText, const NKiltMan::FlatNodeTrie& trie) {
    Clock clock{};

    JSON::Array textArray;

    NKiltMan::TRunes runes;
    auto conversionResult = NKiltMan::StringToRunesNoExcept(queryText, runes);
    if (conversionResult != NKiltMan::ConversionResult::SUCCESS) {
        return {};
    }

    NKiltMan::TRunes fragmentRunes;

    auto flushFragment = [&textArray, &fragmentRunes]() {
        if (!fragmentRunes.empty()) {
            std::string fragmentStr;
            NKiltMan::RunesToString(fragmentRunes, fragmentStr);

            JSON::Object partObject;
            partObject.set("found", false);
            partObject.set("fragment", fragmentStr);
            textArray.add(partObject);

            fragmentRunes.clear();
        }
    };

    for (auto runeIter = runes.begin(); runeIter != runes.end(); ) {
        NKiltMan::TRunes::iterator next;
        auto node = trie.Traverse(runeIter, runes.end(), next);
        if (node) {
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
                    }
                    wordArray.add(word);
                }
                JSON::Object partObject;
                partObject.set("found", true);
                partObject.set("terminals", wordArray);
                textArray.add(partObject);
                runeIter = next;
                continue;
            }
        }
        fragmentRunes.push_back(*runeIter);
        ++runeIter;
    }
    flushFragment();

    JSON::Object response;
    response.set("parts", textArray);
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