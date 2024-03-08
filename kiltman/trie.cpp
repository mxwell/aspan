#include "trie.h"

namespace NKiltMan {

using TWords = std::vector<std::string>;

void SplitBy(const std::string& s, char sep, TWords& words) {
    words.clear();
    size_t start = 0;
    while (start < s.size()) {
        while (start < s.size() && s.at(start) == sep) {
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

void StringToRunes(const std::string& s, TRunes& result) {
    result.clear();
    for (size_t i = 0; i < s.size(); ) {
        uint8_t ch0 = s.at(i);
        if (ch0 < 0x80) {
            result.push_back(ch0);
            ++i;
        } else {
            if (i + 1 > s.size()) {
                throw std::runtime_error("Invalid UTF-8");
            }
            uint8_t ch1 = s.at(i + 1);
            if (ch1 == 0) {
                throw std::runtime_error("Invalid zero byte");
            }
            result.push_back((ch1 << 8) | ch0);
            i += 2;
        }
    }
}

void RunesToString(const TRunes& runes, std::string& result) {
    result.clear();
    for (auto rune : runes) {
        uint8_t ch0 = rune & 0xFF;
        uint8_t ch1 = rune >> 8;
        if (ch1 == 0) {
            result.push_back(ch0);
        } else {
            result.push_back(ch0);
            result.push_back(ch1);
        }
    }
}

void TrieBuilder::AddPath(const TRunes& path, uint16_t keyIndex) {
    TNode* node = nodes_[0];
    for (auto ch : path) {
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
    node->SetKeyIndex(keyIndex);
}

uint16_t TrieBuilder::AddKeyRunes(const TRunes& runes) {
    uint16_t index = (uint16_t) keyRunesVec_.size();
    keyRunesVec_.push_back(runes);
    return index;
}

const TNode* TrieBuilder::Traverse(const TRunes& path) const {
    const TNode* node = nodes_[0];
    for (auto ch : path) {
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

TNode::TNodeId TrieBuilder::CreateNode() {
    TNode::TNodeId id = (TNode::TNodeId) nodes_.size();
    nodes_.emplace_back(new TNode());
    return id;
}

TrieBuilder BuildTrie(Poco::Logger* logger) {
    const std::string filepath = "forms.csv";

    TrieBuilder builder;
    std::ifstream file(filepath);
    std::string line;

    TWords lineParts;
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
        StringToRunes(lineParts[0], runes);
        uint16_t keyIndex = builder.AddKeyRunes(runes);
        for (size_t i = 1; i < lineParts.size(); ++i) {
            StringToRunes(lineParts[i], runes);
            builder.AddPath(runes, keyIndex);
        }
    }
    if (logger) {
        builder.PrintStats(*logger);
    }
    return std::move(builder);
}

}  // namespace NKiltMan