#include "flat_node_trie.h"
#include "runes.h"
#include "trie.h"

#include "Poco/JSON/Object.h"
#include "Poco/Net/HTTPServer.h"
#include "Poco/Net/HTTPRequestHandler.h"
#include "Poco/Net/HTTPRequestHandlerFactory.h"
#include "Poco/Net/HTTPServerRequest.h"
#include "Poco/Net/HTTPServerResponse.h"
#include "Poco/Net/ServerSocket.h"
#include "Poco/Util/ServerApplication.h"
#include "Poco/URI.h"

#include <string>

using namespace Poco;
using namespace Poco::Net;
using namespace Poco::Util;

namespace {

static const std::string kDetectPath = "/detect";

JSON::Object buildDetectResponse(const std::string& queryText, const NKiltMan::FlatNodeTrie& trie) {
    JSON::Array array;
    NKiltMan::TRunes runes;
    NKiltMan::StringToRunes(queryText, runes);
    auto node = trie.Traverse(runes);
    if (node != nullptr && node->IsTerminal()) {
        JSON::Object word;
        std::string wordStr;
        auto keyItem = trie.keys[node->keyIndex];
        NKiltMan::RunesToString(keyItem.runes, wordStr);
        word.set("initial", wordStr);
        if (keyItem.keyException) {
            word.set("exceptional", true);
        }
        if (node->transitionId != NKiltMan::FlatNode::kNoTransitionId) {
            auto transition = trie.transitions[node->transitionId];
            word.set("transition", transition);
        }
        array.add(word);
    }

    JSON::Object response;
    response.set("form", queryText);
    response.set("words", array);
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

        if (uri.getPath() != kDetectPath) {
            response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_NOT_FOUND);
            response.setContentType("text/plain");
            response.send() << "Not found";
            return;
        } else {
            auto params = uri.getQueryParameters();
            std::string queryText;
            for (const auto& [key, value]: params) {
                if (key == "q") {
                    queryText = value;
                    break;
                }
            }
            if (queryText.empty()) {
                response.setStatusAndReason(HTTPServerResponse::HTTPStatus::HTTP_BAD_REQUEST);
                response.setContentType("text/plain");
                response.send() << "Query parameter q is required";
                return;
            }
            auto jsonObject = buildDetectResponse(queryText, trie_);
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
            auto trie = NKiltMan::LoadTrie(args[1]);
            logger().information(
                "Loaded trie with %z runes, %z transitions, %z keys, %z children, %z nodes",
                trie.runes.size(), trie.transitions.size(), trie.keys.size(), trie.childData.size(), trie.nodes.size()
            );
            auto runesSpace = trie.GetRunesSpace();
            auto transitionsSpace = trie.GetTransitionsSpace();
            auto keysSpace = trie.GetKeysSpace();
            auto childDataSpace = trie.GetChildDataSpace();
            auto nodesSpace = trie.GetNodesSpace();
            auto totalSpace = runesSpace + transitionsSpace + keysSpace + childDataSpace + nodesSpace;
            logger().information("Runes space: %z, transitions space: %z, keys space: %z, childData space %z, nodes space: %z, total %z",
                runesSpace, transitionsSpace, keysSpace, nodesSpace, childDataSpace, totalSpace
            );
            logger().information("sizeof(FlatNode) = %z", sizeof(NKiltMan::FlatNode));

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
        } else {
            logger().error("Invalid arguments");
            return Application::EXIT_CONFIG;
        }
    }
};

POCO_SERVER_MAIN(WebServerApp)