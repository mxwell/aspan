#pragma once

#include "flat_node.h"
#include "runes.h"

#include <cstdint>
#include <map>
#include <string>
#include <vector>

namespace NKiltMan {

struct FlatNodeTrie {
    using TRuneId = FlatNode::TRuneId;
    using TRuneMap = std::map<TRuneValue, TRuneId>;

    TRunes runes;
    TRuneMap runeMap;

    std::vector<TRunes> keys;
    std::vector<FlatNode> nodes;

    const FlatNode* Traverse(const TRunes& path) const;

    size_t GetRunesSpace() const {
        return sizeof(runes) + sizeof(runes[0]) * runes.capacity();
    }

    size_t GetKeysSpace() const {
        size_t result = sizeof(keys) + sizeof(keys[0]) * keys.capacity();
        for (const auto& key : keys) {
            result += sizeof(key[0]) * key.capacity();
        }
        return result;
    }

    size_t GetNodesSpace() const {
        size_t result = sizeof(nodes) + sizeof(nodes[0]) * nodes.capacity();
        for (const auto& node : nodes) {
            result += node.GetSpace();
        }
        return result;
    }
};

FlatNodeTrie LoadTrie(const std::string& path);

}  // namespace NKiltMan
