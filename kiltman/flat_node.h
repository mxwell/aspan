#pragma once

#include <cstdint>
#include <limits>
#include <utility>
#include <vector>

namespace NKiltMan {

struct FlatNode {
    using TKey = uint16_t;
    using TRuneId = uint8_t;
    using TNodeId = uint32_t;
    using TChildren = std::vector<std::pair<TRuneId, TNodeId>>;

    static constexpr TKey kNoKey = std::numeric_limits<TKey>::max();
    static constexpr TNodeId kNoChild = std::numeric_limits<TNodeId>::max();

    TKey keyIndex;
    TChildren children;

    TNodeId FindChild(TRuneId ch) const {
        uint32_t n = children.size();
        if (n <= 4) {
            for (const auto& [runeId, childId] : children) {
                if (runeId == ch) {
                    return childId;
                }
            }
        } else {
            uint32_t lf = 0;
            uint32_t rg = n - 1;
            while (lf + 1 < rg) {
                uint32_t mid = (lf + rg) / 2;
                if (children[mid].first < ch) {
                    lf = mid;
                } else {
                    rg = mid;
                }
            }
            for (uint32_t i = lf; i <= rg; ++i) {
                if (children[i].first == ch) {
                    return children[i].second;
                }
            }
        }
        return kNoChild;
    }

    bool IsTerminal() const {
        return keyIndex != kNoKey;
    }

    uint32_t GetSpace() const {
        return sizeof(children[0]) * children.capacity();
    }
};

}  // namespace NKiltMan
