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

void TrieBuilder::AddPath(const TRunes& path, TNode::TWeight weight, TTransitionId transitionId, TKey keyIndex) {
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
    auto valueIndex = AddValueData(path, weight, keyIndex);

    auto terminalId = node->terminalId;
    if (terminalId == kNoTerminal) {
        terminalId = GetTerminalId();
        node->SetTerminalId(terminalId);
    }
    AddTerminalEntry(terminalId, transitionId, keyIndex);
    node->SetValue(valueIndex);
}

TKey TrieBuilder::AddKeyData(const std::string& key, char partOfSpeech, Poco::JSON::Object&& metadata) {
    std::string mapKey = key;
    mapKey.push_back(partOfSpeech);
    auto it = keyIndices_.find(mapKey);
    assert (it == keyIndices_.end());

    TRunes runes;
    StringToRunes(key, runes);
    // explicitly populate the rune mapping as key charset might be different from path charset
    for (const auto& rune : runes) {
        GetRuneId(rune);
    }

    auto index = static_cast<TKey>(keyRunesVec_.size());
    keyMeta_.emplace_back(std::move(metadata));
    keyRunesVec_.push_back(runes);
    keyIndices_[mapKey] = index;
    return index;
}

TNode::TValue TrieBuilder::AddValueData(const TRunes& value, TNode::TWeight weight, TKey keyIndex) {
    auto index = static_cast<TNode::TValue>(valueRunesVec_.size());
    valueRunesVec_.push_back(value);
    valueWeights_.push_back(weight);
    valueKeyIndices_.push_back(keyIndex);
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
    logger.information("Terminals count: %z", terminals_.size());
    logger.information("Text length: %u", textLength_);
    logger.information("Node count: %u", nodeCount_);

    const TNode* root_ = nodes_[0];
    logger.information("Root children: %z", root_->children.size());

    uint64_t totalNodeSpace = 0;
    size_t suggMin = kMaxSuggestions + 1;
    size_t suggMax = 0;
    size_t suggSum = 0;
    for (const auto& node : nodes_) {
        totalNodeSpace += node->GetSpace();
        size_t suggCount = node->suggestions.size();
        suggMin = std::min(suggMin, suggCount);
        suggMax = std::max(suggMax, suggCount);
        suggSum += suggCount;
    }
    logger.information("Tree space: %Lu", totalNodeSpace);
    if (suggMin <= kMaxSuggestions) {
        logger.information("Suggestions: min %z, max %z, total %z, avg %.1f",
            suggMin, suggMax, suggSum, (double) suggSum / nodes_.size());
    } else {
        logger.information("No info on suggestions");
    }
}

