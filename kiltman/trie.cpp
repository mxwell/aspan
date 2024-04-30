#include "trie.h"

#include "Poco/JSON/Parser.h"

#include <cassert>

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

uint16_t TrieBuilder::AddKeyData(const std::string& key, uint8_t keyException, Poco::JSON::Object&& metadata) {
    std::string mapKey = key;
    mapKey.push_back(keyException);
    auto it = keyIndices_.find(mapKey);
    assert (it == keyIndices_.end());

    TRunes runes;
    StringToRunes(key, runes);

    uint16_t index = (uint16_t) keyRunesVec_.size();
    keyMeta_.emplace_back(std::move(metadata));
    keyRunesVec_.push_back(runes);
    keyIndices_[mapKey] = index;
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
    auto n = keyMeta_.size();
    assert(n == keyRunesVec_.size());

    out << keyRunesVec_.size() << '\n';

    for (size_t i = 0; i < n; ++i) {
        keyMeta_[i].stringify(out, 0);
        out << '\n';
        const auto& runes = keyRunesVec_[i];
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
 * Concatenate parts starting from the selected position.
*/
std::string JoinTransition(const TWords& formParts) {
    std::string result = formParts[1];
    for (size_t i = 2; i < formParts.size(); ++i) {
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
        if (keyMetaParts.size() != 2) {
            throw std::runtime_error("Invalid key with meta: " + lineParts[0]);
        }
        auto keyException = std::stoi(keyMetaParts[1]);
        Poco::JSON::Object metadataRoot;
        if (keyException) {
            metadataRoot.set("exceptional", true);
        }
        uint16_t keyIndex = builder.AddKeyData(keyMetaParts[0], keyException, std::move(metadataRoot));
        for (size_t i = 1; i < lineParts.size(); ++i) {
            SplitBy(lineParts[i], ':', metaParts);
            if (metaParts.size() != 5) {
                throw std::runtime_error("Invalid form with meta: " + lineParts[i]);
            }
            StringToRunes(metaParts[0], runes);
            auto transitionId = builder.GetTransitionId(JoinTransition(metaParts));
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

std::string ExtractTransition(const Poco::JSON::Object::Ptr& formObject) {
    std::string result;
    result.append(formObject->getValue<std::string>("sent"));
    result.push_back(':');
    result.append(formObject->getValue<std::string>("tense"));
    result.push_back(':');
    result.append(formObject->getValue<std::string>("person"));
    result.push_back(':');
    result.append(formObject->getValue<std::string>("number"));
    return result;
}

TrieBuilder BuildDetectSuggestTrie(const std::string& filepath, Poco::Logger* logger) {
    using namespace Poco;

    TrieBuilder builder;

    std::ifstream file(filepath);

    std::string line;
    uint32_t lineCounter = 0;
    JSON::Parser parser;
    std::vector<uint16_t> runes;

    while (getline(file, line)) {
        ++lineCounter;
        if (lineCounter % 1000 == 0 && logger) {
            logger->information("Loaded %u lines", lineCounter);
        }
        Dynamic::Var result = parser.parse(line);
        JSON::Object::Ptr root = result.extract<JSON::Object::Ptr>();
        std::string key = root->getValue<std::string>("base");
        JSON::Object metadataRoot;
        int keyException = root->getValue<int>("exceptional");
        if (!(0 <= keyException && keyException <= 1)) {
            throw std::runtime_error("Invalid value of exceptional: " + std::to_string(keyException));
        }
        if (keyException) {
            metadataRoot.set("exceptional", true);
        }
        JSON::Array::Ptr ruwkt = root->getArray("ruwkt");
        if (ruwkt->size() > 0) {
            metadataRoot.set("ruwkt", ruwkt);
        }
        JSON::Array::Ptr enwkt = root->getArray("enwkt");
        if (enwkt->size() > 0) {
            metadataRoot.set("enwkt", enwkt);
        }
        uint16_t keyIndex = builder.AddKeyData(key, keyException, std::move(metadataRoot));

        JSON::Array::Ptr forms = root->getArray("forms");
        for (size_t i = 0; i < forms->size(); ++i) {
            JSON::Object::Ptr formObject = forms->getObject(i);

            std::string form = formObject->getValue<std::string>("form");
            StringToRunes(form, runes);

            auto transitionStr = ExtractTransition(formObject);
            auto transitionId = builder.GetTransitionId(transitionStr);
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