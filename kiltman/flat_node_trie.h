#pragma once

#include "flat_node.h"
#include "runes.h"

#include "Poco/JSON/Object.h"
#include "Poco/Logger.h"

#include <cstdint>
#include <map>
#include <string>
#include <vector>

namespace NKiltMan {

struct TTerminalItem {
    FlatNode::TKey keyIndex;
    FlatNode::TTransitionId transitionId;
};

struct TKeyItem {
    TRunes runes;
    Poco::JSON::Object metadata;

    size_t GetSpace() const {
        // TODO count metadata size
        return sizeof(runes[0]) * runes.capacity();
    }
};

struct TValueItem {
    TRunes runes;
    uint16_t keyIndex;

    size_t GetSpace() const {
        return sizeof(runes[0]) * runes.capacity();
    }
};

struct TSuggestionResult {
    std::string completion;
};

using TSuggestionResults = std::vector<TSuggestionResult>;

struct FlatNodeTrie {
    using TRuneId = FlatNode::TRuneId;
    using TRuneMap = std::map<TRuneValue, TRuneId>;
    using TTransitions = std::vector<std::string>;
    using TTerminal = std::vector<TTerminalItem>;
    using TTerminals = std::vector<TTerminal>;
    using TKeys = std::vector<TKeyItem>;
    using TValues = std::vector<TValueItem>;

    TRunes runes;
    TRuneMap runeMap;

    TTransitions transitions;

    TTerminals terminals;
    TKeys keys;
    TValues values;
    std::vector<FlatNode::TRuneNodeCombo> childData;
    std::vector<FlatNode> nodes;
    std::vector<FlatNode::TValueId> suggestions;

    const FlatNode* Traverse(const TRunes& path) const;

    TSuggestionResults GetSuggestions(const FlatNode* node) const;

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

    size_t GetTerminalsSpace() const {
        size_t result = sizeof(terminals) + sizeof(terminals[0]) * terminals.capacity();
        for (const auto& terminal : terminals) {
            result += terminal.capacity() * sizeof(terminal[0]);
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

    size_t GetValuesSpace() const {
        size_t result = sizeof(values) + sizeof(values[0]) * values.capacity();
        for (const auto& value : values) {
            result += value.GetSpace();
        }
        return result;
    }

    size_t GetChildDataSpace() const {
        return sizeof(childData) + sizeof(childData[0]) * childData.capacity();
    }

    size_t GetNodesSpace() const {
        return sizeof(nodes) + sizeof(nodes[0]) * nodes.capacity();
    }

    size_t GetSuggestionsSpace() const {
        return sizeof(suggestions) + sizeof(suggestions[0]) * suggestions.capacity();
    }
};

FlatNodeTrie LoadTrie(const std::string& path, Poco::Logger* logger);

}  // namespace NKiltMan