void TrieBuilder::PrintTrie(const std::string& filename) const {
    std::ofstream out(filename);
    PrintRunes(out);
    PrintTransitions(out);
    PrintTerminals(out);
    PrintKeys(out);
    PrintValues(out);
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

TTerminalId TrieBuilder::GetTerminalId() {
    auto newId = terminals_.size();
    assert (newId < kNoTerminal);
    auto id = (TTerminalId) newId;
    terminals_.push_back(TTerminalEntries{});
    return id;
}

void TrieBuilder::AddTerminalEntry(TTerminalId terminalId, TTransitionId transitionId, TKey keyIndex) {
    terminals_[terminalId].push_back(TTerminalEntry{
        .keyIndex = keyIndex,
        .transitionId = transitionId,
    });
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

    out << n << '\n';

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

void TrieBuilder::PrintValues(std::ofstream& out) const {
    auto n = valueRunesVec_.size();
    assert(n == valueWeights_.size());
    assert(n == valueKeyIndices_.size());

    out << n << '\n';

    for (size_t i = 0; i < n; ++i) {
        out << valueKeyIndices_[i] << '\n';
        const auto& runes = valueRunesVec_[i];
        out << runes.size();
        for (TRuneValue runeValue : runes) {
            TRuneId runeId = GetRuneIdConst(runeValue);
            out << ' ' << static_cast<uint32_t>(runeId);
        }
        out << '\n';
    }
}

void TrieBuilder::PrintTerminals(std::ofstream& out) const {
    out << terminals_.size() << '\n';
    for (const auto& terminal : terminals_) {
        out << terminal.size();
        for (const auto& [key, transition] : terminal) {
            out << ' ' << key << ' ' << static_cast<int>(transition);
        }
        out << '\n';
    }
}

void TrieBuilder::PrintNodes(std::ofstream& out) const {
    out << nodes_.size() << '\n';
    for (const auto& node : nodes_) {
        out << node->terminalId << ' ';
        out << node->children.size();
        for (const auto& [runeId, childId] : node->children) {
            out << ' ' << static_cast<uint32_t>(runeId) << ' ' << childId;
        }
        out << '\n';
        if (node->IsTerminal()) {
            node->RemoveSuggestion(node->valueIndex, valueWeights_[node->valueIndex]);
            // Add a suggestion for the value itself with infinite weight.
            node->AddSuggestion(node->valueIndex, 1e9);
        }
        out << node->suggestions.size();
        for (auto iter = node->suggestions.rbegin(); iter != node->suggestions.rend(); ++iter) {
            out << ' ' << iter->second;  // BuildValue(iter->second)
        }
        out << '\n';
    }
}

uint32_t TrieBuilder::GetTransitionsCount() const {
    return transitions_.size();
}

void TrieBuilder::DebugPrintTransitions(Poco::Logger& logger) const {
    logger.information("Transitions count: %z", transitions_.size());
    for (const auto& transition : transitions_) {
        logger.information("- %s", transition);
    }
}

std::string TrieBuilder::BuildValue(const TNode::TValue value) const {
    std::string result;
    RunesToString(valueRunesVec_[value], result);
    return result;
}

void TrieBuilder::BuildSuggestions() {
    BuildSuggestionsRec(nodes_[0]);
}

void TrieBuilder::BuildSuggestionsRec(TNode* node) {
    if (node->IsTerminal()) {
        auto value = node->valueIndex;
        auto weight = valueWeights_[value];
        node->AddSuggestion(value, weight);
    }

    for (const auto& childPair : node->children) {
        auto childId = childPair.second;
        TNode* child = nodes_[childId];
        BuildSuggestionsRec(child);
        for (const auto& suggestion : child->suggestions) {
            node->AddSuggestion(suggestion.second, suggestion.first);
        }
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

// order: more common first
constexpr char kNoun                = 'n';
constexpr char kVerbRegular         = 'v';
constexpr char kVerbExceptional     = 'w';
constexpr char kAdjective           = 'j';
constexpr char kAdverb              = 'b';
constexpr char kPronoun             = 'p';
constexpr char kInterjection        = 'i';
constexpr char kOnomatopoeia        = 'o';
constexpr char kNumeral             = 'l';
constexpr char kConjunction         = 'c';
constexpr char kPreposition         = 'e';
constexpr char kParticle            = 't';
constexpr char kPostposition        = 's';


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
        const char partOfSpeech = keyException ? kVerbExceptional : kVerbRegular;
        uint16_t keyIndex = builder.AddKeyData(keyMetaParts[0], partOfSpeech, std::move(metadataRoot));
        for (size_t i = 1; i < lineParts.size(); ++i) {
            SplitBy(lineParts[i], ':', metaParts);
            if (metaParts.size() != 5) {
                throw std::runtime_error("Invalid form with meta: " + lineParts[i]);
            }
            StringToRunes(metaParts[0], runes);
            auto transitionId = builder.GetTransitionId(JoinTransition(metaParts));
            if (transitionId >= kNoTransition) {
                throw std::runtime_error("Too many transitions");
            }
            builder.AddPath(runes, TNode::kNoWeight, transitionId, keyIndex);

        }
    }
    if (logger) {
        builder.PrintStats(*logger);
    }
    return std::move(builder);
}

std::string ExtractOptionalString(const Poco::JSON::Object::Ptr& formObject, const std::string& key) {
    if (formObject->isNull(key)) {
        return {};
    }
    return formObject->getValue<std::string>(key);
}

std::string ExtractTransition(const Poco::JSON::Object::Ptr& formObject) {
    std::string result;
    result.append(ExtractOptionalString(formObject, "sent"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "tense"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "person"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "number"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "septik"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "possPerson"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "possNumber"));
    result.push_back(':');
    result.append(ExtractOptionalString(formObject, "wordgen"));
    return result;
}

static char ExtractPartOfSpeech(const Poco::JSON::Object::Ptr& root) {
    const auto partOfSpeech = root->getValue<std::string>("pos");
    if (partOfSpeech == "noun") {
        return kNoun;
    } else if (partOfSpeech == "verb") {
        int keyException = root->getValue<int>("exceptional");
        if (keyException == 0) {
            return kVerbRegular;
        } else if (keyException == 1) {
            return kVerbExceptional;
        } else {
            throw std::runtime_error("Invalid value of exceptional: " + std::to_string(keyException));
        }
    } else if (partOfSpeech == "adjective") {
        return kAdjective;
    } else if (partOfSpeech == "adverb") {
        return kAdverb;
    } else if (partOfSpeech == "pronoun") {
        return kPronoun;
    } else if (partOfSpeech == "interjection") {
        return kInterjection;
    } else if (partOfSpeech == "onomatopoeia") {
        return kOnomatopoeia;
    } else if (partOfSpeech == "numeral") {
        return kNumeral;
    } else if (partOfSpeech == "conjunction") {
        return kConjunction;
    } else if (partOfSpeech == "preposition") {
        return kPreposition;
    } else if (partOfSpeech == "particle") {
        return kParticle;
    } else if (partOfSpeech == "postposition") {
        return kPostposition;
    } else { // add more parts of speech above, if needed
        throw std::runtime_error("Unsupported part of speech: " + partOfSpeech);
    }
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
        const auto partOfSpeech = ExtractPartOfSpeech(root);
        metadataRoot.set("pos", partOfSpeech);
        if (!root->isNull("ruwkt")) {
            JSON::Array::Ptr ruwkt = root->getArray("ruwkt");
            if (ruwkt->size() > 0) {
                metadataRoot.set("ruwkt", ruwkt);
            }
        }
        if (!root->isNull("rutr")) {
            JSON::Array::Ptr rutr = root->getArray("rutr");
            if (rutr->size() > 0) {
                metadataRoot.set("rutr", rutr);
            }
        }
        if (!root->isNull("enwkt")) {
            JSON::Array::Ptr enwkt = root->getArray("enwkt");
            if (enwkt->size() > 0) {
                metadataRoot.set("enwkt", enwkt);
            }
        }
        uint16_t keyIndex = builder.AddKeyData(key, partOfSpeech, std::move(metadataRoot));

        JSON::Array::Ptr forms = root->getArray("forms");
        for (size_t i = 0; i < forms->size(); ++i) {
            JSON::Object::Ptr formObject = forms->getObject(i);

            std::string form = formObject->getValue<std::string>("form");
            StringToRunes(form, runes);

            auto weight = formObject->getValue<TNode::TWeight>("weight");
            auto transitionStr = ExtractTransition(formObject);
            auto transitionId = builder.GetTransitionId(transitionStr);
            if (transitionId >= kNoTransition) {
                if (logger) {
                    builder.DebugPrintTransitions(*logger);
                }
                throw std::runtime_error("Too many transitions");
            }
            builder.AddPath(runes, weight, transitionId, keyIndex);
        }
    }
    if (builder.GetTransitionsCount() > kNoTransition) {
        if (logger) {
            builder.DebugPrintTransitions(*logger);
        }
        throw std::runtime_error("Too many transitions");
    }
    builder.BuildSuggestions();
    if (logger) {
        builder.PrintStats(*logger);
    }
    return std::move(builder);
}

}  // namespace NKiltMan