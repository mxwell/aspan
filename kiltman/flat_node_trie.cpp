#include "flat_node_trie.h"

#include "Poco/JSON/Parser.h"
#include "Poco/Logger.h"

#include <cassert>
#include <fstream>

namespace NKiltMan {

const FlatNode* FlatNodeTrie::Traverse(TRunes::iterator begin, TRunes::iterator end, TRunes::iterator& next) const {
    auto root = &nodes[0];
    const FlatNode* node = root;
    std::vector<std::pair<const FlatNode*, TRunes::iterator>> results;
    for (next = begin; next != end; ++next) {
        TRuneValue runeValue = *next;

        auto iter = runeMap.find(runeValue);
        if (iter == runeMap.end()) {
            break;
        }
        TRuneId runeId = iter->second;
        FlatNode::TNodeId childId = node->FindChild(runeId, childData.data());
        if (childId == FlatNode::kNoChild) {
            break;
        }
        node = &nodes[childId];
        auto after = next;
        ++after;
        if (node->terminalId != FlatNode::kNoTerminalId && (after == end || !IsAlpha(*after))) {
            results.emplace_back(node, after);
        }
    }
    for (auto resIter = results.rbegin(); resIter != results.rend(); ++resIter) {
        if (resIter->first != root) {
            next = resIter->second;
            return resIter->first;
        }
    }
    return nullptr;
}

const FlatNode* FlatNodeTrie::Traverse(const TRunes& path) const {
    const FlatNode* node = &nodes[0];
    for (TRuneValue runeValue : path) {
        auto iter = runeMap.find(runeValue);
        if (iter == runeMap.end()) {
            return nullptr;
        }
        TRuneId runeId = iter->second;
        FlatNode::TNodeId childId = node->FindChild(runeId, childData.data());
        if (childId == FlatNode::kNoChild) {
            return nullptr;
        }
        node = &nodes[childId];
    }
    return node;
}

TSuggestionResults FlatNodeTrie::GetSuggestions(const FlatNode* node) const {
    FlatNode::TSuggestionsOffset suggestionsStart = COMBO_SUGGESTIONS_START(node->suggestionsPtr);
    FlatNode::TSuggestionsCount suggestionsCount = COMBO_SUGGESTIONS_COUNT(node->suggestionsPtr);

    TSuggestionResults result;
    result.reserve(suggestionsCount);

    FlatNode::TSuggestionsOffset suggestionsEnd = suggestionsStart + suggestionsCount;
    for (FlatNode::TSuggestionsOffset i = suggestionsStart; i < suggestionsEnd; ++i) {
        auto valueId = suggestions[i];
        const TValueItem& valueItem = values[valueId];
        std::string completion;
        RunesToString(valueItem.runes, completion);
        result.emplace_back(
            TSuggestionResult{
                .completion = std::move(completion)
            }
        );
    }
    return std::move(result);
}

TRunes LoadRunes(std::istream& input) {
    size_t runesCount;
    input >> runesCount;
    TRunes runes(runesCount);
    for (size_t i = 0; i < runesCount; ++i) {
        input >> runes[i];
    }
    return runes;
}

FlatNodeTrie::TTransitions LoadTransitions(std::istream& input) {
    size_t transitionsCount;
    input >> transitionsCount;
    // consume end of line from input
    input.ignore(1);
    FlatNodeTrie::TTransitions transitions(transitionsCount);
    for (size_t i = 0; i < transitionsCount; ++i) {
        std::getline(input, transitions[i]);
        assert(transitions[i].size() > 0);
    }
    return transitions;
}

FlatNodeTrie::TTerminals LoadTerminals(std::istream& input) {
    size_t terminalsCount;
    input >> terminalsCount;
    FlatNodeTrie::TTerminals terminals;
    terminals.reserve(terminalsCount);
    size_t entriesCount;
    size_t keyIndex;
    size_t transitionId;
    for (size_t i = 0; i < terminalsCount; ++i) {
        input >> entriesCount;
        FlatNodeTrie::TTerminal terminal(entriesCount);
        for (size_t j = 0; j < entriesCount; ++j) {
            input >> keyIndex >> transitionId;
            assert(keyIndex < FlatNode::kNoKey);
            assert(transitionId < FlatNode::kNoTransitionId);
            terminal[j] = TTerminalItem{
                .keyIndex = static_cast<FlatNode::TKey>(keyIndex),
                .transitionId = static_cast<FlatNode::TTransitionId>(transitionId),
            };
        }
        terminals.emplace_back(std::move(terminal));
    }
    return terminals;
}

FlatNodeTrie::TKeys LoadKeys(std::istream& input, const TRunes& runes) {
    using namespace Poco;

    size_t keysCount;
    input >> keysCount;
    FlatNodeTrie::TKeys keys;
    keys.reserve(keysCount);

    std::string metadata;
    JSON::Parser parser;

    size_t runesCount;
    getline(input, metadata);
    for (size_t i = 0; i < keysCount; ++i) {
        getline(input, metadata);
        assert(metadata.size() > 0);
        Dynamic::Var parsedMetadata = parser.parse(metadata);
        JSON::Object::Ptr metadataRoot = parsedMetadata.extract<JSON::Object::Ptr>();

        input >> runesCount;
        keys.emplace_back(
            TKeyItem{
                .runes = TRunes(runesCount),
                .metadata = *metadataRoot,
            }
        );
        auto& vec = keys.back().runes;
        size_t runeId;
        for (size_t j = 0; j < runesCount; ++j) {
            input >> runeId;
            vec[j] = runes[runeId];
        }
        getline(input, metadata);
    }
    return keys;
}

FlatNodeTrie::TValues LoadValues(std::istream& input, const TRunes& runes) {
    size_t valuesCount;
    input >> valuesCount;
    FlatNodeTrie::TValues values;
    values.reserve(valuesCount);

    uint16_t keyIndex;
    size_t runesCount;
    for (size_t i = 0; i < valuesCount; ++i) {
        input >> keyIndex;
        input >> runesCount;
        values.emplace_back(
            TValueItem{
                .runes = TRunes(runesCount),
                .keyIndex = keyIndex,
            }
        );
        auto& vec = values.back().runes;
        size_t runeId;
        for (size_t j = 0; j < runesCount; ++j) {
            input >> runeId;
            vec[j] = runes[runeId];
        }
    }
    return values;
}

void LoadNodes(std::istream& input, std::vector<FlatNode>& nodes, std::vector<FlatNode::TRuneNodeCombo>& childData, std::vector<FlatNode::TValueId>& suggestions) {
    size_t nodesCount;
    input >> nodesCount;
    nodes.reserve(nodesCount);
    childData.reserve(nodesCount);
    suggestions.reserve(nodesCount * 3);

    uint32_t terminalId;
    size_t childrenCount;
    constexpr uint32_t kMaxRuneId = 0xFF;
    constexpr uint32_t kMaxNodeId = 0x00FFFFFF;
    constexpr uint32_t kMaxSuggestions = 0x01FFFFFF;
    for (size_t i = 0; i < nodesCount; ++i) {
        input >> terminalId >> childrenCount;
        assert(childrenCount <= 45);
        FlatNode::TChildrenStart childrenStart = childData.size();
        size_t runeId;
        size_t childId;
        for (size_t j = 0; j < childrenCount; ++j) {
            input >> runeId >> childId;
            assert(runeId <= kMaxRuneId);
            assert(childId <= kMaxNodeId);
            childData.emplace_back(MAKE_COMBO(runeId, childId));
        }
        size_t suggestionsCount;
        input >> suggestionsCount;
        assert(suggestionsCount > 0);
        assert(suggestionsCount <= 10);
        FlatNode::TSuggestionsOffset suggestionsStart = suggestions.size();
        assert(suggestionsStart <= kMaxSuggestions);
        FlatNode::TValueId value;
        for (size_t j = 0; j < suggestionsCount; ++j) {
            input >> value;
            suggestions.emplace_back(value);
        }
        nodes.emplace_back(
            FlatNode{
                .terminalId = terminalId,
                .childrenCount = static_cast<FlatNode::TChildrenSize>(childrenCount),
                .childrenStart = childrenStart,
                .suggestionsPtr = MAKE_SUGG_COMBO(uint32_t(suggestionsCount), suggestionsStart),
            }
        );
    }
    assert(nodes.size() == nodesCount);
    assert(childData.size() <= nodesCount);
    assert(suggestions.size() <= nodesCount * 3);
}

FlatNodeTrie LoadTrie(const std::string& path, Poco::Logger* logger) {
    std::ifstream input(path);
    if (logger) {
        logger->information("Loading runes");
    }
    auto runes = LoadRunes(input);
    if (logger) {
        logger->information("Loading transitions");
    }
    auto transitions = LoadTransitions(input);

    FlatNodeTrie::TRuneMap runeMap;
    for (size_t i = 0; i < runes.size(); ++i) {
        runeMap[runes[i]] = i;
    }

    if (logger) {
        logger->information("Loading terminals");
    }
    auto terminals = LoadTerminals(input);

    if (logger) {
        logger->information("Loading keys");
    }
    auto keys = LoadKeys(input, runes);

    if (logger) {
        logger->information("Loading values");
    }
    auto values = LoadValues(input, runes);

    std::vector<FlatNode> nodes;
    std::vector<FlatNode::TRuneNodeCombo> childData;
    std::vector<FlatNode::TValueId> suggestions;
    if (logger) {
        logger->information("Loading nodes");
    }
    LoadNodes(input, nodes, childData, suggestions);
    if (logger) {
        logger->information("Nodes %z, suggestions %z, ratio %.3f", nodes.size(), suggestions.size(), double(suggestions.size()) / nodes.size());
    }

    return FlatNodeTrie{
        .runes = std::move(runes),
        .runeMap = std::move(runeMap),
        .transitions = std::move(transitions),
        .terminals = std::move(terminals),
        .keys = std::move(keys),
        .values = std::move(values),
        .childData = std::move(childData),
        .nodes = std::move(nodes),
        .suggestions = std::move(suggestions),
    };
}

}  // namespace NKiltMan