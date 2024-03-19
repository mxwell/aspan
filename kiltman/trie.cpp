#include "trie.h"

namespace NKiltMan {

using TWords = std::vector<std::string>;

void SplitBy(const std::string& s, char sep, TWords& words) {
    words.clear();
    size_t start = 0;
    while (start < s.size()) {
        if (start < s.size() && s.at(start) == sep) {
            ++start;
        }
        size_t pos = start;
        while (pos < s.size() && s.at(pos) != sep) {
            ++pos;
        }
        words.push_back(s.substr(start, pos - start));
        start = pos;
    }
}

void TrieBuilder::AddPath(const TRunes& path, TNode::TTransitionId transitionId, TNode::TKey keyIndex) {
    TNode* node = nodes_[0];
    for (TRuneValue runeValue : path) {
        TRuneId ch = GetRuneId(runeValue);
        auto childId = node->FindChild(ch);
        if (childId == TNode::kNoChild) {
            childId = CreateNode();
            node->AddChild(ch, childId);
            ++nodeCount_;
        }
        node = nodes_[childId];
    }
    ++pathCount_;
    textLength_ += path.size();
    node->SetTransitionAndKey(transitionId, keyIndex);
}

uint16_t TrieBuilder::AddKeyRunes(const TRunes& runes) {
    uint16_t index = (uint16_t) keyRunesVec_.size();
    keyRunesVec_.push_back(runes);
    return index;
}

const TNode* TrieBuilder::Traverse(const TRunes& path) const {
    const TNode* node = nodes_[0];
    for (TRuneValue runeValue : path) {
        TRuneId ch = GetRuneIdConst(runeValue);
        if (ch == kNoRuneId) {
            return nullptr;
        }
        auto childId = node->FindChild(ch);
        if (childId == TNode::kNoChild) {
            return nullptr;
        }
        node = nodes_[childId];
    }
    return node;
}

const TNode* TrieBuilder::Traverse(const std::string& path) const {
    TRunes runes;
    StringToRunes(path, runes);
    return Traverse(runes);
}

std::string TrieBuilder::GetKey(uint16_t index) const {
    if (index >= keyRunesVec_.size()) {
        throw std::runtime_error("Invalid key index");
    }
    std::string result;
    RunesToString(keyRunesVec_[index], result);
    return result;
}

void TrieBuilder::PrintStats(Poco::Logger& logger) const {
    logger.information("Rune count: %z", runeValues_.size());
    logger.information("Transition count: %z", transitions_.size());
    logger.information("Key count: %z", keyRunesVec_.size());
    logger.information("Path count: %u", pathCount_);
    logger.information("Text length: %u", textLength_);
    logger.information("Node count: %u", nodeCount_);

    const TNode* root_ = nodes_[0];
    logger.information("Root children: %z", root_->children.size());

    uint64_t totalNodeSpace = 0;
    for (const auto& node : nodes_) {
        totalNodeSpace += node->GetSpace();
    }
    logger.information("Tree space: %Lu", totalNodeSpace);
}

void TrieBuilder::PrintTrie(const std::string& filename) const {
    std::ofstream out(filename);
    PrintRunes(out);
    PrintTransitions(out);
    PrintKeys(out);
    PrintNodes(out);
    out.close();
}

TRuneId TrieBuilder::GetRuneId(TRuneValue rune) {
    auto it = runeIds_.find(rune);
    if (it != runeIds_.end()) {
        return it->second;
    }
    TRuneId id = (TRuneId) runeValues_.size();
    runeValues_.push_back(rune);
    runeIds_[rune] = id;
    return id;
}

TRuneId TrieBuilder::GetRuneIdConst(TRuneValue rune) const {
    auto it = runeIds_.find(rune);
    if (it == runeIds_.end()) {
        return kNoRuneId;
    }
    return it->second;
}

uint16_t TrieBuilder::GetTransitionId(const std::string& transition) {
    auto it = transitionIds_.find(transition);
    if (it != transitionIds_.end()) {
        return it->second;
    }
    uint16_t id = (uint16_t) transitions_.size();
    transitions_.push_back(transition);
    transitionIds_[transition] = id;
    return id;
}

TNode::TNodeId TrieBuilder::CreateNode() {
    TNode::TNodeId id = (TNode::TNodeId) nodes_.size();
    nodes_.emplace_back(new TNode());
    return id;
}

void TrieBuilder::PrintRunes(std::ofstream& out) const {
    out << runeValues_.size() << '\n';
    for (auto rune : runeValues_) {
        out << static_cast<uint32_t>(rune) << '\n';
    }
}

void TrieBuilder::PrintTransitions(std::ofstream& out) const {
    out << transitions_.size() << '\n';
    for (const auto& transition : transitions_) {
        out << transition << '\n';
    }
}

void TrieBuilder::PrintKeys(std::ofstream& out) const {
    out << keyRunesVec_.size() << '\n';
    for (const auto& runes : keyRunesVec_) {
        out << runes.size();
        for (TRuneValue runeValue : runes) {
            TRuneId runeId = GetRuneIdConst(runeValue);
            out << ' ' << static_cast<uint32_t>(runeId);
        }
        out << '\n';
    }
}

void TrieBuilder::PrintNodes(std::ofstream& out) const {
    out << nodes_.size() << '\n';
    for (const auto& node : nodes_) {
        out << static_cast<int>(node->transitionId) << ' ';
        out << static_cast<int>(node->keyIndex) << ' ';
        out << node->children.size();
        for (const auto& [runeId, nodeId] : node->children) {
            out << ' ' << static_cast<uint32_t>(runeId) << ' ' << nodeId;
        }
        out << '\n';
    }
}

/**
 * Concatenate parts starting from the second element of each vector.
*/
std::string JoinTransition(const TWords& keyParts, const TWords& formParts) {
    std::string result = keyParts[1];
    for (size_t i = 2; i < keyParts.size(); ++i) {
        result.push_back(':');
        result.append(keyParts[i]);
    }
    for (size_t i = 1; i < formParts.size(); ++i) {
        result.push_back(':');
        result.append(formParts[i]);
    }
    return result;
}

TrieBuilder BuildTrie(Poco::Logger* logger) {
    const std::string filepath = "forms.csv";

    TrieBuilder builder;
    std::ifstream file(filepath);
    std::string line;

    TWords lineParts;
    TWords keyMetaParts;
    TWords metaParts;
    std::vector<uint16_t> runes;
    if (logger) {
        logger->information("Reading forms from %s...", filepath);
    }
    uint32_t lineCounter = 0;
    while (getline(file, line)) {
        ++lineCounter;
        if (lineCounter % 1000 == 0 && logger) {
            logger->information("Loaded %u lines", lineCounter);
        }
        SplitBy(line, '\t', lineParts);
        if (lineParts.size() < 2) {
            throw std::runtime_error("Invalid line: " + line);
        }
        SplitBy(lineParts[0], ':', keyMetaParts);
        if (keyMetaParts.size() != 3) {
            throw std::runtime_error("Invalid key with meta: " + lineParts[0]);
        }
        StringToRunes(keyMetaParts[0], runes);
        uint16_t keyIndex = builder.AddKeyRunes(runes);
        for (size_t i = 1; i < lineParts.size(); ++i) {
            SplitBy(lineParts[i], ':', metaParts);
            if (metaParts.size() != 4) {
                throw std::runtime_error("Invalid form with meta: " + lineParts[i]);
            }
            StringToRunes(metaParts[0], runes);
            auto transitionId = builder.GetTransitionId(JoinTransition(keyMetaParts, metaParts));
            if (transitionId >= TNode::kNoTransition) {
                throw std::runtime_error("Too many transitions");
            }
            builder.AddPath(runes, transitionId, keyIndex);

        }
    }
    if (logger) {
        builder.PrintStats(*logger);
    }
    return std::move(builder);
}

}  // namespace NKiltMan