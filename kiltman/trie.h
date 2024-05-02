#pragma once

#include "Poco/JSON/Object.h"
#include "Poco/Logger.h"

#include "runes.h"

#include <algorithm>
#include <fstream>
#include <limits>
#include <map>
#include <set>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

namespace NKiltMan {

using TRuneId = uint8_t;
constexpr TRuneId kNoRuneId = std::numeric_limits<TRuneId>::max();
constexpr size_t kMaxSuggestions = 10;

struct TNode {
    using TTransitionId = uint8_t;
    using TKey = uint16_t;
    using TValue = uint32_t;
    using TNodeId = uint32_t;
    using TWeight = float;

    static constexpr TTransitionId kNoTransition = std::numeric_limits<TTransitionId>::max();
    static constexpr TKey kNoKey = std::numeric_limits<TKey>::max();
    static constexpr TValue kNoValue = std::numeric_limits<TValue>::max();
    static constexpr TNodeId kNoChild = std::numeric_limits<TNodeId>::max();
    static constexpr TWeight kNoWeight = 0.0;

    TNode():
        transitionId(kNoTransition),
        keyIndex(kNoKey),
        valueIndex(kNoValue)
    {}

    TTransitionId transitionId;
    TKey keyIndex;
    TValue valueIndex;
    std::map<TRuneId, TNodeId> children;
    std::set<std::pair<TWeight, TValue>> suggestions;

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

    void AddSuggestion(TValue value, TWeight weight) {
        suggestions.emplace(weight, value);
        while (suggestions.size() > kMaxSuggestions) {
            suggestions.erase(suggestions.begin());
        }
    }

    void RemoveSuggestion(TValue value, TWeight weight) {
        suggestions.erase(std::make_pair(weight, value));
    }

    void SetTransitionAndKeyValue(TTransitionId transition, TKey key, TValue value) {
        transitionId = transition;
        keyIndex = key;
        valueIndex = value;
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

    void AddPath(const TRunes& path, TNode::TWeight weight, TNode::TTransitionId transitionId, TNode::TKey keyIndex);
    uint16_t GetTransitionId(const std::string& transition);
    TNode::TKey AddKeyData(const std::string& key, uint8_t keyException, Poco::JSON::Object&& metadata);
    TNode::TValue AddValueData(const TRunes& value, TNode::TWeight weight);
    const TNode* Traverse(const TRunes& path) const;
    const TNode* Traverse(const std::string& path) const;
    std::string GetKey(uint16_t index) const;
    void PrintStats(Poco::Logger& logger) const;
    void PrintTrie(const std::string& filename) const;

    void BuildSuggestions();
private:
    TRuneId GetRuneId(TRuneValue rune);
    TRuneId GetRuneIdConst(TRuneValue rune) const;
    TNode::TNodeId CreateNode();

    void BuildSuggestionsRec(TNode* node);

    void PrintRunes(std::ofstream& out) const;
    void PrintTransitions(std::ofstream& out) const;
    void PrintKeys(std::ofstream& out) const;
    void PrintNodes(std::ofstream& out) const;

    std::string BuildValue(const TNode::TValue value) const;
private:
    std::vector<TRuneValue> runeValues_;
    std::map<TRuneValue, TRuneId> runeIds_;
    std::vector<std::string> transitions_;
    std::map<std::string, uint16_t> transitionIds_;
    std::vector<Poco::JSON::Object> keyMeta_;
    std::vector<TRunes> keyRunesVec_;
    std::unordered_map<std::string, uint16_t> keyIndices_;
    std::vector<TRunes> valueRunesVec_;
    std::vector<TNode::TWeight> valueWeights_;
    std::vector<TNode*> nodes_;
    uint32_t pathCount_;
    uint32_t textLength_;
    uint32_t nodeCount_;
};

TrieBuilder BuildTrie(Poco::Logger* logger);
TrieBuilder BuildDetectSuggestTrie(const std::string& filepath, Poco::Logger* logger);

}  // namespace NKiltMan