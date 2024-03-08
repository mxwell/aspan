#pragma once

#include "Poco/Logger.h"

#include <algorithm>
#include <fstream>
#include <limits>
#include <map>
#include <set>
#include <string>
#include <utility>
#include <vector>

namespace NKiltMan {

using TRunes = std::vector<uint16_t>;

struct TNode {
    using TKey = uint16_t;
    using TNodeId = uint32_t;

    static constexpr TKey kNoKey = std::numeric_limits<TKey>::max();
    static constexpr TNodeId kNoChild = std::numeric_limits<TNodeId>::max();

    TNode():
        keyIndex(kNoKey)
    {}

    TKey keyIndex;
    std::map<TKey, TNodeId> children;

    TNodeId FindChild(TKey ch) const {
        auto it = children.find(ch);
        if (it == children.end()) {
            return kNoChild;
        }
        return it->second;
    }

    void AddChild(TKey ch, TNodeId childId) {
        children[ch] = childId;
    }

    void SetKeyIndex(TKey index) {
        keyIndex = index;
    }

    bool IsTerminal() const {
        return keyIndex != kNoKey;
    }

    uint64_t GetSpace() const {
        return sizeof(TKey) + children.size() * (sizeof(uint16_t) + sizeof(TNode*)) + 1;
    }
};

class TrieBuilder {
public:
    TrieBuilder() :
        nodes_(1, new TNode()),
        pathCount_(0),
        textLength_(0),
        nodeCount_(1)
    {}

    void AddPath(const TRunes& path, uint16_t keyIndex);
    uint16_t AddKeyRunes(const TRunes& runes);
    const TNode* Traverse(const TRunes& path) const;
    const TNode* Traverse(const std::string& path) const;
    std::string GetKey(uint16_t index) const;
    void PrintStats(Poco::Logger& logger) const;
private:
    TNode::TNodeId CreateNode();
private:
    std::vector<TRunes> keyRunesVec_;
    std::vector<TNode*> nodes_;
    uint32_t pathCount_;
    uint32_t textLength_;
    uint32_t nodeCount_;
};

TrieBuilder BuildTrie(Poco::Logger* logger);

}  // namespace NKiltMan