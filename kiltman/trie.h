#pragma once

#include "Poco/Logger.h"

#include "runes.h"

#include <algorithm>
#include <fstream>
#include <limits>
#include <map>
#include <set>
#include <string>
#include <utility>
#include <vector>

namespace NKiltMan {

using TRuneId = uint8_t;
constexpr TRuneId kNoRuneId = std::numeric_limits<TRuneId>::max();

struct TNode {
    using TTransitionId = uint8_t;
    using TKey = uint16_t;
    using TNodeId = uint32_t;

    static constexpr TTransitionId kNoTransition = std::numeric_limits<TTransitionId>::max();
    static constexpr TKey kNoKey = std::numeric_limits<TKey>::max();
    static constexpr TNodeId kNoChild = std::numeric_limits<TNodeId>::max();

    TNode():
        transitionId(kNoTransition),
        keyIndex(kNoKey)
    {}

    TTransitionId transitionId;
    TKey keyIndex;
    std::map<TRuneId, TNodeId> children;

    TNodeId FindChild(TRuneId ch) const {
        auto it = children.find(ch);
        if (it == children.end()) {
            return kNoChild;
        }
        return it->second;
    }

    void AddChild(TRuneId ch, TNodeId childId) {
        children[ch] = childId;
    }

    void SetTransitionAndKey(TTransitionId transition, TKey index) {
        transitionId = transition;
        keyIndex = index;
    }

    bool IsTerminal() const {
        return keyIndex != kNoKey;
    }

    uint64_t GetSpace() const {
        return sizeof(TKey) + children.size() * (sizeof(TRuneId) + sizeof(TNodeId)) + 1;
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

    void AddPath(const TRunes& path, TNode::TTransitionId transitionId, TNode::TKey keyIndex);
    uint16_t GetTransitionId(const std::string& transition);
    uint16_t AddKeyRunes(uint8_t keyException, const std::string& key);
    const TNode* Traverse(const TRunes& path) const;
    const TNode* Traverse(const std::string& path) const;
    std::string GetKey(uint16_t index) const;
    void PrintStats(Poco::Logger& logger) const;
    void PrintTrie(const std::string& filename) const;
private:
    TRuneId GetRuneId(TRuneValue rune);
    TRuneId GetRuneIdConst(TRuneValue rune) const;
    TNode::TNodeId CreateNode();

    void PrintRunes(std::ofstream& out) const;
    void PrintTransitions(std::ofstream& out) const;
    void PrintKeys(std::ofstream& out) const;
    void PrintNodes(std::ofstream& out) const;
private:
    std::vector<TRuneValue> runeValues_;
    std::map<TRuneValue, TRuneId> runeIds_;
    std::vector<std::string> transitions_;
    std::map<std::string, uint16_t> transitionIds_;
    std::vector<uint8_t> keyExceptions_;
    std::vector<TRunes> keyRunesVec_;
    std::unordered_map<std::string, uint16_t> keyIndices_;
    std::vector<TNode*> nodes_;
    uint32_t pathCount_;
    uint32_t textLength_;
    uint32_t nodeCount_;
};

TrieBuilder BuildTrie(Poco::Logger* logger);

}  // namespace NKiltMan