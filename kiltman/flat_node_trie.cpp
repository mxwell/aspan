#include "flat_node_trie.h"

#include "Poco/JSON/Parser.h"

#include <cassert>
#include <fstream>

namespace NKiltMan {

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

void LoadNodes(std::istream& input, std::vector<FlatNode>& nodes, std::vector<FlatNode::TRuneNodeCombo>& childData) {
    size_t nodesCount;
    input >> nodesCount;
    nodes.reserve(nodesCount);
    childData.reserve(nodesCount);

    FlatNode::TKey keyIndex;
    uint32_t transitionId;
    size_t childrenCount;
    constexpr uint32_t kMaxRuneId = 0xFF;
    constexpr uint32_t kMaxNodeId = 0x00FFFFFF;
    for (size_t i = 0; i < nodesCount; ++i) {
        input >> transitionId >> keyIndex >> childrenCount;
        assert(transitionId < 256);
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
        nodes.emplace_back(
            FlatNode{
                .keyIndex = keyIndex,
                .transitionId = static_cast<FlatNode::TTransitionId>(transitionId),
                .childrenCount = static_cast<FlatNode::TChildrenSize>(childrenCount),
                .childrenStart = childrenStart,
            }
        );
    }
    assert(nodes.size() == nodesCount);
    assert(childData.size() <= nodesCount);
}

FlatNodeTrie LoadTrie(const std::string& path) {
    std::ifstream input(path);
    auto runes = LoadRunes(input);
    auto transitions = LoadTransitions(input);

    FlatNodeTrie::TRuneMap runeMap;
    for (size_t i = 0; i < runes.size(); ++i) {
        runeMap[runes[i]] = i;
    }

    auto keys = LoadKeys(input, runes);

    std::vector<FlatNode> nodes;
    std::vector<FlatNode::TRuneNodeCombo> childData;
    LoadNodes(input, nodes, childData);

    return FlatNodeTrie{
        .runes = std::move(runes),
        .runeMap = std::move(runeMap),
        .transitions = std::move(transitions),
        .keys = std::move(keys),
        .childData = std::move(childData),
        .nodes = std::move(nodes)
    };
}

}  // namespace NKiltMan