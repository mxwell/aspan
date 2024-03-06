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

uint64_t GetNodeChildrenCount(const TNode* node) {
    uint64_t result = node->children.size();
    for (const auto& [ch, child] : node->children) {
        result += GetNodeChildrenCount(child);
    }
    return result;
}

uint64_t GetNodeSpace(const TNode* node) {
    uint64_t result = node->GetSpace();
    for (const auto& [ch, child] : node->children) {
        result += GetNodeSpace(child);
    }
    return result;
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
    TNode* node = &root_;
    for (auto ch : path) {
        TNode* child = node->FindChild(ch);
        if (child == nullptr) {
            child = node->AddChild(ch);
            ++nodeCount_;
        }
        node = child;
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
    const TNode* node = &root_;
    for (auto ch : path) {
        TNode* child = node->FindChild(ch);
        if (child == nullptr) {
            return nullptr;
        }
        node = child;
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

void TrieBuilder::PrintStats() const {
    std::cerr << "Key count: " << keyRunesVec_.size() << "\n";
    std::cerr << "Path count: " << pathCount_ << "\n";
    std::cerr << "Text length: " << textLength_ << "\n";
    std::cerr << "Node count: " << nodeCount_ << "\n";
    auto totalChildren = GetNodeChildrenCount(&root_);
    std::cerr << "Total children: " << totalChildren << "\n";
    std::cerr << "Average children per node: " << (double) totalChildren / nodeCount_ << "\n";
    std::cerr << "Root children: " << root_.children.size() << "\n";
    std::cerr << "Tree space: " << GetNodeSpace(&root_) << "\n";
}


TrieBuilder BuildTrie() {
    const std::string filepath = "forms.csv";

    TrieBuilder builder;
    std::ifstream file(filepath);
    std::string line;

    TWords lineParts;
    std::vector<uint16_t> runes;
    std::cerr << "Reading " << filepath << "...\n";
    uint32_t lineCounter = 0;
    while (getline(file, line)) {
        ++lineCounter;
        if (lineCounter % 1000 == 0) {
            std::cerr << "Loaded " << lineCounter << " lines\n";
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
    builder.PrintStats();
    return std::move(builder);
}

}  // namespace NKiltMan