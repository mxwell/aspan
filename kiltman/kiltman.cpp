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

static constexpr std::string_view kDetectPath = "/detect";

const NKiltMan::TrieBuilder& GetTrieBuilder(Logger* logger) {
    static NKiltMan::TrieBuilder instance = NKiltMan::BuildTrie(logger);
    return instance;
}

JSON::Object buildDetectResponse(const std::string& queryText) {
    JSON::Array array;
    const auto& trieBuilder = GetTrieBuilder(nullptr);
    auto node = trieBuilder.Traverse(queryText);
    if (node != nullptr && node->IsTerminal()) {
        JSON::Object word;
        word.set("initial", trieBuilder.GetKey(node->keyIndex));
        array.add(word);
    }

    JSON::Object response;
    response.set("form", queryText);
    response.set("words", array);
    return response;
}

JSON::Object buildDetectResponse(const std::string& queryText, const NKiltMan::FlatNodeTrie& trie) {
    JSON::Array array;
    NKiltMan::TRunes runes;
    NKiltMan::StringToRunes(queryText, runes);
    auto node = trie.Traverse(runes);
    if (node != nullptr && node->IsTerminal()) {
        JSON::Object word;
        std::string wordStr;
        NKiltMan::RunesToString(trie.keys[node->keyIndex], wordStr);
        word.set("initial", wordStr);
        array.add(word);
    }

    JSON::Object response;
    response.set("form", queryText);
    response.set("words", array);
    return response;
}

}  // namespace

class KiltmanRequestHandler: public HTTPRequestHandler
{
    /**
     * Response for /detect?q=аламын :
     *
     *   {
     *      form: "аламын",
     *      words: [
     *        { "initial": "алу", "meta": <...> },
     *        { "initial": <...> }
     *      ]
     *   }
     *
    */
    void handleRequest(HTTPServerRequest& request, HTTPServerResponse& response)
    {
        Application& app = Application::instance();
        app.logger().information("Request from %s", request.clientAddress().toString());

        auto uriString = request.getURI();
        auto uri = Poco::URI(uriString);
        app.logger().information("path: %s", uri.getPath());

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
            auto jsonObject = buildDetectResponse(queryText);
            response.setContentType("application/json");
            jsonObject.stringify(response.send());
            return;
        }
    }
};

class KiltmanRequestHandlerFactory: public HTTPRequestHandlerFactory
{
    HTTPRequestHandler* createRequestHandler(const HTTPServerRequest&)
    {
        return new KiltmanRequestHandler;
    }

};

class FlatNodeTrieRequestHandler: public HTTPRequestHandler
{
public:
    FlatNodeTrieRequestHandler(const NKiltMan::FlatNodeTrie& trie) :
        trie_(trie)
    {}
private:
    /**
     * Response for /detect?q=аламын :
     *
     *   {
     *      form: "аламын",
     *      words: [
     *        { "initial": "алу", "meta": <...> },
     *        { "initial": <...> }
     *      ]
     *   }
     *
    */
    void handleRequest(HTTPServerRequest& request, HTTPServerResponse& response) override
    {
        Application& app = Application::instance();
        app.logger().information("Request from %s", request.clientAddress().toString());

        auto uriString = request.getURI();
        auto uri = Poco::URI(uriString);
        app.logger().information("path: %s", uri.getPath());

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
        if (args.size() > 0) {
            // kiltman convert trie.txt
            if (args.size() == 2 && args[0] == "convert") {
                auto& trieBuilder = GetTrieBuilder(&logger());
                trieBuilder.PrintTrie(args[1]);
                logger().information("Trie printed to %s", args[1]);
                return Application::EXIT_OK;
            } else if (args.size() == 2 && args[0] == "load") {
                logger().information("Loading trie from %s", args[1]);
                auto trie = NKiltMan::LoadTrie(args[1]);
                logger().information(
                    "Loaded trie with %z runes, %z keys, %z nodes",
                    trie.runes.size(), trie.keys.size(), trie.nodes.size()
                );
                auto runesSpace = trie.GetRunesSpace();
                auto keysSpace = trie.GetKeysSpace();
                auto nodesSpace = trie.GetNodesSpace();
                logger().information("Runes space: %z, keys space: %z, nodes space: %z, total %z",
                    runesSpace, keysSpace, nodesSpace, runesSpace + keysSpace + nodesSpace
                );

                UInt16 port = static_cast<UInt16>(config().getUInt("port", 8080));

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

        (void) GetTrieBuilder(&logger());

        UInt16 port = static_cast<UInt16>(config().getUInt("port", 8080));

        HTTPServer srv(new KiltmanRequestHandlerFactory, port);
        srv.start();
        logger().information("HTTP Server started on port %hu.", port);
        waitForTerminationRequest();
        logger().information("Stopping HTTP Server...");
        srv.stop();

        return Application::EXIT_OK;
    }
};

POCO_SERVER_MAIN(WebServerApp)