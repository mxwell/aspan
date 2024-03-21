#pragma once

#include "flat_node.h"
#include "runes.h"

#include <cstdint>
#include <map>
#include <string>
#include <vector>

namespace NKiltMan {

struct TKeyItem {
    TRunes runes;
    bool keyException;

    size_t GetSpace() const {
        return sizeof(runes[0]) * runes.capacity();
    }
};

struct FlatNodeTrie {
    using TRuneId = FlatNode::TRuneId;
    using TRuneMap = std::map<TRuneValue, TRuneId>;
    using TTransitions = std::vector<std::string>;
    using TKeys = std::vector<TKeyItem>;

    TRunes runes;
    TRuneMap runeMap;

    TTransitions transitions;

    TKeys keys;
    std::vector<FlatNode::TRuneNodeCombo> childData;
    std::vector<FlatNode> nodes;

    const FlatNode* Traverse(const TRunes& path) const;

    size_t GetRunesSpace() const {
        return sizeof(runes) + sizeof(runes[0]) * runes.capacity();
    }

    size_t GetTransitionsSpace() const {
        size_t result = sizeof(transitions) + sizeof(transitions[0]) * transitions.capacity();
        for (const auto& transition : transitions) {
            result += transition.size();
        }
        return result;
    }

    size_t GetKeysSpace() const {
        size_t result = sizeof(keys) + sizeof(keys[0]) * keys.capacity();
        for (const auto& key : keys) {
            result += key.GetSpace();
        }
        return result;
    }

    size_t GetChildDataSpace() const {
        return sizeof(childData) + sizeof(childData[0]) * childData.capacity();
    }

    size_t GetNodesSpace() const {
        return sizeof(nodes) + sizeof(nodes[0]) * nodes.capacity();
    }
};

FlatNodeTrie LoadTrie(const std::string& path);

}  // namespace NKiltMan
