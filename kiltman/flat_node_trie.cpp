#include "flat_node_trie.h"

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
        FlatNode::TNodeId childId = node->FindChild(runeId);
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

std::vector<TRunes> LoadKeys(std::istream& input, const TRunes& runes) {
    size_t keysCount;
    input >> keysCount;
    std::vector<TRunes> keys;
    keys.reserve(keysCount);

    size_t runesCount;
    for (size_t i = 0; i < keysCount; ++i) {
        input >> runesCount;
        keys.emplace_back(runesCount);
        auto& vec = keys.back();
        size_t runeId;
        for (size_t j = 0; j < runesCount; ++j) {
            input >> runeId;
            vec[j] = runes[runeId];
        }
    }
    return keys;
}

std::vector<FlatNode> LoadNodes(std::istream& input) {
    size_t nodesCount;
    input >> nodesCount;
    std::vector<FlatNode> nodes;
    nodes.reserve(nodesCount);
    FlatNode::TKey keyIndex;
    size_t childrenCount;
    constexpr uint32_t kMaxRuneId = 0xFF;
    constexpr uint32_t kMaxNodeId = 0x00FFFFFF;
    for (size_t i = 0; i < nodesCount; ++i) {
        input >> keyIndex >> childrenCount;
        assert(childrenCount <= 45);
        FlatNode::TChildren children(childrenCount);
        size_t runeId;
        size_t childId;
        for (size_t j = 0; j < childrenCount; ++j) {
            input >> runeId >> childId;
            assert(runeId <= kMaxRuneId);
            assert(childId <= kMaxNodeId);
            children[j] = MAKE_COMBO(runeId, childId);
        }
        nodes.emplace_back(
            FlatNode{
                .keyIndex = keyIndex,
                .children = std::move(children)
            }
        );
    }
    return nodes;
}

FlatNodeTrie LoadTrie(const std::string& path) {
    std::ifstream input(path);
    auto runes = LoadRunes(input);

    FlatNodeTrie::TRuneMap runeMap;
    for (size_t i = 0; i < runes.size(); ++i) {
        runeMap[runes[i]] = i;
    }

    auto keys = LoadKeys(input, runes);

    auto nodes = LoadNodes(input);

    return FlatNodeTrie{
        .runes = std::move(runes),
        .runeMap = std::move(runeMap),
        .keys = std::move(keys),
        .nodes = std::move(nodes)
    };
}

}  // namespace NKiltMan