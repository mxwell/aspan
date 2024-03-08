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

}

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

class WebServerApp: public ServerApplication
{
    void initialize(Application& self)
    {
        loadConfiguration();
        ServerApplication::initialize(self);
    }

    int main(const std::vector<std::string>&)
    {
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