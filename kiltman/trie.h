#pragma once

#include <algorithm>
#include <iostream>
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

    static const TKey kNoKey = std::numeric_limits<TKey>::max();

    TNode():
        keyIndex(kNoKey)
    {}

    TKey keyIndex;
    std::map<uint16_t, TNode*> children;

    TNode* FindChild(uint16_t ch) const {
        auto it = children.find(ch);
        if (it == children.end()) {
            return nullptr;
        }
        return it->second;
    }

    TNode* AddChild(uint16_t ch) {
        TNode* child = new TNode();
        children[ch] = child;
        return child;
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
        pathCount_(0),
        textLength_(0),
        nodeCount_(1)
    {}

    void AddPath(const TRunes& path, uint16_t keyIndex);
    uint16_t AddKeyRunes(const TRunes& runes);
    const TNode* Traverse(const TRunes& path) const;
    const TNode* Traverse(const std::string& path) const;
    std::string GetKey(uint16_t index) const;
    void PrintStats() const;
private:
    TNode root_;
    std::vector<TRunes> keyRunesVec_;
    uint32_t pathCount_;
    uint32_t textLength_;
    uint32_t nodeCount_;
};

TrieBuilder BuildTrie();

}  // namespace NKiltMan